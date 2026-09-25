package com.wangheng.mapper;

import com.wangheng.pojo.MerchantInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商家认证信息表 Mapper（v1.1 商家认证机制）
 */
@Mapper
public interface MerchantInfoMapper {

    @Insert("insert into merchant_info(user_id,shop_name,shop_logo,shop_description,license_number,license_img," +
            "contact_name,contact_phone,province,city,district,address,merchant_status,create_time,update_time) " +
            "values(#{userId},#{shopName},#{shopLogo},#{shopDescription},#{licenseNumber},#{licenseImg}," +
            "#{contactName},#{contactPhone},#{province},#{city},#{district},#{address},#{merchantStatus},now(),now())")
    void insert(MerchantInfo info);

    /** 按用户ID查询（uk_user 唯一索引，一用户一条记录） */
    @Select("select * from merchant_info where user_id=#{userId}")
    MerchantInfo findByUserId(Integer userId);

    /** 认证通过数（userId 维度） */
    @Select("select count(*) from merchant_info where user_id=#{userId} and merchant_status=1")
    int countApproved(Integer userId);

    /** 审核/封禁：更新状态与备注（update_time 由数据库 ON UPDATE 自动刷新） */
    @Update("update merchant_info set merchant_status=#{status},audit_remark=#{auditRemark} " +
            "where user_id=#{userId}")
    int updateAuditStatus(Integer userId, Integer status, String auditRemark);

    /** 被拒绝后重新提交：覆盖申请材料，状态重置为待审核、清空审核备注 */
    @Update("update merchant_info set shop_name=#{shopName},shop_logo=#{shopLogo},shop_description=#{shopDescription}," +
            "license_number=#{licenseNumber},license_img=#{licenseImg},contact_name=#{contactName}," +
            "contact_phone=#{contactPhone},province=#{province},city=#{city},district=#{district}," +
            "address=#{address},merchant_status=0,audit_remark=null where user_id=#{userId}")
    int updateApply(MerchantInfo info);

    /** 认证商家修改店铺信息（仅 status=1 可改） */
    @Update("update merchant_info set shop_name=#{shopName},shop_logo=#{shopLogo},shop_description=#{shopDescription} " +
            "where user_id=#{userId} and merchant_status=1")
    int updateShopInfo(MerchantInfo info);

    /** 按状态筛选（管理员审核列表） */
    @Select("select * from merchant_info where merchant_status=#{status} order by create_time asc")
    List<MerchantInfo> findByStatus(Integer status);

    /** 全量列表（管理员审核列表，不分状态） */
    @Select("select * from merchant_info order by create_time asc")
    List<MerchantInfo> findAll();
}
