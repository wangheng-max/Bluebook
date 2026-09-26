package com.wangheng.article.controller;

import com.wangheng.article.pojo.ForwardDTO;
import com.wangheng.article.pojo.InteractStatusVO;
import com.wangheng.article.service.InteractionService;
import com.wangheng.common.Result;













import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/article")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("id");
    }

    /**
     * 点赞（SADD + 热点加分），返回最新点赞数
     */
    @PostMapping("/{id}/like")
    public Result<Long> like(@PathVariable Integer id) {
        return Result.success(interactionService.like(id, currentUserId()));
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{id}/like")
    public Result<Long> unlike(@PathVariable Integer id) {
        return Result.success(interactionService.unlike(id, currentUserId()));
    }

    /**
     * 收藏
     */
    @PostMapping("/{id}/favorite")
    public Result<Long> favorite(@PathVariable Integer id) {
        return Result.success(interactionService.favorite(id, currentUserId()));
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{id}/favorite")
    public Result<Long> unfavorite(@PathVariable Integer id) {
        return Result.success(interactionService.unfavorite(id, currentUserId()));
    }

    /**
     * 我的互动状态（是否已赞/已藏 + 计数）
     */
    @GetMapping("/{id}/interact-status")
    public Result<InteractStatusVO> status(@PathVariable Integer id) {
        return Result.success(interactionService.status(id, currentUserId()));
    }

    /**
     * 转发文章给好友，body: {"toUserId": 2}
     */
    @PostMapping("/{id}/forward")
    public Result forward(@PathVariable Integer id, @RequestBody @Validated ForwardDTO dto) {
        interactionService.forward(id, currentUserId(), dto.getToUserId());
        return Result.success();
    }
}
