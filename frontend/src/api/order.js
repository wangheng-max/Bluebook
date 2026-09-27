import request from '@/utils/request.js'

// ---------- 用户侧 ----------

// 创建订单（带幂等键，重复提交返回首次结果）
export const orderCreateService = (data, idempotencyKey) => {
    return request.post('/order', data, {
        headers: { 'Idempotency-Key': idempotencyKey }
    })
}

// 我的订单列表（status 可选筛选）
export const orderListService = (params) => {
    return request.get('/order/my-list', { params })
}

// 订单详情
export const orderDetailService = (id) => {
    return request.get(`/order/${id}`)
}

// 我的退款单列表
export const myRefundListService = (params) => {
    return request.get('/order/refund-list', { params })
}

// 支付订单（模拟支付：payType 1微信 2支付宝）
export const orderPayService = (id, payType = 1) => {
    return request.post(`/order/${id}/pay`, { payType })
}

// 取消订单
export const orderCancelService = (id) => {
    return request.put(`/order/${id}/cancel`)
}

// 申请退款
export const orderRefundService = (id, refundReason) => {
    return request.post(`/order/${id}/refund`, { refundReason })
}

// ---------- 商家侧 ----------

// 本店订单列表
export const merchantOrderListService = (params) => {
    return request.get('/merchant/order', { params })
}

// 本店订单详情
export const merchantOrderDetailService = (id) => {
    return request.get(`/merchant/order/${id}`)
}

// 发货
export const merchantShipService = (id) => {
    return request.post(`/merchant/order/${id}/ship`)
}

// 本店退款申请列表
export const merchantRefundListService = (params) => {
    return request.get('/merchant/refund/list', { params })
}

// 退款审核（approved: true 通过 / false 拒绝）
export const merchantRefundAuditService = (id, approved, remark) => {
    return request.post(`/merchant/refund/${id}/audit`, { approved, remark })
}

// 我购买某商品的订单明细（商品评论晒单用，新的在前）
export const myPurchasedService = (productId) => {
    return request.get('/order/purchased', { params: { productId } })
}
