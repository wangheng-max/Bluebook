package com.wangheng.address.controller;

import com.wangheng.address.pojo.AddressSaveDTO;
import com.wangheng.address.pojo.UserAddress;
import com.wangheng.address.service.UserAddressService;
import com.wangheng.common.Result;
import com.wangheng.user.controller.UserController;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;




/**
 * 收货地址接口（商城团购功能，登录用户）。
 * 与现有 UserController（/user 注册/登录等）路径并存，仅新增 /user/address 子路径。
 */
@RestController
@RequestMapping("/user/address")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;

    /** 收货地址列表 */
    @GetMapping("/list")
    public Result<List<UserAddress>> list() {
        return Result.success(userAddressService.list());
    }

    /** 添加收货地址 */
    @PostMapping
    public Result add(@RequestBody @Validated AddressSaveDTO dto) {
        userAddressService.add(dto);
        return Result.success();
    }

    /** 修改收货地址 */
    @PutMapping
    public Result update(@RequestBody @Validated AddressSaveDTO dto) {
        userAddressService.update(dto);
        return Result.success();
    }

    /** 删除收货地址 */
    @DeleteMapping
    public Result delete(@RequestParam Integer id) {
        userAddressService.delete(id);
        return Result.success();
    }
}
