<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { productDetailService, productSkuListService } from '@/api/product.js'
import { shopInfoService } from '@/api/shop.js'
import { money } from '@/utils/mall.js'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()

const productId = Number(route.params.id)
const product = ref({})
const skus = ref([])
const shop = ref(null) // 商品所属店铺（认证商家才有）
const selectedSkuId = ref(null)
const quantity = ref(1)
const loading = ref(false)
// 视频源加载失败标记（分享页链接/防盗链/Content-Type 不对都会触发）
const videoError = ref(false)

// 详情图：后端存 JSON 数组字符串
const detailImages = computed(() => {
    try {
        const arr = JSON.parse(product.value.images || '[]')
        return Array.isArray(arr) ? arr : []
    } catch (e) {
        return []
    }
})

// 轮播图：封面 + 详情图（去重）
const galleryImages = computed(() => {
    const list = []
    if (product.value.coverImg) list.push(product.value.coverImg)
    detailImages.value.forEach(img => {
        if (img && !list.includes(img)) list.push(img)
    })
    return list.length ? list : ['']
})

const selectedSku = computed(() => skus.value.find(s => s.id === selectedSkuId.value) || null)

// 单价：选中 SKU 用 SKU 价，否则用商品价
const unitPrice = computed(() => selectedSku.value ? selectedSku.value.price : product.value.price)

// 可购库存
const availableStock = computed(() => {
    if (skus.value.length > 0) {
        return selectedSku.value ? Number(selectedSku.value.stock || 0) : 0
    }
    return Number(product.value.stock || 0)
})

const loadDetail = async () => {
    loading.value = true
    videoError.value = false
    try {
        const result = await productDetailService(productId)
        product.value = result.data || {}
    } catch (e) {
        product.value = {}
        loading.value = false
        return
    }
    // SKU 列表（公开接口，纯新增见 Bug 清单 B-004）：失败不影响商品主体展示
    try {
        const skuResult = await productSkuListService(productId)
        skus.value = skuResult.data || []
        if (skus.value.length > 0) {
            selectedSkuId.value = skus.value[0].id
        }
    } catch (e) {
        skus.value = []
    } finally {
        loading.value = false
    }
    // 店铺信息（商家认证店铺才有）：失败不影响商品主体展示
    try {
        if (product.value.createUserId) {
            const shopResult = await shopInfoService(product.value.createUserId)
            shop.value = shopResult.data || null
        }
    } catch (e) {
        shop.value = null
    }
}

const buyNow = () => {
    if (product.value.status !== 1) {
        ElMessage.warning('商品已下架，暂不可购买')
        return
    }
    if (skus.value.length > 0 && !selectedSkuId.value) {
        ElMessage.warning('请选择商品规格')
        return
    }
    if (availableStock.value <= 0) {
        ElMessage.warning('该规格库存不足')
        return
    }
    if (quantity.value > availableStock.value) {
        ElMessage.warning(`最多可购买 ${availableStock.value} 件`)
        return
    }
    router.push({
        path: '/mall/order/confirm',
        query: {
            orderType: 1,
            productId: productId,
            skuId: selectedSkuId.value || '',
            quantity: quantity.value
        }
    })
}

loadDetail()
</script>

<template>
    <div class="product-page">
        <el-card class="page-container" v-loading="loading">
        <template #header>
            <div class="header">
                <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
                <span>商品详情</span>
            </div>
        </template>

        <div class="detail-wrap" v-if="product.id">
            <!-- 图片区 -->
            <div class="gallery">
                <el-carousel height="360px" :autoplay="false" indicator-position="outside">
                    <el-carousel-item v-for="(img, idx) in galleryImages" :key="idx">
                        <img :src="img" class="gallery-img" />
                    </el-carousel-item>
                </el-carousel>
            </div>

            <!-- 信息区 -->
            <div class="info">
                <h2 class="title">{{ product.name }}</h2>
                <div class="price-box">
                    <span class="price">¥{{ money(unitPrice) }}</span>
                    <span class="market-price" v-if="Number(product.marketPrice) > Number(unitPrice)">
                        ¥{{ money(product.marketPrice) }}
                    </span>
                    <el-tag v-if="product.status !== 1" type="info">已下架</el-tag>
                </div>
                <div class="meta">
                    <span>销量 {{ product.salesCount || 0 }}</span>
                    <span>浏览 {{ product.viewCount || 0 }}</span>
                    <span>库存 {{ availableStock }}</span>
                </div>

                <!-- 店铺卡片：评分 + 进店看全部商品 -->
                <div class="shop-card" v-if="shop" @click="router.push(`/mall/shop/${shop.userId}`)">
                    <el-avatar :size="40" :src="shop.shopLogo" shape="square">
                        {{ shop.shopName?.charAt(0) }}
                    </el-avatar>
                    <div class="shop-mid">
                        <div class="shop-name">{{ shop.shopName }}</div>
                        <div class="shop-score">
                            <el-rate v-if="shop.avgScore" :model-value="shop.avgScore" disabled allow-half
                                size="small" />
                            <span class="score-text">
                                {{ shop.avgScore ? shop.avgScore.toFixed(1) + ' 分' : '暂无评分' }}
                                · {{ shop.ratingCount }} 人评价 · {{ shop.productCount }} 件商品
                            </span>
                        </div>
                    </div>
                    <el-button type="warning" size="small" round plain>进店 ></el-button>
                </div>

                <!-- SKU 规格选择 -->
                <div class="sku-block" v-if="skus.length > 0">
                    <div class="label">规格</div>
                    <el-radio-group v-model="selectedSkuId">
                        <el-radio-button v-for="sku in skus" :key="sku.id" :label="sku.id"
                            :disabled="Number(sku.stock) <= 0">
                            {{ sku.skuName }}（¥{{ money(sku.price) }}）
                        </el-radio-button>
                    </el-radio-group>
                </div>

                <!-- 数量 -->
                <div class="qty-block">
                    <div class="label">数量</div>
                    <el-input-number v-model="quantity" :min="1" :max="availableStock > 0 ? availableStock : 1" />
                </div>

                <div class="actions">
                    <el-button type="danger" size="large" :icon="ShoppingCart" @click="buyNow">
                        立即购买
                    </el-button>
                    <span class="tip">下单后可选择优惠券抵扣，团购商品请前往团购专区下单</span>
                </div>
            </div>
        </div>

        <el-empty v-else-if="!loading" description="商品不存在或已下架" />

        <!-- 商品详情 -->
        <div class="description" v-if="product.description || product.videoUrl">
            <h3>商品详情</h3>
            <template v-if="product.videoUrl">
                <video :src="product.videoUrl" controls class="detail-video" @error="videoError = true"></video>
                <div v-if="videoError" class="video-error-tip">
                    视频加载失败：请确认商家填写的是 mp4/webm 视频直链，且链接未开启防盗链
                </div>
            </template>
            <div class="desc-text" v-if="product.description">{{ product.description }}</div>
        </div>
        </el-card>

        <!-- 评论区（商品评论：点赞/回复/按时间与点赞排序） -->
        <CommentSection v-if="product.id" target-type="product" :target-id="productId" />
    </div>
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
        grid-template-columns: 420px 1fr;
        gap: 30px;

        .gallery {
            .gallery-img {
                width: 100%;
                height: 360px;
                object-fit: contain;
                background-color: #f5f7fa;
                border-radius: 4px;
            }
        }

        .info {
            .title {
                font-size: 22px;
                margin: 0 0 16px;
            }

            .price-box {
                background-color: #fef0f0;
                border-radius: 4px;
                padding: 16px;
                display: flex;
                align-items: baseline;
                gap: 12px;

                .price {
                    color: #f56c6c;
                    font-size: 28px;
                    font-weight: bold;
                }

                .market-price {
                    color: #bbb;
                    text-decoration: line-through;
                }
            }

            .meta {
                display: flex;
                gap: 24px;
                color: #999;
                font-size: 13px;
                margin: 16px 0;
            }

            .shop-card {
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 12px;
                border: 1px solid #ebeef5;
                border-radius: 10px;
                margin-bottom: 16px;
                cursor: pointer;
                transition: box-shadow 0.2s;

                &:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); }

                .shop-mid {
                    flex: 1;
                    min-width: 0;

                    .shop-name {
                        font-weight: 600;
                        margin-bottom: 4px;
                    }

                    .shop-score {
                        display: flex;
                        align-items: center;
                        gap: 6px;

                        .score-text {
                            color: #999;
                            font-size: 12px;
                        }
                    }
                }
            }

            .sku-block,
            .qty-block {
                display: flex;
                align-items: center;
                gap: 16px;
                margin-bottom: 16px;

                .label {
                    color: #666;
                    width: 40px;
                }
            }

            .actions {
                display: flex;
                align-items: center;
                gap: 16px;
                margin-top: 24px;

                .tip {
                    color: #999;
                    font-size: 12px;
                }
            }
        }
    }

    .description {
        margin-top: 30px;
        border-top: 1px solid #ebeef5;
        padding-top: 20px;

        h3 {
            font-size: 16px;
            margin: 0 0 16px;
        }

        .detail-video {
            width: 100%;
            max-width: 720px;
            display: block;
            margin-bottom: 16px;
        }

        .video-error-tip {
            max-width: 720px;
            color: #e6a23c;
            font-size: 13px;
            margin-bottom: 16px;
        }

        .desc-text {
            color: #666;
            line-height: 1.8;
            white-space: pre-wrap;
        }
    }
}
</style>
