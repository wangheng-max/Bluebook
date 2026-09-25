import request from '@/utils/request.js'

// 当前登录用户是否管理员（前端据此决定是否渲染"平台管理"菜单，不包含管理员账号信息）
export const adminStatusService = () => {
    return request.get('/admin/status')
}

// 商家审核列表（分页；params.status 可选：0待审核 1通过 2拒绝 3封禁，不传=全部）
export const adminMerchantListService = ({ pageNum = 1, pageSize = 10, status = null } = {}) => {
    return request.get('/admin/merchant/audit-list', {
        params: {
            pageNum,
            pageSize,
            status: status === '' ? null : status
        }
    })
}

// 审核商家（approved: true 通过 / false 拒绝）
export const adminMerchantAuditService = (userId, approved, auditRemark) => {
    return request.post(`/admin/merchant/${userId}/audit`, { approved, auditRemark })
}

// 封禁商家
export const adminMerchantBanService = (userId) => {
    return request.post(`/admin/merchant/${userId}/ban`)
}
