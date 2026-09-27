package com.wangheng.coupon.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.common.PageBean;
import com.wangheng.coupon.mapper.CouponStockMapper;
import com.wangheng.coupon.mapper.CouponTypeMapper;
import com.wangheng.coupon.mapper.UserCouponMapper;
import com.wangheng.coupon.pojo.CouponStatisticsVO;
import com.wangheng.coupon.pojo.CouponStock;
import com.wangheng.coupon.pojo.CouponStockDTO;
import com.wangheng.coupon.pojo.CouponType;
import com.wangheng.coupon.pojo.CouponTypeDTO;
import com.wangheng.coupon.pojo.UserCoupon;
import com.wangheng.coupon.service.MerchantCouponService;
import com.wangheng.coupon.util.CouponStockStatusHelper;
import com.wangheng.exception.MerchantAuthException;
import com.wangheng.user.mapper.UserMapper;
import com.wangheng.utils.ThreadLocalUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


































/**
 * 商家优惠券管理服务实现。
 * 所有写操作校验归属（create_user_id == 当前用户），统计按本店数据隔离。
 */
@Service
public class MerchantCouponServiceImpl implements MerchantCouponService {

    private static final String STOCK_LIST_CACHE_PREFIX = "coupon:stock:list:";

    @Autowired
    private CouponTypeMapper couponTypeMapper;

    @Autowired
    private CouponStockMapper couponStockMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CouponStockStatusHelper couponStockStatusHelper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createType(CouponTypeDTO dto) {
        validateTypeRule(dto);
        Integer userId = currentUserId();
        CouponType type = new CouponType();
        type.setName(dto.getName());
        type.setMinSpend(dto.getMinSpend());
        type.setDiscountAmount(dto.getDiscountAmount());
        type.setDiscountRate(dto.getDiscountRate());
        type.setValidDays(dto.getValidDays());
        type.setMaxUses(dto.getMaxUses() == null ? 0 : dto.getMaxUses());
        type.setCreateUserId(userId);
        couponTypeMapper.insert(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(CouponTypeDTO dto) {
        Integer userId = currentUserId();
        if (dto.getId() == null) {
            throw new RuntimeException("券类型ID不能为空");
        }
        if (couponTypeMapper.countOwned(dto.getId(), userId) == 0) {
            throw new MerchantAuthException("无权操作他人优惠券类型");
        }
        if (couponTypeMapper.countActiveStockByType(dto.getId()) > 0) {
            throw new RuntimeException("该类型正被进行中的发放活动使用，不可修改");
        }
        validateTypeRule(dto);
        CouponType type = new CouponType();
        type.setId(dto.getId());
        type.setName(dto.getName());
        type.setMinSpend(dto.getMinSpend());
        type.setDiscountAmount(dto.getDiscountAmount());
        type.setDiscountRate(dto.getDiscountRate());
        type.setValidDays(dto.getValidDays());
        type.setMaxUses(dto.getMaxUses() == null ? 0 : dto.getMaxUses());
        type.setCreateUserId(userId);
        couponTypeMapper.update(type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createStock(CouponStockDTO dto) {
        Integer userId = currentUserId();
        CouponType type = couponTypeMapper.findById(dto.getCouponTypeId());
        if (type == null) {
            throw new RuntimeException("优惠券类型不存在");
        }
        if (!type.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("无权使用他人优惠券类型");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("开始时间必须早于结束时间");
        }
        if (!dto.getEndTime().isAfter(now)) {
            throw new RuntimeException("结束时间必须晚于当前时间");
        }

        CouponStock stock = new CouponStock();
        stock.setCouponTypeId(dto.getCouponTypeId());
        stock.setTotalCount(dto.getTotalCount());
        stock.setRemainCount(dto.getTotalCount());
        stock.setUsedCount(0);
        stock.setStartTime(dto.getStartTime());
        stock.setEndTime(dto.getEndTime());
        stock.setStatus(now.isBefore(dto.getStartTime()) ? 0 : 1);
        stock.setCreateUserId(userId);
        couponStockMapper.insert(stock);

        // 同步写 Redis 库存（抢券 Lua 原子扣减的数据源），TTL 到活动结束后 1 小时
        try {
            stringRedisTemplate.opsForValue().set(CouponGrabServiceImpl.STOCK_KEY_PREFIX + stock.getId(),
                    String.valueOf(dto.getTotalCount()),
                    Duration.between(now, dto.getEndTime()).plusHours(1));
        } catch (Exception ignored) {
            // Redis 不可用时首次抢购会从 DB 初始化
        }
        evictStockListCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Integer stockId, Integer targetUserId) {
        Integer userId = currentUserId();
        CouponStock stock = couponStockMapper.findById(stockId);
        if (stock == null) {
            throw new RuntimeException("发放活动不存在");
        }
        if (!stock.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("无权操作他人发放活动");
        }
        // 懒刷新：0→1、1→2（防止开始/结束时间已到但状态未流转）
        couponStockStatusHelper.refreshStatus(stock);
        if (stock.getStatus() != 1) {
            throw new RuntimeException("发放活动未开始或已结束");
        }
        if (userMapper.findById(targetUserId) == null) {
            throw new RuntimeException("目标用户不存在");
        }

        // 扣减库存：Redis key 存在则 DECR，缺失则以 DB 为准重建
        String stockKey = CouponGrabServiceImpl.STOCK_KEY_PREFIX + stockId;
        try {
            Boolean exists = stringRedisTemplate.hasKey(stockKey);
            if (Boolean.TRUE.equals(exists)) {
                String v = stringRedisTemplate.opsForValue().get(stockKey);
                if (v == null || Long.parseLong(v) <= 0) {
                    throw new RuntimeException("优惠券库存不足");
                }
                stringRedisTemplate.opsForValue().decrement(stockKey);
            } else {
                if (stock.getRemainCount() == null || stock.getRemainCount() <= 0) {
                    throw new RuntimeException("优惠券库存不足");
                }
                stringRedisTemplate.opsForValue().set(stockKey,
                        String.valueOf(stock.getRemainCount() - 1),
                        Duration.between(LocalDateTime.now(), stock.getEndTime()).plusHours(1));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
            // Redis 不可用时以 DB 为准（学习项目容忍短暂不一致）
        }

        // 落库用户优惠券（uk_user_stock 保证同活动一人一张）
        CouponType type = couponTypeMapper.findById(stock.getCouponTypeId());
        UserCoupon uc = new UserCoupon();
        uc.setUserId(targetUserId);
        uc.setCouponTypeId(stock.getCouponTypeId());
        uc.setCouponStockId(stockId);
        uc.setStatus(0);
        uc.setStartTime(LocalDateTime.now());
        int validDays = type == null || type.getValidDays() == null ? 7 : type.getValidDays();
        uc.setEndTime(LocalDateTime.now().plusDays(validDays));
        try {
            userCouponMapper.insert(uc);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("该用户已领取过此活动的优惠券");
        }
        couponStockMapper.incrUsedCount(stockId);
        couponStockMapper.updateRemainCount(stockId,
                (stock.getRemainCount() == null ? 0 : stock.getRemainCount()) - 1);
        // 失效目标用户可用券缓存
        try {
            stringRedisTemplate.delete("coupon:user:available:" + targetUserId);
        } catch (Exception ignored) {
        }
    }

    @Override
    public PageBean<CouponStock> myStocks(Integer pageNum, Integer pageSize, Integer status) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<CouponStock> list = couponStockMapper.listOwn(userId, status);
        Page<CouponStock> page = (Page<CouponStock>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public CouponStatisticsVO statistics() {
        Integer userId = currentUserId();
        CouponStatisticsVO vo = new CouponStatisticsVO();
        vo.setTypeCount(couponTypeMapper.countByUser(userId));
        vo.setStockCount(couponStockMapper.countByUser(userId));
        vo.setGrantedCount(userCouponMapper.countGranted(userId));
        vo.setUsedCount(userCouponMapper.countUsed(userId));
        return vo;
    }

    /** 满减/折扣二选一校验 */
    private void validateTypeRule(CouponTypeDTO dto) {
        boolean fullReduce = dto.getDiscountAmount() != null && dto.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean discount = dto.getDiscountRate() != null
                && dto.getDiscountRate().compareTo(BigDecimal.ZERO) > 0
                && dto.getDiscountRate().compareTo(BigDecimal.ONE) < 0;
        if (fullReduce && discount) {
            throw new RuntimeException("满减券与折扣券规则只能二选一");
        }
        if (!fullReduce && !discount) {
            throw new RuntimeException("请配置优惠规则（满减金额或折扣率）");
        }
        if (fullReduce && (dto.getMinSpend() == null || dto.getMinSpend().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new RuntimeException("满减券必须配置最低消费");
        }
    }

    private void evictStockListCache() {
        try {
            var keys = stringRedisTemplate.keys(STOCK_LIST_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
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
