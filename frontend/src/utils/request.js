//定制请求的实例

//导入axios  npm install axios
import axios from 'axios';

import { ElMessage } from 'element-plus'
//定义一个变量,记录公共的前缀  ,  baseURL
//const baseURL = 'http://localhost:8080';
const baseURL = '/api';
const instance = axios.create({ baseURL })

import {useTokenStore} from '@/stores/token.js'
//添加请求拦截器
instance.interceptors.request.use(
    (config)=>{
        //请求前的回调
        //添加token
        const tokenStore = useTokenStore();
        //判断有没有token
        if(tokenStore.token){
            config.headers.Authorization =  tokenStore.token;
        }
        return config;
    },
    (err)=>{
        //请求错误的回调
        Promise.reject(err)
    }
)

/* import {useRouter} from 'vue-router'
const router = useRouter(); */

import router from '@/router'
//添加响应拦截器
instance.interceptors.response.use(
    result => {
        //判断业务状态码
        if(result.data.code===0){
            return result.data;
        }

        //操作失败（后端 Result 字段名为 message，兼容历史 msg 写法）
        ElMessage.error(result.data.message || result.data.msg || '服务异常')
        //异步操作的状态转换为失败
        return Promise.reject(result.data)

    },
    err => {
        //后端返回的具体原因（403 商家/管理员权限不足时尤为重要）
        const backendMsg = err.response && err.response.data
            ? (err.response.data.message || err.response.data.msg)
            : ''
        //判断响应状态码,如果为401,则证明未登录,提示请登录,并跳转到登录页面
        if(err.response && err.response.status===401){
            ElMessage.error('请先登录')
            router.push('/login')
        }else if(err.response && err.response.status===403){
            //商家/管理员权限不足：展示后端 403 提示（如"无商家权限，请先完成商家认证"）
            ElMessage.error(backendMsg || '无权限执行该操作')
        }else{
            ElMessage.error(backendMsg || '服务异常')
        }

        return Promise.reject(err);//异步的状态转化成失败的状态
    }
)

export default instance;