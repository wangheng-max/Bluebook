package com.wangheng.groupbuy.service.impl;

import com.wangheng.common.PageBean;
import com.wangheng.groupbuy.mapper.GroupBuyMapper;
import com.wangheng.groupbuy.mapper.GroupBuyRecordMapper;
import com.wangheng.groupbuy.pojo.GroupBuy;
import com.wangheng.groupbuy.pojo.GroupBuyDetailVO;
import com.wangheng.groupbuy.pojo.GroupBuyProgressVO;
import com.wangheng.groupbuy.pojo.GroupBuySaveDTO;
import com.wangheng.groupbuy.service.GroupBuyService;
import com.wangheng.product.mapper.ProductMapper;
import com.wangheng.product.pojo.Product;















import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.exception.MerchantAuthException;










import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 团购活动服务实现。
 * 状态懒刷新：读取时按时间自动推进（0未开始→1进行中→2成团/3未成团关闭），
 * 到期未成团的自动退款由订单模块 + 定时任务完成（见设计文档）。
 */
@Service
public class GroupBuyServiceImpl implements GroupBuyService {

    private static final String GROUP_BUY_KEY_PREFIX = "group-buy:";
    private static final String GROUP_BUY_LIST_KEY_PREFIX = "group-buy:list:";
    private static final long GROUP_BUY_TTL_MINUTES = 5;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyRecordMapper groupBuyRecordMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageBean<GroupBuy> pageList(Integer pageNum, Integer pageSize, Integer status) {
        // 公开列表缺省看进行中
        int st = status == null ? 1 : status;
        String cacheKey = GROUP_BUY_LIST_KEY_PREFIX + st;

        if (pageNum == 1) {
            PageBean<GroupBuy> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        PageHelper.startPage(pageNum, pageSize);
        List<GroupBuy> list = groupBuyMapper.list(st);
        // 懒刷新状态（时间推进自动流转）
        for (GroupBuy g : list) {
            refreshStatus(g);
        }
        Page<GroupBuy> page = (Page<GroupBuy>) list;
        PageBean<GroupBuy> bean = new PageBean<>(page.getTotal(), page.getResult());

        if (pageNum == 1) {
            writeCache(cacheKey, bean, GROUP_BUY_TTL_MINUTES);
        }
        return bean;
    }

    @Override
    public GroupBuyDetailVO detail(Integer id) {
        GroupBuy g = groupBuyMapper.findById(id);
        if (g == null) {
            throw new RuntimeException("团购活动不存在");
        }
        refreshStatus(g);

        Product product = productMapper.findById(g.getProductId());
        GroupBuyDetailVO vo = new GroupBuyDetailVO();
        vo.setId(g.getId());
        vo.setProductId(g.getProductId());
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setProductCover(product.getCoverImg());
            vo.setOriginPrice(product.getPrice());
        }
        vo.setTitle(g.getTitle());
        vo.setDescription(g.getDescription());
        vo.setGroupPrice(g.getGroupPrice());
        vo.setGroupSize(g.getGroupSize());
        vo.setMaxGroupSize(g.getMaxGroupSize());
        vo.setStartTime(g.getStartTime());
        vo.setEndTime(g.getEndTime());
        vo.setStatus(g.getStatus());
        // 进度以记录表实时统计为准（DB 列由定时任务对账）
        vo.setProgressCount(groupBuyRecordMapper.countByGroupBuyId(g.getId()));
        Integer myUserId = currentUserIdOrNull();
        if (myUserId != null) {
            vo.setJoined(groupBuyRecordMapper.countByUserAndGroupBuy(myUserId, g.getId()) > 0);
            vo.setMyGroupNum(groupBuyRecordMapper.sumGroupNum(myUserId, g.getId()));
        } else {
            vo.setJoined(false);
            vo.setMyGroupNum(0);
        }
        return vo;
    }

    @Override
    public PageBean<GroupBuy> myList(Integer pageNum, Integer pageSize) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<GroupBuy> list = groupBuyMapper.listJoined(userId);
        for (GroupBuy g : list) {
            refreshStatus(g);
        }
        Page<GroupBuy> page = (Page<GroupBuy>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    public List<GroupBuyProgressVO> myProgress() {
        Integer userId = currentUserId();
        List<GroupBuy> list = groupBuyMapper.listJoined(userId);
        List<GroupBuyProgressVO> result = new ArrayList<>();
        for (GroupBuy g : list) {
            refreshStatus(g);
            GroupBuyProgressVO vo = new GroupBuyProgressVO();
            vo.setId(g.getId());
            vo.setTitle(g.getTitle());
            vo.setGroupSize(g.getGroupSize());
            vo.setProgressCount(groupBuyRecordMapper.countByGroupBuyId(g.getId()));
            vo.setGroupPrice(g.getGroupPrice());
            vo.setEndTime(g.getEndTime());
            vo.setStatus(g.getStatus());
            vo.setMyGroupNum(groupBuyRecordMapper.sumGroupNum(userId, g.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(GroupBuySaveDTO dto) {
        Integer userId = currentUserId();
        Product product = productMapper.findById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("只能为本店商品创建团购");
        }
        validate(dto, product, null);

        GroupBuy g = new GroupBuy();
        fillEntity(g, dto);
        g.setCreateUserId(userId);
        g.setProgressCount(0);
        g.setStatus(LocalDateTime.now().isBefore(dto.getStartTime()) ? 0 : 1);
        groupBuyMapper.insert(g);
        evictCaches();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GroupBuySaveDTO dto) {
        Integer userId = currentUserId();
        if (dto.getId() == null) {
            throw new RuntimeException("团购活动ID不能为空");
        }
        GroupBuy exist = groupBuyMapper.findById(dto.getId());
        if (exist == null) {
            throw new RuntimeException("团购活动不存在");
        }
        if (!exist.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("无权操作他人团购活动");
        }
        if (exist.getStatus() != 0) {
            throw new RuntimeException("活动已开始，不可修改");
        }
        Product product = productMapper.findById(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        validate(dto, product, exist.getId());

        GroupBuy g = new GroupBuy();
        fillEntity(g, dto);
        g.setCreateUserId(userId);
        groupBuyMapper.update(g);
        evictCaches();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Integer id) {
        Integer userId = currentUserId();
        GroupBuy exist = groupBuyMapper.findById(id);
        if (exist == null) {
            throw new RuntimeException("团购活动不存在");
        }
        if (!exist.getCreateUserId().equals(userId)) {
            throw new MerchantAuthException("无权操作他人团购活动");
        }
        if (exist.getStatus() == 2) {
            throw new RuntimeException("活动已成团，不能关闭");
        }
        if (exist.getStatus() == 3) {
            throw new RuntimeException("活动已关闭");
        }
        // 关闭：进行中未成团 → 3（自动退款由订单模块定时任务处理）；未开始 → 直接关闭
        groupBuyMapper.updateStatus(id, 3);
        evictCaches();
    }

    @Override
    public PageBean<GroupBuy> myActivities(Integer pageNum, Integer pageSize, Integer status) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<GroupBuy> list = groupBuyMapper.listOwn(userId, status);
        for (GroupBuy g : list) {
            refreshStatus(g);
        }
        Page<GroupBuy> page = (Page<GroupBuy>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    /** 校验创建/修改参数（时间、价格、同商品重复开团） */
    private void validate(GroupBuySaveDTO dto, Product product, Integer excludeId) {
        LocalDateTime now = LocalDateTime.now();
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {
            throw new RuntimeException("开始时间必须早于结束时间");
        }
        if (!dto.getEndTime().isAfter(now)) {
            throw new RuntimeException("结束时间必须晚于当前时间");
        }
        if (dto.getGroupPrice().compareTo(product.getPrice()) >= 0) {
            throw new RuntimeException("团购价必须低于商品原价");
        }
        if (dto.getMaxGroupSize() != null && dto.getMaxGroupSize() < dto.getGroupSize()) {
            throw new RuntimeException("最大团购人数不能小于成团门槛");
        }
        // 同一商品不允许同时存在多个进行中/未开始的团购
        if (groupBuyMapper.countActiveByProduct(dto.getProductId()) > 0) {
            throw new RuntimeException("该商品已有进行中的团购活动");
        }
    }

    private void fillEntity(GroupBuy g, GroupBuySaveDTO dto) {
        g.setId(dto.getId());
        g.setProductId(dto.getProductId());
        g.setTitle(dto.getTitle());
        g.setDescription(dto.getDescription());
        g.setGroupPrice(dto.getGroupPrice());
        g.setGroupSize(dto.getGroupSize());
        g.setMaxGroupSize(dto.getMaxGroupSize() == null ? dto.getGroupSize() * 10 : dto.getMaxGroupSize());
        g.setStartTime(dto.getStartTime());
        g.setEndTime(dto.getEndTime());
    }

    /**
     * 状态懒刷新：0未开始→1进行中；进行中到期→2成团/3未成团关闭。
     * 未成团的自动退款由订单模块的定时任务统一处理。
     */
    private int refreshStatus(GroupBuy g) {
        LocalDateTime now = LocalDateTime.now();
        if (g.getStatus() != null && g.getStatus() == 0 && !now.isBefore(g.getStartTime())) {
            g.setStatus(1);
            groupBuyMapper.updateStatus(g.getId(), 1);
        }
        if (g.getStatus() != null && g.getStatus() == 1 && now.isAfter(g.getEndTime())) {
            int joined = groupBuyRecordMapper.countByGroupBuyId(g.getId());
            int newStatus = joined >= g.getGroupSize() ? 2 : 3;
            g.setStatus(newStatus);
            groupBuyMapper.updateStatus(g.getId(), newStatus);
        }
        return g.getStatus();
    }

    private void evictCaches() {
        try {
            var detailKeys = stringRedisTemplate.keys(GROUP_BUY_KEY_PREFIX + "*");
            var listKeys = stringRedisTemplate.keys(GROUP_BUY_LIST_KEY_PREFIX + "*");
            if (detailKeys != null && !detailKeys.isEmpty()) {
                stringRedisTemplate.delete(detailKeys);
            }
            if (listKeys != null && !listKeys.isEmpty()) {
                stringRedisTemplate.delete(listKeys);
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }

    private PageBean<GroupBuy> readCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<PageBean<GroupBuy>>() {});
            }
        } catch (Exception e) {
            // 数据损坏按未命中处理
        }
        return null;
    }

    private void writeCache(String key, PageBean<GroupBuy> bean, long ttlMinutes) {
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
