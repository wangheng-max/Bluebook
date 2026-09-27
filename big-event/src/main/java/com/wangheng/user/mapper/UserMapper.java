package com.wangheng.user.mapper;

import com.wangheng.user.pojo.User;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    //根据用户名查询用户
    @Select("select * from user where username=#{username}")
    User findByUserName(String username);

    //根据ID查询用户
    @Select("select * from user where id=#{id}")
    User findById(Integer id);

    //添加
    @Insert("insert into user(username,password,create_time,update_time)" +
            " values(#{username},#{password},now(),now())")
    void add(String username, String password);

    //修改信息
    @Update("update user set nickname=#{nickname},email=#{email},bio=#{bio},update_time=#{updateTime} where id=#{id}")
    void update(User user);
    //修改头像
    @Update("update user set user_pic=#{avatarUrl},update_time=now() where id=#{id}")
    void updateAvatar(String avatarUrl,Integer id);
    //修改密码
    @Update("update user set bio=#{bio},update_time=now() where id=#{id}")
    void updateBio(@Param("bio") String bio, @Param("id") Integer id);

    @Update("update user set password=#{md5String},update_time=now() where id=#{id}")
    void updatePwd(String md5String, Integer id);

    //根据用户名或昵称搜索用户
    @Select("select id, username, nickname, user_pic as userPic from user " +
            "where (username like concat('%',#{keyword},'%') or nickname like concat('%',#{keyword},'%')) " +
            "and id != #{currentUserId}")
    List<User> searchUsers(String keyword, Integer currentUserId);
}
