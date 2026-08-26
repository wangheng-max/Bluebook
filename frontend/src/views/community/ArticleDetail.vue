<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { communityArticleDetailService } from '@/api/community.js'
import {
    articleInteractStatusService,
    articleLikeService,
    articleUnlikeService,
    articleFavoriteService,
    articleUnfavoriteService,
    articleForwardService
} from '@/api/article.js'
import { friendListService } from '@/api/friend.js'

const route = useRoute()
const router = useRouter()
const noteId = route.params.noteId

const article = ref({})
const liked = ref(false)
const favorited = ref(false)
const likeCount = ref(0)
const favoriteCount = ref(0)

// 转发弹窗
const forwardVisible = ref(false)
const friends = ref([])
const selectedFriendId = ref('')
const forwarding = ref(false)

const loadDetail = async () => {
    let result = await communityArticleDetailService(noteId)
    article.value = result.data
}

const loadStatus = async () => {
    let result = await articleInteractStatusService(noteId)
    liked.value = result.data.liked
    favorited.value = result.data.favorited
    likeCount.value = result.data.likeCount
    favoriteCount.value = result.data.favoriteCount
}

const toggleLike = async () => {
    if (liked.value) {
        likeCount.value = (await articleUnlikeService(noteId)).data
        liked.value = false
    } else {
        likeCount.value = (await articleLikeService(noteId)).data
        liked.value = true
    }
}

const toggleFavorite = async () => {
    if (favorited.value) {
        favoriteCount.value = (await articleUnfavoriteService(noteId)).data
        favorited.value = false
    } else {
        favoriteCount.value = (await articleFavoriteService(noteId)).data
        favorited.value = true
    }
}

const openForward = async () => {
    let result = await friendListService({ page: 1, size: 100 })
    friends.value = result.data.items || []
    if (friends.value.length === 0) {
        ElMessage.warning('还没有好友，先去添加好友吧')
        return
    }
    forwardVisible.value = true
}

const doForward = async () => {
    if (!selectedFriendId.value) {
        ElMessage.warning('请选择要转发的好友')
        return
    }
    forwarding.value = true
    try {
        await articleForwardService(noteId, { toUserId: selectedFriendId.value })
        ElMessage.success('转发成功')
        forwardVisible.value = false
        selectedFriendId.value = ''
    } finally {
        forwarding.value = false
    }
}

onMounted(() => {
    loadDetail()
    loadStatus()
})
</script>

<template>
    <div class="article-detail">
        <el-card>
            <template #header>
                <el-button text @click="router.back()">← 返回</el-button>
            </template>

            <template v-if="article.noteId">
                <!-- 标题与作者 -->
                <h1 class="detail-title">{{ article.title }}</h1>
                <div class="detail-meta">
                    <el-avatar :size="32" :src="article.authorAvatar">
                        {{ article.authorName?.charAt(0) }}
                    </el-avatar>
                    <span class="author">{{ article.authorName }}</span>
                    <span class="time">{{ article.createTime }}</span>
                    <span class="view">👁 {{ article.viewCount || 0 }} 次浏览</span>
                </div>

                <!-- 封面 -->
                <img v-if="article.coverImage" :src="article.coverImage" class="detail-cover" />

                <!-- 正文（富文本内容） -->
                <div class="detail-content" v-html="article.content"></div>

                <!-- 互动栏 -->
                <div class="action-bar">
                    <el-button
                        :type="liked ? 'danger' : 'default'"
                        round
                        @click="toggleLike"
                    >
                        {{ liked ? '❤️ 已点赞' : '🤍 点赞' }} {{ likeCount || 0 }}
                    </el-button>
                    <el-button
                        :type="favorited ? 'warning' : 'default'"
                        round
                        @click="toggleFavorite"
                    >
                        {{ favorited ? '⭐ 已收藏' : '☆ 收藏' }} {{ favoriteCount || 0 }}
                    </el-button>
                    <el-button type="primary" round @click="openForward">
                        ↗ 转发给好友
                    </el-button>
                </div>
            </template>

            <el-empty v-else description="加载中..." />
        </el-card>

        <!-- 转发好友选择弹窗 -->
        <el-dialog v-model="forwardVisible" title="转发给好友" width="400px">
            <el-select
                v-model="selectedFriendId"
                placeholder="选择好友"
                style="width: 100%"
                size="large"
            >
                <el-option
                    v-for="f in friends"
                    :key="f.friendId"
                    :label="f.nickname || f.username"
                    :value="f.friendId"
                >
                    <div class="friend-option">
                        <el-avatar :size="24" :src="f.avatar">{{ (f.nickname || f.username)?.charAt(0) }}</el-avatar>
                        <span>{{ f.nickname || f.username }}</span>
                    </div>
                </el-option>
            </el-select>
            <template #footer>
                <el-button @click="forwardVisible = false">取消</el-button>
                <el-button type="primary" :loading="forwarding" @click="doForward">确认转发</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.article-detail {
    max-width: 860px;
    margin: 0 auto;
    .detail-title {
        font-size: 24px;
        margin: 0 0 12px;
    }
    .detail-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 20px;
        color: #666;
        .author { font-weight: 600; }
        .time, .view { font-size: 13px; color: #999; }
    }
    .detail-cover {
        width: 100%;
        max-height: 400px;
        object-fit: cover;
        border-radius: 8px;
        margin-bottom: 20px;
    }
    .detail-content {
        line-height: 1.8;
        font-size: 15px;
        color: #333;
    }
    .action-bar {
        display: flex;
        gap: 12px;
        margin-top: 24px;
        padding-top: 20px;
        border-top: 1px solid #eee;
    }
    .friend-option {
        display: flex;
        align-items: center;
        gap: 8px;
    }
}
</style>
