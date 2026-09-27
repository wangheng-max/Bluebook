package com.wangheng.follow.service;

import com.wangheng.follow.pojo.FollowStatusVO;


/**
 * 关注服务（博主/店铺统一，type 1=博主 2=店铺）。
 */
public interface FollowService {

    void follow(Integer userId, Integer type, Integer targetId);

    void unfollow(Integer userId, Integer type, Integer targetId);

    FollowStatusVO status(Integer type, Integer targetId);
}
