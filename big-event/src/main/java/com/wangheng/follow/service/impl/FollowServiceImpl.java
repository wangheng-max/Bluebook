package com.wangheng.follow.service.impl;

import com.wangheng.exception.MerchantAuthException;
import com.wangheng.follow.mapper.FollowMapper;
import com.wangheng.follow.pojo.FollowStatusVO;
import com.wangheng.follow.service.FollowService;
import com.wangheng.merchant.mapper.MerchantInfoMapper;
import com.wangheng.user.mapper.UserMapper;
import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import java.util.Map;
import org.springframework.stereotype.Service;


/**
 * 关注服务实现。目标校验：博主=存在用户且非自己；店铺=认证通过商家。重复关注幂等。
 */
@Service
public class FollowServiceImpl implements FollowService {

    private static final int TYPE_BLOGGER = 1;
    private static final int TYPE_SHOP = 2;

    @Autowired
    private FollowMapper followMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Override
    public void follow(Integer userId, Integer type, Integer targetId) {
        validateTarget(type, targetId);
        if (type == TYPE_BLOGGER && targetId.equals(userId)) {
            throw new MerchantAuthException("不能关注自己");
        }
        try {
            followMapper.insert(userId, targetId, type);
        } catch (DuplicateKeyException e) {
            // 重复关注幂等处理
        }
    }

    @Override
    public void unfollow(Integer userId, Integer type, Integer targetId) {
        followMapper.delete(userId, targetId, type);
    }

    @Override
    public FollowStatusVO status(Integer type, Integer targetId) {
        validateTarget(type, targetId);
        FollowStatusVO vo = new FollowStatusVO();
        vo.setFollowerCount(followMapper.countFollowers(type, targetId));
        Integer me = currentUserIdOrNull();
        vo.setFollowed(me != null && followMapper.exists(me, targetId, type) > 0);
        return vo;
    }

    private void validateTarget(Integer type, Integer targetId) {
        if (type == null || (type != TYPE_BLOGGER && type != TYPE_SHOP)) {
            throw new MerchantAuthException("关注类型不正确");
        }
        if (type == TYPE_BLOGGER && userMapper.findById(targetId) == null) {
            throw new MerchantAuthException("该用户不存在");
        }
        if (type == TYPE_SHOP && merchantInfoMapper.countApproved(targetId) == 0) {
            throw new MerchantAuthException("该店铺不存在");
        }
    }

    private Integer currentUserIdOrNull() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }
}
