package com.wangheng.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.common.PageBean;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


/**
 * 公开笔记搜索结果的 Redis 缓存。
 * 公开搜索是热点数据，按「查询词 + 页码 + 页大小」缓存 5 分钟；
 * 文章写入（新增/更新/删除）后调用 {@link #evictNotes()} 整体失效。
 */
@Service
public class SearchCacheService {

    /** 缓存 key 前缀：search:notes:kw:{keyword}:{page}:{size} 或 search:notes:tag:{tag}:{page}:{size} */
    private static final String KEY_PREFIX = "search:notes:";
    private static final long TTL_MINUTES = 5;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public PageBean<NoteSearchVO> getNotesByKeyword(String keyword, Integer page, Integer size) {
        return get(buildKey("kw", keyword, page, size));
    }

    public void putNotesByKeyword(String keyword, Integer page, Integer size, PageBean<NoteSearchVO> pageBean) {
        put(buildKey("kw", keyword, page, size), pageBean);
    }

    public PageBean<NoteSearchVO> getNotesByTag(String tag, Integer page, Integer size) {
        return get(buildKey("tag", tag, page, size));
    }

    public void putNotesByTag(String tag, Integer page, Integer size, PageBean<NoteSearchVO> pageBean) {
        put(buildKey("tag", tag, page, size), pageBean);
    }

    /**
     * 清除所有公开笔记搜索缓存（文章新增/更新/删除后调用）
     */
    public void evictNotes() {
        try {
            // 当前规模下 keys 匹配即可；量大时可改用 SCAN 游标避免阻塞
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，缓存最长 5 分钟自然过期
        }
    }

    private String buildKey(String type, String query, Integer page, Integer size) {
        return KEY_PREFIX + type + ":" + query + ":" + page + ":" + size;
    }

    private PageBean<NoteSearchVO> get(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<PageBean<NoteSearchVO>>() {});
        } catch (Exception e) {
            // Redis 不可用或缓存数据损坏时回源数据库
            return null;
        }
    }

    private void put(String key, PageBean<NoteSearchVO> pageBean) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(pageBean), TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
        }
    }
}
