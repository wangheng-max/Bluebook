package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户角色实体（表 user_role，v1.1 商家认证机制）。
 * role_type: 1=普通用户 2=商家 3=管理员；status: 1=角色生效 0=禁用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    private Integer id;
    private Integer userId;
    private Integer roleType;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
