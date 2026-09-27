package com.wangheng.article.controller;

import com.wangheng.article.pojo.FavoriteFolderVO;
import com.wangheng.article.pojo.ForwardDTO;
import com.wangheng.article.pojo.InteractStatusVO;
import com.wangheng.article.pojo.NoteSearchVO;
import com.wangheng.article.service.InteractionService;
import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.utils.ThreadLocalUtil;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



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
    public Result<Long> favorite(@PathVariable Integer id,
                                 @RequestParam(required = false) Integer folderId) {
        return Result.success(interactionService.favorite(id, currentUserId(), folderId));
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

    // ================= 收藏夹管理（登录） =================

    /** 我的收藏夹列表（首项恒为"默认收藏夹" id=null） */
    @GetMapping("/favorite/folders")
    public Result<java.util.List<FavoriteFolderVO>> myFolders() {
        return Result.success(interactionService.myFolders(currentUserId()));
    }

    /** 创建收藏夹 */
    @PostMapping("/favorite/folder")
    public Result createFolder(@RequestParam String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 30) {
            return Result.error("收藏夹名称须为 1-30 字");
        }
        interactionService.createFolder(currentUserId(), name.trim());
        return Result.success();
    }

    /** 重命名收藏夹 */
    @PutMapping("/favorite/folder/{id}")
    public Result renameFolder(@PathVariable Integer id, @RequestParam String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 30) {
            return Result.error("收藏夹名称须为 1-30 字");
        }
        interactionService.renameFolder(currentUserId(), id, name.trim());
        return Result.success();
    }

    /** 删除收藏夹（夹内收藏自动回退默认收藏夹） */
    @DeleteMapping("/favorite/folder/{id}")
    public Result deleteFolder(@PathVariable Integer id) {
        interactionService.deleteFolder(currentUserId(), id);
        return Result.success();
    }

    // ================= 个人主页公开列表 =================

    /** TA 赞过的文章（主页"赞过"标签） */
    @GetMapping("/user/{userId}/liked")
    public Result<PageBean<NoteSearchVO>> likedList(@PathVariable Integer userId,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(interactionService.likedList(userId, pageNum, pageSize));
    }

    /** TA 的收藏文章（主页"收藏"标签；folderId 空=默认收藏夹） */
    @GetMapping("/user/{userId}/favorites")
    public Result<PageBean<NoteSearchVO>> favoriteList(@PathVariable Integer userId,
                                                       @RequestParam(required = false) Integer folderId,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(interactionService.favoriteList(userId, folderId, pageNum, pageSize));
    }
}
