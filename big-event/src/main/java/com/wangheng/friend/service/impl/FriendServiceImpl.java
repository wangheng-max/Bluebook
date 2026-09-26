package com.wangheng.friend.service.impl;

import com.wangheng.common.PageBean;
import com.wangheng.friend.mapper.FriendMapper;
import com.wangheng.friend.pojo.FriendRelation;
import com.wangheng.friend.pojo.FriendRequestVO;
import com.wangheng.friend.pojo.FriendVO;
import com.wangheng.friend.service.FriendService;
import com.wangheng.user.mapper.UserMapper;
import com.wangheng.user.pojo.User;















import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;








import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FriendServiceImpl implements FriendService {

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void sendRequest(Integer targetUserId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) map.get("id");

        // 不能添加自己为好友
        if (currentUserId.equals(targetUserId)) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查目标用户是否存在
        User targetUser = userMapper.findById(targetUserId);
        if (targetUser == null) {
            throw new RuntimeException("目标用户不存在");
        }
        FriendRelation existing = friendMapper.findRelation(currentUserId, targetUserId);
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new RuntimeException("已经是好友");
            } else if (existing.getStatus() == 0) {
                // 如果请求是对方发来的且待确认，检查方向
                if (existing.getUserId().equals(targetUserId) && existing.getFriendId().equals(currentUserId)) {
                    throw new RuntimeException("对方已向您发送好友请求，请直接确认");
                }
                throw new RuntimeException("已有待处理的好友请求");
            }
            // status=2 被拒绝过，允许重新发送，更新记录
        }

        friendMapper.insertRequest(currentUserId, targetUserId);
    }

    @Override
    public PageBean<FriendRequestVO> getPendingRequests(Integer pageNum, Integer pageSize) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) map.get("id");

        PageBean<FriendRequestVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<FriendRequestVO> list = friendMapper.findPendingRequests(currentUserId);
        Page<FriendRequestVO> p = (Page<FriendRequestVO>) list;

        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public void confirmRequest(Integer relationId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) map.get("id");

        // 查找该请求
        FriendRelation relation = friendMapper.findById(relationId);
        if (relation == null) {
            throw new RuntimeException("好友请求不存在");
        }

        // 只有请求的接收者（friend_id）才能确认
        if (!relation.getFriendId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该好友请求");
        }

        // 只有待确认状态的请求才能确认
        if (relation.getStatus() != 0) {
            throw new RuntimeException("该好友请求已处理");
        }

        friendMapper.confirmRequest(relationId);
    }

    @Override
    public void rejectRequest(Integer relationId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) map.get("id");

        // 查找该请求
        FriendRelation relation = friendMapper.findById(relationId);
        if (relation == null) {
            throw new RuntimeException("好友请求不存在");
        }

        // 只有请求的接收者才能拒绝
        if (!relation.getFriendId().equals(currentUserId)) {
            throw new RuntimeException("无权操作该好友请求");
        }

        // 只有待确认状态的请求才能拒绝
        if (relation.getStatus() != 0) {
            throw new RuntimeException("该好友请求已处理");
        }

        friendMapper.rejectRequest(relationId);
    }

    @Override
    public PageBean<FriendVO> getFriendList(Integer pageNum, Integer pageSize, String keyword) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) map.get("id");

        PageBean<FriendVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        List<FriendVO> list = friendMapper.findFriends(currentUserId, keyword);
        Page<FriendVO> p = (Page<FriendVO>) list;

        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }

    @Override
    public void deleteFriend(Integer friendId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer currentUserId = (Integer) map.get("id");

        // 不能删除自己
        if (currentUserId.equals(friendId)) {
            throw new RuntimeException("不能删除自己");
        }

        int rows = friendMapper.deleteFriendRelation(currentUserId, friendId);
        if (rows == 0) {
            throw new RuntimeException("好友关系不存在");
        }
    }
}
