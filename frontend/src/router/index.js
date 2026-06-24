import { createRouter, createWebHistory } from 'vue-router'

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
            // 个人中心
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

//导出路由
export default router
