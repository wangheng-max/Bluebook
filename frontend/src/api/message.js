import request from '@/utils/request.js'

// 与某好友的历史消息
export const messageHistoryService = (params) => {
    return request.get('/message/history', { params })
}

// 上线后拉取未读/离线消息
export const messageOfflineService = () => {
    return request.get('/message/offline')
}
