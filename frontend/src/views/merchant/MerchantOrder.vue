<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantOrderListService, merchantOrderDetailService, merchantShipService } from '@/api/order.js'
import {
    money, formatTime, ORDER_STATUS_MAP, ORDER_STATUS_TAG, ORDER_TYPE_MAP,
    REFUND_STATUS_MAP, textOf
} from '@/utils/mall.js'

const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const loading = ref(false)
const shippingId = ref(null)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({})

const loadList = async () => {
    loading.value = true
    try {
        const result = await merchantOrderListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value === '' ? null : status.value
        })
        orders.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        orders.value = []
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

const openDetail = async (row) => {
    detailVisible.value = true
    detailLoading.value = true
    try {
        const result = await merchantOrderDetailService(row.id)
        detail.value = result.data || {}
    } catch (e) {
        detail.value = {}
    } finally {
        detailLoading.value = false
    }
}

// 发货：仅待发货（status=1）可操作
const ship = (row) => {
    ElMessageBox.confirm(`确认对订单 ${row.orderNo} 发货吗？`, '发货确认', {
        confirmButtonText: '确认发货',
        cancelButtonText: '取消',
        type: 'info'
    }).then(async () => {
        shippingId.value = row.id
        try {
            await merchantShipService(row.id)
            ElMessage.success('已发货')
            loadList()
        } finally {
            shippingId.value = null
        }
    }).catch(() => { })
}

const sumQuantity = (items) => (items || []).reduce((sum, i) => sum + Number(i.quantity || 0), 0)

loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>📋 订单管理</span>
                <span class="subtitle">仅显示本店商品订单</span>
            </div>
        </template>

        <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button :label="0">待支付</el-radio-button>
            <el-radio-button :label="1">待发货</el-radio-button>
            <el-radio-button :label="2">已发货</el-radio-button>
            <el-radio-button :label="3">已完成</el-radio-button>
            <el-radio-button :label="4">已取消</el-radio-button>
            <el-radio-button :label="5">退款中</el-radio-button>
            <el-radio-button :label="6">已退款</el-radio-button>
        </el-radio-group>

        <el-table :data="orders" v-loading="loading" style="width: 100%">
            <el-table-column label="订单号" prop="orderNo" min-width="180"></el-table-column>
            <el-table-column label="类型" width="100">
                <template #default="{ row }">{{ textOf(ORDER_TYPE_MAP, row.orderType) }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="220">
                <template #default="{ row }">
                    <div v-for="item in row.items" :key="item.id" class="item-line">
                        {{ item.productName }}
                        <span v-if="item.skuName" class="sku">（{{ item.skuName }}）</span>
                        × {{ item.quantity }}
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="件数" width="80">
                <template #default="{ row }">{{ sumQuantity(row.items) }}</template>
            </el-table-column>
            <el-table-column label="实付金额" width="120">
                <template #default="{ row }">
                    <span class="pay">¥{{ money(row.payAmount) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
                <template #default="{ row }">
                    <el-tag :type="textOf(ORDER_STATUS_TAG, row.status, 'info')" size="small">
                        {{ textOf(ORDER_STATUS_MAP, row.status) }}
                    </el-tag>
                    <div v-if="row.status === 5" class="refund-tip">
                        退款：{{ textOf(REFUND_STATUS_MAP, row.refundStatus) }}
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="下单时间" width="170">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                    <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
                    <el-button v-if="row.status === 1" type="success" link size="small"
                        :loading="shippingId === row.id" @click="ship(row)">发货</el-button>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="暂无订单" />
            </template>
        </el-table>

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center" />

        <!-- 订单详情 -->
        <el-dialog v-model="detailVisible" title="订单详情" width="640px">
            <div v-loading="detailLoading">
                <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
                    <el-descriptions-item label="订单类型">
                        {{ textOf(ORDER_TYPE_MAP, detail.orderType) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="订单状态">
                        {{ textOf(ORDER_STATUS_MAP, detail.status) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="退款状态">
                        {{ detail.refundStatus === null || detail.refundStatus === undefined
                            ? '无退款申请' : textOf(REFUND_STATUS_MAP, detail.refundStatus) }}
                    </el-descriptions-item>
                    <el-descriptions-item label="商品金额">¥{{ money(detail.totalAmount) }}</el-descriptions-item>
                    <el-descriptions-item label="实付金额">
                        <span class="pay">¥{{ money(detail.payAmount) }}</span>
                    </el-descriptions-item>
                    <el-descriptions-item label="下单时间">{{ formatTime(detail.createTime) }}</el-descriptions-item>
                    <el-descriptions-item label="支付时间">{{ formatTime(detail.payTime) }}</el-descriptions-item>
                    <el-descriptions-item label="发货时间">{{ formatTime(detail.shipTime) }}</el-descriptions-item>
                    <el-descriptions-item label="完成时间">{{ formatTime(detail.finishTime) }}</el-descriptions-item>
                    <el-descriptions-item label="买家备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
                </el-descriptions>

                <el-table :data="detail.items || []" size="small" border style="margin-top: 16px">
                    <el-table-column label="商品" prop="productName" min-width="160" show-overflow-tooltip></el-table-column>
                    <el-table-column label="规格" prop="skuName" width="120">
                        <template #default="{ row }">{{ row.skuName || '默认规格' }}</template>
                    </el-table-column>
                    <el-table-column label="单价" width="100">
                        <template #default="{ row }">¥{{ money(row.price) }}</template>
                    </el-table-column>
                    <el-table-column label="数量" prop="quantity" width="70"></el-table-column>
                    <el-table-column label="小计" width="100">
                        <template #default="{ row }">¥{{ money(row.totalPrice) }}</template>
                    </el-table-column>
                </el-table>
            </div>
            <template #footer>
                <el-button @click="detailVisible = false">关闭</el-button>
                <el-button v-if="detail.status === 1" type="success" :loading="shippingId === detail.id"
                    @click="ship(detail)">发货</el-button>
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

    .item-line {
        font-size: 13px;
        line-height: 1.8;

        .sku {
            color: #999;
        }
    }

    .pay {
        color: #f56c6c;
        font-weight: bold;
    }

    .refund-tip {
        color: #e6a23c;
        font-size: 12px;
        margin-top: 2px;
    }
}
</style>
