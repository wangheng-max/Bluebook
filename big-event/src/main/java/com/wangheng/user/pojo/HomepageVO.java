package com.wangheng.user.pojo;

import lombok.Data;


/**
 * 用户/博主公开主页（GET /user/homepage/{userId}）。
 * 本人访问额外可编辑 bio；认证商家展示店铺入口。
 */
@Data
public class HomepageVO {
    private Integer userId;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer articleCount;//已发布作品数
    private Integer followerCount;//粉丝数（关注我的博主粉丝）
    private Integer followingCount;//我关注的博主数
    private Boolean followed;//当前登录用户是否已关注（匿名恒 false）
    private Boolean isSelf;//是否本人主页
    private Integer shopUserId;//认证店铺（merchant_info.user_id）
    private String shopName;
    private String shopLogo;
}
