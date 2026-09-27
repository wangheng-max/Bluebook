import request from '@/utils/request.js'

// 热点文章分页
export const communityHotService = (params) => {
    return request.get('/community/hot', { params })
}

// 按分类查询已发布文章
export const communityArticlesService = (params) => {
    return request.get('/community/articles', { params })
}

// 文章详情（含计数）
export const communityArticleDetailService = (articleId) => {
    return request.get(`/community/article/${articleId}`)
}

// 热门标签 Top N（发布表单联想）
export const communityHotTagsService = (limit = 10) => {
    return request.get('/community/hot-tags', { params: { limit } })
}

// 某作者的已发布文章（博主主页作品列表，公开）
export const communityUserArticlesService = (userId, params) => {
    return request.get(`/community/user/${userId}/articles`, { params })
}
