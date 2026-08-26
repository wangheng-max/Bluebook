package com.wangheng.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.mapper.CategoryMapper;
import com.wangheng.pojo.Category;
import com.wangheng.service.CategoryService;
import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryServiceImpl implements CategoryService {

    /** 分类缓存 Key：系统内置分类全量缓存，用户自建分类按用户区分（key 带 userId） */
    private static final String CATEGORY_LIST_KEY_PREFIX = "category:list:";
    private static final long CATEGORY_TTL_DAYS = 1;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void add(Category category) {
        //补充属性值
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());

        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        category.setCreateUser(userId);
        category.setIsSystem(0);
        categoryMapper.add(category);
        evictCache(userId);
    }

    @Override
    public List<Category> list() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        String key = CATEGORY_LIST_KEY_PREFIX + userId;

        // 先查缓存，命中直接返回
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<Category>>() {});
            }
        } catch (Exception e) {
            // Redis 不可用或数据损坏时回源数据库
        }

        // 未命中：查系统内置分类 + 用户自建分类，写回缓存
        List<Category> categories = categoryMapper.list(userId);
        try {
            stringRedisTemplate.opsForValue().set(
                    key, objectMapper.writeValueAsString(categories), CATEGORY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
        }
        return categories;
    }

    @Override
    public Category findById(Integer id) {
        Category c = categoryMapper.findById(id);
        return c;
    }

    @Override
    public boolean isSystemCategory(Integer id) {
        return categoryMapper.countSystemById(id) > 0;
    }

    @Override
    public boolean isValidForUser(Integer id) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        return categoryMapper.countValidForUser(id, userId) > 0;
    }

    @Override
    public void update(Category category) {
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
        evictCache(null);
    }

    @Override
    public void deleteById(Integer id) {
        categoryMapper.deleteById(id);
        evictCache(null);
    }

    /**
     * 失效缓存：已知 userId 时只删该用户缓存；未知时（管理端操作）清空全部分类缓存
     */
    private void evictCache(Integer userId) {
        try {
            if (userId != null) {
                stringRedisTemplate.delete(CATEGORY_LIST_KEY_PREFIX + userId);
            } else {
                // 当前规模 keys 匹配即可；量大时可改用 SCAN
                var keys = stringRedisTemplate.keys(CATEGORY_LIST_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    stringRedisTemplate.delete(keys);
                }
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }
}
