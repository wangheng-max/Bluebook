package com.wangheng.controller;

import com.wangheng.pojo.NoteSearchVO;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Result;
import com.wangheng.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    /**
     * 热点文章分页（Redis ZSET 取 Top N，score=热度分）
     */
    @GetMapping("/hot")
    public Result<PageBean<NoteSearchVO>> hot(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(communityService.hotArticles(pageNum, pageSize));
    }

    /**
     * 按分类查询已发布文章
     */
    @GetMapping("/articles")
    public Result<PageBean<NoteSearchVO>> articles(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(communityService.articlesByCategory(categoryId, pageNum, pageSize));
    }

    /**
     * 文章详情（含计数）
     */
    @GetMapping("/article/{id}")
    public Result<NoteSearchVO> detail(@PathVariable Integer id) {
        return Result.success(communityService.articleDetail(id));
    }
}
