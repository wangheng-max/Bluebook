import request from '@/utils/request.js'

// ---------- 商品（公开读 / 商家写） ----------

// 商品列表（分页，可传 categoryId、status）
export const productListService = (params) => {
    return request.get('/product', { params })
}

// 商品搜索（关键词、分类、价格区间）
export const productSearchService = (params) => {
    return request.get('/product/search', { params })
}

// 商品详情
export const productDetailService = (id) => {
    return request.get(`/product/${id}`)
}

// 商品 SKU 列表（规格选择 / 商家编辑回显）
export const productSkuListService = (id) => {
    return request.get(`/product/${id}/sku`)
}

// 发布商品（商家）
export const productAddService = (data) => {
    return request.post('/product', data)
}

// 修改商品（商家）
export const productUpdateService = (data) => {
    return request.put('/product', data)
}

// 下架商品（商家）
export const productOffShelfService = (id) => {
    return request.delete(`/product/${id}`)
}

// 本店商品列表（商家，含下架）
export const merchantProductListService = (params) => {
    return request.get('/merchant/product', { params })
}

// ---------- 商品分类 ----------

// 分类平铺列表（公开）
export const productCategoryListService = () => {
    return request.get('/product/category/list')
}

// 分类树（公开）
export const productCategoryTreeService = () => {
    return request.get('/product/category/tree')
}

// 添加分类（管理员）
export const productCategoryAddService = (data) => {
    return request.post('/product/category', data)
}

// 修改分类（管理员）
export const productCategoryUpdateService = (data) => {
    return request.put('/product/category', data)
}

// 删除分类（管理员）
export const productCategoryDeleteService = (id) => {
    return request.delete('/product/category', { params: { id } })
}
