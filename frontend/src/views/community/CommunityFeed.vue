<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { searchNotesService, searchNotesByTagService } from '@/api/search.js'
import { communityHotService, communityArticlesService } from '@/api/community.js'
import { articleCategoryListService, articleLikeService, articleUnlikeService } from '@/api/article.js'
import { useRouter } from 'vue-router'
import useUserInfoStore from '@/stores/userInfo.js'

const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()

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

//卡片点赞：与详情页同一套接口；乐观更新失败回滚，请求锁防连点
const likePendingIds = new Set()
const toggleCardLike = async (article) => {
    if (!userInfoStore.info?.id) {
        ElMessage.warning('请先登录后点赞')
        router.push('/login')
        return
    }
    if (likePendingIds.has(article.noteId)) return
    likePendingIds.add(article.noteId)
    const prevLiked = !!article.liked
    const prevCount = Number(article.likeCount || 0)
    article.liked = !prevLiked
    article.likeCount = prevCount + (prevLiked ? -1 : 1)
    try {
        const result = prevLiked
            ? await articleUnlikeService(article.noteId)
            : await articleLikeService(article.noteId)
        if (result.data != null) article.likeCount = Number(result.data)
    } catch (e) {
        article.liked = prevLiked
        article.likeCount = prevCount
    } finally {
        likePendingIds.delete(article.noteId)
    }
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
                <div v-if="article.coverImage" class="cover-wrapper">
                    <img :src="article.coverImage" class="cover-img" />
                </div>
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
                        <div class="author-info" @click.stop="router.push(`/user/homepage/${article.authorId}`)">
                            <el-avatar :size="24" :src="article.authorAvatar">
                                {{ article.authorName?.charAt(0) }}
                            </el-avatar>
                            <span class="author-name">{{ article.authorName }}</span>
                        </div>
                        <div class="stats">
                            <span>👁 {{ article.viewCount || 0 }}</span>
                            <span :class="['like-chip', { liked: article.liked }]"
                                @click.stop="toggleCardLike(article)"
                                :title="article.liked ? '取消点赞' : '点赞'">
                                {{ article.liked ? '❤️' : '🤍' }} {{ article.likeCount || 0 }}
                            </span>
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
@use '../../assets/styles/tokens.scss' as *;

.community-feed {
    min-height: 100%;
    .feed-header {
        display: flex;
        align-items: baseline;
        gap: 12px;
        .title { font-size: 22px; font-weight: 800; color: $text-primary; letter-spacing: -0.5px; }
        .subtitle { color: $text-secondary; font-size: 13.5px; }
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
            transition: $transition-base;

            &:hover { color: $color-primary; border-color: rgba(51, 112, 255, 0.5); background: $color-primary-light; }
        }
    }
    .product-chip {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 10px;
        margin-bottom: 12px;
        background: $color-primary-light;
        border: 1px solid #d6e2ff;
        border-radius: $radius-base;
        cursor: pointer;
        transition: box-shadow 0.2s, transform 0.2s;
        &:hover {
            box-shadow: $shadow-card;
            transform: translateY(-1px);
        }
        .chip-icon { font-size: 16px; }
        .chip-name {
            flex: 1;
            font-size: 13px;
            color: $text-regular;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .chip-price { color: $color-danger; font-weight: bold; font-size: 13px; }
        .chip-go { color: $color-primary; font-size: 12px; white-space: nowrap; }
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
        gap: 18px;
    }
    .article-card {
        cursor: pointer;
        transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.3s ease-out;

        &:hover {
            transform: translateY(-4px);
            box-shadow: $shadow-card-hover;
        }

        :deep(.el-card__body) { padding: 0; }

        .cover-wrapper {
            height: 172px;
            overflow: hidden;
            background: #f5f6f7;

            .cover-img {
                width: 100%;
                height: 100%;
                object-fit: cover;
                display: block;
                transition: transform 0.7s cubic-bezier(0.25, 0.8, 0.25, 1);
            }
        }

        &:hover .cover-wrapper .cover-img { transform: scale(1.05); }

        .card-content {
            padding: 14px 16px 16px;

            .article-title {
                font-size: 15.5px;
                font-weight: 700;
                color: $text-primary;
                margin: 0 0 8px;
                letter-spacing: -0.2px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .article-summary {
                color: $text-regular;
                font-size: 13px;
                line-height: 1.6;
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
                    min-width: 0;
                    .author-name {
                        font-size: 12px;
                        color: $text-regular;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                    }
                }
                .stats {
                    display: flex;
                    gap: 8px;
                    font-size: 12px;
                    color: $text-secondary;
                    flex-shrink: 0;
                    .time { color: $text-placeholder; }

                    // 卡片点赞入口：与详情页同一套接口
                    .like-chip {
                        cursor: pointer;
                        user-select: none;
                        transition: $transition-fast;

                        &:hover { color: #f56c6c; transform: scale(1.08); }
                        &.liked { color: #f56c6c; }
                    }
                }
            }
        }
    }
}
</style>
