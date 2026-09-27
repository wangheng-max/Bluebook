package com.wangheng.address.service;

import com.wangheng.address.pojo.AddressSaveDTO;
import com.wangheng.address.pojo.UserAddress;
import java.util.List;

/**
 * 收货地址服务（登录用户，数据隔离）
 */
public interface UserAddressService {

    /** 我的收货地址列表（默认地址在前） */
    List<UserAddress> list();

    void add(AddressSaveDTO dto);

    void update(AddressSaveDTO dto);

    void delete(Integer id);
}
