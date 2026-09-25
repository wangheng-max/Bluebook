import request from '@/utils/request.js'

// 提交商家认证申请
export const merchantApplyService = (data) => {
    return request.post('/merchant/apply', data)
}

// 被拒绝后重新提交申请
export const merchantReSubmitService = (data) => {
    return request.post('/merchant/apply/re-submit', data)
}

// 我的商家认证状态（前端据此显示"商家中心"入口）
export const merchantStatusService = () => {
    return request.get('/merchant/status')
}

// 店铺信息
export const merchantShopInfoService = () => {
    return request.get('/merchant/info')
}

// 修改店铺信息（认证通过后可用）
export const merchantShopInfoUpdateService = (data) => {
    return request.put('/merchant/info', data)
}
