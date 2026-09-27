<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    commentListService,
    commentAddService,
    commentRepliesService,
    commentLikeService,
    commentUnlikeService,
    commentDeleteService,
    commentStatsService
} from '@/api/comment.js'
import { myPurchasedService } from '@/api/order.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { useTokenStore } from '@/stores/token.js'
import { formatTimeShort } from '@/utils/mall.js'
import { Plus, CircleCloseFilled } from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'

//通用评论区组件：文章/商品/商家评价共用。props.targetType = 'article' | 'product' | 'rating'
const props = defineProps({
    targetType: { type: String, required: true },
    targetId: { type: [Number, String], required: true },
    //紧凑模式：嵌入店铺评价等场景下去掉卡片外框与留白
    compact: { type: Boolean, default: false }
})

const emit = defineEmits(['count-change'])

const router = useRouter()
const userInfoStore = useUserInfoStore()
const tokenStore = useTokenStore()

const comments = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const sort = ref('time')//time-按时间 / likes-按点赞
const loading = ref(false)

//商品评分统计（仅商品模式请求）
const stats = ref(null)

//发布顶级评论
const content = ref('')
const submitting = ref(false)

//商品评论扩展：评分 / 晒图 / 已购订单快照
const isProduct = () => props.targetType === 'product'
const score = ref(5)
const commentImages = ref([])//图片 URL 列表，最多 4 张
const purchasedOrders = ref([])//我买过该商品的订单明细
const selectedOrderId = ref(null)//选中的晒单订单

//上传评论图片（复用 /api/upload）
const onImageUploadSuccess = (result) => {
    if (result && result.code === 0) {
        if (commentImages.value.length >= 4) {
            ElMessage.warning('评论图片最多 4 张')
            return
        }
        commentImages.value.push(result.data)
    } else {
        ElMessage.error((result && result.message) || '图片上传失败')
    }
}
const removeCommentImage = (index) => commentImages.value.splice(index, 1)

//加载我买过该商品的订单（晒单"自动带上买的东西"）
const loadPurchased = async () => {
    purchasedOrders.value = []
    selectedOrderId.value = null
    if (!isProduct() || !isLogin()) return
    try {
        const result = await myPurchasedService(props.targetId)
        purchasedOrders.value = result.data || []
        //默认自动带上最近一单
        if (purchasedOrders.value.length > 0) {
            selectedOrderId.value = purchasedOrders.value[0].orderId
        }
    } catch (e) {
        purchasedOrders.value = []
    }
}

const loadStats = async () => {
    stats.value = null
    if (!isProduct()) return
    try {
        const result = await commentStatsService({ targetType: props.targetType, targetId: props.targetId })
        stats.value = result.data || null
    } catch (e) {
        stats.value = null
    }
}

//回复状态：记录当前正在回复的评论ID与内容（根评论和回复共用一套）
const replyActiveId = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)

//楼中楼展开状态：rootId -> { page, hasMore, loading }
const repliesState = reactive({})

const isLogin = () => !!userInfoStore.info?.id
const isOwn = (c) => isLogin() && c.userId === userInfoStore.info.id
const isExpanded = (root) => !!repliesState[root.id]

const requireLogin = () => {
    if (!isLogin()) {
        ElMessage.warning('请先登录后参与互动')
        router.push('/login')
        return true
    }
    return false
}

const loadComments = async () => {
    loading.value = true
    try {
        const result = await commentListService({
            targetType: props.targetType,
            targetId: props.targetId,
            sort: sort.value,
            pageNum: pageNum.value,
            pageSize
        })
        comments.value = result.data?.items || []
        total.value = Number(result.data?.total || 0)
        Object.keys(repliesState).forEach(k => delete repliesState[k])
        emit('count-change', total.value)
    } finally {
        loading.value = false
    }
}

const handleSortChange = () => {
    pageNum.value = 1
    loadComments()
}

const submitComment = async () => {
    if (requireLogin()) return
    if (!content.value.trim()) {
        ElMessage.warning('评论内容不能为空')
        return
    }
    submitting.value = true
    try {
        const payload = {
            targetType: props.targetType,
            targetId: props.targetId,
            content: content.value.trim()
        }
        if (isProduct()) {
            payload.score = score.value
            if (commentImages.value.length) {
                payload.images = commentImages.value
            }
            if (selectedOrderId.value) {
                payload.orderId = selectedOrderId.value
            }
        }
        await commentAddService(payload)
        ElMessage.success('评论发布成功')
        content.value = ''
        commentImages.value = []
        selectedOrderId.value = purchasedOrders.value[0]?.orderId || null
        pageNum.value = 1
        await Promise.all([loadComments(), loadStats()])
    } finally {
        submitting.value = false
    }
}

const startReply = (comment) => {
    if (requireLogin()) return
    replyActiveId.value = comment.id
    replyContent.value = ''
}

const cancelReply = () => {
    replyActiveId.value = null
    replyContent.value = ''
}

//找到回复所属的根评论（楼中楼在根评论下展开）
const rootOf = (comment) => comment.parentId === 0
    ? comment
    : comments.value.find(r => r.id === comment.rootId)

const submitReply = async (comment) => {
    if (!replyContent.value.trim()) {
        ElMessage.warning('回复内容不能为空')
        return
    }
    replySubmitting.value = true
    try {
        await commentAddService({
            targetType: props.targetType,
            targetId: props.targetId,
            content: replyContent.value.trim(),
            parentId: comment.id
        })
        ElMessage.success('回复成功')
        cancelReply()
        const root = rootOf(comment)
        //回复数超过预览条数时直接展开该楼层，保证新回复可见
        if (root && root.replyCount + 1 > (root.replies?.length || 0)) {
            await expandRoot(root)
        } else {
            await loadComments()
        }
    } finally {
        replySubmitting.value = false
    }
}

//点赞：乐观更新 + 失败回滚，请求中的评论加锁防连点
const likePendingIds = new Set()
const toggleLike = async (comment) => {
    if (requireLogin()) return
    if (likePendingIds.has(comment.id)) return
    likePendingIds.add(comment.id)
    const prevLiked = !!comment.liked
    const prevCount = Number(comment.likeCount || 0)
    comment.liked = !prevLiked
    comment.likeCount = prevCount + (prevLiked ? -1 : 1)
    try {
        const result = prevLiked
            ? await commentUnlikeService(comment.id)
            : await commentLikeService(comment.id)
        if (result.data != null) comment.likeCount = Number(result.data)
    } catch (e) {
        comment.liked = prevLiked
        comment.likeCount = prevCount
    } finally {
        likePendingIds.delete(comment.id)
    }
}

const removeComment = async (comment) => {
    ElMessageBox.confirm(comment.parentId === 0 ? '删除该评论将同时删除其下所有回复，确认删除？' : '确认删除这条回复？', '温馨提示', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await commentDeleteService(comment.id)
        ElMessage.success('删除成功')
        await loadComments()
    }).catch(() => { })
}

const expandRoot = async (root) => {
    await fetchReplies(root, 1, true)
}

const loadMoreReplies = async (root) => {
    await fetchReplies(root, (repliesState[root.id]?.page || 1) + 1, false)
}

const collapseRoot = (root) => {
    delete repliesState[root.id]
    //恢复为列表接口返回的预览条数
    if ((root.replies?.length || 0) > 2) {
        root.replies = root.replies.slice(0, 2)
    }
}

const fetchReplies = async (root, page, replace) => {
    const state = repliesState[root.id] = repliesState[root.id] || { page: 0, hasMore: false, loading: false }
    state.loading = true
    try {
        const result = await commentRepliesService(root.id, { pageNum: page, pageSize: 20 })
        const items = result.data?.items || []
        const cnt = Number(result.data?.total || 0)
        root.replies = replace ? items : [...(root.replies || []), ...items]
        root.replyCount = cnt
        state.page = page
        state.hasMore = root.replies.length < cnt
    } finally {
        state.loading = false
    }
}

watch(() => [props.targetType, props.targetId], () => {
    pageNum.value = 1
    loadComments()
    loadStats()
    loadPurchased()
}, { immediate: true })
</script>

<template>
    <el-card class="comment-section" :class="{ 'is-compact': compact }" :shadow="compact ? 'never' : 'always'">
        <template #header>
            <div class="section-header">
                <span class="section-title">全部评论（{{ total }}）</span>
                <el-radio-group v-model="sort" size="small" @change="handleSortChange">
                    <el-radio-button label="time">按时间</el-radio-button>
                    <el-radio-button label="likes">按点赞</el-radio-button>
                </el-radio-group>
            </div>
        </template>

        <!-- 发布评论 -->
        <div class="publish-box" v-if="isLogin()">
            <div class="product-score" v-if="isProduct()">
                <span class="score-label">商品评分</span>
                <el-rate v-model="score" :colors="['#ff9900', '#ff9900', '#ff9900']"
                    show-text :texts="['很差', '较差', '一般', '满意', '非常满意']" />
            </div>
            <el-input v-model="content" type="textarea" :rows="2" maxlength="1000" show-word-limit
                resize="none" :placeholder="isProduct() ? '说说商品质量、使用体验…' : '友善发言，理性讨论…'" />

            <!-- 晒图（商品评论） -->
            <div class="image-upload" v-if="isProduct()">
                <div class="image-list">
                    <div class="image-item" v-for="(img, i) in commentImages" :key="img">
                        <el-image :src="img" fit="cover" class="thumb"
                            :preview-src-list="commentImages" :initial-index="i" />
                        <el-icon class="img-remove" @click="removeCommentImage(i)">
                            <CircleCloseFilled />
                        </el-icon>
                    </div>
                    <el-upload v-if="commentImages.length < 4" :auto-upload="true" :show-file-list="false"
                        action="/api/upload" name="file" accept="image/*"
                        :headers="{ Authorization: tokenStore.token }"
                        :on-success="onImageUploadSuccess">
                        <el-icon class="img-add"><Plus /></el-icon>
                    </el-upload>
                    <span class="upload-tip">晒图最多 4 张</span>
                </div>
            </div>

            <!-- 已购快照（自动带上买的东西） -->
            <div class="purchase-select" v-if="isProduct() && purchasedOrders.length">
                <span class="score-label">购买凭证</span>
                <el-select v-model="selectedOrderId" placeholder="选择购买订单（可清空不展示）" clearable
                    size="small" style="flex:1">
                    <el-option v-for="o in purchasedOrders" :key="o.orderId" :value="o.orderId"
                        :label="`已购「${o.productName}」${o.skuName ? ' ' + o.skuName : ''}`" />
                </el-select>
            </div>

            <div class="publish-actions">
                <el-button type="primary" :loading="submitting" @click="submitComment">发布评论</el-button>
            </div>
        </div>
        <div class="publish-box" v-else>
            <el-button type="primary" plain @click="requireLogin()">登录后参与评论</el-button>
        </div>

        <!-- 评论列表 -->
        <div v-loading="loading" class="comment-list">
            <el-empty v-if="!loading && comments.length === 0" description="还没有评论，快来抢沙发吧" />

            <div v-for="root in comments" :key="root.id" class="comment-item">
                <el-avatar :size="36" :src="root.userPic || avatar">{{ (root.nickname || '匿')[0] }}</el-avatar>
                <div class="comment-body">
                    <div class="comment-head">
                        <span class="nickname">{{ root.nickname || '用户已注销' }}</span>
                        <el-rate v-if="root.score" :model-value="root.score" disabled size="small" class="c-score" />
                        <span class="time">{{ formatTimeShort(root.createTime) }}</span>
                    </div>
                    <el-tag v-if="root.purchasedText" size="small" type="warning" effect="plain" class="purchased-tag">
                        🛍 {{ root.purchasedText }}
                    </el-tag>
                    <div class="comment-images" v-if="root.images?.length">
                        <el-image v-for="(img, i) in root.images" :key="img" :src="img" fit="cover"
                            class="c-thumb" :preview-src-list="root.images" :initial-index="i" />
                    </div>
                    <div class="comment-content">{{ root.content }}</div>
                    <div class="comment-actions">
                        <span :class="['like-btn', { liked: root.liked }]" @click="toggleLike(root)">
                            {{ root.liked ? '❤️' : '🤍' }} {{ root.likeCount || 0 }}
                        </span>
                        <span class="action-link" @click="startReply(root)">回复</span>
                        <span v-if="isOwn(root)" class="action-link danger" @click="removeComment(root)">删除</span>
                    </div>

                    <!-- 回复输入框（挂在根评论下） -->
                    <div v-if="replyActiveId === root.id" class="reply-box">
                        <el-input v-model="replyContent" type="textarea" :rows="2" maxlength="1000"
                            :placeholder="`回复 @${root.nickname || '用户'}：`" />
                        <div class="reply-actions">
                            <el-button size="small" @click="cancelReply">取消</el-button>
                            <el-button size="small" type="primary" :loading="replySubmitting"
                                @click="submitReply(root)">回复</el-button>
                        </div>
                    </div>

                    <!-- 楼中楼回复 -->
                    <div v-if="root.replies?.length" class="replies-box">
                        <div v-for="reply in root.replies" :key="reply.id" class="reply-item">
                            <div class="reply-line">
                                <span class="nickname">{{ reply.nickname || '用户已注销' }}</span>
                                <span v-if="reply.replyNickname" class="reply-to">回复 @{{ reply.replyNickname }}</span>
                                <span class="time">{{ formatTimeShort(reply.createTime) }}</span>
                            </div>
                            <div class="comment-content">{{ reply.content }}</div>
                            <div class="comment-actions">
                                <span :class="['like-btn', { liked: reply.liked }]" @click="toggleLike(reply)">
                                    {{ reply.liked ? '❤️' : '🤍' }} {{ reply.likeCount || 0 }}
                                </span>
                                <span class="action-link" @click="startReply(reply)">回复</span>
                                <span v-if="isOwn(reply)" class="action-link danger"
                                    @click="removeComment(reply)">删除</span>
                            </div>

                            <!-- 回复输入框（挂在被回复的评论下） -->
                            <div v-if="replyActiveId === reply.id" class="reply-box">
                                <el-input v-model="replyContent" type="textarea" :rows="2" maxlength="1000"
                                    :placeholder="`回复 @${reply.nickname || '用户'}：`" />
                                <div class="reply-actions">
                                    <el-button size="small" @click="cancelReply">取消</el-button>
                                    <el-button size="small" type="primary" :loading="replySubmitting"
                                        @click="submitReply(reply)">回复</el-button>
                                </div>
                            </div>
                        </div>

                        <div class="replies-toggle">
                            <el-button v-if="!isExpanded(root) && root.replyCount > root.replies.length"
                                text type="primary" size="small" @click="expandRoot(root)">
                                展开 {{ root.replyCount }} 条回复 ▾
                            </el-button>
                            <el-button v-if="isExpanded(root) && repliesState[root.id]?.hasMore"
                                text type="primary" size="small" :loading="repliesState[root.id]?.loading"
                                @click="loadMoreReplies(root)">
                                加载更多回复
                            </el-button>
                            <el-button v-if="isExpanded(root)" text size="small" @click="collapseRoot(root)">
                                收起 ▴
                            </el-button>
                        </div>
                    </div>
                    <div v-else-if="root.replyCount > 0" class="replies-toggle">
                        <el-button text type="primary" size="small" @click="expandRoot(root)">
                            展开 {{ root.replyCount }} 条回复 ▾
                        </el-button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
            <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="pageSize"
                v-model:current-page="pageNum" @current-change="loadComments" />
        </div>
    </el-card>
</template>

<style lang="scss" scoped>
.comment-section {
    margin-top: 20px;

    // 紧凑模式：嵌入店铺评价等场景，弱化卡片外观
    &.is-compact {
        margin-top: 10px;
        --el-card-border-color: transparent;
        --el-card-border-radius: 0;
        background-color: #f7f8fa;

        :deep(.el-card__header) {
            padding: 8px 0 4px;
        }

        :deep(.el-card__body) {
            padding: 0 0 8px;
        }
    }

    .section-header {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .section-title {
            font-size: 16px;
            font-weight: bold;
        }
    }

    .publish-box {
        margin-bottom: 20px;

        .product-score,
        .purchase-select {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 10px;

            .score-label {
                font-size: 13px;
                color: #666;
                white-space: nowrap;
            }
        }

        .image-upload {
            margin-bottom: 10px;

            .image-list {
                display: flex;
                align-items: center;
                gap: 10px;

                .thumb {
                    width: 72px;
                    height: 72px;
                    border-radius: 6px;
                }

                .image-item {
                    position: relative;

                    .img-remove {
                        position: absolute;
                        top: -6px;
                        right: -6px;
                        color: #909399;
                        font-size: 16px;
                        cursor: pointer;
                        background: #fff;
                        border-radius: 50%;

                        &:hover { color: #f56c6c; }
                    }
                }

                .img-add {
                    width: 72px;
                    height: 72px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    border: 1px dashed var(--el-border-color);
                    border-radius: 6px;
                    color: #8c939d;
                    font-size: 22px;
                    cursor: pointer;

                    &:hover { border-color: var(--el-color-primary); color: var(--el-color-primary); }
                }

                .upload-tip {
                    color: #bbb;
                    font-size: 12px;
                }
            }
        }

        .publish-actions {
            margin-top: 8px;
            display: flex;
            justify-content: flex-end;
        }
    }

    .purchased-tag {
        margin: 4px 0;
    }

    .comment-images {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin: 6px 0;

        .c-thumb {
            width: 80px;
            height: 80px;
            border-radius: 6px;
        }
    }

    .c-score {
        margin-left: 2px;
    }

    .comment-item {
        display: flex;
        gap: 12px;
        padding: 14px 0;
        border-bottom: 1px solid #f0f0f0;

        &:last-of-type {
            border-bottom: none;
        }

        .comment-body {
            flex: 1;
            min-width: 0;

            .comment-head {
                display: flex;
                align-items: baseline;
                gap: 10px;

                .nickname {
                    font-weight: 600;
                    font-size: 14px;
                }

                .time {
                    font-size: 12px;
                    color: #999;
                }
            }

            .comment-content {
                margin: 6px 0;
                font-size: 14px;
                color: #333;
                line-height: 1.6;
                white-space: pre-wrap;
                word-break: break-word;
            }

            .comment-actions {
                display: flex;
                align-items: center;
                gap: 16px;
                font-size: 13px;
                color: #666;

                .like-btn {
                    cursor: pointer;
                    user-select: none;

                    &.liked {
                        color: #f56c6c;
                    }
                }

                .action-link {
                    cursor: pointer;

                    &:hover {
                        color: #409eff;
                    }

                    &.danger:hover {
                        color: #f56c6c;
                    }
                }
            }

            .reply-box {
                margin-top: 10px;

                .reply-actions {
                    margin-top: 6px;
                    display: flex;
                    justify-content: flex-end;
                    gap: 8px;
                }
            }

            .replies-box {
                margin-top: 10px;
                padding: 10px 12px;
                background-color: #f7f8fa;
                border-radius: 6px;

                .reply-item {
                    padding: 8px 0;

                    .reply-line {
                        display: flex;
                        align-items: baseline;
                        gap: 8px;
                        flex-wrap: wrap;

                        .nickname {
                            font-weight: 600;
                            font-size: 13px;
                        }

                        .reply-to {
                            font-size: 12px;
                            color: #409eff;
                        }

                        .time {
                            font-size: 12px;
                            color: #999;
                        }
                    }

                    .comment-content {
                        font-size: 13px;
                    }

                    .comment-actions {
                        font-size: 12px;
                    }
                }

                .replies-toggle {
                    display: flex;
                    gap: 8px;
                }
            }
        }
    }

    .pagination {
        margin-top: 16px;
        display: flex;
        justify-content: center;
    }
}
</style>
