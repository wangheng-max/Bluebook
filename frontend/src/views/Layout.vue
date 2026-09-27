<script setup>
import {
    Management,
    Promotion,
    UserFilled,
    User,
    Crop,
    EditPen,
    SwitchButton,
    CaretBottom,
    HomeFilled,
    Search,
    ChatDotSquare,
    Connection,
    Goods,
    Ticket,
    List,
    Location,
    Shop,
    Sell,
    Discount,
    ShoppingCart,
    Grid,
    Files
} from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'

import {userInfoService} from '@/api/user.js'
import useUserInfoStore from '@/stores/userInfo.js'
import {useTokenStore} from '@/stores/token.js'
import useRoleInfoStore from '@/stores/roleInfo.js'
import { merchantStatusService } from '@/api/merchant.js'
import { ref } from 'vue'
const tokenStore = useTokenStore();
const userInfoStore = useUserInfoStore();
const roleStore = useRoleInfoStore();

//管理员身份：仅管理员（user_role.role_type=3）渲染"平台管理"菜单
const loadAdminStatus = async () => {
    await roleStore.loadAdminStatus()
}
loadAdminStatus()

//商家认证状态：认证通过才展开完整商家中心，否则只显示"商家入驻"
const isMerchant = ref(false)
const loadMerchantStatus = async () => {
    try {
        const result = await merchantStatusService()
        isMerchant.value = !!result.data && result.data.merchantStatus === 1
    } catch (e) {
        isMerchant.value = false
    }
}
loadMerchantStatus()
//调用函数,获取用户详细信息
const getUserInfo = async()=>{
    //调用接口
    let result = await userInfoService();
    //数据存储到pinia中
    userInfoStore.setInfo(result.data);
}

getUserInfo();
//条目被点击后,调用的函数
import {useRouter, useRoute} from 'vue-router'
const router = useRouter();
const route = useRoute();
import {ElMessage,ElMessageBox} from 'element-plus'
const handleCommand = (command)=>{
    //判断指令
    if(command === 'logout'){
        //退出登录
        ElMessageBox.confirm(
        '您确认要退出吗?',
        '温馨提示',
        {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            type: 'warning',
        }
    )
        .then(async () => {
            //退出登录
            //1.清空pinia中存储的token、个人信息以及角色状态
            tokenStore.removeToken()
            userInfoStore.removeInfo()
            roleStore.reset()

            //2.跳转到登录页面
            router.push('/login')
            ElMessage({
                type: 'success',
                message: '退出登录成功',
            })
            
        })
        .catch(() => {
            ElMessage({
                type: 'info',
                message: '用户取消了退出登录',
            })
        })
    }else{
        //路由
        router.push('/user/'+command)
    }
}
</script>

<template>
    <!-- element-plus中的容器 -->
    <el-container class="layout-container">
        <!-- 左侧菜单 -->
        <el-aside width="220px">
            <div class="brand-bar">
                <div class="brand-icon">蓝</div>
                <div class="brand-text">
                    <div class="brand-name">小蓝书<span class="brand-badge">BLUE BOOK</span></div>
                    <div class="brand-slogan">知识与生活的灵感集</div>
                </div>
            </div>
            <!-- element-plus的菜单标签（default-active 绑定当前路由，刷新后高亮不丢） -->
            <el-menu router :default-active="route.path">
                <div class="menu-group-label">社区</div>
                <el-menu-item index="/community/feed">
                    <el-icon><HomeFilled /></el-icon>
                    <span>社区首页</span>
                </el-menu-item>
                <el-menu-item index="/community/discover">
                    <el-icon><Search /></el-icon>
                    <span>发现用户</span>
                </el-menu-item>
                <el-menu-item index="/article/manage">
                    <el-icon><Promotion /></el-icon>
                    <span>写笔记</span>
                </el-menu-item>
                <el-menu-item index="/article/category">
                    <el-icon><Management /></el-icon>
                    <span>笔记分类</span>
                </el-menu-item>
                <el-menu-item index="/friend/list">
                    <el-icon><Connection /></el-icon>
                    <span>我的好友</span>
                </el-menu-item>
                <el-menu-item index="/friend/requests">
                    <el-icon><ChatDotSquare /></el-icon>
                    <span>好友请求</span>
                </el-menu-item>
                <!-- 商城（用户侧） -->
                <div class="menu-group-label">商城</div>
                <el-menu-item index="/mall">
                    <el-icon><Goods /></el-icon>
                    <span>商城首页</span>
                </el-menu-item>
                <el-menu-item index="/mall/group-buy">
                    <el-icon><Sell /></el-icon>
                    <span>团购专区</span>
                </el-menu-item>
                <el-menu-item index="/mall/coupon">
                    <el-icon><Ticket /></el-icon>
                    <span>抢券中心</span>
                </el-menu-item>
                <el-menu-item index="/mall/my-order">
                    <el-icon><List /></el-icon>
                    <span>我的订单</span>
                </el-menu-item>
                <el-menu-item index="/mall/my-coupon">
                    <el-icon><Discount /></el-icon>
                    <span>我的优惠券</span>
                </el-menu-item>
                <el-menu-item index="/mall/my-group-buy">
                    <el-icon><ShoppingCart /></el-icon>
                    <span>我的团购</span>
                </el-menu-item>
                <el-menu-item index="/user/address">
                    <el-icon><Location /></el-icon>
                    <span>收货地址</span>
                </el-menu-item>
                <!-- 商家中心：认证通过展开完整功能，未通过只显示入驻入口 -->
                <div class="menu-group-label">经营</div>
                <template v-if="isMerchant">
                    <el-menu-item index="/merchant/home">
                        <el-icon><Shop /></el-icon>
                        <span>商家概览</span>
                    </el-menu-item>
                    <el-menu-item index="/merchant/products">
                        <el-icon><Goods /></el-icon>
                        <span>商品管理</span>
                    </el-menu-item>
                    <el-menu-item index="/merchant/group-buys">
                        <el-icon><Sell /></el-icon>
                        <span>团购管理</span>
                    </el-menu-item>
                    <el-menu-item index="/merchant/coupons">
                        <el-icon><Ticket /></el-icon>
                        <span>优惠券管理</span>
                    </el-menu-item>
                    <el-menu-item index="/merchant/orders">
                        <el-icon><List /></el-icon>
                        <span>订单管理</span>
                    </el-menu-item>
                    <el-menu-item index="/merchant/refunds">
                        <el-icon><Files /></el-icon>
                        <span>退款审核</span>
                    </el-menu-item>
                </template>
                <el-menu-item v-else index="/merchant/apply">
                    <el-icon><Shop /></el-icon>
                    <span>商家入驻</span>
                </el-menu-item>
                <!-- 平台管理：仅管理员可见（后端接口另有 @RequireAdmin 403 兜底） -->
                <template v-if="roleStore.isAdmin">
                    <div class="menu-group-label">平台</div>
                    <el-menu-item index="/admin/merchants">
                        <el-icon><Shop /></el-icon>
                        <span>商家审核</span>
                    </el-menu-item>
                    <el-menu-item index="/admin/product-categories">
                        <el-icon><Grid /></el-icon>
                        <span>商品分类</span>
                    </el-menu-item>
                </template>
                <div class="menu-group-label">账号</div>
                <el-menu-item index="/user/info">
                    <el-icon><User /></el-icon>
                    <span>基本资料</span>
                </el-menu-item>
                <el-menu-item index="/user/avatar">
                    <el-icon><Crop /></el-icon>
                    <span>更换头像</span>
                </el-menu-item>
                <el-menu-item index="/user/resetPassword">
                    <el-icon><EditPen /></el-icon>
                    <span>重置密码</span>
                </el-menu-item>
            </el-menu>
        </el-aside>
        <!-- 右侧主区域 -->
        <el-container>
            <!-- 头部区域 -->
            <el-header>
                <div class="header-title">
                    <span class="hello">你好，<strong>{{ userInfoStore.info.nickname }}</strong></span>
                    <span class="sub">今天想记录或发现点什么</span>
                </div>
                <!-- 下拉菜单 -->
                <!-- command: 条目被点击后会触发,在事件函数上可以声明一个参数,接收条目对应的指令 -->
                <el-dropdown placement="bottom-end" @command="handleCommand">
                    <span class="el-dropdown__box">
                        <el-avatar :size="34" :src="userInfoStore.info.userPic? userInfoStore.info.userPic:avatar" />
                        <span class="dropdown-name">{{ userInfoStore.info.nickname }}</span>
                        <el-icon>
                            <CaretBottom />
                        </el-icon>
                    </span>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="info" :icon="User">基本资料</el-dropdown-item>
                            <el-dropdown-item v-if="userInfoStore.info?.id"
                                :command="'homepage/' + userInfoStore.info.id"
                                :icon="UserFilled">个人主页</el-dropdown-item>
                            <el-dropdown-item command="avatar" :icon="Crop">更换头像</el-dropdown-item>
                            <el-dropdown-item command="resetPassword" :icon="EditPen">重置密码</el-dropdown-item>
                            <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </el-header>
            <!-- 中间区域 -->
            <el-main>
                <router-view v-slot="{ Component }">
                    <transition name="fade-slide" mode="out-in">
                        <component :is="Component" />
                    </transition>
                </router-view>
            </el-main>
            <!-- 底部区域 -->
            <el-footer>小蓝书 · 知识与生活的灵感集 ©2026</el-footer>
        </el-container>
    </el-container>
</template>

<style lang="scss" scoped>
@use '../assets/styles/tokens.scss' as *;

.layout-container {
    height: 100vh;

    .el-aside {
        display: flex;
        flex-direction: column;
        background-color: $bg-color-container;
        border-right: 1px solid $border-color-lighter;
        overflow-y: auto;

        .brand-bar {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 18px 20px 14px;

            .brand-icon {
                width: 40px;
                height: 40px;
                display: flex;
                align-items: center;
                justify-content: center;
                background: linear-gradient(135deg, $color-primary 0%, #1a56db 100%);
                border-radius: $radius-medium;
                color: #fff;
                font-size: 20px;
                font-weight: 800;
                box-shadow: 0 4px 10px rgba(51, 112, 255, 0.35);
                flex-shrink: 0;
            }

            .brand-name {
                font-size: 17px;
                font-weight: 800;
                color: $text-primary;
                letter-spacing: -0.3px;
                display: flex;
                align-items: center;

                .brand-badge {
                    font-size: 9px;
                    font-weight: 700;
                    color: $color-primary;
                    background: $color-primary-light;
                    padding: 1px 5px;
                    border-radius: 4px;
                    margin-left: 6px;
                    letter-spacing: 0.5px;
                }
            }

            .brand-slogan {
                font-size: 11px;
                color: $text-secondary;
                margin-top: 2px;
                letter-spacing: 0.4px;
            }
        }

        .menu-group-label {
            font-size: 11px;
            font-weight: 600;
            color: $text-placeholder;
            letter-spacing: 1px;
            padding: 14px 22px 4px;
            user-select: none;
        }

        .el-menu {
            border-right: none;
            padding-bottom: 16px;
        }
    }

    .el-header {
        height: 60px;
        background-color: $bg-color-container;
        border-bottom: 1px solid $border-color-lighter;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .header-title {
            display: flex;
            align-items: baseline;
            gap: 12px;

            .hello {
                font-size: 15px;
                color: $text-primary;

                strong {
                    font-weight: 700;
                }
            }

            .sub {
                font-size: 12.5px;
                color: $text-secondary;
            }
        }

        .el-dropdown__box {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 4px 8px;
            border-radius: $radius-base;
            cursor: pointer;
            transition: $transition-base;

            &:hover {
                background-color: $bg-color-hover;
            }

            .dropdown-name {
                font-size: 13.5px;
                font-weight: 600;
                color: $text-primary;
            }

            .el-icon {
                color: $text-secondary;
                font-size: 14px;
            }

            &:active,
            &:focus {
                outline: none;
            }
        }
    }

    .el-main {
        background-color: $bg-color-base;
        padding: 20px;
    }

    .el-footer {
        height: 44px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12.5px;
        color: $text-secondary;
        background-color: $bg-color-container;
        border-top: 1px solid $border-color-lighter;
    }
}
</style>