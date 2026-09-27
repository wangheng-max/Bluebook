package com.wangheng.coupon.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.common.PageBean;
import com.wangheng.coupon.mapper.CouponStockMapper;
import com.wangheng.coupon.mapper.CouponTypeMapper;
import com.wangheng.coupon.mapper.UserCouponMapper;
import com.wangheng.coupon.pojo.CouponStock;
import com.wangheng.coupon.pojo.CouponStockVO;
import com.wangheng.coupon.pojo.CouponType;
import com.wangheng.coupon.pojo.UserCoupon;
import com.wangheng.coupon.pojo.UserCouponVO;
import com.wangheng.coupon.service.CouponGrabService;
import com.wangheng.coupon.service.CouponService;
import com.wangheng.coupon.util.CouponStockStatusHelper;
import com.wangheng.exception.MerchantAuthException;
import com.wangheng.utils.ThreadLocalUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;































/**
 * 优惠券服务实现（用户视角）。
 * 可用券缓存 10 分钟（coupon:user:available:{userId}），抢/退/用券时失效。
 */
@Service
public class CouponServiceImpl implements CouponService {

    private static final String USER_COUPON_CACHE_PREFIX = "coupon:user:available:";
    private static final String STOCK_LIST_CACHE_PREFIX = "coupon:stock:list:";
    private static final String STOCK_INFO_CACHE_PREFIX = "coupon:stock:info:";
    private static final long USER_COUPON_TTL_MINUTES = 10;
    private static final long STOCK_INFO_TTL_MINUTES = 10;
    private static final long STOCK_LIST_TTL_MINUTES = 5;

    @Autowired
    private CouponStockMapper couponStockMapper;

    @Autowired
    private CouponTypeMapper couponTypeMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponGrabService couponGrabService;

    @Autowired
    private CouponStockStatusHelper couponStockStatusHelper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<UserCouponVO> available(BigDecimal minSpend) {
        Integer userId = currentUserId();
        String cacheKey = USER_COUPON_CACHE_PREFIX + userId;

        List<UserCouponVO> list = null;
        try {
            String json = stringRedisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                list = objectMapper.readValue(json, new TypeReference<List<UserCouponVO>>() {});
            }
        } catch (Exception e) {
            // 缓存不可用回源
        }
        if (list == null) {
            list = userCouponMapper.listWithType(userId, 0);
            // 过滤已到期的（定时任务有 1 小时延迟，读时过滤保证准确）
            list.removeIf(c -> c.getEndTime() == null || c.getEndTime().isBefore(LocalDateTime.now()));
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(list),
                        USER_COUPON_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                // 缓存写入失败不影响业务
            }
        }
        // 最低消费过滤（满减券）
        if (minSpend != null) {
            List<UserCouponVO> filtered = new ArrayList<>();
            for (UserCouponVO c : list) {
                if (c.getMinSpend() == null || c.getMinSpend().compareTo(minSpend) <= 0) {
                    filtered.add(c);
                }
            }
            return filtered;
        }
        return list;
    }

    @Override
    public PageBean<UserCouponVO> myList(Integer pageNum, Integer pageSize, Integer status) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<UserCouponVO> list = userCouponMapper.listWithType(userId, status);
        Page<UserCouponVO> page = (Page<UserCouponVO>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public List<CouponType> typeList() {
        return couponTypeMapper.findAll();
    }

    @Override
    public PageBean<CouponStockVO> stockPageList(Integer pageNum, Integer pageSize, Integer status) {
        int st = status == null ? 1 : status;
        String cacheKey = STOCK_LIST_CACHE_PREFIX + st;

        if (pageNum == 1) {
            PageBean<CouponStockVO> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        PageHelper.startPage(pageNum, pageSize);
        List<CouponStock> list = couponStockMapper.list(st);
        List<CouponStockVO> voList = toVOList(list);
        Page<CouponStock> page = (Page<CouponStock>) list;
        PageBean<CouponStockVO> bean = new PageBean<>(page.getTotal(), voList);

        if (pageNum == 1) {
            writeCache(cacheKey, bean, STOCK_LIST_TTL_MINUTES);
        }
        return bean;
    }

    @Override
    public CouponStockVO stockDetail(Integer id) {
        String cacheKey = STOCK_INFO_CACHE_PREFIX + id;
        // 详情缓存只存"无登录态"的公共数据；登录态下的 grabbed 字段单独补
        CouponStock stock = couponStockMapper.findById(id);
        if (stock == null) {
            throw new RuntimeException("抢购活动不存在");
        }
        couponStockStatusHelper.refreshStatus(stock);
        CouponStockVO vo = toVO(stock);
        Integer userId = currentUserIdOrNull();
        vo.setGrabbed(userId != null && userCouponMapper.countByUserAndStock(userId, id) > 0);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo),
                    STOCK_INFO_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
        }
        return vo;
    }

    @Override
    public PageBean<UserCouponVO> myRecords(Integer pageNum, Integer pageSize) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<UserCouponVO> list = userCouponMapper.listWithType(userId, null);
        Page<UserCouponVO> page = (Page<UserCouponVO>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelGrab(Integer stockId) {
        Integer userId = currentUserId();
        UserCoupon uc = userCouponMapper.findActiveByUserAndStock(userId, stockId);
        if (uc == null) {
            throw new RuntimeException("未找到可取消的优惠券");
        }
        userCouponMapper.cancel(uc.getId(), userId);

        // 回补库存：Redis key 存在则 INCR，缺失则 DB 补偿（避免 INCR 凭空创建 key）
        String stockKey = CouponGrabServiceImpl.STOCK_KEY_PREFIX + stockId;
        try {
            Boolean exists = stringRedisTemplate.hasKey(stockKey);
            if (Boolean.TRUE.equals(exists)) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            } else {
                CouponStock stock = couponStockMapper.findById(stockId);
                if (stock != null) {
                    int remain = (stock.getRemainCount() == null ? 0 : stock.getRemainCount()) + 1;
                    couponStockMapper.updateRemainCount(stockId, remain);
                }
            }
        } catch (Exception ignored) {
            // 缓存不可用时由对账任务修正
        }
        // 失效用户可用券缓存
        try {
            stringRedisTemplate.delete(USER_COUPON_CACHE_PREFIX + userId);
        } catch (Exception ignored) {
        }
    }

    /** 实体列表 → VO 列表（联类型摘要 + 实时剩余 + 登录态抢购标记） */
    private List<CouponStockVO> toVOList(List<CouponStock> list) {
        Integer userId = currentUserIdOrNull();
        List<CouponStockVO> result = new ArrayList<>();
        for (CouponStock stock : list) {
            couponStockStatusHelper.refreshStatus(stock);
            CouponStockVO vo = toVO(stock);
            vo.setGrabbed(userId != null && userCouponMapper.countByUserAndStock(userId, stock.getId()) > 0);
            result.add(vo);
        }
        return result;
    }

    private CouponStockVO toVO(CouponStock stock) {
        CouponStockVO vo = new CouponStockVO();
        vo.setId(stock.getId());
        vo.setCouponTypeId(stock.getCouponTypeId());
        CouponType type = couponTypeMapper.findById(stock.getCouponTypeId());
        if (type != null) {
            vo.setTypeName(type.getName());
            vo.setMinSpend(type.getMinSpend());
            vo.setDiscountAmount(type.getDiscountAmount());
            vo.setDiscountRate(type.getDiscountRate());
            vo.setValidDays(type.getValidDays());
        }
        vo.setTotalCount(stock.getTotalCount());
        vo.setUsedCount(stock.getUsedCount());
        vo.setRemainCount(couponStockStatusHelper.liveRemain(stock.getId(), stock.getRemainCount()));
        vo.setStartTime(stock.getStartTime());
        vo.setEndTime(stock.getEndTime());
        vo.setStatus(stock.getStatus());
        vo.setGrabbed(false);
        return vo;
    }

    private PageBean<CouponStockVO> readCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<PageBean<CouponStockVO>>() {});
            }
        } catch (Exception e) {
            // 数据损坏按未命中处理
        }
        return null;
    }

    private void writeCache(String key, PageBean<CouponStockVO> bean, long ttlMinutes) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(bean),
                    ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
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

    private Integer currentUserIdOrNull() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return claims == null ? null : (Integer) claims.get("id");
    }
}
