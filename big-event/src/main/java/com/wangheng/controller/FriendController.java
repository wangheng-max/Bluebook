package com.wangheng.controller;

import com.wangheng.pojo.*;
import com.wangheng.service.FriendService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friends")
@Validated
public class FriendController {

    @Autowired
    private FriendService friendService;

    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public Result sendRequest(@RequestBody @Validated FriendRequestDTO dto) {
        friendService.sendRequest(dto.getTargetUserId());
        return Result.success("好友请求已发送", null);
    }

    /**
     * 获取待确认的好友请求列表
     */
    @GetMapping("/pending")
    public Result<PageBean<FriendRequestVO>> getPendingRequests(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageBean<FriendRequestVO> pb = friendService.getPendingRequests(page, size);
        return Result.success(pb);
    }

    /**
     * 确认好友请求
     */
    @PutMapping("/confirm/{relationId}")
    public Result confirmRequest(@PathVariable @NotNull Integer relationId) {
        friendService.confirmRequest(relationId);
        return Result.success("已添加为好友", null);
    }

    /**
     * 拒绝好友请求
     */
    @PutMapping("/reject/{relationId}")
    public Result rejectRequest(@PathVariable @NotNull Integer relationId) {
        friendService.rejectRequest(relationId);
        return Result.success("已拒绝好友请求", null);
    }

    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<PageBean<FriendVO>> getFriendList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        PageBean<FriendVO> pb = friendService.getFriendList(page, size, keyword);
        return Result.success(pb);
    }

    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result deleteFriend(@PathVariable @NotNull Integer friendId) {
        friendService.deleteFriend(friendId);
        return Result.success("已删除好友", null);
    }
}
