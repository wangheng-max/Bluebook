import request from '@/utils/request.js'

// 搜索公开笔记（关键词）
export const searchNotesService = (params) => {
    return request.get('/search/notes', { params })
}

// 按标签搜索公开笔记
export const searchNotesByTagService = (params) => {
    return request.get('/search/notes/by-tag', { params })
}

// 搜索用户
export const searchUsersService = (params) => {
    return request.get('/search/users', { params })
}
