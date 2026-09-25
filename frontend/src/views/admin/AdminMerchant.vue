<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminMerchantListService, adminMerchantAuditService, adminMerchantBanService } from '@/api/admin.js'
import { formatTime, MERCHANT_STATUS_MAP, MERCHANT_STATUS_TAG, textOf } from '@/utils/mall.js'

const merchants = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref(0)
const loading = ref(false)
const actingId = ref(null)

const dialogVisible = ref(false)
const auditFormRef = ref()
const current = reactive({ userId: null, shopName: '', approved: true, auditRemark: '' })

const rules = {
    auditRemark: [
        {
            validator: (rule, value, callback) => {
                if (!current.approved && (!value || !value.trim())) {
                    callback(new Error('拒绝时必须填写审核意见'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
}

const loadList = async () => {
    loading.value = true
    try {
        const result = await adminMerchantListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value === '' ? null : status.value
        })
        merchants.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        merchants.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const changeStatus = (val) => {
    status.value = val
    pageNum.value = 1
    loadList()
}

const onPageChange = (num) => {
    pageNum.value = num
    loadList()
}

const openAudit = (row, approved) => {
    Object.assign(current, {
        userId: row.userId,
        shopName: row.shopName,
        approved: approved,
        auditRemark: ''
    })
    dialogVisible.value = true
}

const submitAudit = async () => {
    await auditFormRef.value.validate()
    actingId.value = current.userId
    try {
        await adminMerchantAuditService(current.userId, current.approved, current.auditRemark)
        ElMessage.success(current.approved ? '已通过商家认证' : '已拒绝商家认证')
        dialogVisible.value = false
        loadList()
    } finally {
        actingId.value = null
    }
}

const ban = (row) => {
    ElMessageBox.confirm(
        `确认封禁商家「${row.shopName}」吗？封禁后该商家无法使用商家中心。`,
        '封禁商家',
        { confirmButtonText: '确认封禁', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
        actingId.value = row.userId
        try {
            await adminMerchantBanService(row.userId)
            ElMessage.success('已封禁')
            loadList()
        } finally {
            actingId.value = null
        }
    }).catch(() => { })
}

loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>✅ 商家审核</span>
                <span class="subtitle">审核商家入驻申请、封禁违规商家</span>
            </div>
        </template>

        <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
            <el-radio-button :label="0">待审核</el-radio-button>
            <el-radio-button :label="1">已通过</el-radio-button>
            <el-radio-button :label="2">已拒绝</el-radio-button>
            <el-radio-button :label="3">已封禁</el-radio-button>
            <el-radio-button label="">全部</el-radio-button>
        </el-radio-group>

        <el-table :data="merchants" v-loading="loading" style="width: 100%">
            <el-table-column label="Logo" width="80">
                <template #default="{ row }">
                    <img :src="row.shopLogo" class="logo" />
                </template>
            </el-table-column>
            <el-table-column label="店铺名称" prop="shopName" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column label="用户ID" prop="userId" width="90"></el-table-column>
            <el-table-column label="联系人" min-width="120">
                <template #default="{ row }">
                    <div>{{ row.contactName }}</div>
                    <div class="sub">{{ row.contactPhone }}</div>
                </template>
            </el-table-column>
            <el-table-column label="营业执照号" prop="licenseNumber" min-width="170" show-overflow-tooltip></el-table-column>
            <el-table-column label="地址" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                    {{ row.province }}{{ row.city }}{{ row.district }}{{ row.address }}
                </template>
            </el-table-column>
            <el-table-column label="申请时间" width="170">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="textOf(MERCHANT_STATUS_TAG, row.merchantStatus, 'info')" size="small">
                        {{ textOf(MERCHANT_STATUS_MAP, row.merchantStatus) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="审核意见" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.auditRemark || '—' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                    <template v-if="row.merchantStatus === 0">
                        <el-button type="success" link size="small" :loading="actingId === row.userId"
                            @click="openAudit(row, true)">通过</el-button>
                        <el-button type="danger" link size="small" :loading="actingId === row.userId"
                            @click="openAudit(row, false)">拒绝</el-button>
                    </template>
                    <el-button v-else-if="row.merchantStatus === 1" type="danger" link size="small"
                        :loading="actingId === row.userId" @click="ban(row)">封禁</el-button>
                    <span v-else class="no-op">—</span>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="该状态下暂无商家" />
            </template>
        </el-table>

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center" />

        <!-- 审核 -->
        <el-dialog v-model="dialogVisible" :title="current.approved ? '通过商家认证' : '拒绝商家认证'" width="480px">
            <el-form ref="auditFormRef" :model="current" :rules="rules" label-width="90px">
                <el-form-item label="店铺名称">
                    <span>{{ current.shopName }}</span>
                </el-form-item>
                <el-form-item label="审核意见" prop="auditRemark">
                    <el-input v-model="current.auditRemark" type="textarea" :rows="3" maxlength="100" show-word-limit
                        :placeholder="current.approved ? '选填，如：资质齐全，通过' : '必填，请说明拒绝原因'" />
                </el-form-item>
                <el-alert v-if="!current.approved" type="info" :closable="false" show-icon
                    title="拒绝后商家可在「商家入驻」页修改资料后重新提交申请" />
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button :type="current.approved ? 'primary' : 'danger'" :loading="actingId === current.userId"
                    @click="submitAudit">确认{{ current.approved ? '通过' : '拒绝' }}</el-button>
            </template>
        </el-dialog>
    </el-card>
</template>

<style lang="scss" scoped>
.page-container {
    min-height: 100%;
    box-sizing: border-box;

    .header {
        display: flex;
        align-items: baseline;
        gap: 12px;

        span:first-child {
            font-size: 18px;
            font-weight: bold;
        }

        .subtitle {
            color: #999;
            font-size: 14px;
        }
    }

    .status-filter {
        margin-bottom: 20px;
    }

    .logo {
        width: 46px;
        height: 46px;
        object-fit: cover;
        border-radius: 4px;
        background-color: #f5f7fa;
    }

    .sub {
        color: #999;
        font-size: 12px;
    }

    .no-op {
        color: #ccc;
    }
}
</style>
