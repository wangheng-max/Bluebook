import request from '@/utils/request.js'

// 发送好友请求
export const friendSendRequestService = (targetUserId) => {
    return request.post('/friends/request', { targetUserId })
}

// 获取待确认的好友请求列表
export const friendPendingListService = (params) => {
    return request.get('/friends/pending', { params })
}

// 确认好友请求
export const friendConfirmService = (relationId) => {
    return request.put(`/friends/confirm/${relationId}`)
}

// 拒绝好友请求
export const friendRejectService = (relationId) => {
    return request.put(`/friends/reject/${relationId}`)
}

// 获取好友列表
export const friendListService = (params) => {
    return request.get('/friends/list', { params })
}

// 删除好友
export const friendDeleteService = (friendId) => {
    return request.delete(`/friends/${friendId}`)
}
