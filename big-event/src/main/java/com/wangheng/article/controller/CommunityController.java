package com.wangheng.article.controller;

import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.article.service.CommunityService;
import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
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

    /**
     * 某作者的已发布文章（博主主页作品列表，公开）
     */
    @GetMapping("/user/{userId}/articles")
    public Result<PageBean<NoteSearchVO>> userArticles(@PathVariable Integer userId,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(communityService.articlesByUser(userId, pageNum, pageSize));
    }

    /**
     * 热门标签 Top N（发布表单联想 + 标签点击搜索的入口词库）
     */
    @GetMapping("/hot-tags")
    public Result<java.util.List<String>> hotTags(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(communityService.hotTags(Math.min(Math.max(limit, 1), 30)));
    }
}
