package com.wangheng.mapper;

import com.wangheng.pojo.UserAddress;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 收货地址表 Mapper（数据隔离：全部操作带 userId 条件）
 */
@Mapper
public interface UserAddressMapper {

    @Insert("insert into user_address(user_id,receiver_name,receiver_phone,province,city,district,detail_address," +
            "is_default,create_time,update_time) " +
            "values(#{userId},#{receiverName},#{receiverPhone},#{province},#{city},#{district},#{detailAddress}," +
            "#{isDefault},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserAddress address);

    @Select("select * from user_address where user_id=#{userId} order by is_default desc, id desc")
    List<UserAddress> findByUserId(Integer userId);

    @Select("select * from user_address where id=#{id} and user_id=#{userId}")
    UserAddress findByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("select count(*) from user_address where id=#{id} and user_id=#{userId}")
    int countOwned(@Param("id") Integer id, @Param("userId") Integer userId);

    @Update("update user_address set receiver_name=#{receiverName},receiver_phone=#{receiverPhone}," +
            "province=#{province},city=#{city},district=#{district},detail_address=#{detailAddress}," +
            "is_default=#{isDefault},update_time=now() where id=#{id} and user_id=#{userId}")
    int update(UserAddress address);

    /** 取消该用户全部默认标记（设置新默认前调用） */
    @Update("update user_address set is_default=0 where user_id=#{userId}")
    void clearDefault(Integer userId);

    @Delete("delete from user_address where id=#{id} and user_id=#{userId}")
    int delete(@Param("id") Integer id, @Param("userId") Integer userId);
}
