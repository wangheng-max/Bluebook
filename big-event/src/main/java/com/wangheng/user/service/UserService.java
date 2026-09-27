package com.wangheng.user.service;

import com.wangheng.user.pojo.User;

public interface UserService {
    //根据用户名查询用户
    User findByUserName(String username);

    //注册
    void register(String username, String password);

    //更新
    void update(User user);

    //更新头像
    void updateAvatar(String avatarUrl);

    //更新密码
    void updatePwd(String newPwd);

    /**
     * 按 ID 查用户（主页用）
     */
    User findById(Integer id);

    /**
     * 更新自我介绍
     */
    void updateBio(Integer userId, String bio);
}
