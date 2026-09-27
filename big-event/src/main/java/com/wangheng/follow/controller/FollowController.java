package com.wangheng.follow.controller;

import com.wangheng.anno.RequireMerchant;
import com.wangheng.common.Result;
import com.wangheng.follow.pojo.FollowStatusVO;
import com.wangheng.follow.service.FollowService;
import com.wangheng.utils.ThreadLocalUtil;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;



/**
 * 关注接口（type：1=关注博主 2=关注店铺）。
 * 关注/取关需登录；状态与粉丝数公开（登录态存在时返回 followed）。
 */
@RestController
@RequestMapping("/follow")
@Validated
public class FollowController {

    @Autowired
    private FollowService followService;

    /** 关注（博主/店铺） */
    @PostMapping("/{type}/{targetId}")
    public Result follow(@PathVariable Integer type, @PathVariable Integer targetId) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        followService.follow(userId, type, targetId);
        return Result.success();
    }

    /** 取消关注 */
    @DeleteMapping("/{type}/{targetId}")
    public Result unfollow(@PathVariable Integer type, @PathVariable Integer targetId) {
        Integer userId = currentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        followService.unfollow(userId, type, targetId);
        return Result.success();
    }

    /** 关注状态 + 粉丝数（公开，登录后带 followed） */
    @GetMapping("/status")
    public Result<FollowStatusVO> status(@RequestParam Integer type, @RequestParam Integer targetId) {
        return Result.success(followService.status(type, targetId));
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }
}
