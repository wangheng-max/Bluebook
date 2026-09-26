import request from '@/utils/request.js'

// 店铺信息 + 评分汇总（公开）
export const shopInfoService = (merchantUserId) => {
    return request.get(`/shop/${merchantUserId}`)
}

// 店铺全部在架商品（公开，分页）
export const shopProductsService = (merchantUserId, params) => {
    return request.get(`/shop/${merchantUserId}/products`, { params })
}

// 店铺买家评价（公开，分页）
export const shopRatingsService = (merchantUserId, params) => {
    return request.get(`/shop/${merchantUserId}/ratings`, { params })
}

// 提交商家评价（登录，订单维度一单一评）
export const rateMerchantService = (data) => {
    return request.post('/merchant/rating', data)
}

// 订单是否已评价
export const ratingExistsService = (orderId) => {
    return request.get('/merchant/rating/exists', { params: { orderId } })
}
