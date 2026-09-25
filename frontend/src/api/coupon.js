import request from '@/utils/request.js'

// ---------- 用户侧 ----------

// 抢购活动列表（公开分页，status 缺省进行中）
export const couponStockListService = (params) => {
    return request.get('/coupon/stock/list', { params })
}

// 抢购活动详情（公开，剩余数量实时）
export const couponStockDetailService = (id) => {
    return request.get(`/coupon/stock/${id}`)
}

// 抢券（已登录；后端 Lua 原子扣减 + 防重 + 限流）
export const couponGrabService = (id) => {
    return request.post(`/coupon/stock/${id}/grab`)
}

// 取消抢购（券作废，见 Bug 清单 B-003）
export const couponCancelService = (id) => {
    return request.put(`/coupon/stock/${id}/cancel`)
}

// 我的抢购记录
export const myGrabRecordService = (params) => {
    return request.get('/coupon/stock/my-records', { params })
}

// 我的优惠券（status：0未使用 1已使用 2已过期）
export const myCouponListService = (params) => {
    return request.get('/coupon/my-list', { params })
}

// 我的可用优惠券（下单页选择用，可传 minSpend 门槛）
export const availableCouponService = (params) => {
    return request.get('/coupon/available', { params })
}

// 优惠券类型列表（公开）
export const couponTypeListService = () => {
    return request.get('/coupon/type/list')
}

// ---------- 商家侧 ----------

// 创建券类型（商家）
export const couponTypeAddService = (data) => {
    return request.post('/merchant/coupon/type', data)
}

// 修改券类型（商家）
export const couponTypeUpdateService = (data) => {
    return request.put(`/merchant/coupon/type/${data.id}`, data)
}

// 创建发放活动（商家）
export const couponStockAddService = (data) => {
    return request.post('/merchant/coupon/stock', data)
}

// 本店发放活动列表（商家）
export const merchantCouponStockListService = (params) => {
    return request.get('/merchant/coupon/stock', { params })
}

// 定向发放给指定用户（商家）
export const couponGrantService = (stockId, userId) => {
    return request.post(`/merchant/coupon/${stockId}/grant`, { userId })
}

// 本店优惠券统计（商家）
export const merchantCouponStatisticsService = () => {
    return request.get('/merchant/coupon/statistics')
}
