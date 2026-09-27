package com.wangheng.merchant.service.impl;

import com.wangheng.auth.MerchantPermissionChecker;
import com.wangheng.exception.MerchantAuthException;
import com.wangheng.merchant.mapper.MerchantInfoMapper;
import com.wangheng.merchant.pojo.MerchantApplyDTO;
import com.wangheng.merchant.pojo.MerchantInfo;
import com.wangheng.merchant.pojo.MerchantStatusVO;
import com.wangheng.merchant.pojo.ShopInfoDTO;
import com.wangheng.merchant.service.MerchantService;
import com.wangheng.user.mapper.UserRoleMapper;
import com.wangheng.user.pojo.UserRole;
import com.wangheng.utils.ThreadLocalUtil;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 商家认证服务实现（v1.1 商家认证机制）。
 * 认证状态机：0待审核 → 1已通过 / 2已拒绝（可重新提交回 0） / 3已封禁（不可再提交）。
 */
@Service
public class MerchantServiceImpl implements MerchantService {

    private static final String MERCHANT_INFO_KEY_PREFIX = "merchant:info:";

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private MerchantPermissionChecker merchantPermissionChecker;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(MerchantApplyDTO dto) {
        Integer userId = currentUserId();
        MerchantInfo exist = merchantInfoMapper.findByUserId(userId);
        if (exist != null) {
            switch (exist.getMerchantStatus()) {
                case 0:
                    throw new MerchantAuthException("认证申请审核中，请勿重复提交");
                case 1:
                    throw new MerchantAuthException("您已是认证商家，无需重复申请");
                case 3:
                    throw new MerchantAuthException("商家账号已被封禁，请联系平台客服");
                default:
                    // 2=已拒绝：允许按补充后的材料重新提交（走覆盖更新）
                    break;
            }
        }
        saveApply(dto, userId, exist != null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reSubmit(MerchantApplyDTO dto) {
        Integer userId = currentUserId();
        MerchantInfo exist = merchantInfoMapper.findByUserId(userId);
        if (exist == null) {
            throw new MerchantAuthException("请先提交商家认证申请");
        }
        if (exist.getMerchantStatus() != 2) {
            throw new MerchantAuthException("当前状态无需重新提交申请");
        }
        saveApply(dto, userId, true);
    }

    @Override
    public MerchantStatusVO getStatus() {
        Integer userId = currentUserId();
        MerchantInfo info = merchantInfoMapper.findByUserId(userId);
        if (info == null) {
            return new MerchantStatusVO(userId, 4, "未申请", null, null, null, null, null);
        }
        MerchantStatusVO vo = new MerchantStatusVO();
        vo.setUserId(userId);
        vo.setMerchantStatus(info.getMerchantStatus());
        vo.setStatusDesc(statusDesc(info.getMerchantStatus()));
        vo.setAuditRemark(info.getAuditRemark());
        vo.setShopName(info.getShopName());
        vo.setShopLogo(info.getShopLogo());
        vo.setApplyTime(info.getCreateTime());
        vo.setAuditTime(info.getUpdateTime());
        return vo;
    }

    @Override
    public MerchantInfo getShopInfo() {
        Integer userId = currentUserId();
        return merchantInfoMapper.findByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShopInfo(ShopInfoDTO dto) {
        Integer userId = currentUserId();
        MerchantInfo info = new MerchantInfo();
        info.setUserId(userId);
        info.setShopName(dto.getShopName());
        info.setShopLogo(dto.getShopLogo());
        info.setShopDescription(dto.getShopDescription());
        int rows = merchantInfoMapper.updateShopInfo(info);
        if (rows == 0) {
            throw new MerchantAuthException("无商家权限，请先完成商家认证");
        }
        // 失效店铺信息缓存
        try {
            stringRedisTemplate.delete(MERCHANT_INFO_KEY_PREFIX + userId);
        } catch (Exception ignored) {
            // 缓存失效失败不影响主流程
        }
    }

    /**
     * 写入申请（首次 insert / 已拒绝覆盖 update）+ 同步商家角色记录（status=0 待生效）
     */
    private void saveApply(MerchantApplyDTO dto, Integer userId, boolean update) {
        MerchantInfo info = new MerchantInfo();
        info.setUserId(userId);
        info.setShopName(dto.getShopName());
        info.setShopLogo(dto.getShopLogo());
        info.setShopDescription(dto.getShopDescription());
        info.setLicenseNumber(dto.getLicenseNumber());
        info.setLicenseImg(dto.getLicenseImg());
        info.setContactName(dto.getContactName());
        info.setContactPhone(dto.getContactPhone());
        info.setProvince(dto.getProvince());
        info.setCity(dto.getCity());
        info.setDistrict(dto.getDistrict());
        info.setAddress(dto.getAddress());
        info.setMerchantStatus(0);

        if (update) {
            merchantInfoMapper.updateApply(info);
        } else {
            merchantInfoMapper.insert(info);
        }

        // 同步商家角色记录：不存在则插入(status=0)，存在则重置为待生效
        UserRole role = userRoleMapper.findByUserAndType(userId, 2);
        if (role == null) {
            UserRole newRole = new UserRole();
            newRole.setUserId(userId);
            newRole.setRoleType(2);
            newRole.setStatus(0);
            userRoleMapper.insert(newRole);
        } else if (role.getStatus() != 0) {
            userRoleMapper.updateStatus(userId, 2, 0);
        }
        merchantPermissionChecker.evictMerchantCache(userId);
    }

    private String statusDesc(Integer status) {
        switch (status == null ? -1 : status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已封禁";
            default: return "未申请";
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
