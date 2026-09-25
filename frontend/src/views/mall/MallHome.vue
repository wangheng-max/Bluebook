<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Sell, Ticket, Goods } from '@element-plus/icons-vue'
import {
    productListService,
    productSearchService,
    productCategoryListService
} from '@/api/product.js'
import { money } from '@/utils/mall.js'

const router = useRouter()

const keyword = ref('')
const minPrice = ref(null)
const maxPrice = ref(null)
const categories = ref([])
const activeCategoryId = ref('')
const products = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(12)
const loading = ref(false)

// 加载分类导航（公开接口）
const loadCategories = async () => {
    try {
        const result = await productCategoryListService()
        categories.value = result.data || []
    } catch (e) {
        categories.value = []
    }
}

// 加载商品：有搜索条件走 search，否则走列表（第 1 页有缓存）
const loadProducts = async () => {
    loading.value = true
    try {
        const hasFilter = keyword.value.trim() || minPrice.value || maxPrice.value
        const result = hasFilter
            ? await productSearchService({
                keyword: keyword.value.trim() || null,
                categoryId: activeCategoryId.value || null,
                minPrice: minPrice.value || null,
                maxPrice: maxPrice.value || null,
                pageNum: pageNum.value,
                pageSize: pageSize.value
            })
            : await productListService({
                pageNum: pageNum.value,
                pageSize: pageSize.value,
                categoryId: activeCategoryId.value || null,
                status: 1
            })
        products.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        products.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const doSearch = () => {
    pageNum.value = 1
    loadProducts()
}

const resetSearch = () => {
    keyword.value = ''
    minPrice.value = null
    maxPrice.value = null
    activeCategoryId.value = ''
    pageNum.value = 1
    loadProducts()
}

const selectCategory = (id) => {
    activeCategoryId.value = activeCategoryId.value === id ? '' : id
    pageNum.value = 1
    loadProducts()
}

const onPageChange = (num) => {
    pageNum.value = num
    loadProducts()
}

const onSizeChange = (size) => {
    pageSize.value = size
    pageNum.value = 1
    loadProducts()
}

const toDetail = (id) => router.push(`/mall/product/${id}`)

loadCategories()
loadProducts()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>🛍 商城首页</span>
                <span class="subtitle">好物精选 · 团购更省 · 抢券立减</span>
            </div>
        </template>

        <!-- 快捷入口 -->
        <div class="quick-entry">
            <el-card shadow="hover" class="entry-card" @click="router.push('/mall/group-buy')">
                <el-icon class="entry-icon" color="#e6a23c"><Sell /></el-icon>
                <div>
                    <div class="entry-title">团购专区</div>
                    <div class="entry-desc">多人成团，享团购价</div>
                </div>
            </el-card>
            <el-card shadow="hover" class="entry-card" @click="router.push('/mall/coupon')">
                <el-icon class="entry-icon" color="#f56c6c"><Ticket /></el-icon>
                <div>
                    <div class="entry-title">抢券中心</div>
                    <div class="entry-desc">限时抢券，下单立减</div>
                </div>
            </el-card>
        </div>

        <!-- 搜索栏 -->
        <div class="search-bar">
            <el-input v-model="keyword" placeholder="搜索商品名称..." size="large" clearable @keyup.enter="doSearch">
                <template #append>
                    <el-button type="primary" :icon="Search" @click="doSearch" :loading="loading">搜索</el-button>
                </template>
            </el-input>
            <div class="price-filter">
                <el-input-number v-model="minPrice" :min="0" :controls="false" placeholder="最低价" class="price-input" />
                <span class="sep">—</span>
                <el-input-number v-model="maxPrice" :min="0" :controls="false" placeholder="最高价" class="price-input" />
                <el-button @click="doSearch">按价格筛选</el-button>
                <el-button text @click="resetSearch">重置</el-button>
            </div>
        </div>

        <!-- 分类导航 -->
        <div class="category-nav">
            <el-tag :type="!activeCategoryId ? 'primary' : 'info'" effect="dark" class="cat-tag"
                @click="selectCategory('')">全部商品</el-tag>
            <el-tag v-for="c in categories" :key="c.id" :type="activeCategoryId === c.id ? 'primary' : 'info'"
                effect="plain" class="cat-tag" @click="selectCategory(c.id)">{{ c.name }}</el-tag>
        </div>

        <!-- 商品网格 -->
        <div class="product-grid" v-loading="loading" v-if="products.length > 0">
            <el-card v-for="p in products" :key="p.id" class="product-card" shadow="hover" @click="toDetail(p.id)">
                <img :src="p.coverImg" class="cover-img" />
                <div class="card-body">
                    <h3 class="product-name">{{ p.name }}</h3>
                    <div class="price-row">
                        <span class="price">¥{{ money(p.price) }}</span>
                        <span class="market-price" v-if="Number(p.marketPrice) > Number(p.price)">
                            ¥{{ money(p.marketPrice) }}
                        </span>
                    </div>
                    <div class="meta-row">
                        <span>销量 {{ p.salesCount || 0 }}</span>
                        <span :class="{ 'out-of-stock': !p.stock }">
                            {{ p.stock > 0 ? '库存 ' + p.stock : '已售罄' }}
                        </span>
                    </div>
                </div>
            </el-card>
        </div>

        <el-empty v-else-if="!loading" description="没有找到相关商品" />

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            :page-sizes="[8, 12, 24]" layout="total, sizes, prev, pager, next" background :total="total"
            @size-change="onSizeChange" @current-change="onPageChange"
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

    .quick-entry {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 16px;
        margin-bottom: 20px;

        .entry-card {
            cursor: pointer;

            :deep(.el-card__body) {
                display: flex;
                align-items: center;
                gap: 14px;
            }

            .entry-icon {
                font-size: 32px;
            }

            .entry-title {
                font-size: 16px;
                font-weight: bold;
            }

            .entry-desc {
                font-size: 12px;
                color: #999;
                margin-top: 4px;
            }
        }
    }

    .search-bar {
        max-width: 720px;
        margin: 0 auto 20px;

        .price-filter {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-top: 12px;
            justify-content: center;

            .price-input {
                width: 120px;
            }

            .sep {
                color: #ccc;
            }
        }
    }

    .category-nav {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        justify-content: center;
        margin-bottom: 20px;

        .cat-tag {
            cursor: pointer;
            font-size: 14px;
        }
    }

    .product-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 16px;
        min-height: 120px;

        .product-card {
            cursor: pointer;
            transition: transform 0.2s;

            &:hover {
                transform: translateY(-2px);
            }

            .cover-img {
                width: 100%;
                height: 180px;
                object-fit: cover;
                border-radius: 4px;
                background-color: #f5f7fa;
            }

            .card-body {
                padding-top: 8px;

                .product-name {
                    font-size: 15px;
                    margin: 0 0 8px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .price-row {
                    display: flex;
                    align-items: baseline;
                    gap: 8px;

                    .price {
                        color: #f56c6c;
                        font-size: 18px;
                        font-weight: bold;
                    }

                    .market-price {
                        color: #bbb;
                        font-size: 12px;
                        text-decoration: line-through;
                    }
                }

                .meta-row {
                    display: flex;
                    justify-content: space-between;
                    font-size: 12px;
                    color: #999;
                    margin-top: 8px;

                    .out-of-stock {
                        color: #f56c6c;
                    }
                }
            }
        }
    }
}
</style>
