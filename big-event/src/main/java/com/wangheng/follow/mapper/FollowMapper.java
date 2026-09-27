package com.wangheng.follow.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * 关注表 Mapper（uk_follow 唯一键防重复关注；type 1=博主 2=店铺）。
 */
@Mapper
public interface FollowMapper {

    @Insert("insert ignore into follow(follower_id, followee_id, follow_type, create_time) " +
            "values(#{followerId}, #{followeeId}, #{followType}, now())")
    int insert(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId,
               @Param("followType") Integer followType);

    @Delete("delete from follow where follower_id=#{followerId} and followee_id=#{followeeId} and follow_type=#{followType}")
    int delete(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId,
               @Param("followType") Integer followType);

    /** 我是否已关注该目标 */
    @Select("select count(*) from follow where follower_id=#{followerId} and followee_id=#{followeeId} and follow_type=#{followType}")
    int exists(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId,
               @Param("followType") Integer followType);

    /** 目标的粉丝数 */
    @Select("select count(*) from follow where follow_type=#{followType} and followee_id=#{followeeId}")
    int countFollowers(@Param("followType") Integer followType, @Param("followeeId") Integer followeeId);

    /** 我关注的人数 */
    @Select("select count(*) from follow where follower_id=#{followerId} and follow_type=#{followType}")
    int countFollowing(@Param("followerId") Integer followerId, @Param("followType") Integer followType);
}
