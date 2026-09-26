package com.wangheng.article.service.impl;

import com.wangheng.article.mapper.ArticleMapper;
import com.wangheng.article.pojo.Article;
import com.wangheng.article.service.ArticleService;
import com.wangheng.article.service.CategoryService;
import com.wangheng.common.PageBean;
import com.wangheng.search.service.SearchCacheService;













import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;






import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private SearchCacheService searchCacheService;

    @Autowired
    private CategoryService categoryService;

    @Override
    public void add(Article article) {
        //发布校验：分类必须为系统内置分类 或 当前用户自建分类
        if (article.getCategoryId() == null || !categoryService.isValidForUser(article.getCategoryId())) {
            throw new RuntimeException("分类不存在，请从分类列表中选择");
        }

        //补充属性值
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());

        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        article.setCreateUser(userId);

        articleMapper.add(article);

        // 文章写入后公开笔记搜索结果可能变化，清除搜索缓存
        searchCacheService.evictNotes();
    }

    @Override
    public PageBean<Article> list(Integer pageNum, Integer pageSize, Integer categoryId, String state) {
        //1.创建PageBean对象
        PageBean<Article> pb = new PageBean<>();

        //2.开启分页查询 PageHelper
        PageHelper.startPage(pageNum,pageSize);

        //3.调用mapper
        Map<String,Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        List<Article> as = articleMapper.list(userId,categoryId,state);
        //Page中提供了方法,可以获取PageHelper分页查询后 得到的总记录条数和当前页数据
        Page<Article> p = (Page<Article>) as;

        //把数据填充到PageBean对象中
        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }
}
