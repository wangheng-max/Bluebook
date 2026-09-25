<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { groupBuyListService } from '@/api/groupBuy.js'
import { money, formatTime, GROUP_BUY_STATUS_MAP, GROUP_BUY_STATUS_TAG, textOf } from '@/utils/mall.js'

const router = useRouter()

const activities = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(9)
const status = ref(1)
const loading = ref(false)

const loadList = async () => {
    loading.value = true
    try {
        const result = await groupBuyListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value
        })
        activities.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        activities.value = []
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

// 成团进度百分比（最高 100）
const progressPercent = (item) => {
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
                <span>👥 团购专区</span>
                <span class="subtitle">凑够人数即成团，享团购价</span>
            </div>
        </template>

        <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
            <el-radio-button :label="1">进行中</el-radio-button>
            <el-radio-button :label="0">未开始</el-radio-button>
            <el-radio-button :label="2">已成团</el-radio-button>
            <el-radio-button :label="3">已关闭</el-radio-button>
        </el-radio-group>

        <div class="group-grid" v-loading="loading" v-if="activities.length > 0">
            <el-card v-for="item in activities" :key="item.id" class="group-card" shadow="hover"
                @click="toDetail(item.id)">
                <div class="card-head">
                    <h3 class="title">{{ item.title }}</h3>
                    <el-tag :type="textOf(GROUP_BUY_STATUS_TAG, item.status, 'info')" size="small">
                        {{ textOf(GROUP_BUY_STATUS_MAP, item.status) }}
                    </el-tag>
                </div>
                <div class="price-row">
                    <span class="group-price">¥{{ money(item.groupPrice) }}</span>
                    <span class="size-tip">{{ item.groupSize }} 人成团</span>
                </div>
                <el-progress :percentage="progressPercent(item)" :stroke-width="10"
                    :status="item.status === 2 ? 'success' : undefined" />
                <div class="progress-text">
                    已参团 {{ item.progressCount || 0 }} / {{ item.groupSize }} 人
                    <span v-if="item.maxGroupSize">（上限 {{ item.maxGroupSize }} 人）</span>
                </div>
                <div class="time-row">
                    <span>{{ formatTime(item.startTime) }} ~ {{ formatTime(item.endTime) }}</span>
                </div>
            </el-card>
        </div>

        <el-empty v-else-if="!loading" description="该状态下暂无团购活动" />

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

    .group-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
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
                gap: 10px;
                margin: 12px 0 8px;

                .group-price {
                    color: #f56c6c;
                    font-size: 22px;
                    font-weight: bold;
                }

                .size-tip {
                    color: #e6a23c;
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
        }
    }
}
</style>
