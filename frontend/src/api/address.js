import request from '@/utils/request.js'

// 收货地址列表
export const addressListService = () => {
    return request.get('/user/address/list')
}

// 添加收货地址
export const addressAddService = (data) => {
    return request.post('/user/address', data)
}

// 修改收货地址
export const addressUpdateService = (data) => {
    return request.put('/user/address', data)
}

// 删除收货地址
export const addressDeleteService = (id) => {
    return request.delete('/user/address', { params: { id } })
}
