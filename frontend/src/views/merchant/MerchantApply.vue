<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApplyService, merchantReSubmitService, merchantStatusService } from '@/api/merchant.js'
import { MERCHANT_STATUS_MAP, MERCHANT_STATUS_TAG, formatTime, textOf } from '@/utils/mall.js'
import { regionData, codeToText } from 'element-china-area-data'
import ImageUploader from '@/components/ImageUploader.vue'

const router = useRouter()

const statusInfo = ref({})
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

// merchantStatus: 0待审核 1已通过 2已拒绝 3已封禁 4未申请（后端对未申请合成 4）
const merchantStatus = computed(() => statusInfo.value.merchantStatus)

// 未申请 / 已拒绝 时展示申请表单
const showForm = computed(() => merchantStatus.value === 4 || merchantStatus.value === 2)

const form = reactive({
    shopName: '',
    shopDescription: '',
    shopLogo: '',
    licenseNumber: '',
    licenseImg: '',
    contactName: '',
    contactPhone: '',
    province: '',
    city: '',
    district: '',
    address: ''
})

// 省市区级联选择（静态数据，支持输入关键字过滤），选择后行政码转中文写入表单
const selectedRegion = ref([])
const onRegionChange = (codes) => {
    if (codes && codes.length === 3) {
        form.province = codeToText[codes[0]]
        form.city = codeToText[codes[1]]
        form.district = codeToText[codes[2]]
    }
}

const rules = {
    shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
    licenseNumber: [{ required: true, message: '请输入营业执照号', trigger: 'blur' }],
    licenseImg: [{ required: true, message: '请上传营业执照图片', trigger: 'change' }],
    contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
    province: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
    contactPhone: [
        { required: true, message: '请输入联系电话', trigger: 'blur' },
        { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
    ]
}

const loadStatus = async () => {
    loading.value = true
    try {
        const result = await merchantStatusService()
        statusInfo.value = result.data || {}
        if (statusInfo.value.shopName && !form.shopName) {
            form.shopName = statusInfo.value.shopName
        }
    } catch (e) {
        statusInfo.value = {}
    } finally {
        loading.value = false
    }
}

const submit = async () => {
    await formRef.value.validate()
    submitting.value = true
    try {
        if (merchantStatus.value === 2) {
            await merchantReSubmitService({ ...form })
            ElMessage.success('已重新提交申请，请等待平台审核')
        } else {
            await merchantApplyService({ ...form })
            ElMessage.success('申请已提交，请等待平台审核')
        }
        await loadStatus()
    } finally {
        submitting.value = false
    }
}

loadStatus()
</script>

<template>
    <el-card class="page-container" v-loading="loading">
        <template #header>
            <div class="header">
                <span>🏪 商家入驻</span>
                <el-tag v-if="statusInfo.merchantStatus !== undefined"
                    :type="textOf(MERCHANT_STATUS_TAG, merchantStatus, 'info')">
                    {{ statusInfo.statusDesc || textOf(MERCHANT_STATUS_MAP, merchantStatus) }}
                </el-tag>
            </div>
        </template>

        <!-- 审核中 -->
        <el-result v-if="merchantStatus === 0" icon="info" title="申请已提交，等待平台审核"
            :sub-title="`店铺名称：${statusInfo.shopName || '—'}　提交时间：${formatTime(statusInfo.applyTime)}`" />

        <!-- 已通过 -->
        <el-result v-else-if="merchantStatus === 1" icon="success" title="恭喜，您已是认证商家"
            :sub-title="`店铺名称：${statusInfo.shopName || '—'}`">
            <template #extra>
                <el-button type="primary" @click="router.push('/merchant/home')">进入商家中心</el-button>
            </template>
        </el-result>

        <!-- 已封禁 -->
        <el-alert v-else-if="merchantStatus === 3" type="error" :closable="false" show-icon
            title="账号已被封禁"
            :description="statusInfo.auditRemark || '如有疑问请联系平台客服'" />

        <!-- 已拒绝 -->
        <el-alert v-else-if="merchantStatus === 2" type="warning" :closable="false" show-icon
            title="申请未通过，可修改后重新提交" :description="`审核意见：${statusInfo.auditRemark || '无'}`"
            style="margin-bottom: 20px" />

        <!-- 申请表单（未申请 / 已拒绝） -->
        <div v-if="showForm" class="form-wrap">
            <el-alert type="info" :closable="false" show-icon
                title="提交后由平台管理员审核，审核通过即可使用商家中心（商品、团购、优惠券、订单管理）"
                style="margin-bottom: 20px" />

            <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
                <el-divider content-position="left">店铺信息</el-divider>
                <el-form-item label="店铺名称" prop="shopName">
                    <el-input v-model="form.shopName" maxlength="50" placeholder="请输入店铺名称" />
                </el-form-item>
                <el-form-item label="店铺 Logo">
                    <ImageUploader v-model="form.shopLogo" :width="100" :height="100" tip="建议 1:1 正方形图片" />
                </el-form-item>
                <el-form-item label="店铺简介">
                    <el-input v-model="form.shopDescription" type="textarea" :rows="2" maxlength="200"
                        show-word-limit placeholder="简单介绍主营业务" />
                </el-form-item>

                <el-divider content-position="left">资质信息</el-divider>
                <el-form-item label="营业执照号" prop="licenseNumber">
                    <el-input v-model="form.licenseNumber" maxlength="40" placeholder="请输入统一社会信用代码" />
                </el-form-item>
                <el-form-item label="营业执照图" prop="licenseImg">
                    <ImageUploader v-model="form.licenseImg" :width="180" :height="120" tip="请上传清晰的执照照片" />
                </el-form-item>

                <el-divider content-position="left">联系人 & 地址</el-divider>
                <el-form-item label="联系人" prop="contactName">
                    <el-input v-model="form.contactName" maxlength="20" placeholder="请输入联系人姓名" />
                </el-form-item>
                <el-form-item label="联系电话" prop="contactPhone">
                    <el-input v-model="form.contactPhone" maxlength="11" placeholder="请输入 11 位手机号" />
                </el-form-item>
                <el-form-item label="所在地区" prop="province">
                    <el-cascader v-model="selectedRegion" :options="regionData" filterable
                        placeholder="输入关键字搜索，如：杭州" style="width: 100%" @change="onRegionChange" />
                </el-form-item>
                <el-form-item label="详细地址">
                    <el-input v-model="form.address" maxlength="100" placeholder="街道、门牌号等" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" :loading="submitting" @click="submit">
                        {{ merchantStatus === 2 ? '重新提交申请' : '提交入驻申请' }}
                    </el-button>
                    <el-button @click="loadStatus">刷新状态</el-button>
                </el-form-item>
            </el-form>
        </div>

        <el-empty v-else-if="!loading && merchantStatus === undefined" description="状态加载失败，请刷新重试" />
    </el-card>
</template>

<style lang="scss" scoped>
.page-container {
    min-height: 100%;
    box-sizing: border-box;

    .header {
        display: flex;
        align-items: center;
        gap: 12px;

        span {
            font-size: 18px;
            font-weight: bold;
        }
    }

    .form-wrap {
        max-width: 720px;
    }
}
</style>
