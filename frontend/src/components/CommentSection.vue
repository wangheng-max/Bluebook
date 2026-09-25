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
    commentDeleteService
} from '@/api/comment.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { formatTimeShort } from '@/utils/mall.js'
import avatar from '@/assets/default.png'

//通用评论区组件：文章/商品共用。props.targetType = 'article' | 'product'
const props = defineProps({
    targetType: { type: String, required: true },
    targetId: { type: [Number, String], required: true }
})

const router = useRouter()
const userInfoStore = useUserInfoStore()

const comments = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const sort = ref('time')//time-按时间 / likes-按点赞
const loading = ref(false)

//发布顶级评论
const content = ref('')
const submitting = ref(false)

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
        await commentAddService({
            targetType: props.targetType,
            targetId: props.targetId,
            content: content.value.trim()
        })
        ElMessage.success('评论发布成功')
        content.value = ''
        pageNum.value = 1
        await loadComments()
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

const toggleLike = async (comment) => {
    if (requireLogin()) return
    if (comment.liked) {
        const result = await commentUnlikeService(comment.id)
        comment.liked = false
        comment.likeCount = result.data
    } else {
        const result = await commentLikeService(comment.id)
        comment.liked = true
        comment.likeCount = result.data
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
}, { immediate: true })
</script>

<template>
    <el-card class="comment-section">
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
            <el-input v-model="content" type="textarea" :rows="2" maxlength="1000" show-word-limit
                resize="none" placeholder="友善发言，理性讨论…" />
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
                        <span class="time">{{ formatTimeShort(root.createTime) }}</span>
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

        .publish-actions {
            margin-top: 8px;
            display: flex;
            justify-content: flex-end;
        }
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
