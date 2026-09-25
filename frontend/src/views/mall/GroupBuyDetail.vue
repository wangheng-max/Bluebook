<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { groupBuyDetailService } from '@/api/groupBuy.js'
import { money, formatTime, remainText, GROUP_BUY_STATUS_MAP, GROUP_BUY_STATUS_TAG, textOf } from '@/utils/mall.js'

const route = useRoute()
const router = useRouter()

const groupBuyId = Number(route.params.id)
const detail = ref({})
const loading = ref(false)

const loadDetail = async () => {
    loading.value = true
    try {
        const result = await groupBuyDetailService(groupBuyId)
        detail.value = result.data || {}
    } catch (e) {
        detail.value = {}
    } finally {
        loading.value = false
    }
}

// 参团：跳到下单确认页（团购订单 orderType=2）
const joinGroup = () => {
    if (detail.value.status !== 1) {
        ElMessage.warning('团购活动未开始或已结束')
        return
    }
    if (detail.value.joined) {
        ElMessage.warning('您已参与过该团购，每人限参一次')
        return
    }
    router.push({
        path: '/mall/order/confirm',
        query: {
            orderType: 2,
            groupBuyId: groupBuyId,
            productId: detail.value.productId,
            quantity: 1
        }
    })
}

const percent = () => {
    const need = Number(detail.value.groupSize || 0)
    if (need <= 0) return 0
    return Math.min(100, Math.floor((Number(detail.value.progressCount || 0) / need) * 100))
}

loadDetail()
</script>

<template>
    <el-card class="page-container" v-loading="loading">
        <template #header>
            <div class="header">
                <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
                <span>团购详情</span>
            </div>
        </template>

        <div class="detail-wrap" v-if="detail.id">
            <img :src="detail.productCover" class="cover" />

            <div class="info">
                <div class="title-row">
                    <h2>{{ detail.title }}</h2>
                    <el-tag :type="textOf(GROUP_BUY_STATUS_TAG, detail.status, 'info')">
                        {{ textOf(GROUP_BUY_STATUS_MAP, detail.status) }}
                    </el-tag>
                </div>
                <p class="product-name">商品：{{ detail.productName }}</p>

                <div class="price-box">
                    <span class="group-price">团购价 ¥{{ money(detail.groupPrice) }}</span>
                    <span class="origin-price" v-if="detail.originPrice">原价 ¥{{ money(detail.originPrice) }}</span>
                </div>

                <el-progress :percentage="percent()" :stroke-width="14"
                    :status="detail.status === 2 ? 'success' : undefined" />
                <div class="progress-text">
                    已参团 <strong>{{ detail.progressCount || 0 }}</strong> / {{ detail.groupSize }} 人成团
                    <span v-if="detail.maxGroupSize">（活动上限 {{ detail.maxGroupSize }} 人）</span>
                </div>

                <div class="time-box">
                    <div>开始：{{ formatTime(detail.startTime) }}</div>
                    <div>结束：{{ formatTime(detail.endTime) }}（{{ remainText(detail.endTime) }}）</div>
                </div>

                <el-alert v-if="detail.joined" type="success" :closable="false" show-icon
                    :title="`您已参与本团购，份额 ${detail.myGroupNum || 1} 份，等待成团后商家发货`" />

                <div class="actions">
                    <el-button type="danger" size="large" :disabled="detail.status !== 1 || detail.joined"
                        @click="joinGroup">
                        {{ detail.joined ? '已参团' : '立即参团' }}
                    </el-button>
                    <span class="tip">未成团到期将自动退款，成团后商家发货</span>
                </div>

                <div class="description" v-if="detail.description">
                    <h3>活动说明</h3>
                    <p>{{ detail.description }}</p>
                </div>
            </div>
        </div>

        <el-empty v-else-if="!loading" description="团购活动不存在" />
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
        font-size: 18px;
        font-weight: bold;
    }

    .detail-wrap {
        display: grid;
        grid-template-columns: 360px 1fr;
        gap: 30px;

        .cover {
            width: 100%;
            height: 320px;
            object-fit: cover;
            border-radius: 4px;
            background-color: #f5f7fa;
        }

        .info {
            .title-row {
                display: flex;
                align-items: center;
                gap: 12px;

                h2 {
                    margin: 0;
                    font-size: 22px;
                }
            }

            .product-name {
                color: #666;
                font-size: 14px;
                margin: 8px 0 16px;
            }

            .price-box {
                display: flex;
                align-items: baseline;
                gap: 16px;
                background-color: #fef0f0;
                border-radius: 4px;
                padding: 14px 16px;
                margin-bottom: 16px;

                .group-price {
                    color: #f56c6c;
                    font-size: 26px;
                    font-weight: bold;
                }

                .origin-price {
                    color: #bbb;
                    text-decoration: line-through;
                }
            }

            .progress-text {
                margin: 8px 0 16px;
                color: #666;
                font-size: 13px;
            }

            .time-box {
                background-color: #f5f7fa;
                border-radius: 4px;
                padding: 12px 16px;
                color: #666;
                font-size: 13px;
                line-height: 1.8;
                margin-bottom: 16px;
            }

            .actions {
                display: flex;
                align-items: center;
                gap: 16px;
                margin: 20px 0;

                .tip {
                    color: #999;
                    font-size: 12px;
                }
            }

            .description {
                border-top: 1px solid #ebeef5;
                padding-top: 16px;

                h3 {
                    font-size: 15px;
                    margin: 0 0 8px;
                }

                p {
                    color: #666;
                    line-height: 1.8;
                    white-space: pre-wrap;
                    margin: 0;
                }
            }
        }
    }
}
</style>
