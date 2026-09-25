import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import useRoleInfoStore from '@/stores/roleInfo.js'

//导入组件
import LoginVue from '@/views/Login.vue'
import LayoutVue from '@/views/Layout.vue'

import ArticleCategoryVue from '@/views/article/ArticleCategory.vue'
import ArticleManageVue from '@/views/article/ArticleManage.vue'
import UserAvatarVue from '@/views/user/UserAvatar.vue'
import UserInfoVue from '@/views/user/UserInfo.vue'
import UserResetPasswordVue from '@/views/user/UserResetPassword.vue'

// 社区 & 好友模块
import CommunityFeedVue from '@/views/community/CommunityFeed.vue'
import ArticleDetailVue from '@/views/community/ArticleDetail.vue'
import DiscoverUsersVue from '@/views/community/DiscoverUsers.vue'
import FriendListVue from '@/views/friend/FriendList.vue'
import FriendRequestsVue from '@/views/friend/FriendRequests.vue'

// 商城团购模块（用户侧）
import MallHomeVue from '@/views/mall/MallHome.vue'
import ProductDetailVue from '@/views/mall/ProductDetail.vue'
import GroupBuyListVue from '@/views/mall/GroupBuyList.vue'
import GroupBuyDetailVue from '@/views/mall/GroupBuyDetail.vue'
import CouponCenterVue from '@/views/mall/CouponCenter.vue'
import MyCouponVue from '@/views/mall/MyCoupon.vue'
import OrderConfirmVue from '@/views/mall/OrderConfirm.vue'
import MyOrderVue from '@/views/mall/MyOrder.vue'
import MyGroupBuyVue from '@/views/mall/MyGroupBuy.vue'
import UserAddressVue from '@/views/user/UserAddress.vue'

// 商城团购模块（商家侧）
import MerchantApplyVue from '@/views/merchant/MerchantApply.vue'
import MerchantHomeVue from '@/views/merchant/MerchantHome.vue'
import MerchantProductVue from '@/views/merchant/MerchantProduct.vue'
import MerchantGroupBuyVue from '@/views/merchant/MerchantGroupBuy.vue'
import MerchantCouponVue from '@/views/merchant/MerchantCoupon.vue'
import MerchantOrderVue from '@/views/merchant/MerchantOrder.vue'
import MerchantRefundVue from '@/views/merchant/MerchantRefund.vue'

// 商城团购模块（平台管理）
import AdminMerchantVue from '@/views/admin/AdminMerchant.vue'
import AdminProductCategoryVue from '@/views/admin/AdminProductCategory.vue'

//定义路由关系
const routes = [
    { path: '/login', component: LoginVue },
    {
        path: '/', component: LayoutVue, redirect: '/community/feed', children: [
            // 社区发现
            { path: '/community/feed', component: CommunityFeedVue },
            { path: '/community/discover', component: DiscoverUsersVue },
            { path: '/article/detail/:noteId', component: ArticleDetailVue },
            // 文章管理
            { path: '/article/category', component: ArticleCategoryVue },
            { path: '/article/manage', component: ArticleManageVue },
            // 好友
            { path: '/friend/list', component: FriendListVue },
            { path: '/friend/requests', component: FriendRequestsVue },
            // 商城（用户侧）
            { path: '/mall', component: MallHomeVue },
            { path: '/mall/product/:id', component: ProductDetailVue },
            { path: '/mall/group-buy', component: GroupBuyListVue },
            { path: '/mall/group-buy/:id', component: GroupBuyDetailVue },
            { path: '/mall/coupon', component: CouponCenterVue },
            { path: '/mall/my-coupon', component: MyCouponVue },
            { path: '/mall/order/confirm', component: OrderConfirmVue },
            { path: '/mall/my-order', component: MyOrderVue },
            { path: '/mall/my-group-buy', component: MyGroupBuyVue },
            // 商城（商家侧）
            { path: '/merchant/apply', component: MerchantApplyVue },
            { path: '/merchant/home', component: MerchantHomeVue },
            { path: '/merchant/products', component: MerchantProductVue },
            { path: '/merchant/group-buys', component: MerchantGroupBuyVue },
            { path: '/merchant/coupons', component: MerchantCouponVue },
            { path: '/merchant/orders', component: MerchantOrderVue },
            { path: '/merchant/refunds', component: MerchantRefundVue },
            // 商城（平台管理）
            { path: '/admin/merchants', component: AdminMerchantVue },
            { path: '/admin/product-categories', component: AdminProductCategoryVue },
            // 个人中心
            { path: '/user/address', component: UserAddressVue },
            { path: '/user/info', component: UserInfoVue },
            { path: '/user/avatar', component: UserAvatarVue },
            { path: '/user/resetPassword', component: UserResetPasswordVue }
        ]
    }
]

//创建路由器
const router = createRouter({
    history: createWebHistory(),
    routes: routes
})

//平台管理页面仅管理员可访问：非管理员直接输入网址也会被拦回社区首页
//（菜单可见性由 Layout 控制，这里防止绕过菜单直达；后端接口另有 @RequireAdmin 403 兜底）
router.beforeEach(async (to) => {
    if (to.path.startsWith('/admin')) {
        const roleStore = useRoleInfoStore()
        if (!roleStore.loaded) {
            await roleStore.loadAdminStatus()
        }
        if (!roleStore.isAdmin) {
            ElMessage.warning('平台管理仅管理员可访问')
            return '/community/feed'
        }
    }
})

//导出路由器
export default router
