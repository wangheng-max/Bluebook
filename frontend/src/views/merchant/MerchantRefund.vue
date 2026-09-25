<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantRefundListService, merchantRefundAuditService } from '@/api/order.js'
import { money, formatTime, ORDER_TYPE_MAP, REFUND_STATUS_MAP, REFUND_STATUS_TAG, textOf } from '@/utils/mall.js'

const refunds = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const auditingId = ref(null)

const dialogVisible = ref(false)
const auditFormRef = ref()
const current = reactive({ id: null, orderNo: '', payAmount: null, refundAmount: null, approved: true, remark: '' })

const rules = {
    remark: [
        {
            validator: (rule, value, callback) => {
                if (!current.approved && (!value || !value.trim())) {
                    callback(new Error('拒绝退款时必须填写理由'))
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
        const result = await merchantRefundListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value
        })
        refunds.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        refunds.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const onPageChange = (num) => {
    pageNum.value = num
    loadList()
}

const openAudit = (row, approved) => {
    Object.assign(current, {
        id: row.id,
        orderNo: row.orderNo,
        payAmount: row.payAmount,
        refundAmount: row.refundAmount,
        approved: approved,
        remark: ''
    })
    dialogVisible.value = true
}

const submitAudit = async () => {
    await auditFormRef.value.validate()
    auditingId.value = current.id
    try {
        await merchantRefundAuditService(current.id, current.approved, current.remark)
        ElMessage.success(current.approved ? '已通过退款申请' : '已拒绝退款申请')
        dialogVisible.value = false
        loadList()
    } finally {
        auditingId.value = null
    }
}

const pendingCount = () => refunds.value.filter(r => r.refundStatus === 0).length

loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>↩️ 退款审核</span>
                <el-tag type="warning" v-if="pendingCount() > 0">本页 {{ pendingCount() }} 条待处理</el-tag>
            </div>
        </template>

        <el-table :data="refunds" v-loading="loading" style="width: 100%">
            <el-table-column label="退款单号" prop="id" width="90"></el-table-column>
            <el-table-column label="订单号" prop="orderNo" min-width="180"></el-table-column>
            <el-table-column label="订单类型" width="100">
                <template #default="{ row }">{{ textOf(ORDER_TYPE_MAP, row.orderType) }}</template>
            </el-table-column>
            <el-table-column label="订单实付" width="110">
                <template #default="{ row }">¥{{ money(row.payAmount) }}</template>
            </el-table-column>
            <el-table-column label="退款金额" width="110">
                <template #default="{ row }">
                    <span class="refund-amount">¥{{ money(row.refundAmount) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="退款原因" prop="refundReason" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column label="审核备注" min-width="130" show-overflow-tooltip>
                <template #default="{ row }">{{ row.remark || '—' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="textOf(REFUND_STATUS_TAG, row.refundStatus, 'info')" size="small">
                        {{ textOf(REFUND_STATUS_MAP, row.refundStatus) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="申请时间" width="170">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                    <template v-if="row.refundStatus === 0">
                        <el-button type="success" link size="small"
                            :loading="auditingId === row.id" @click="openAudit(row, true)">通过</el-button>
                        <el-button type="danger" link size="small"
                            :loading="auditingId === row.id" @click="openAudit(row, false)">拒绝</el-button>
                    </template>
                    <span v-else class="no-op">已处理</span>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="暂无退款申请" />
            </template>
        </el-table>

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center" />

        <!-- 审核退款 -->
        <el-dialog v-model="dialogVisible" :title="current.approved ? '通过退款申请' : '拒绝退款申请'" width="480px">
            <el-form ref="auditFormRef" :model="current" :rules="rules" label-width="90px">
                <el-form-item label="订单号">
                    <span>{{ current.orderNo }}</span>
                </el-form-item>
                <el-form-item label="退款金额">
                    <span class="refund-amount">¥{{ money(current.refundAmount) }}</span>
                    <span class="tip">（订单实付 ¥{{ money(current.payAmount) }}）</span>
                </el-form-item>
                <el-form-item label="审核备注" prop="remark">
                    <el-input v-model="current.remark" type="textarea" :rows="3" maxlength="100" show-word-limit
                        :placeholder="current.approved ? '选填，如：同意退款' : '必填，请说明拒绝理由'" />
                </el-form-item>
                <el-alert v-if="current.approved" type="warning" :closable="false" show-icon
                    title="通过后款项将按原路退回买家，优惠券按规则退回，操作不可撤销" />
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button :type="current.approved ? 'primary' : 'danger'" :loading="auditingId === current.id"
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
        align-items: center;
        gap: 12px;

        span {
            font-size: 18px;
            font-weight: bold;
        }
    }

    .refund-amount {
        color: #f56c6c;
        font-weight: bold;
    }

    .no-op {
        color: #999;
        font-size: 12px;
    }

    .tip {
        margin-left: 8px;
        color: #999;
        font-size: 12px;
    }
}
</style>
