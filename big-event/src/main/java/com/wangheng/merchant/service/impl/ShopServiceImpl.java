package com.wangheng.merchant.service.impl;

import com.wangheng.common.PageBean;
import com.wangheng.merchant.mapper.MerchantInfoMapper;
import com.wangheng.merchant.mapper.MerchantRatingMapper;
import com.wangheng.merchant.pojo.MerchantInfo;
import com.wangheng.merchant.pojo.MerchantRatingVO;
import com.wangheng.merchant.pojo.ShopVO;
import com.wangheng.merchant.service.ShopService;
import com.wangheng.product.mapper.ProductMapper;
import com.wangheng.product.pojo.Product;















import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;









import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 店铺主页服务实现（公开读接口，匿名可访问，见 LoginInterceptor 白名单）。
 */
@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired
    private MerchantRatingMapper merchantRatingMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public ShopVO getShop(Integer merchantUserId) {
        MerchantInfo info = merchantInfoMapper.findByUserId(merchantUserId);
        if (info == null || !Integer.valueOf(1).equals(info.getMerchantStatus())) {
            throw new RuntimeException("店铺不存在");
        }
        ShopVO vo = new ShopVO();
        vo.setUserId(info.getUserId());
        vo.setShopName(info.getShopName());
        vo.setShopLogo(info.getShopLogo());
        vo.setShopDescription(info.getShopDescription());
        vo.setProvince(info.getProvince());
        vo.setCity(info.getCity());
        vo.setDistrict(info.getDistrict());
        vo.setAddress(info.getAddress());
        vo.setCreateTime(info.getCreateTime());
        Double avg = merchantRatingMapper.avgScore(merchantUserId);
        vo.setAvgScore(avg == null ? null : Math.round(avg * 10) / 10.0);
        vo.setRatingCount(merchantRatingMapper.countByMerchant(merchantUserId));
        vo.setProductCount(productMapper.countShopApproved(merchantUserId));
        return vo;
    }

    @Override
    public PageBean<Product> getShopProducts(Integer merchantUserId, Integer pageNum, Integer pageSize) {
        // 店铺必须存在且认证通过（与 getShop 口径一致）
        getShop(merchantUserId);
        PageHelper.startPage(pageNum, pageSize);
        List<Product> list = productMapper.listShopApproved(merchantUserId);
        Page<Product> page = (Page<Product>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public PageBean<MerchantRatingVO> getShopRatings(Integer merchantUserId, Integer pageNum, Integer pageSize) {
        getShop(merchantUserId);
        PageHelper.startPage(pageNum, pageSize);
        List<MerchantRatingVO> list = merchantRatingMapper.listByMerchant(merchantUserId);
        Page<MerchantRatingVO> page = (Page<MerchantRatingVO>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }
}
