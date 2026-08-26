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
