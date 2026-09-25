<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { userPasswordUpdateService } from '@/api/user.js'
import { useTokenStore } from '@/stores/token.js'

const tokenStore = useTokenStore()

//表单数据模型
const passwordData = ref({
    old_pwd: '',
    new_pwd: '',
    re_pwd: ''
})

//校验两次新密码是否一致
const checkRePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次确认新密码'))
    } else if (value !== passwordData.value.new_pwd) {
        callback(new Error('两次输入的新密码不一致'))
    } else {
        callback()
    }
}

//密码格式：6-16位，须同时包含字母和数字（与后端校验一致）
const PWD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)\S{6,16}$/

//表单校验规则
const rules = {
    old_pwd: [
        { required: true, message: '请输入原密码', trigger: 'blur' },
        { min: 5, max: 16, message: '长度为5~16位非空字符', trigger: 'blur' }
    ],
    new_pwd: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { pattern: PWD_PATTERN, message: '新密码为6-16位，且必须同时包含字母和数字', trigger: 'blur' }
    ],
    re_pwd: [
        { validator: checkRePassword, trigger: 'blur' }
    ]
}

const formRef = ref()

//提交修改密码
const updatePassword = async () => {
    const valid = await formRef.value.validate()
    if (!valid) return

    //调用接口，token通过拦截器自动携带
    await userPasswordUpdateService(passwordData.value)
    ElMessage.success('密码修改成功')

    //清空表单
    passwordData.value = { old_pwd: '', new_pwd: '', re_pwd: '' }
    formRef.value.resetFields()
}
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>重置密码</span>
            </div>
        </template>
        <el-row>
            <el-col :span="12">
                <el-form ref="formRef" :model="passwordData" :rules="rules" label-width="100px" size="large">
                    <el-form-item label="原密码" prop="old_pwd">
                        <el-input v-model="passwordData.old_pwd" type="password" show-password></el-input>
                    </el-form-item>
                    <el-form-item label="新密码" prop="new_pwd">
                        <el-input v-model="passwordData.new_pwd" type="password" show-password
                            placeholder="6-16位，须含字母和数字"></el-input>
                        <div class="pwd-tip">密码由 6-16 位字符组成，须同时包含字母和数字，例如：abc12345</div>
                    </el-form-item>
                    <el-form-item label="确认新密码" prop="re_pwd">
                        <el-input v-model="passwordData.re_pwd" type="password" show-password></el-input>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="updatePassword">提交修改</el-button>
                    </el-form-item>
                </el-form>
            </el-col>
        </el-row>
    </el-card>
</template>

<style lang="scss" scoped>
.pwd-tip {
    width: 100%;
    color: #999;
    font-size: 12px;
    line-height: 1.6;
    margin-top: 4px;
    text-align: left;
}
</style>
