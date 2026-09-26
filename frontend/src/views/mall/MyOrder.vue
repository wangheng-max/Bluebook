<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    orderListService,
    orderPayService,
    orderCancelService,
    orderRefundService,
    myRefundListService
} from '@/api/order.js'
import {
    money, formatTime, ORDER_STATUS_MAP, ORDER_STATUS_TAG, ORDER_TYPE_MAP,
    REFUND_STATUS_MAP, REFUND_STATUS_TAG, textOf
} from '@/utils/mall.js'
import { rateMerchantService, ratingExistsService } from '@/api/shop.js'

const activeTab = ref('orders')

// ---------- 我的订单 ----------
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(5)
const status = ref('')
const loading = ref(false)
const actingId = ref(null)

const loadOrders = async () => {
    loading.value = true
    try {
        const result = await orderListService({
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
    loadOrders()
}

const onPageChange = (num) => {
    pageNum.value = num
    loadOrders()
}

// ---------- 订单操作 ----------
// 模拟支付：真实项目此处跳转第三方收银台
const pay = (order) => {
    ElMessageBox.confirm(`确认支付订单 ${order.orderNo}，金额 ¥${money(order.payAmount)} 吗？`, '模拟支付', {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'info'
    }).then(async () => {
        actingId.value = order.id
        try {
            await orderPayService(order.id, 1)
            ElMessage.success('支付成功')
            loadOrders()
        } finally {
            actingId.value = null
        }
    }).catch(() => { })
}

const cancel = (order) => {
    ElMessageBox.confirm('取消后优惠券将退回，库存也会归还，确认取消吗？', '取消订单', {
        confirmButtonText: '确认取消',
        cancelButtonText: '再想想',
        type: 'warning'
    }).then(async () => {
        actingId.value = order.id
        try {
            await orderCancelService(order.id)
            ElMessage.success('订单已取消')
            loadOrders()
        } finally {
            actingId.value = null
        }
    }).catch(() => { })
}

// ---------- 商家评价（订单已发货/完成后，一单一评） ----------
const ratingVisible = ref(false)
const ratingOrder = ref(null)
const ratingScore = ref(5)
const ratingContent = ref('')
const ratingSubmitting = ref(false)
const ratedOrderIds = ref({}) // { [orderId]: true }，对象映射保证响应式

const openRating = async (order) => {
    // 先查是否已评价，避免重复提交
    try {
        const exists = await ratingExistsService(order.id)
        if (exists.data) {
            ratedOrderIds.value[order.id] = true
            ElMessage.info('该订单已评价过，感谢您的反馈')
            return
        }
    } catch (e) {
        // 查询失败不阻塞，提交时后端还会兜底校验
    }
    ratingOrder.value = order
    ratingScore.value = 5
    ratingContent.value = ''
    ratingVisible.value = true
}

const submitRating = async () => {
    if (!ratingScore.value) {
        ElMessage.warning('请先打分')
        return
    }
    ratingSubmitting.value = true
    try {
        await rateMerchantService({
            orderId: ratingOrder.value.id,
            score: ratingScore.value,
            content: ratingContent.value.trim() || null
        })
        ElMessage.success('评价成功，感谢您的反馈')
        ratedOrderIds.value[ratingOrder.value.id] = true
        ratingVisible.value = false
    } finally {
        ratingSubmitting.value = false
    }
}

const applyRefund = (order) => {
    ElMessageBox.prompt('请填写退款原因', '申请退款', {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputPlaceholder: '如：不想要了 / 商品与描述不符',
        inputValidator: (v) => (v && v.trim().length > 0) || '退款原因不能为空'
    }).then(async ({ value }) => {
        actingId.value = order.id
        try {
            await orderRefundService(order.id, value.trim())
            ElMessage.success('退款申请已提交，等待商家审核')
            loadOrders()
        } finally {
            actingId.value = null
        }
    }).catch(() => { })
}

// ---------- 退款/售后 ----------
const refunds = ref([])
const refundTotal = ref(0)
const refundPageNum = ref(1)
const refundPageSize = ref(10)
const refundStatus = ref('')
const refundLoading = ref(false)

const loadRefunds = async () => {
    refundLoading.value = true
    try {
        const result = await myRefundListService({
            pageNum: refundPageNum.value,
            pageSize: refundPageSize.value,
            status: refundStatus.value === '' ? null : refundStatus.value
        })
        refunds.value = result.data.items || []
        refundTotal.value = result.data.total || 0
    } catch (e) {
        refunds.value = []
        refundTotal.value = 0
    } finally {
        refundLoading.value = false
    }
}

const changeRefundStatus = (val) => {
    refundStatus.value = val
    refundPageNum.value = 1
    loadRefunds()
}

const onRefundPageChange = (num) => {
    refundPageNum.value = num
    loadRefunds()
}

const onTabChange = (name) => {
    if (name === 'refunds') {
        loadRefunds()
    } else {
        loadOrders()
    }
}

loadOrders()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>📋 我的订单</span>
                <span class="subtitle">支付、取消、退款都在这里操作</span>
            </div>
        </template>

        <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <!-- 订单列表 -->
            <el-tab-pane label="我的订单" name="orders">
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

                <div class="order-list" v-loading="loading">
                    <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
                        <div class="order-head">
                            <div>
                                <span class="order-no">订单号：{{ order.orderNo }}</span>
                                <el-tag size="small" type="info" class="type-tag">
                                    {{ textOf(ORDER_TYPE_MAP, order.orderType) }}
                                </el-tag>
                            </div>
                            <el-tag :type="textOf(ORDER_STATUS_TAG, order.status, 'info')">
                                {{ textOf(ORDER_STATUS_MAP, order.status) }}
                                <span v-if="order.status === 5 && order.refundStatus !== null">
                                    · {{ textOf(REFUND_STATUS_MAP, order.refundStatus) }}
                                </span>
                            </el-tag>
                        </div>

                        <div class="order-body">
                            <div v-for="item in order.items" :key="item.id" class="order-item">
                                <img :src="item.productImg" class="item-img" />
                                <div class="item-info">
                                    <div class="item-name">{{ item.productName }}</div>
                                    <div class="item-sku" v-if="item.skuName">{{ item.skuName }}</div>
                                </div>
                                <div class="item-price">
                                    ¥{{ money(item.price) }} × {{ item.quantity }}
                                </div>
                            </div>
                        </div>

                        <div class="order-foot">
                            <div class="meta">
                                <span>下单时间：{{ formatTime(order.createTime) }}</span>
                                <span v-if="order.remark">备注：{{ order.remark }}</span>
                            </div>
                            <div class="amount">
                                <span class="total">商品金额 ¥{{ money(order.totalAmount) }}</span>
                                <span class="pay">实付 <strong>¥{{ money(order.payAmount) }}</strong></span>
                            </div>
                            <div class="actions">
                                <template v-if="order.status === 0">
                                    <el-button type="danger" size="small" :loading="actingId === order.id"
                                        @click="pay(order)">去支付</el-button>
                                    <el-button size="small" @click="cancel(order)">取消订单</el-button>
                                </template>
                                <template v-else-if="order.status === 1">
                                    <el-button type="warning" size="small" plain
                                        @click="applyRefund(order)">申请退款</el-button>
                                </template>
                                <template v-else-if="order.status === 5">
                                    <el-button size="small" text type="info">退款处理中，请等待商家审核</el-button>
                                </template>
                                <template v-else-if="order.status === 2 || order.status === 3">
                                    <el-button type="warning" size="small" plain
                                        @click="openRating(order)">
                                        {{ ratedOrderIds[order.id] ? '已评价' : '评价商家' }}
                                    </el-button>
                                    <el-button v-if="order.status === 2" size="small" text type="info">
                                        商家已发货，等待收货
                                    </el-button>
                                </template>
                            </div>
                        </div>
                    </el-card>
                </div>

                <el-empty v-if="orders.length === 0 && !loading" description="暂无订单，去商城逛逛吧">
                    <el-button type="primary" @click="$router.push('/mall')">去商城</el-button>
                </el-empty>

                <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
                    layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
                    style="margin-top: 20px; justify-content: center" />
            </el-tab-pane>

            <!-- 退款/售后 -->
            <el-tab-pane label="退款/售后" name="refunds">
                <el-radio-group v-model="refundStatus" @change="changeRefundStatus" class="status-filter">
                    <el-radio-button label="">全部</el-radio-button>
                    <el-radio-button :label="0">待处理</el-radio-button>
                    <el-radio-button :label="1">已通过</el-radio-button>
                    <el-radio-button :label="2">已拒绝</el-radio-button>
                    <el-radio-button :label="3">已退款</el-radio-button>
                </el-radio-group>

                <el-table :data="refunds" v-loading="refundLoading" style="width: 100%">
                    <el-table-column label="订单号" prop="orderNo" min-width="180"></el-table-column>
                    <el-table-column label="订单类型" width="100">
                        <template #default="{ row }">{{ textOf(ORDER_TYPE_MAP, row.orderType) }}</template>
                    </el-table-column>
                    <el-table-column label="实付金额" width="110">
                        <template #default="{ row }">¥{{ money(row.payAmount) }}</template>
                    </el-table-column>
                    <el-table-column label="退款金额" width="110">
                        <template #default="{ row }">
                            <span class="refund-amount">¥{{ money(row.refundAmount) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="退款原因" prop="refundReason" min-width="160"
                        show-overflow-tooltip></el-table-column>
                    <el-table-column label="商家备注" prop="remark" min-width="140" show-overflow-tooltip>
                        <template #default="{ row }">{{ row.remark || '—' }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                        <template #default="{ row }">
                            <el-tag :type="textOf(REFUND_STATUS_TAG, row.refundStatus, 'info')" size="small">
                                {{ textOf(REFUND_STATUS_MAP, row.refundStatus) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="申请时间" min-width="170">
                        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
                    </el-table-column>
                    <template #empty>
                        <el-empty description="暂无退款记录" />
                    </template>
                </el-table>

                <el-pagination v-if="refundTotal > 0" v-model:current-page="refundPageNum"
                    v-model:page-size="refundPageSize" layout="total, prev, pager, next" background
                    :total="refundTotal" @current-change="onRefundPageChange"
                    style="margin-top: 20px; justify-content: center" />
            </el-tab-pane>
        </el-tabs>

        <!-- 商家评价弹窗 -->
        <el-dialog v-model="ratingVisible" title="评价商家" width="440px">
            <div class="rating-dialog" v-if="ratingOrder">
                <div class="rating-target">订单号：{{ ratingOrder.orderNo }}</div>
                <div class="rating-stars">
                    <span class="label">店铺评分</span>
                    <el-rate v-model="ratingScore" allow-half :colors="['#ff9900', '#ff9900', '#ff9900']"
                        show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
                </div>
                <el-input v-model="ratingContent" type="textarea" :rows="3" maxlength="200" show-word-limit
                    placeholder="说说你的购物体验（选填）：商品质量、发货速度、服务态度…" />
            </div>
            <template #footer>
                <el-button @click="ratingVisible = false">取消</el-button>
                <el-button type="primary" :loading="ratingSubmitting" @click="submitRating">提交评价</el-button>
            </template>
        </el-dialog>
    </el-card>
</template>

<style lang="scss" scoped>
.page-container {
    min-height: 100%;
    box-sizing: border-box;

    .rating-dialog {
        .rating-target {
            color: #999;
            font-size: 13px;
            margin-bottom: 16px;
        }

        .rating-stars {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 16px;

            .label {
                font-size: 14px;
                color: #666;
            }
        }
    }

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

    .order-list {
        min-height: 120px;

        .order-card {
            margin-bottom: 16px;

            .order-head {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding-bottom: 12px;
                border-bottom: 1px solid #ebeef5;

                .order-no {
                    color: #666;
                    font-size: 13px;
                }

                .type-tag {
                    margin-left: 10px;
                }
            }

            .order-body {
                padding: 12px 0;

                .order-item {
                    display: flex;
                    align-items: center;
                    gap: 14px;
                    padding: 8px 0;

                    .item-img {
                        width: 64px;
                        height: 64px;
                        object-fit: cover;
                        border-radius: 4px;
                        background-color: #f5f7fa;
                    }

                    .item-info {
                        flex: 1;

                        .item-name {
                            font-size: 14px;
                        }

                        .item-sku {
                            color: #999;
                            font-size: 12px;
                            margin-top: 4px;
                        }
                    }

                    .item-price {
                        color: #666;
                        font-size: 13px;
                    }
                }
            }

            .order-foot {
                display: flex;
                align-items: center;
                gap: 20px;
                flex-wrap: wrap;
                border-top: 1px solid #ebeef5;
                padding-top: 12px;

                .meta {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    gap: 4px;
                    color: #999;
                    font-size: 12px;
                }

                .amount {
                    display: flex;
                    align-items: baseline;
                    gap: 16px;
                    font-size: 13px;
                    color: #666;

                    .pay strong {
                        color: #f56c6c;
                        font-size: 18px;
                    }
                }

                .actions {
                    display: flex;
                    gap: 8px;
                }
            }
        }
    }

    .refund-amount {
        color: #f56c6c;
        font-weight: bold;
    }
}
</style>
