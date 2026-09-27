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
    Checked,
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
import {useRouter} from 'vue-router'
const router = useRouter();
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
        <el-aside width="200px">
            <div class="el-aside__logo"></div>
            <!-- element-plus的菜单标签 -->
            <el-menu active-text-color="#ffd04b" background-color="#232323"  text-color="#fff"
                router>
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
                <el-sub-menu index="mall">
                    <template #title>
                        <el-icon><Goods /></el-icon>
                        <span>商城</span>
                    </template>
                    <el-menu-item index="/mall">
                        <el-icon><HomeFilled /></el-icon>
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
                </el-sub-menu>
                <!-- 商家中心：认证通过展开完整功能，未通过只显示入驻入口 -->
                <el-sub-menu index="merchant">
                    <template #title>
                        <el-icon><Shop /></el-icon>
                        <span>商家中心</span>
                    </template>
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
                        <el-icon><Files /></el-icon>
                        <span>商家入驻</span>
                    </el-menu-item>
                </el-sub-menu>
                <!-- 平台管理：仅管理员可见（后端接口另有 @RequireAdmin 403 兜底） -->
                <el-sub-menu v-if="roleStore.isAdmin" index="admin">
                    <template #title>
                        <el-icon><Checked /></el-icon>
                        <span>平台管理</span>
                    </template>
                    <el-menu-item index="/admin/merchants">
                        <el-icon><Shop /></el-icon>
                        <span>商家审核</span>
                    </el-menu-item>
                    <el-menu-item index="/admin/product-categories">
                        <el-icon><Grid /></el-icon>
                        <span>商品分类</span>
                    </el-menu-item>
                </el-sub-menu>
                <el-sub-menu >
                    <template #title>
                        <el-icon><UserFilled /></el-icon>
                        <span>个人中心</span>
                    </template>
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
                </el-sub-menu>
            </el-menu>
        </el-aside>
        <!-- 右侧主区域 -->
        <el-container>
            <!-- 头部区域 -->
            <el-header>
                <div>小蓝书 · 社区：<strong>{{ userInfoStore.info.nickname }}</strong></div>
                <!-- 下拉菜单 -->
                <!-- command: 条目被点击后会触发,在事件函数上可以声明一个参数,接收条目对应的指令 -->
                <el-dropdown placement="bottom-end" @command="handleCommand">
                    <span class="el-dropdown__box">
                        <el-avatar :src="userInfoStore.info.userPic? userInfoStore.info.userPic:avatar" />
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
                <!-- <div style="width: 1290px; height: 570px;border: 1px solid red;">
                    内容展示区
                </div> -->
                <router-view></router-view>
            </el-main>
            <!-- 底部区域 -->
            <el-footer>小蓝书 · 社区 ©2026</el-footer>
        </el-container>
    </el-container>
</template>

<style lang="scss" scoped>
.layout-container {
    height: 100vh;

    .el-aside {
        background-color: #232323;

        &__logo {
            height: 120px;
            background: url('@/assets/logo.png') no-repeat center / 120px auto;
        }

        .el-menu {
            border-right: none;
        }
    }

    .el-header {
        background-color: #fff;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .el-dropdown__box {
            display: flex;
            align-items: center;

            .el-icon {
                color: #999;
                margin-left: 10px;
            }

            &:active,
            &:focus {
                outline: none;
            }
        }
    }

    .el-footer {
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        color: #666;
    }
}
</style>