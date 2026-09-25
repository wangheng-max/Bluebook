package com.wangheng.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.exception.MerchantAuthException;
import com.wangheng.mapper.ProductCategoryMapper;
import com.wangheng.mapper.ProductMapper;
import com.wangheng.mapper.ProductSkuMapper;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Product;
import com.wangheng.pojo.ProductSaveDTO;
import com.wangheng.pojo.ProductSku;
import com.wangheng.pojo.ProductSkuDTO;
import com.wangheng.service.ProductService;
import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现。
 * 缓存策略（设计文档《缓存策略设计》）：详情/列表 5 分钟、搜索 10 分钟、分类树 1 天；
 * 写操作先改库再删缓存；列表与搜索只缓存第 1 页，其余页直接回源。
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_KEY_PREFIX = "product:";
    private static final String PRODUCT_LIST_KEY_PREFIX = "product:list:";
    private static final String PRODUCT_SEARCH_KEY_PREFIX = "product:search:";
    private static final long PRODUCT_TTL_MINUTES = 5;
    private static final long SEARCH_TTL_MINUTES = 10;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private ProductCategoryMapper productCategoryMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageBean<Product> pageList(Integer pageNum, Integer pageSize, Integer categoryId, Integer status) {
        // 公开列表：缺省只看上架
        int st = status == null ? 1 : status;
        String cacheKey = PRODUCT_LIST_KEY_PREFIX + (categoryId == null ? "all" : categoryId) + ":" + st;

        if (pageNum == 1) {
            PageBean<Product> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Product> list = productMapper.list(categoryId, st);
        Page<Product> page = (Page<Product>) list;
        PageBean<Product> bean = new PageBean<>(page.getTotal(), page.getResult());

        if (pageNum == 1) {
            writeCache(cacheKey, bean, PRODUCT_TTL_MINUTES);
        }
        return bean;
    }

    @Override
    public PageBean<Product> search(String keyword, Integer categoryId,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    Integer pageNum, Integer pageSize) {
        String cacheKey = PRODUCT_SEARCH_KEY_PREFIX + (keyword == null ? "" : keyword) + ":"
                + (categoryId == null ? "all" : categoryId) + ":"
                + (minPrice == null ? "" : minPrice) + ":" + (maxPrice == null ? "" : maxPrice);

        if (pageNum == 1) {
            PageBean<Product> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Product> list = productMapper.search(keyword, categoryId, minPrice, maxPrice);
        Page<Product> page = (Page<Product>) list;
        PageBean<Product> bean = new PageBean<>(page.getTotal(), page.getResult());

        if (pageNum == 1) {
            writeCache(cacheKey, bean, SEARCH_TTL_MINUTES);
        }
        return bean;
    }

    @Override
    public Product detail(Integer id) {
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        // 下架商品仅创建者可预览；上架商品走缓存
        if (product.getStatus() != null && product.getStatus() == 1) {
            String cacheKey = PRODUCT_KEY_PREFIX + id;
            try {
                String json = stringRedisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    return objectMapper.readValue(json, Product.class);
                }
            } catch (Exception e) {
                // 缓存不可用回源
            }
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(product),
                        PRODUCT_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                // 缓存写入失败不影响业务
            }
            return product;
        }
        // 下架：仅创建者可见
        if (!product.getCreateUserId().equals(currentUserIdOrNull())) {
            throw new RuntimeException("商品已下架");
        }
        return product;
    }

    @Override
    public List<ProductSku> listSku(Integer productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        // 下架商品的规格仅创建者可查看（与 detail 可见性保持一致）
        if (product.getStatus() == null || product.getStatus() != 1) {
            if (!product.getCreateUserId().equals(currentUserIdOrNull())) {
                throw new RuntimeException("商品已下架");
            }
        }
        return productSkuMapper.findByProductId(productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(ProductSaveDTO dto) {
        Integer userId = currentUserId();
        if (productCategoryMapper.findById(dto.getCategoryId()) == null) {
            throw new RuntimeException("商品分类不存在");
        }
        Product product = buildProduct(dto);
        product.setCreateUserId(userId);
        product.setSalesCount(0);
        product.setViewCount(0);
        productMapper.insert(product);
        insertSkus(product.getId(), dto.getSkuList());
        evictListAndSearchCaches();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ProductSaveDTO dto) {
        Integer userId = currentUserId();
        if (dto.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        Product exist = productMapper.findById(dto.getId());
        if (exist == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!exist.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("无权操作他人商品");
        }
        if (productCategoryMapper.findById(dto.getCategoryId()) == null) {
            throw new RuntimeException("商品分类不存在");
        }
        Product product = buildProduct(dto);
        product.setCreateUserId(userId);
        productMapper.update(product);

        // SKU 整体替换（订单明细已存快照，历史订单不受影响）
        productSkuMapper.deleteByProductId(dto.getId());
        insertSkus(dto.getId(), dto.getSkuList());

        evictListAndSearchCaches();
        deleteProductCache(dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Integer id) {
        Integer userId = currentUserId();
        Product exist = productMapper.findById(id);
        if (exist == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!exist.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("无权操作他人商品");
        }
        productMapper.offShelf(id, userId);
        evictListAndSearchCaches();
        deleteProductCache(id);
    }

    @Override
    public PageBean<Product> myProducts(Integer pageNum, Integer pageSize, Integer status) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<Product> list = productMapper.listOwn(userId, status);
        Page<Product> page = (Page<Product>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    /** DTO → 实体（images 列表转 JSON 字符串，stock/status/marketPrice 补默认值） */
    private Product buildProduct(ProductSaveDTO dto) {
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setCategoryId(dto.getCategoryId());
        product.setCoverImg(dto.getCoverImg());
        product.setImages(toJson(dto.getImages()));
        product.setVideoUrl(dto.getVideoUrl());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setMarketPrice(dto.getMarketPrice() == null ? BigDecimal.ZERO : dto.getMarketPrice());
        product.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        product.setStock(dto.getStock() == null ? 0 : dto.getStock());
        return product;
    }

    private void insertSkus(Integer productId, List<ProductSkuDTO> skuList) {
        if (skuList == null || skuList.isEmpty()) {
            return;
        }
        for (ProductSkuDTO dto : skuList) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSkuName(dto.getSkuName());
            sku.setSpecValues(toJson(dto.getSpecValues()));
            sku.setPrice(dto.getPrice());
            sku.setStock(dto.getStock() == null ? 0 : dto.getStock());
            sku.setSalesCount(0);
            productSkuMapper.insert(sku);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("数据序列化失败");
        }
    }

    /** 失效列表/搜索缓存（写操作后调用） */
    private void evictListAndSearchCaches() {
        try {
            deleteKeys(PRODUCT_LIST_KEY_PREFIX);
            deleteKeys(PRODUCT_SEARCH_KEY_PREFIX);
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }

    private void deleteProductCache(Integer id) {
        try {
            stringRedisTemplate.delete(PRODUCT_KEY_PREFIX + id);
        } catch (Exception e) {
            // 忽略
        }
    }

    /** 按前缀批量删除（当前规模 keys 匹配即可；量大时可改用 SCAN） */
    private void deleteKeys(String prefix) {
        var keys = stringRedisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private PageBean<Product> readCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<PageBean<Product>>() {});
            }
        } catch (Exception e) {
            // 数据损坏按未命中处理
        }
        return null;
    }

    private void writeCache(String key, PageBean<Product> bean, long ttlMinutes) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(bean), ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
        }
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = claims == null ? null : (Integer) claims.get("id");
        if (userId == null) {
            throw new MerchantAuthException("请先登录");
        }
        return userId;
    }

    /** 公开详情页使用：可能未登录，返回 null */
    private Integer currentUserIdOrNull() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }
}
