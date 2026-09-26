package com.wangheng.user.mapper;

import com.wangheng.user.pojo.UserRole;






import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户角色表 Mapper（v1.1 商家认证机制）
 */
@Mapper
public interface UserRoleMapper {

    /** 查询用户某角色是否生效（status=1） */
    @Select("select count(*) from user_role where user_id=#{userId} and role_type=#{roleType} and status=1")
    int countEffectiveRole(Integer userId, Integer roleType);

    /** 查询用户某角色记录（可能不存在） */
    @Select("select * from user_role where user_id=#{userId} and role_type=#{roleType}")
    UserRole findByUserAndType(Integer userId, Integer roleType);

    @Insert("insert into user_role(user_id,role_type,status,create_time,update_time) " +
            "values(#{userId},#{roleType},#{status},now(),now())")
    void insert(UserRole role);

    /** 更新角色生效状态，返回影响行数 */
    @Update("update user_role set status=#{status},update_time=now() " +
            "where user_id=#{userId} and role_type=#{roleType}")
    int updateStatus(Integer userId, Integer roleType, Integer status);
}
