<script setup>
import { ref, watch } from 'vue'
import { searchNotesService } from '@/api/search.js'
import { communityHotService, communityArticlesService } from '@/api/community.js'
import { articleCategoryListService } from '@/api/article.js'
import { useRouter } from 'vue-router'

const router = useRouter()

const keyword = ref('')
const articles = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 分类导航：全部 + 内置分类
const categories = ref([])
const activeCategoryId = ref('')

// 当前内容形态：hot(热门) / category(按分类浏览) / search(搜索结果)
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

loadCategories()
loadList()
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
