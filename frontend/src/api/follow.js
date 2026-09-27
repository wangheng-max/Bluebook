import request from '@/utils/request.js'

// 关注（type: 1=博主 2=店铺）
export const followService = (type, targetId) => {
    return request.post(`/follow/${type}/${targetId}`)
}

// 取消关注
export const unfollowService = (type, targetId) => {
    return request.delete(`/follow/${type}/${targetId}`)
}

// 关注状态 + 粉丝数（公开）
export const followStatusService = (type, targetId) => {
    return request.get('/follow/status', { params: { type, targetId } })
}
