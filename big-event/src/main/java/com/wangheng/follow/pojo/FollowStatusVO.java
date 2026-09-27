package com.wangheng.follow.pojo;

import lombok.Data;


/**
 * 关注状态（公开：粉丝数所有人可看；followed 仅登录用户有意义，匿名恒为 false）。
 */
@Data
public class FollowStatusVO {
    private Boolean followed;
    private Integer followerCount;
}
