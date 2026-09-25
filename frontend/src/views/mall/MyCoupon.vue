<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { myCouponListService } from '@/api/coupon.js'
import { formatTime, couponValueText, couponThresholdText, COUPON_STATUS_MAP, textOf } from '@/utils/mall.js'

const router = useRouter()

const coupons = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const status = ref(0)
const loading = ref(false)

const loadList = async () => {
    loading.value = true
    try {
        const result = await myCouponListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value
        })
        coupons.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        coupons.value = []
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

loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>🎟 我的优惠券</span>
                <span class="subtitle">下单时可在确认页选择使用</span>
            </div>
        </template>

        <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
            <el-radio-button :label="0">未使用</el-radio-button>
            <el-radio-button :label="1">已使用</el-radio-button>
            <el-radio-button :label="2">已过期</el-radio-button>
        </el-radio-group>

        <div class="coupon-grid" v-loading="loading" v-if="coupons.length > 0">
            <el-card v-for="c in coupons" :key="c.id" class="coupon-card" shadow="hover"
                :class="{ disabled: c.status !== 0 }">
                <div class="coupon-left">
                    <div class="value">{{ couponValueText(c) }}</div>
                    <div class="threshold">{{ couponThresholdText(c) }}</div>
                </div>
                <div class="coupon-right">
                    <div class="name-row">
                        <span class="name">{{ c.typeName }}</span>
                        <el-tag size="small" :type="c.status === 0 ? 'danger' : 'info'">
                            {{ textOf(COUPON_STATUS_MAP, c.status) }}
                        </el-tag>
                    </div>
                    <div class="info-row">有效期至 {{ formatTime(c.endTime) }}</div>
                    <div class="action-row" v-if="c.status === 0">
                        <el-button type="primary" size="small" plain @click="router.push('/mall')">
                            去使用
                        </el-button>
                    </div>
                </div>
            </el-card>
        </div>

        <el-empty v-else-if="!loading" description="暂无优惠券，去抢券中心看看" />

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center" />
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

            &.disabled {
                opacity: 0.6;
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
                    font-size: 12px;
                    color: #999;
                    margin-top: 8px;
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
