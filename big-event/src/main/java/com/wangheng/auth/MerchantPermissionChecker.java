package com.wangheng.auth;

import com.wangheng.exception.MerchantAuthException;
import com.wangheng.mapper.MerchantInfoMapper;
import com.wangheng.mapper.UserRoleMapper;
import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 商家/管理员权限校验器（v1.1 商家认证机制，设计文档《商家认证流程设计》）。
 *
 * 商家校验 = user_role 商家角色生效(status=1) + merchant_info 认证通过(merchant_status=1) 双重校验；
 * 认证状态高频读取，命中后写 Redis 缓存（merchant:status:{userId}），审核/封禁时主动失效。
 */
@Component
public class MerchantPermissionChecker {

    private static final String MERCHANT_STATUS_KEY_PREFIX = "merchant:status:";
    private static final long MERCHANT_STATUS_TTL_MINUTES = 30;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    /**
     * 校验当前登录用户是否为认证通过的商家，不通过抛 MerchantAuthException（403）
     */
    public void checkMerchant() {
        Integer userId = currentUserId();

        String cacheKey = MERCHANT_STATUS_KEY_PREFIX + userId;
        // 1. 先查缓存
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if ("1".equals(cached)) {
                    return;
                }
                throw new MerchantAuthException("无商家权限，请先完成商家认证");
            }
        } catch (MerchantAuthException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用时回源数据库
        }

        // 2. 回源：商家角色生效 + 认证通过
        int roleOk = userRoleMapper.countEffectiveRole(userId, 2);
        int certOk = merchantInfoMapper.countApproved(userId);
        if (roleOk > 0 && certOk > 0) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, "1", MERCHANT_STATUS_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception ignored) {
                // 缓存写入失败不影响鉴权结果
            }
            return;
        }
        throw new MerchantAuthException("无商家权限，请先完成商家认证");
    }

    /**
     * 校验当前登录用户是否为管理员（user_role role_type=3 且生效），不通过抛 MerchantAuthException（403）
     */
    public void checkAdmin() {
        Integer userId = currentUserId();
        int adminOk = userRoleMapper.countEffectiveRole(userId, 3);
        if (adminOk == 0) {
            throw new MerchantAuthException("无管理员权限");
        }
    }

    /**
     * 审核/封禁后主动失效商家状态缓存
     */
    public void evictMerchantCache(Integer userId) {
        try {
            stringRedisTemplate.delete(MERCHANT_STATUS_KEY_PREFIX + userId);
        } catch (Exception ignored) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = claims == null ? null : (Integer) claims.get("id");
        if (userId == null) {
            throw new MerchantAuthException("请先登录");
        }
        return userId;
    }
}
