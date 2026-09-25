<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { myGroupBuyProgressService } from '@/api/groupBuy.js'
import { money, formatTime, remainText, GROUP_BUY_STATUS_MAP, GROUP_BUY_STATUS_TAG, textOf } from '@/utils/mall.js'

const router = useRouter()

const list = ref([])
const loading = ref(false)

const loadList = async () => {
    loading.value = true
    try {
        const result = await myGroupBuyProgressService()
        list.value = result.data || []
    } catch (e) {
        list.value = []
    } finally {
        loading.value = false
    }
}

// 成团进度（最高 100）
const percent = (item) => {
    const need = Number(item.groupSize || 0)
    if (need <= 0) return 0
    return Math.min(100, Math.floor((Number(item.progressCount || 0) / need) * 100))
}

const toDetail = (id) => router.push(`/mall/group-buy/${id}`)

loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>👥 我的团购</span>
                <span class="subtitle">参团记录与成团进度（支付成功后计入）</span>
            </div>
        </template>

        <div class="group-grid" v-loading="loading" v-if="list.length > 0">
            <el-card v-for="item in list" :key="item.id" class="group-card" shadow="hover"
                @click="toDetail(item.id)">
                <div class="card-head">
                    <h3 class="title">{{ item.title }}</h3>
                    <el-tag :type="textOf(GROUP_BUY_STATUS_TAG, item.status, 'info')" size="small">
                        {{ textOf(GROUP_BUY_STATUS_MAP, item.status) }}
                    </el-tag>
                </div>

                <div class="price-row">
                    <span class="group-price">¥{{ money(item.groupPrice) }}</span>
                    <span class="my-num">我的份额：{{ item.myGroupNum || 1 }} 份</span>
                </div>

                <el-progress :percentage="percent(item)" :stroke-width="10"
                    :status="item.status === 2 ? 'success' : undefined" />
                <div class="progress-text">
                    已参团 <strong>{{ item.progressCount || 0 }}</strong> / {{ item.groupSize }} 人成团
                </div>

                <div class="time-row">
                    <span>结束：{{ formatTime(item.endTime) }}（{{ remainText(item.endTime) }}）</span>
                </div>

                <el-alert v-if="item.status === 2" type="success" :closable="false" show-icon
                    title="已成团，等待商家发货" class="status-alert" />
                <el-alert v-else-if="item.status === 3" type="info" :closable="false" show-icon
                    title="活动已关闭，款项将原路退回" class="status-alert" />
                <el-alert v-else-if="item.status === 1" type="warning" :closable="false" show-icon
                    title="未成团到期将自动退款" class="status-alert" />
            </el-card>
        </div>

        <el-empty v-else-if="!loading" description="还没有参团记录，去团购专区看看">
            <el-button type="primary" @click="router.push('/mall/group-buy')">去团购专区</el-button>
        </el-empty>
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

    .group-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 16px;
        min-height: 120px;

        .group-card {
            cursor: pointer;
            transition: transform 0.2s;

            &:hover {
                transform: translateY(-2px);
            }

            .card-head {
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                gap: 8px;

                .title {
                    font-size: 16px;
                    margin: 0;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }

            .price-row {
                display: flex;
                align-items: baseline;
                justify-content: space-between;
                margin: 12px 0 8px;

                .group-price {
                    color: #f56c6c;
                    font-size: 22px;
                    font-weight: bold;
                }

                .my-num {
                    color: #409eff;
                    font-size: 12px;
                }
            }

            .progress-text {
                font-size: 12px;
                color: #666;
                margin-top: 6px;
            }

            .time-row {
                font-size: 12px;
                color: #999;
                margin-top: 8px;
            }

            .status-alert {
                margin-top: 12px;
            }
        }
    }
}
</style>
