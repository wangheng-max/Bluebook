<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { searchNotesService, searchNotesByTagService } from '@/api/search.js'
import { communityHotService, communityArticlesService } from '@/api/community.js'
import { articleCategoryListService } from '@/api/article.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const keyword = ref('')
const articles = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 分类导航：全部 + 内置分类
const categories = ref([])
const activeCategoryId = ref('')

// 当前标签（点击任意标签进入标签检索模式，路由 query 驱动）
const activeTag = ref('')

// 当前内容形态：hot(热门) / category(按分类浏览) / search(搜索结果) / tag(标签检索)
const viewMode = ref('hot')

// 加载分类导航（社区首页只展示系统内置分类，用户自建分类仅个人可见）
const loadCategories = async () => {
    let result = await articleCategoryListService()
    categories.value = (result.data || []).filter(c => c.isSystem === 1)
}

// 拉取当前形态的数据
const loadList = async () => {
    loading.value = true
    try {
        let result
        if (viewMode.value === 'search') {
            result = await searchNotesService({
                keyword: keyword.value,
                page: pageNum.value,
                size: pageSize.value
            })
        } else if (viewMode.value === 'tag') {
            result = await searchNotesByTagService({
                tag: activeTag.value,
                page: pageNum.value,
                size: pageSize.value
            })
        } else if (viewMode.value === 'category') {
            result = await communityArticlesService({
                categoryId: activeCategoryId.value,
                pageNum: pageNum.value,
                pageSize: pageSize.value
            })
        } else {
            result = await communityHotService({
                pageNum: pageNum.value,
                pageSize: pageSize.value
            })
        }
        articles.value = result.data.items
        total.value = result.data.total
    } finally {
        loading.value = false
    }
}

const searchNotes = () => {
    if (!keyword.value.trim()) {
        // 清空搜索词回到热门
        viewMode.value = 'hot'
        pageNum.value = 1
        loadList()
        return
    }
    viewMode.value = 'search'
    pageNum.value = 1
    loadList()
}

const selectCategory = (categoryId) => {
    activeCategoryId.value = categoryId
    viewMode.value = categoryId ? 'category' : 'hot'
    pageNum.value = 1
    loadList()
}

// 点击标签 → 标签检索模式（其他页面跳转 /community/feed?tag=xx 也走这里）
const selectTag = (tag) => {
    router.push({ path: '/community/feed', query: { tag } })
}

const clearTag = () => {
    router.push({ path: '/community/feed' })
}

// 路由 query 变化驱动标签模式（含从其他页面点标签跳回来的场景）
watch(() => route.query.tag, (tag) => {
    activeTag.value = tag || ''
    if (tag) {
        viewMode.value = 'tag'
    } else if (viewMode.value === 'tag') {
        viewMode.value = 'hot'
    }
    pageNum.value = 1
    loadList()
}, { immediate: true })

const onPageChange = (num) => {
    pageNum.value = num
    loadList()
}

const onSizeChange = (size) => {
    pageSize.value = size
    pageNum.value = 1
    loadList()
}

const viewDetail = (noteId) => {
    router.push(`/article/detail/${noteId}`)
}

const goProduct = (productId) => {
    router.push(`/mall/product/${productId}`)
}

loadCategories()
</script>

<template>
    <el-card class="community-feed">
        <template #header>
            <div class="feed-header">
                <span class="title">🏠 社区首页</span>
                <span class="subtitle">热点推荐 · 分类浏览 · 发现精彩</span>
            </div>
        </template>

        <!-- 搜索栏 -->
        <div class="search-bar">
            <el-input
                v-model="keyword"
                placeholder="搜索你感兴趣的内容..."
                size="large"
                clearable
                @keyup.enter="searchNotes"
            >
                <template #append>
                    <el-button type="primary" @click="searchNotes" :loading="loading">
                        <el-icon><Search /></el-icon>
                        搜索
                    </el-button>
                </template>
            </el-input>
        </div>

        <!-- 分类导航条（内置分类，Redis 缓存秒开） -->
        <div class="category-nav">
            <el-tag
                :type="!activeCategoryId && viewMode !== 'search' ? 'primary' : 'info'"
                effect="dark"
                class="cat-tag"
                @click="selectCategory('')"
            >🔥 热门</el-tag>
            <el-tag
                v-for="c in categories"
                :key="c.id"
                :type="activeCategoryId == c.id ? 'primary' : 'info'"
                effect="plain"
                class="cat-tag"
                @click="selectCategory(c.id)"
            >{{ c.categoryName }}</el-tag>
        </div>

        <!-- 标签检索模式横幅 -->
        <el-alert v-if="viewMode === 'tag'" type="primary" :closable="true" class="tag-banner"
            @close="clearTag">
            <template #title>
                标签检索：<b># {{ activeTag }}</b>　共 {{ total }} 篇相关文章
            </template>
        </el-alert>

        <!-- 文章卡片列表 -->
        <div class="article-grid" v-if="articles.length > 0">
            <el-card
                v-for="article in articles"
                :key="article.noteId"
                class="article-card"
                shadow="hover"
                @click="viewDetail(article.noteId)"
            >
                <img v-if="article.coverImage" :src="article.coverImage" class="cover-img" />
                <div class="card-content">
                    <h3 class="article-title">{{ article.title }}</h3>
                    <p class="article-summary">{{ article.summary }}</p>

                    <!-- 标签：点击直接检索该标签（与带货商品区分开） -->
                    <div class="tag-row" v-if="article.tags && article.tags.length">
                        <el-tag v-for="t in article.tags.slice(0, 3)" :key="t" size="small" effect="plain"
                            class="feed-tag" @click.stop="selectTag(t)"># {{ t }}</el-tag>
                        <el-tag v-if="article.tags.length > 3" size="small" type="info" effect="plain">
                            +{{ article.tags.length - 3 }}
                        </el-tag>
                    </div>

                    <!-- 带货商品：点击直接跳商品页购买 -->
                    <div class="product-chip" v-if="article.products && article.products.length"
                        @click.stop="goProduct(article.products[0].id)">
                        <span class="chip-icon">🛍</span>
                        <span class="chip-name">{{ article.products[0].name }}</span>
                        <span class="chip-price">¥{{ article.products[0].price }}</span>
                        <span class="chip-go">去看看 →</span>
                    </div>

                    <div class="article-meta">
                        <div class="author-info">
                            <el-avatar :size="24" :src="article.authorAvatar">
                                {{ article.authorName?.charAt(0) }}
                            </el-avatar>
                            <span class="author-name">{{ article.authorName }}</span>
                        </div>
                        <div class="stats">
                            <span>👁 {{ article.viewCount || 0 }}</span>
                            <span>❤️ {{ article.likeCount || 0 }}</span>
                            <span>⭐ {{ article.favoriteCount || 0 }}</span>
                            <span class="time">{{ formatDate(article.createTime) }}</span>
                        </div>
                    </div>
                </div>
            </el-card>
        </div>

        <el-empty v-else-if="viewMode === 'search'" description="未找到相关内容，换个关键词试试" />
        <el-empty v-else-if="viewMode === 'tag'" :description="`还没有带「# ${activeTag}」标签的文章`" />
        <el-empty v-else-if="viewMode === 'category'" description="该分类下暂无文章" />
        <el-empty v-else description="还没有热门文章，快去发布第一篇吧" />

        <!-- 分页 -->
        <el-pagination
            v-if="total > 0"
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            background
            :total="total"
            @size-change="onSizeChange"
            @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center"
        />
    </el-card>
</template>

<script>
import { Search } from '@element-plus/icons-vue'

function formatDate(dateStr) {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    const now = new Date()
    const diff = now - d
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
    return dateStr.substring(0, 10)
}
</script>

<style lang="scss" scoped>
.community-feed {
    min-height: 100%;
    .feed-header {
        display: flex;
        align-items: baseline;
        gap: 12px;
        .title { font-size: 18px; font-weight: bold; }
        .subtitle { color: #999; font-size: 14px; }
    }
    .search-bar {
        max-width: 600px;
        margin: 0 auto 24px;
    }
    .tag-banner {
        margin-bottom: 20px;
    }
    .tag-row {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        margin-bottom: 10px;
        .feed-tag {
            cursor: pointer;
        }
    }
    .product-chip {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 10px;
        margin-bottom: 12px;
        background: #fff7f0;
        border: 1px solid #ffe3c9;
        border-radius: 8px;
        cursor: pointer;
        transition: box-shadow 0.2s;
        &:hover { box-shadow: 0 2px 8px rgba(230, 140, 60, 0.15); }
        .chip-icon { font-size: 16px; }
        .chip-name {
            flex: 1;
            font-size: 13px;
            color: #666;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .chip-price { color: #f56c6c; font-weight: bold; font-size: 13px; }
        .chip-go { color: #e68c3c; font-size: 12px; white-space: nowrap; }
    }
    .category-nav {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        justify-content: center;
        margin-bottom: 24px;
        .cat-tag {
            cursor: pointer;
            font-size: 14px;
        }
    }
    .article-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 16px;
    }
    .article-card {
        cursor: pointer;
        transition: transform 0.2s;
        &:hover { transform: translateY(-2px); }
        .cover-img {
            width: 100%;
            height: 160px;
            object-fit: cover;
            border-radius: 4px;
        }
        .card-content {
            padding: 8px 0;
            .article-title {
                font-size: 16px;
                margin: 0 0 8px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .article-summary {
                color: #666;
                font-size: 13px;
                line-height: 1.5;
                display: -webkit-box;
                -webkit-line-clamp: 3;
                -webkit-box-orient: vertical;
                overflow: hidden;
                margin-bottom: 12px;
            }
            .article-meta {
                display: flex;
                justify-content: space-between;
                align-items: center;
                .author-info {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    .author-name { font-size: 12px; color: #666; }
                }
                .stats {
                    display: flex;
                    gap: 8px;
                    font-size: 12px;
                    color: #999;
                    .time { color: #bbb; }
                }
            }
        }
    }
}
</style>
