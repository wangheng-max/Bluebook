import request from '@/utils/request.js'

// 团购活动列表（公开分页）
export const groupBuyListService = (params) => {
    return request.get('/group-buy', { params })
}

// 团购详情（公开，含我的参团情况）
export const groupBuyDetailService = (id) => {
    return request.get(`/group-buy/${id}`)
}

// 我的团购进度
export const myGroupBuyProgressService = () => {
    return request.get('/group-buy/my-progress')
}

// 创建团购活动（商家）
export const groupBuyAddService = (data) => {
    return request.post('/group-buy', data)
}

// 修改团购活动（商家）
export const groupBuyUpdateService = (data) => {
    return request.put(`/group-buy/${data.id}`, data)
}

// 关闭团购活动（商家）
export const groupBuyCloseService = (id) => {
    return request.delete(`/group-buy/${id}`)
}

// 本店团购活动列表（商家）
export const merchantGroupBuyListService = (params) => {
    return request.get('/merchant/group-buy', { params })
}
