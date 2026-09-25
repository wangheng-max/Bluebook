package com.wangheng.controller;

import com.wangheng.mapper.UserRoleMapper;
import com.wangheng.pojo.AdminStatusVO;
import com.wangheng.pojo.Result;
import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 平台管理通用接口。
 * 仅返回调用者自身的角色判断（是否管理员），供前端决定"平台管理"菜单可见性；
 * 不提供任何管理员账号信息查询。管理操作接口见 AdminMerchantController
 * 及 ProductController 分类管理，均受 @RequireAdmin 保护（非管理员 HTTP 403）。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRoleMapper userRoleMapper;

    /** 当前登录用户是否管理员（user_role role_type=3 且生效） */
    @GetMapping("/status")
    public Result<AdminStatusVO> status() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = claims == null ? null : (Integer) claims.get("id");
        boolean admin = userId != null && userRoleMapper.countEffectiveRole(userId, 3) > 0;
        return Result.success(new AdminStatusVO(admin));
    }
}
