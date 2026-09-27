<script setup>
import { User, Lock } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
//控制注册与登录表单的显示， 默认显示注册
const isRegister = ref(false)
//定义数据模型
const registerData = ref({
    username: '',
    password: '',
    rePassword: ''
})

//校验密码的函数
const checkRePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次确认密码'))
    } else if (value !== registerData.value.password) {
        callback(new Error('请确保两次输入的密码一样'))
    } else {
        callback()
    }
}

//密码格式：6-16位，须同时包含字母和数字（与后端校验一致，登录不校验格式，兼容老账号）
const PWD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)\S{6,16}$/

//注册表单校验规则（密码带格式要求）
const registerRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { pattern: PWD_PATTERN, message: '密码为6-16位，且必须同时包含字母和数字', trigger: 'blur' }
    ],
    rePassword: [
        { validator: checkRePassword, trigger: 'blur' }
    ]
}

//登录表单校验规则（老账号密码可能是旧格式，只做基本长度校验）
const loginRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
    ]
}

//调用后台接口,完成注册
import { userRegisterService, userLoginService} from '@/api/user.js'
const register = async () => {
    //registerData是一个响应式对象,如果要获取值,需要.value
    let result = await userRegisterService(registerData.value);
    /* if (result.code === 0) {
        //成功了
        alert(result.msg ? result.msg : '注册成功');
    }else{
        //失败了
        alert('注册失败')
    } */
    //alert(result.msg ? result.msg : '注册成功');
    ElMessage.success(result.msg ? result.msg : '注册成功')
}

//绑定数据,复用注册表单的数据模型
//表单数据校验
//登录函数
import {useTokenStore} from '@/stores/token.js'
import {useRouter} from 'vue-router'
const router = useRouter()
const tokenStore = useTokenStore();
const login = async () => {
    try {
        let result = await userLoginService(registerData.value);
        if (result.code === 0) {
            ElMessage.success('登录成功');
            tokenStore.setToken(result.data);
            await router.push('/');
        } else {
            ElMessage.error(result.msg || '登录失败');
            // 可选：清空密码
            registerData.value.password = '';
        }
    } catch (err) {
        ElMessage.error('网络错误');
    }
};
//定义函数,清空数据模型的数据
const clearRegisterData = ()=>{
    registerData.value={
        username:'',
        password:'',
        rePassword:''
    }
}
</script>

<template>
    <div class="login-container">
        <!-- 柔和流体光斑背景 -->
        <div class="bg-glowing-blob blob-1"></div>
        <div class="bg-glowing-blob blob-2"></div>

        <div class="login-card-wrapper">
            <!-- 左侧品牌展示区 -->
            <div class="brand-panel">
                <div class="brand-header">
                    <div class="brand-mark">
                        <div class="brand-icon">蓝</div>
                        <h1 class="brand-title">小蓝书<span class="brand-badge">BLUE BOOK</span></h1>
                    </div>
                    <p class="brand-description">知识与生活的灵感集 · 记录 · 分享 · 好物</p>
                </div>
                <div class="brand-illustration">
                    <img src="@/assets/login_bg.png" alt="小蓝书" />
                </div>
                <ul class="brand-points">
                    <li>写笔记、逛社区，发现同频的人</li>
                    <li>文章带货直达商城，好物即看即买</li>
                    <li>关注博主与店铺，动态不错过</li>
                </ul>
            </div>

            <!-- 右侧表单区 -->
            <div class="form-panel">
                <div class="welcome-header">
                    <span class="hello">Hello,</span>
                    <span class="tips">{{ isRegister ? '加入小蓝书！' : '欢迎回到小蓝书！' }}</span>
                </div>

                <!-- 注册表单 -->
                <el-form ref="form" size="large" autocomplete="off" v-if="isRegister" :model="registerData"
                    :rules="registerRules" class="auth-form">
                    <el-form-item prop="username">
                        <el-input :prefix-icon="User" placeholder="请输入用户名（5-16位）"
                            v-model="registerData.username"></el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input :prefix-icon="Lock" type="password" show-password placeholder="请输入密码"
                            v-model="registerData.password"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <div class="pwd-tip">密码由 6-16 位字符组成，须同时包含字母和数字，例如：abc12345</div>
                    </el-form-item>
                    <el-form-item prop="rePassword">
                        <el-input :prefix-icon="Lock" type="password" show-password placeholder="请输入再次密码"
                            v-model="registerData.rePassword"></el-input>
                    </el-form-item>
                    <!-- 注册按钮 -->
                    <el-form-item>
                        <el-button class="submit-btn" type="primary" auto-insert-space @click="register">
                            注 册
                        </el-button>
                    </el-form-item>
                    <div class="form-footer-links">
                        <span class="link-item" @click="isRegister = false;clearRegisterData()">已有账号，去登录</span>
                    </div>
                </el-form>
                <!-- 登录表单 -->
                <el-form ref="form" size="large" autocomplete="off" v-else :model="registerData" :rules="loginRules"
                    class="auth-form">
                    <el-form-item prop="username">
                        <el-input :prefix-icon="User" placeholder="请输入用户名" v-model="registerData.username"></el-input>
                    </el-form-item>
                    <el-form-item prop="password">
                        <el-input name="password" :prefix-icon="Lock" type="password" show-password
                            placeholder="请输入密码" v-model="registerData.password"></el-input>
                    </el-form-item>
                    <el-form-item class="flex">
                        <div class="flex">
                            <el-checkbox>记住我</el-checkbox>
                            <el-link type="primary" :underline="false">忘记密码？</el-link>
                        </div>
                    </el-form-item>
                    <!-- 登录按钮 -->
                    <el-form-item>
                        <el-button class="submit-btn" type="primary" auto-insert-space @click="login">登 录</el-button>
                    </el-form-item>
                    <div class="form-footer-links">
                        <span class="link-item" @click="isRegister = true;clearRegisterData()">还没有账号？立即注册</span>
                    </div>
                </el-form>
            </div>
        </div>

        <div class="copyright">小蓝书 · 知识与生活的灵感集 ©2026</div>
    </div>
</template>

<style lang="scss" scoped>
@use '../assets/styles/tokens.scss' as *;

.login-container {
    height: 100vh;
    width: 100vw;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f7f9fa;
    position: relative;
    overflow: hidden;
}

// 柔和炫彩流体背景光斑
.bg-glowing-blob {
    position: absolute;
    border-radius: 50%;
    filter: blur(120px);
    z-index: 1;
    opacity: 0.65;
    pointer-events: none;

    &.blob-1 {
        width: 600px;
        height: 600px;
        background: radial-gradient(circle, rgba(51, 112, 255, 0.28) 0%, rgba(121, 194, 243, 0.05) 70%);
        top: -150px;
        left: -150px;
        animation: blobFloat1 25s ease-in-out infinite;
    }

    &.blob-2 {
        width: 700px;
        height: 700px;
        background: radial-gradient(circle, rgba(247, 185, 233, 0.28) 0%, rgba(251, 241, 186, 0.05) 70%);
        bottom: -200px;
        right: -150px;
        animation: blobFloat2 30s ease-in-out infinite;
    }
}

@keyframes blobFloat1 {
    0% { transform: translate(0, 0) scale(1); }
    33% { transform: translate(100px, 80px) scale(1.15); }
    66% { transform: translate(-60px, 150px) scale(0.9); }
    100% { transform: translate(0, 0) scale(1); }
}

@keyframes blobFloat2 {
    0% { transform: translate(0, 0) scale(1); }
    50% { transform: translate(-120px, -100px) scale(1.1); }
    100% { transform: translate(0, 0) scale(1); }
}

// 玻璃拟态分屏大卡
.login-card-wrapper {
    display: flex;
    align-items: stretch;
    background: rgba(255, 255, 255, 0.72);
    backdrop-filter: blur(24px);
    -webkit-backdrop-filter: blur(24px);
    border-radius: $radius-hero;
    border: 1px solid rgba(255, 255, 255, 0.6);
    box-shadow: $shadow-hero, inset 0 1px 1px rgba(255, 255, 255, 0.6);
    padding: 32px;
    width: 960px;
    max-width: 92%;
    min-height: 540px;
    z-index: 10;
}

// 左侧品牌展示区
.brand-panel {
    flex: 1.15;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding-right: 44px;
    border-right: 1px solid rgba(31, 35, 41, 0.08);

    .brand-header {
        margin-bottom: 20px;

        .brand-mark {
            display: flex;
            align-items: center;
            gap: 12px;

            .brand-icon {
                width: 46px;
                height: 46px;
                display: flex;
                align-items: center;
                justify-content: center;
                background: linear-gradient(135deg, $color-primary 0%, #1a56db 100%);
                border-radius: $radius-medium;
                color: #fff;
                font-size: 24px;
                font-weight: 800;
                box-shadow: 0 6px 16px rgba(51, 112, 255, 0.35);
            }

            .brand-title {
                font-size: 30px;
                font-weight: 850;
                color: $color-primary;
                margin: 0;
                letter-spacing: -0.8px;
                display: flex;
                align-items: center;

                .brand-badge {
                    font-size: 10px;
                    font-weight: 800;
                    color: #fff;
                    background: linear-gradient(135deg, $color-primary 0%, #1a56db 100%);
                    padding: 2px 6px;
                    border-radius: 5px;
                    margin-left: 10px;
                    letter-spacing: 0.8px;
                }
            }
        }

        .brand-description {
            font-size: 14.5px;
            color: $text-regular;
            margin: 10px 0 0;
            letter-spacing: 0.5px;
        }
    }

    .brand-illustration {
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: $radius-large;
        overflow: hidden;

        img {
            width: 100%;
            max-width: 380px;
            height: auto;
            border-radius: $radius-large;
            filter: drop-shadow(0 20px 40px rgba(31, 35, 41, 0.08));
        }
    }

    .brand-points {
        list-style: none;
        margin: 18px 0 0;
        padding: 0;

        li {
            position: relative;
            font-size: 13.5px;
            color: $text-regular;
            line-height: 2;

            &::before {
                content: '';
                display: inline-block;
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: $color-primary;
                margin-right: 10px;
                vertical-align: 2px;
            }
        }
    }
}

// 右侧表单区
.form-panel {
    flex: 0.85;
    padding-left: 44px;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .welcome-header {
        margin-bottom: 24px;
        display: flex;
        flex-direction: column;

        .hello {
            font-size: 30px;
            font-weight: 850;
            color: $text-primary;
            line-height: 1.2;
            letter-spacing: -0.5px;
            margin-bottom: 4px;
        }

        .tips {
            font-size: 14.5px;
            font-weight: 500;
            color: $text-regular;
            letter-spacing: 0.5px;
        }
    }

    .auth-form {
        .el-form-item {
            margin-bottom: 20px;
        }

        .flex {
            width: 100%;
            display: flex;
            justify-content: space-between;
        }

        .pwd-tip {
            color: $text-secondary;
            font-size: 12px;
            line-height: 1.5;
            text-align: left;
        }

        .submit-btn {
            width: 100%;
            height: 44px;
            font-size: 15px;
            font-weight: 600;
            letter-spacing: 2px;
            border: none;
            background: linear-gradient(135deg, $color-primary 0%, #1a56db 100%);
            transition: $transition-base;

            &:hover,
            &:focus {
                background: linear-gradient(135deg, $color-primary-hover 0%, #2a66e0 100%);
            }

            &:active {
                background: linear-gradient(135deg, $color-primary-active 0%, #1a56db 100%);
            }
        }
    }

    .form-footer-links {
        display: flex;
        justify-content: center;
        margin-top: 4px;

        .link-item {
            font-size: 13.5px;
            color: $color-primary;
            cursor: pointer;
            user-select: none;

            &:hover { color: $color-primary-active; }
        }
    }
}

.copyright {
    position: absolute;
    bottom: 20px;
    left: 0;
    right: 0;
    text-align: center;
    font-size: 12.5px;
    color: $text-secondary;
    z-index: 10;
}

@media (max-width: 900px) {
    .brand-panel {
        display: none;
    }

    .form-panel {
        padding-left: 0;
    }
}
</style>