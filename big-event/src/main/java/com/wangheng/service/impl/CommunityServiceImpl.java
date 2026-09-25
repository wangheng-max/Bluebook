package com.wangheng.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.mapper.ArticleMapper;
import com.wangheng.pojo.Article;
import com.wangheng.pojo.NoteSearchVO;
import com.wangheng.pojo.PageBean;
import com.wangheng.service.CommunityService;
import com.wangheng.utils.NoteVOConverter;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CommunityServiceImpl implements CommunityService {

    /** 热点文章 ZSET：成员=文章ID，score=热度分 */
    private static final String HOT_KEY = "hot:articles";

    /** 冷启动兜底条数 */
    private static final int HOT_SEED_LIMIT = 100;

    @Autowired
    private ArticleMapper articleMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private NoteVOConverter noteVOConverter;

    @Override
    public PageBean<NoteSearchVO> hotArticles(Integer pageNum, Integer pageSize) {
        PageBean<NoteSearchVO> pb = new PageBean<>();

        // 从 ZSET 按 score 倒序取（page-1）*size 之后的 size 条文章 ID
        long start = (long) (pageNum - 1) * pageSize;
        Set<String> ids = stringRedisTemplate.opsForZSet()
                .reverseRange(HOT_KEY, start, start + pageSize - 1);

        // 冷启动：ZSET 为空时按浏览量查库兜底，并顺带初始化 ZSET
        if (ids == null || ids.isEmpty()) {
            return hotFromDb(pageNum, pageSize, pb);
        }

        // 批量查文章 → 组装 VO
        List<Integer> idList = ids.stream().map(Integer::valueOf).toList();
        List<Article> articles = articleMapper.findByIds(idList);
        List<NoteSearchVO> voList = articles.stream().map(noteVOConverter::convert).toList();

        Long total = stringRedisTemplate.opsForZSet().size(HOT_KEY);
        pb.setTotal(total == null ? 0 : total);
        pb.setItems(voList);
        return pb;
    }

    @Override
    public PageBean<NoteSearchVO> articlesByCategory(Integer categoryId, Integer pageNum, Integer pageSize) {
        PageBean<NoteSearchVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<Article> articles = articleMapper.listByCategory(categoryId);
        Page<Article> p = (Page<Article>) articles;

        List<NoteSearchVO> voList = new ArrayList<>();
        for (Article a : p.getResult()) {
            voList.add(noteVOConverter.convert(a));
        }

        pb.setTotal(p.getTotal());
        pb.setItems(voList);
        return pb;
    }

    @Override
    public NoteSearchVO articleDetail(Integer articleId) {
        Article article = articleMapper.findById(articleId);
        if (article == null || !"已发布".equals(article.getState())) {
            throw new RuntimeException("文章不存在或未发布");
        }
        // 浏览计数：Redis INCR，定时任务批量落库
        stringRedisTemplate.opsForValue().increment("article:view:" + articleId);
        NoteSearchVO vo = noteVOConverter.convert(article);
        vo.setContent(article.getContent());
        return vo;
    }

    /**
     * ZSET 冷启动兜底：按浏览量查库 TopN，并把热度分初始写回 ZSET
     */
    private PageBean<NoteSearchVO> hotFromDb(Integer pageNum, Integer pageSize, PageBean<NoteSearchVO> pb) {
        List<Article> hot = articleMapper.findHotTop(HOT_SEED_LIMIT);

        // 初始化 ZSET：score = view_count×1（存量数据暂无点赞/收藏/转发，按浏览量计）
        for (Article a : hot) {
            int view = a.getViewCount() == null ? 0 : a.getViewCount();
            stringRedisTemplate.opsForZSet().add(HOT_KEY, String.valueOf(a.getId()), view);
        }

        // 手动分页
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= hot.size()) {
            pb.setTotal((long) hot.size());
            pb.setItems(new ArrayList<>());
            return pb;
        }
        int toIndex = Math.min(fromIndex + pageSize, hot.size());
        List<Article> pageArticles = hot.subList(fromIndex, toIndex);

        List<NoteSearchVO> voList = pageArticles.stream().map(noteVOConverter::convert).toList();
        pb.setTotal((long) hot.size());
        pb.setItems(voList);
        return pb;
    }
}
