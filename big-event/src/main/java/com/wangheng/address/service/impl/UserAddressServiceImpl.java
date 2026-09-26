package com.wangheng.address.service.impl;

import com.wangheng.address.mapper.UserAddressMapper;
import com.wangheng.address.pojo.AddressSaveDTO;
import com.wangheng.address.pojo.UserAddress;
import com.wangheng.address.service.UserAddressService;









import com.wangheng.exception.MerchantAuthException;




import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 收货地址服务实现（数据隔离：只能操作自己的地址）。
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> list() {
        return userAddressMapper.findByUserId(currentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AddressSaveDTO dto) {
        Integer userId = currentUserId();
        boolean isDefault = Boolean.TRUE.equals(dto.getIsDefault());
        // 第一条地址自动设为默认
        if (!isDefault && userAddressMapper.findByUserId(userId).isEmpty()) {
            isDefault = true;
        }
        if (isDefault) {
            userAddressMapper.clearDefault(userId);
        }
        UserAddress address = new UserAddress();
        fill(address, dto);
        address.setUserId(userId);
        address.setIsDefault(isDefault ? 1 : 0);
        userAddressMapper.insert(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AddressSaveDTO dto) {
        Integer userId = currentUserId();
        if (dto.getId() == null) {
            throw new RuntimeException("地址ID不能为空");
        }
        if (userAddressMapper.countOwned(dto.getId(), userId) == 0) {
            throw new MerchantAuthException("无权操作他人地址");
        }
        boolean isDefault = Boolean.TRUE.equals(dto.getIsDefault());
        if (isDefault) {
            userAddressMapper.clearDefault(userId);
        }
        UserAddress address = new UserAddress();
        fill(address, dto);
        address.setUserId(userId);
        address.setIsDefault(isDefault ? 1 : 0);
        userAddressMapper.update(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Integer userId = currentUserId();
        if (userAddressMapper.delete(id, userId) == 0) {
            throw new MerchantAuthException("无权操作他人地址");
        }
    }

    private void fill(UserAddress address, AddressSaveDTO dto) {
        address.setId(dto.getId());
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetailAddress(dto.getDetailAddress());
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
