<script setup>
import { ref } from 'vue'
import { searchNotesService } from '@/api/search.js'
import { useRouter } from 'vue-router'

const router = useRouter()

const keyword = ref('')
const articles = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchNotes = async () => {
    if (!keyword.value.trim()) return
    loading.value = true
    try {
        let params = {
            keyword: keyword.value,
            page: pageNum.value,
            size: pageSize.value
        }
        let result = await searchNotesService(params)
        articles.value = result.data.items
        total.value = result.data.total
    } finally {
        loading.value = false
    }
}

const onPageChange = (num) => {
    pageNum.value = num
    searchNotes()
}

const onSizeChange = (size) => {
    pageSize.value = size
    pageNum.value = 1
    searchNotes()
}

const viewDetail = (noteId) => {
    router.push(`/article/detail/${noteId}`)
}
</script>

<template>
    <el-card class="community-feed">
        <template #header>
            <div class="feed-header">
                <span class="title">🔍 探索社区笔记</span>
                <span class="subtitle">发现大家的精彩内容</span>
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
                            <span>❤️ {{ article.likeCount || 0 }}</span>
                            <span>💬 {{ article.commentCount || 0 }}</span>
                            <span class="time">{{ formatDate(article.createTime) }}</span>
                        </div>
                    </div>
                </div>
            </el-card>
        </div>

        <el-empty v-else-if="keyword" description="未找到相关内容，换个关键词试试" />
        <el-empty v-else description="输入关键词，开始探索社区笔记" />

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
