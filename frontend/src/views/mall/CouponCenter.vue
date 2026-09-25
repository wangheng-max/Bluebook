<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    couponStockListService,
    couponGrabService,
    couponCancelService,
    myGrabRecordService
} from '@/api/coupon.js'
import {
    money, formatTime, remainText, couponValueText, couponThresholdText,
    STOCK_STATUS_MAP, STOCK_STATUS_TAG, COUPON_STATUS_MAP, COUPON_STATUS_TAG, textOf
} from '@/utils/mall.js'

const activeTab = ref('center')

// ---------- 抢券中心 ----------
const stocks = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const status = ref(1)
const loading = ref(false)
const grabbingId = ref(null)

const loadStocks = async () => {
    loading.value = true
    try {
        const result = await couponStockListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value
        })
        stocks.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        stocks.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const changeStatus = (val) => {
    status.value = val
    pageNum.value = 1
    loadStocks()
}

const onPageChange = (num) => {
    pageNum.value = num
    loadStocks()
}

// 抢券：后端 限流 → 幂等 → Lua 原子扣减 → 落库
const grab = async (item) => {
    grabbingId.value = item.id
    try {
        const result = await couponGrabService(item.id)
        ElMessage.success(`抢券成功，剩余 ${result.data.remainStock} 张`)
        loadStocks()
    } catch (e) {
        // 失败原因（已抢过/库存不足/过于频繁）由拦截器统一提示
        loadStocks()
    } finally {
        grabbingId.value = null
    }
}

// 取消抢券：券作废（见 Bug 清单 B-003，取消后不可重抢）
const cancel = (item) => {
    ElMessageBox.confirm(
        '取消后该券将作废，且同一活动无法再次抢购，确认取消吗？',
        '温馨提示',
        { confirmButtonText: '确认取消', cancelButtonText: '再想想', type: 'warning' }
    ).then(async () => {
        await couponCancelService(item.id)
        ElMessage.success('已取消抢购')
        loadStocks()
    }).catch(() => { })
}

// ---------- 我的抢购记录 ----------
const records = ref([])
const recordTotal = ref(0)
const recordPageNum = ref(1)
const recordPageSize = ref(10)
const recordLoading = ref(false)

const loadRecords = async () => {
    recordLoading.value = true
    try {
        const result = await myGrabRecordService({
            pageNum: recordPageNum.value,
            pageSize: recordPageSize.value
        })
        records.value = result.data.items || []
        recordTotal.value = result.data.total || 0
    } catch (e) {
        records.value = []
        recordTotal.value = 0
    } finally {
        recordLoading.value = false
    }
}

const onRecordPageChange = (num) => {
    recordPageNum.value = num
    loadRecords()
}

const onTabChange = (name) => {
    if (name === 'records') {
        loadRecords()
    } else {
        loadStocks()
    }
}

const canGrab = (item) => item.status === 1 && !item.grabbed && Number(item.remainCount) > 0

loadStocks()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>🎫 抢券中心</span>
                <span class="subtitle">限时抢券 · 下单立减</span>
            </div>
        </template>

        <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <!-- 抢券活动 -->
            <el-tab-pane label="抢券活动" name="center">
                <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
                    <el-radio-button :label="1">进行中</el-radio-button>
                    <el-radio-button :label="0">未开始</el-radio-button>
                    <el-radio-button :label="2">已结束</el-radio-button>
                    <el-radio-button :label="3">已售罄</el-radio-button>
                </el-radio-group>

                <div class="coupon-grid" v-loading="loading" v-if="stocks.length > 0">
                    <el-card v-for="item in stocks" :key="item.id" class="coupon-card" shadow="hover">
                        <div class="coupon-left">
                            <div class="value">{{ couponValueText(item) }}</div>
                            <div class="threshold">{{ couponThresholdText(item) }}</div>
                        </div>
                        <div class="coupon-right">
                            <div class="name-row">
                                <span class="name">{{ item.typeName }}</span>
                                <el-tag :type="textOf(STOCK_STATUS_TAG, item.status, 'info')" size="small">
                                    {{ textOf(STOCK_STATUS_MAP, item.status) }}
                                </el-tag>
                            </div>
                            <div class="info-row">
                                <span>剩余 {{ item.remainCount }} / {{ item.totalCount }}</span>
                                <span>领取后 {{ item.validDays }} 天有效</span>
                            </div>
                            <div class="info-row">
                                <span>{{ formatTime(item.endTime) }} 结束</span>
                                <span class="remain">{{ remainText(item.endTime) }}</span>
                            </div>
                            <div class="action-row">
                                <el-button v-if="item.grabbed" type="info" plain size="small" @click="cancel(item)">
                                    已抢到 · 取消抢购
                                </el-button>
                                <el-button v-else type="danger" size="small" :disabled="!canGrab(item)"
                                    :loading="grabbingId === item.id" @click="grab(item)">
                                    {{ Number(item.remainCount) > 0 ? '立即抢券' : '已抢光' }}
                                </el-button>
                            </div>
                        </div>
                    </el-card>
                </div>

                <el-empty v-else-if="!loading" description="该状态下暂无抢券活动" />

                <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
                    layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
                    style="margin-top: 20px; justify-content: center" />
            </el-tab-pane>

            <!-- 我的抢购记录 -->
            <el-tab-pane label="我的抢购记录" name="records">
                <el-table :data="records" v-loading="recordLoading" style="width: 100%">
                    <el-table-column label="优惠券" prop="typeName" min-width="140"></el-table-column>
                    <el-table-column label="面额" width="100">
                        <template #default="{ row }">{{ couponValueText(row) }}</template>
                    </el-table-column>
                    <el-table-column label="使用门槛" width="140">
                        <template #default="{ row }">{{ couponThresholdText(row) }}</template>
                    </el-table-column>
                    <el-table-column label="有效期" min-width="320">
                        <template #default="{ row }">
                            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                        <template #default="{ row }">
                            <el-tag :type="textOf(COUPON_STATUS_TAG, row.status, 'info')" size="small">
                                {{ textOf(COUPON_STATUS_MAP, row.status) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <template #empty>
                        <el-empty description="还没有抢购记录，快去抢一张吧" />
                    </template>
                </el-table>

                <el-pagination v-if="recordTotal > 0" v-model:current-page="recordPageNum"
                    v-model:page-size="recordPageSize" layout="total, prev, pager, next" background
                    :total="recordTotal" @current-change="onRecordPageChange"
                    style="margin-top: 20px; justify-content: center" />
            </el-tab-pane>
        </el-tabs>
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

    .coupon-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
        gap: 16px;
        min-height: 120px;

        .coupon-card {
            :deep(.el-card__body) {
                display: flex;
                gap: 16px;
                padding: 0;
            }

            .coupon-left {
                width: 130px;
                background: linear-gradient(135deg, #409eff, #79bbff);
                color: #fff;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                padding: 20px 10px;

                .value {
                    font-size: 24px;
                    font-weight: bold;
                }

                .threshold {
                    font-size: 12px;
                    margin-top: 6px;
                    opacity: 0.9;
                }
            }

            .coupon-right {
                flex: 1;
                padding: 14px 16px 14px 0;

                .name-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    gap: 8px;

                    .name {
                        font-size: 15px;
                        font-weight: bold;
                    }
                }

                .info-row {
                    display: flex;
                    justify-content: space-between;
                    font-size: 12px;
                    color: #999;
                    margin-top: 8px;

                    .remain {
                        color: #f56c6c;
                    }
                }

                .action-row {
                    margin-top: 12px;
                    text-align: right;
                }
            }
        }
    }
}
</style>
