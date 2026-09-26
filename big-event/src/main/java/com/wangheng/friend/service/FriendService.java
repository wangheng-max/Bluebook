package com.wangheng.friend.service;

import com.wangheng.common.PageBean;
import com.wangheng.friend.pojo.FriendRequestVO;
import com.wangheng.friend.pojo.FriendVO;











public interface FriendService {

    // 发送好友请求
    void sendRequest(Integer targetUserId);

    // 获取待确认的好友请求列表
    PageBean<FriendRequestVO> getPendingRequests(Integer pageNum, Integer pageSize);

    // 确认好友请求
    void confirmRequest(Integer relationId);

    // 拒绝好友请求
    void rejectRequest(Integer relationId);

    // 获取好友列表
    PageBean<FriendVO> getFriendList(Integer pageNum, Integer pageSize, String keyword);

    // 删除好友
    void deleteFriend(Integer friendId);
}
