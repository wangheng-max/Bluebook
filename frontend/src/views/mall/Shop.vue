<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { shopInfoService, shopProductsService, shopRatingsService } from '@/api/shop.js'
import { money } from '@/utils/mall.js'

const route = useRoute()
const router = useRouter()

const shop = ref(null)
const products = ref([])
const productTotal = ref(0)
const productPage = ref(1)
const productPageSize = ref(12)
const ratings = ref([])
const ratingTotal = ref(0)
const ratingPage = ref(1)
const ratingPageSize = ref(5)
const loading = ref(false)

const merchantUserId = computed(() => Number(route.params.merchantUserId))

const loadShop = async () => {
    loading.value = true
    try {
        const result = await shopInfoService(merchantUserId.value)
        shop.value = result.data || null
    } catch (e) {
        shop.value = null
    } finally {
        loading.value = false
    }
}

const loadProducts = async () => {
    try {
        const result = await shopProductsService(merchantUserId.value, {
            pageNum: productPage.value,
            pageSize: productPageSize.value
        })
        products.value = result.data.items || []
        productTotal.value = result.data.total || 0
    } catch (e) {
        products.value = []
        productTotal.value = 0
    }
}

const loadRatings = async () => {
    try {
        const result = await shopRatingsService(merchantUserId.value, {
            pageNum: ratingPage.value,
            pageSize: ratingPageSize.value
        })
        ratings.value = result.data.items || []
        ratingTotal.value = result.data.total || 0
    } catch (e) {
        ratings.value = []
        ratingTotal.value = 0
    }
}

const onProductPage = (num) => {
    productPage.value = num
    loadProducts()
}

const onRatingPage = (num) => {
    ratingPage.value = num
    loadRatings()
}

watch(merchantUserId, () => {
    productPage.value = 1
    ratingPage.value = 1
    loadShop()
    loadProducts()
    loadRatings()
}, { immediate: true })
</script>

<template>
    <div class="shop-page" v-loading="loading">
        <!-- 店铺头部 -->
        <el-card class="shop-header" v-if="shop">
            <div class="header-wrap">
                <el-avatar :size="72" :src="shop.shopLogo" shape="square">
                    {{ shop.shopName?.charAt(0) }}
                </el-avatar>
                <div class="shop-info">
                    <h2 class="shop-name">{{ shop.shopName }}</h2>
                    <div class="shop-desc">{{ shop.shopDescription || '这家店主很懒，还没有店铺简介' }}</div>
                    <div class="shop-meta">
                        <span v-if="shop.province || shop.city || shop.district">
                            📍 {{ shop.province }}{{ shop.city }}{{ shop.district }} {{ shop.address }}
                        </span>
                        <span>📦 在架商品 {{ shop.productCount }}</span>
                    </div>
                </div>
                <div class="shop-score">
                    <div class="score-top">
                        <span class="score-num">{{ shop.avgScore ? shop.avgScore.toFixed(1) : '暂无' }}</span>
                        <el-rate v-if="shop.avgScore" :model-value="shop.avgScore" disabled allow-half
                            size="small" class="score-rate" />
                    </div>
                    <div class="score-count">{{ shop.ratingCount }} 人评价</div>
                </div>
            </div>
        </el-card>

        <el-empty v-else-if="!loading" description="店铺不存在或未通过认证" />

        <template v-if="shop">
            <!-- 全部商品 -->
            <el-card class="section">
                <template #header>
                    <span class="section-title">全部商品</span>
                </template>
                <div class="product-grid" v-if="products.length > 0">
                    <div class="product-item" v-for="p in products" :key="p.id"
                        @click="router.push(`/mall/product/${p.id}`)">
                        <img :src="p.coverImg" class="product-cover" />
                        <div class="p-name">{{ p.name }}</div>
                        <div class="p-bottom">
                            <span class="p-price">¥{{ money(p.price) }}</span>
                            <span class="p-sales">已售 {{ p.salesCount || 0 }}</span>
                        </div>
                    </div>
                </div>
                <el-empty v-else description="店铺还没有在架商品" />
                <el-pagination v-if="productTotal > productPageSize"
                    v-model:current-page="productPage" :page-size="productPageSize"
                    layout="prev, pager, next" background :total="productTotal"
                    @current-change="onProductPage" style="margin-top: 16px; justify-content: center" />
            </el-card>

            <!-- 买家评价 -->
            <el-card class="section">
                <template #header>
                    <span class="section-title">买家评价（{{ ratingTotal }}）</span>
                </template>
                <div v-if="ratings.length > 0" class="rating-list">
                    <div class="rating-item" v-for="r in ratings" :key="r.id">
                        <el-avatar :size="36" :src="r.userPic">{{ r.username?.charAt(0) }}</el-avatar>
                        <div class="rating-body">
                            <div class="rating-head">
                                <span class="r-name">{{ r.username }}</span>
                                <el-rate :model-value="r.score" disabled size="small" class="r-rate" />
                            </div>
                            <div class="r-content" v-if="r.content">{{ r.content }}</div>
                            <div class="r-time">{{ r.createTime }}</div>
                        </div>
                    </div>
                </div>
                <el-empty v-else description="还没有评价，购买后可以来评价" />
                <el-pagination v-if="ratingTotal > ratingPageSize"
                    v-model:current-page="ratingPage" :page-size="ratingPageSize"
                    layout="prev, pager, next" background :total="ratingTotal"
                    @current-change="onRatingPage" style="margin-top: 16px; justify-content: center" />
            </el-card>
        </template>
    </div>
</template>

<style lang="scss" scoped>
.shop-page {
    min-height: 100%;

    .shop-header {
        margin-bottom: 20px;

        .header-wrap {
            display: flex;
            align-items: center;
            gap: 20px;
        }

        .shop-info {
            flex: 1;
            min-width: 0;

            .shop-name {
                margin: 0 0 6px;
                font-size: 22px;
            }

            .shop-desc {
                color: #666;
                font-size: 14px;
                margin-bottom: 8px;
            }

            .shop-meta {
                display: flex;
                gap: 20px;
                color: #999;
                font-size: 13px;
            }
        }

        .shop-score {
            text-align: center;
            padding: 0 12px;

            .score-top {
                display: flex;
                align-items: baseline;
                gap: 8px;
            }

            .score-num {
                font-size: 32px;
                font-weight: bold;
                color: #ff9900;
            }

            .score-count {
                color: #999;
                font-size: 12px;
                margin-top: 4px;
            }
        }
    }

    .section {
        margin-bottom: 20px;

        .section-title {
            font-size: 16px;
            font-weight: bold;
        }
    }

    .product-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
        gap: 16px;

        .product-item {
            border: 1px solid #ebeef5;
            border-radius: 10px;
            overflow: hidden;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
            background: #fff;

            &:hover {
                transform: translateY(-3px);
                box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
            }

            .product-cover {
                width: 100%;
                aspect-ratio: 1;
                object-fit: cover;
                display: block;
                background: #f5f7fa;
            }

            .p-name {
                padding: 10px 12px 4px;
                font-size: 14px;
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
                overflow: hidden;
            }

            .p-bottom {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 4px 12px 12px;

                .p-price {
                    color: #f56c6c;
                    font-weight: bold;
                }

                .p-sales {
                    color: #bbb;
                    font-size: 12px;
                }
            }
        }
    }

    .rating-list {
        .rating-item {
            display: flex;
            gap: 12px;
            padding: 14px 0;
            border-bottom: 1px solid #f2f4f7;

            &:last-child { border-bottom: none; }

            .rating-body {
                flex: 1;

                .rating-head {
                    display: flex;
                    align-items: center;
                    gap: 10px;

                    .r-name { font-weight: 600; font-size: 14px; }
                }

                .r-content {
                    margin: 8px 0 4px;
                    color: #444;
                    font-size: 14px;
                    line-height: 1.6;
                }

                .r-time { color: #bbb; font-size: 12px; }
            }
        }
    }
}
</style>
