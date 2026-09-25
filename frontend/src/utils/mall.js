/**
 * 商城团购模块公共常量与格式化方法（前端统一出口）。
 * 状态取值与后端 pojo 注释严格一致，避免各页面各写一套。
 */

// ---------- 订单 ----------
/** 订单状态：0待支付 1待发货 2已发货 3已完成 4已取消 5退款中 6已退款 */
export const ORDER_STATUS_MAP = {
    0: '待支付',
    1: '待发货',
    2: '已发货',
    3: '已完成',
    4: '已取消',
    5: '退款中',
    6: '已退款'
}
/** 订单状态对应的 el-tag 类型 */
export const ORDER_STATUS_TAG = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'success',
    4: 'info',
    5: 'warning',
    6: 'danger'
}
/** 订单类型：1普通订单 2团购订单 3秒杀订单(预留) */
export const ORDER_TYPE_MAP = { 1: '普通订单', 2: '团购订单', 3: '秒杀订单' }

// ---------- 退款单 ----------
/** 退款单状态：0待处理 1已通过 2已拒绝 3已退款 */
export const REFUND_STATUS_MAP = { 0: '待处理', 1: '已通过', 2: '已拒绝', 3: '已退款' }
export const REFUND_STATUS_TAG = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'success' }

// ---------- 团购 ----------
/** 团购状态：0未开始 1进行中 2已成团结束 3已关闭 */
export const GROUP_BUY_STATUS_MAP = { 0: '未开始', 1: '进行中', 2: '已成团', 3: '已关闭' }
export const GROUP_BUY_STATUS_TAG = { 0: 'info', 1: 'danger', 2: 'success', 3: 'info' }

// ---------- 抢券活动 ----------
/** 券活动状态：0未开始 1进行中 2已结束 3已售罄 */
export const STOCK_STATUS_MAP = { 0: '未开始', 1: '进行中', 2: '已结束', 3: '已售罄' }
export const STOCK_STATUS_TAG = { 0: 'info', 1: 'danger', 2: 'info', 3: 'warning' }

// ---------- 用户优惠券 ----------
/** 券状态：0未使用 1已使用 2已过期 */
export const COUPON_STATUS_MAP = { 0: '未使用', 1: '已使用', 2: '已过期' }
export const COUPON_STATUS_TAG = { 0: 'danger', 1: 'info', 2: 'info' }

// ---------- 商家认证 ----------
/** 商家状态：0待审核 1已通过 2已拒绝 3已封禁 4未申请 */
export const MERCHANT_STATUS_MAP = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已封禁', 4: '未申请' }
export const MERCHANT_STATUS_TAG = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'danger', 4: 'info' }

// ---------- 商品 / 分类 ----------
/** 商品状态：1上架 0下架 */
export const PRODUCT_STATUS_MAP = { 0: '已下架', 1: '已上架' }

/** 商品状态文案 */
export const productStatusText = (status) => textOf(PRODUCT_STATUS_MAP, status)

/** 取值助手：map 中不存在时返回占位 */
export const textOf = (map, key, fallback = '-') =>
    map[key] === undefined || map[key] === null ? fallback : map[key]

/** 金额显示：统一两位小数，空值按 0 处理 */
export const money = (v) => Number(v || 0).toFixed(2)

/** 时间显示：ISO/带T 字符串统一成 yyyy-MM-dd HH:mm:ss */
export const formatTime = (t) => (t ? String(t).replace('T', ' ') : '-')

/** 只显示到分钟：yyyy-MM-dd HH:mm */
export const formatTimeShort = (t) => (t ? String(t).replace('T', ' ').substring(0, 16) : '-')

/** 剩余时间文案（用于抢券/团购倒计时展示，非实时刷新） */
export const remainText = (endTime) => {
    if (!endTime) return '-'
    const diff = new Date(String(endTime).replace('T', ' ').replace(/-/g, '/')) - new Date()
    if (diff <= 0) return '已结束'
    const d = Math.floor(diff / 86400000)
    const h = Math.floor((diff % 86400000) / 3600000)
    const m = Math.floor((diff % 3600000) / 60000)
    if (d > 0) return `剩 ${d} 天 ${h} 小时`
    if (h > 0) return `剩 ${h} 小时 ${m} 分`
    return `剩 ${m} 分钟`
}

/** 优惠券面额文案：满减券/折扣券二选一 */
export const couponValueText = (coupon) => {
    const amount = Number(coupon?.discountAmount || 0)
    if (amount > 0) return `¥${money(amount)}`
    const rate = Number(coupon?.discountRate || 0)
    if (rate > 0 && rate < 1) return `${(rate * 10).toFixed(1)} 折`
    return '优惠券'
}

/** 优惠券使用门槛文案 */
export const couponThresholdText = (coupon) => {
    const min = Number(coupon?.minSpend || 0)
    return min > 0 ? `满 ${money(min)} 可用` : '无门槛'
}

/** 生成幂等键（下单/抢券用） */
export const genIdempotencyKey = () =>
    `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
