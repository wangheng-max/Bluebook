import request from '@/utils/request.js'

// 评论分页（targetType: article/product；sort: time-按时间 / likes-按点赞）
export const commentListService = (params) => {
    return request.get('/comment/list', { params })
}

// 发布评论/回复（parentId 不传=顶级评论）
export const commentAddService = (data) => {
    return request.post('/comment', data)
}

// 某顶级评论下的回复分页（按时间正序）
export const commentRepliesService = (rootId, params) => {
    return request.get(`/comment/${rootId}/replies`, { params })
}

// 点赞评论，返回最新点赞数
export const commentLikeService = (id) => {
    return request.post(`/comment/${id}/like`)
}

// 取消点赞
export const commentUnlikeService = (id) => {
    return request.delete(`/comment/${id}/like`)
}

// 删除自己的评论（顶级评论连带全部回复）
export const commentDeleteService = (id) => {
    return request.delete(`/comment/${id}`)
}

// 商品评分统计（公开）：{ avgScore, scoreCount }
export const commentStatsService = (params) => {
    return request.get('/comment/stats', { params })
}
