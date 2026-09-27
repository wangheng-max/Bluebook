package com.wangheng.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.admin.pojo.AuditRequestDTO;
import com.wangheng.admin.service.AdminMerchantService;
import com.wangheng.auth.MerchantPermissionChecker;
import com.wangheng.common.PageBean;
import com.wangheng.exception.MerchantAuthException;
import com.wangheng.merchant.mapper.MerchantInfoMapper;
import com.wangheng.merchant.pojo.MerchantInfo;
import com.wangheng.user.mapper.UserRoleMapper;
import com.wangheng.user.pojo.UserRole;
import com.wangheng.websocket.ChatWebSocketHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



/**
 * 管理员商家审核服务实现（v1.1 商家认证机制）。
 * 审核通过：merchant_info=1 + user_role(role_type=2) 生效；拒绝/封禁：角色失效。
 */
@Service
public class AdminMerchantServiceImpl implements AdminMerchantService {

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private MerchantPermissionChecker merchantPermissionChecker;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageBean<MerchantInfo> auditList(Integer pageNum, Integer pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<MerchantInfo> list = status == null
                ? merchantInfoMapper.findAll()
                : merchantInfoMapper.findByStatus(status);
        Page<MerchantInfo> page = (Page<MerchantInfo>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Integer userId, AuditRequestDTO dto) {
        MerchantInfo info = merchantInfoMapper.findByUserId(userId);
        if (info == null) {
            throw new MerchantAuthException("该用户未提交商家认证申请");
        }
        if (info.getMerchantStatus() != 0) {
            throw new MerchantAuthException("该申请不在待审核状态");
        }

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        // 1. 认证状态：1=通过 2=拒绝
        merchantInfoMapper.updateAuditStatus(userId, approved ? 1 : 2, dto.getAuditRemark());

        // 2. 商家角色生效标记：通过=1，拒绝=0；角色记录缺失时补一条（异常数据兜底）
        int rows = userRoleMapper.updateStatus(userId, 2, approved ? 1 : 0);
        if (rows == 0 && approved) {
            UserRole role = new UserRole();
            role.setUserId(userId);
            role.setRoleType(2);
            role.setStatus(1);
            userRoleMapper.insert(role);
        }

        // 3. 失效商家状态缓存（下次鉴权重新加载）
        merchantPermissionChecker.evictMerchantCache(userId);

        // 4. WebSocket 通知（离线用户下次登录查 /merchant/status）
        notify(userId, approved
                ? "您的商家认证已通过，已开通商家功能"
                : "很抱歉，您的商家认证未通过" + (dto.getAuditRemark() == null ? "" : "：" + dto.getAuditRemark()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ban(Integer userId) {
        MerchantInfo info = merchantInfoMapper.findByUserId(userId);
        if (info == null) {
            throw new MerchantAuthException("该用户未提交商家认证申请");
        }
        merchantInfoMapper.updateAuditStatus(userId, 3, "平台封禁");
        userRoleMapper.updateStatus(userId, 2, 0);
        merchantPermissionChecker.evictMerchantCache(userId);
        notify(userId, "您的商家账号已被封禁，请联系平台客服");
    }

    /**
     * 推送审核结果通知（MERCHANT_AUDIT_RESULT）
     */
    private void notify(Integer userId, String text) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "MERCHANT_AUDIT_RESULT");
            message.put("message", text);
            chatWebSocketHandler.sendToUser(userId, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 通知失败不影响审核主流程
        }
    }
}
