package com.wangheng.product.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.article.pojo.CategorySaveDTO;
import com.wangheng.exception.MerchantAuthException;
import com.wangheng.product.mapper.ProductCategoryMapper;
import com.wangheng.product.pojo.ProductCategory;
import com.wangheng.product.pojo.ProductCategoryVO;
import com.wangheng.product.service.ProductCategoryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 商品分类服务实现。
 * 分类树缓存 1 天，增删改时主动失效（先改库后删缓存，见设计文档缓存一致性）。
 */
@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private static final String CATEGORY_TREE_KEY = "product:category:list";
    private static final long CATEGORY_TTL_DAYS = 1;

    @Autowired
    private ProductCategoryMapper productCategoryMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ProductCategoryVO> tree() {
        // 先查缓存
        try {
            String json = stringRedisTemplate.opsForValue().get(CATEGORY_TREE_KEY);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<ProductCategoryVO>>() {});
            }
        } catch (Exception e) {
            // Redis 不可用或数据损坏时回源数据库
        }

        List<ProductCategoryVO> tree = buildTree(productCategoryMapper.findAll());

        try {
            stringRedisTemplate.opsForValue().set(
                    CATEGORY_TREE_KEY, objectMapper.writeValueAsString(tree), CATEGORY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
        }
        return tree;
    }

    @Override
    public List<ProductCategory> listFlat() {
        return productCategoryMapper.findAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CategorySaveDTO dto) {
        Integer parentId = dto.getParentId() == null ? 0 : dto.getParentId();
        if (parentId != 0 && productCategoryMapper.findById(parentId) == null) {
            throw new MerchantAuthException("父分类不存在");
        }
        ProductCategory category = new ProductCategory();
        category.setName(dto.getName());
        category.setParentId(parentId);
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setIsSystem(1);
        productCategoryMapper.insert(category);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new MerchantAuthException("分类ID不能为空");
        }
        ProductCategory exist = productCategoryMapper.findById(dto.getId());
        if (exist == null) {
            throw new MerchantAuthException("分类不存在");
        }
        Integer parentId = dto.getParentId() == null ? 0 : dto.getParentId();
        // 父分类不能是自己（防止环）
        if (parentId.equals(exist.getId())) {
            throw new MerchantAuthException("父分类不能是自身");
        }
        if (parentId != 0 && productCategoryMapper.findById(parentId) == null) {
            throw new MerchantAuthException("父分类不存在");
        }
        ProductCategory category = new ProductCategory();
        category.setId(exist.getId());
        category.setName(dto.getName());
        category.setParentId(parentId);
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        productCategoryMapper.update(category);
        evictCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        if (productCategoryMapper.findById(id) == null) {
            throw new MerchantAuthException("分类不存在");
        }
        if (productCategoryMapper.countChildren(id) > 0) {
            throw new MerchantAuthException("存在子分类，不能删除");
        }
        if (productCategoryMapper.countProducts(id) > 0) {
            throw new MerchantAuthException("分类下存在商品，不能删除");
        }
        productCategoryMapper.deleteById(id);
        evictCache();
    }

    /** 构建分类树：parentId=0 为顶级，按 sortOrder 排序 */
    private List<ProductCategoryVO> buildTree(List<ProductCategory> all) {
        Map<Integer, ProductCategoryVO> map = new HashMap<>();
        for (ProductCategory c : all) {
            ProductCategoryVO vo = new ProductCategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setParentId(c.getParentId());
            vo.setIcon(c.getIcon());
            vo.setSortOrder(c.getSortOrder());
            vo.setIsSystem(c.getIsSystem());
            vo.setChildren(new ArrayList<>());
            map.put(c.getId(), vo);
        }
        List<ProductCategoryVO> roots = new ArrayList<>();
        for (ProductCategory c : all) {
            ProductCategoryVO vo = map.get(c.getId());
            Integer parentId = c.getParentId() == null ? 0 : c.getParentId();
            if (parentId == 0 || !map.containsKey(parentId)) {
                roots.add(vo);
            } else {
                map.get(parentId).getChildren().add(vo);
            }
        }
        return roots;
    }

    /** 失效分类缓存（先改库后删缓存，失败靠 TTL 兜底） */
    private void evictCache() {
        try {
            stringRedisTemplate.delete(CATEGORY_TREE_KEY);
        } catch (Exception e) {
            // 缓存失效失败不影响主流程
        }
    }
}
