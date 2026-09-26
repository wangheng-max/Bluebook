package com.wangheng.comment.controller;

import com.wangheng.comment.pojo.CommentSaveDTO;
import com.wangheng.comment.pojo.CommentVO;
import com.wangheng.comment.service.CommentService;
import com.wangheng.common.PageBean;
import com.wangheng.common.Result;














import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评论接口（文章/商品通用，targetType=article/product）。
 * 读接口（list/replies）匿名可访问（商品详情页公开）；发布/点赞/删除需登录。
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }

    /** 发布评论/回复（parentId 空=顶级评论） */
    @PostMapping
    public Result add(@RequestBody @Validated CommentSaveDTO dto) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        commentService.add(dto, userId);
        return Result.success();
    }

    /**
     * 评论分页：sort=time 按时间（默认）/ sort=likes 按点赞数；
     * 每条顶级评论带楼中楼预览与当前用户点赞状态
     */
    @GetMapping("/list")
    public Result<PageBean<CommentVO>> list(@RequestParam String targetType,
                                            @RequestParam Integer targetId,
                                            @RequestParam(defaultValue = "time") String sort,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        return Result.success(commentService.list(targetType, targetId, sort, pageNum, pageSize));
    }

    /** 某顶级评论下的全部回复（分页，按时间正序） */
    @GetMapping("/{rootId}/replies")
    public Result<PageBean<CommentVO>> replies(@PathVariable Integer rootId,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageSize > 50) {
            pageSize = 50;
        }
        return Result.success(commentService.replies(rootId, pageNum, pageSize));
    }

    /** 点赞评论，返回最新点赞数 */
    @PostMapping("/{id}/like")
    public Result<Long> like(@PathVariable Integer id) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(commentService.like(id, userId));
    }

    /** 取消点赞，返回最新点赞数 */
    @DeleteMapping("/{id}/like")
    public Result<Long> unlike(@PathVariable Integer id) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(commentService.unlike(id, userId));
    }

    /** 删除自己的评论（顶级评论连带全部回复） */
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        commentService.delete(id, userId);
        return Result.success();
    }
}
