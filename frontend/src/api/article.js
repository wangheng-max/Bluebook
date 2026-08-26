import request from '@/utils/request.js'
//文章分类列表查询（社区化改造后为系统内置分类，Redis 缓存）
export const articleCategoryListService = ()=>{
    return request.get('/category/list')
}

//文章分类添加
export const articleCategoryAddService = (categoryData)=>{
    return request.post('/category',categoryData)
}

//文章分类修改
export const articleCategoryUpdateService = (categoryData)=>{
   return  request.put('/category',categoryData)
}

//文章分类删除
export const articleCategoryDeleteService = (id)=>{
    return request.delete('/category?id='+id)
}

//文章列表查询
export const articleListService = (params)=>{
   return  request.get('/article',{params:params})
}

//文章添加
export const articleAddService = (articleData)=>{
    return request.post('/article',articleData);

}

// ===== 互动接口（点赞/收藏/转发） =====
//点赞
export const articleLikeService = (id)=>{
    return request.post(`/article/${id}/like`)
}

//取消点赞
export const articleUnlikeService = (id)=>{
    return request.delete(`/article/${id}/like`)
}

//收藏
export const articleFavoriteService = (id)=>{
    return request.post(`/article/${id}/favorite`)
}

//取消收藏
export const articleUnfavoriteService = (id)=>{
    return request.delete(`/article/${id}/favorite`)
}

//互动状态（是否已赞/已藏 + 计数）
export const articleInteractStatusService = (id)=>{
    return request.get(`/article/${id}/interact-status`)
}

//转发给好友
export const articleForwardService = (id, data)=>{
    return request.post(`/article/${id}/forward`, data)
}