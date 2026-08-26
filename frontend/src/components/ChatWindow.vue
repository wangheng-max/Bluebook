<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'
import { messageHistoryService, messageOfflineService } from '@/api/message.js'
import { useRouter } from 'vue-router'

const props = defineProps({
    friendId: { type: Number, required: true },
    friendName: { type: String, default: '' }
})
const emit = defineEmits(['close'])

const router = useRouter()
const tokenStore = useTokenStore()

const messages = ref([])
const inputText = ref('')
const ws = ref(null)
const wsConnected = ref(false)
const messageListRef = ref(null)

// 滚动到底部
const scrollToBottom = async () => {
    await nextTick()
    const el = messageListRef.value
    if (el) el.scrollTop = el.scrollHeight
}

const loadHistory = async () => {
    let result = await messageHistoryService({ friendId: props.friendId, pageNum: 1, pageSize: 50 })
    messages.value = (result.data.items || []).reverse()
    scrollToBottom()
}

// 拉取离线消息，把该好友发来的合并显示
const loadOffline = async () => {
    try {
        let result = await messageOfflineService()
        let offline = (result.data || []).filter(m => m.fromUserId == props.friendId)
        if (offline.length > 0) {
            messages.value = [...messages.value, ...offline.reverse()]
            scrollToBottom()
        }
    } catch (e) {
        // 离线拉取失败不阻塞聊天
    }
}

const connectWs = () => {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    ws.value = new WebSocket(`${protocol}://${location.host}/ws/chat?token=${tokenStore.token}`)
    ws.value.onopen = () => {
        wsConnected.value = true
    }
    ws.value.onmessage = (event) => {
        try {
            const msg = JSON.parse(event.data)
            // 只显示当前会话好友发来的消息
            if (msg.fromUserId == props.friendId) {
                messages.value.push(msg)
                scrollToBottom()
            }
        } catch (e) {
            // 忽略无法解析的消息
        }
    }
    ws.value.onclose = () => {
        wsConnected.value = false
    }
    ws.value.onerror = () => {
        wsConnected.value = false
        ElMessage.warning('连接聊天服务失败')
    }
}

const sendText = () => {
    const content = inputText.value.trim()
    if (!content) return
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
        ElMessage.warning('聊天连接已断开，请重试')
        return
    }
    const payload = { toUserId: props.friendId, msgType: 0, content }
    ws.value.send(JSON.stringify(payload))
    // 本地先追加（消息已落库，服务端不回显给发送者）
    messages.value.push({
        fromUserId: tokenStore.userId || undefined,
        toUserId: props.friendId,
        msgType: 0,
        content,
        createTime: new Date().toLocaleString('zh-CN')
    })
    inputText.value = ''
    scrollToBottom()
}

const viewForwardedArticle = (articleId) => {
    if (articleId) {
        router.push(`/article/detail/${articleId}`)
    }
}

onMounted(() => {
    loadHistory()
    loadOffline()
    connectWs()
})

onUnmounted(() => {
    if (ws.value) {
        ws.value.close()
    }
})
</script>

<template>
    <div class="chat-window">
        <!-- 消息列表 -->
        <div class="chat-list" ref="messageListRef">
            <div v-for="(m, i) in messages" :key="i" class="chat-item" :class="m.fromUserId == friendId ? 'in' : 'out'">
                <div class="bubble">
                    <!-- 文本消息 -->
                    <span v-if="m.msgType == 0">{{ m.content }}</span>
                    <!-- 文章转发卡片 -->
                    <div v-else-if="m.msgType == 1" class="forward-card" @click="viewForwardedArticle(m.articleId)">
                        <div class="fc-title">📄 {{ m.articleTitle || m.content }}</div>
                        <img v-if="m.articleCover" :src="m.articleCover" class="fc-cover" />
                        <div class="fc-hint">点击查看文章</div>
                    </div>
                </div>
                <span class="chat-time">{{ m.createTime }}</span>
            </div>
        </div>

        <!-- 输入区 -->
        <div class="chat-input">
            <el-input
                v-model="inputText"
                placeholder="输入消息，回车发送..."
                @keyup.enter="sendText"
            >
                <template #append>
                    <el-button type="primary" @click="sendText">发送</el-button>
                </template>
            </el-input>
        </div>
    </div>
</template>

<style lang="scss" scoped>
.chat-window {
    display: flex;
    flex-direction: column;
    height: 460px;
    .chat-list {
        flex: 1;
        overflow-y: auto;
        padding: 12px;
        background: #f7f8fa;
        border-radius: 6px;
        .chat-item {
            display: flex;
            flex-direction: column;
            margin-bottom: 12px;
            &.out {
                align-items: flex-end;
                .bubble { background: #409eff; color: #fff; }
            }
            &.in {
                align-items: flex-start;
                .bubble { background: #fff; color: #333; border: 1px solid #ebeef5; }
            }
            .bubble {
                max-width: 70%;
                padding: 8px 12px;
                border-radius: 8px;
                word-break: break-word;
                .forward-card {
                    cursor: pointer;
                    .fc-title { font-weight: 600; margin-bottom: 6px; }
                    .fc-cover {
                        width: 160px;
                        height: 90px;
                        object-fit: cover;
                        border-radius: 4px;
                        display: block;
                    }
                    .fc-hint { font-size: 12px; opacity: 0.7; margin-top: 6px; }
                }
            }
            .chat-time {
                font-size: 11px;
                color: #bbb;
                margin-top: 4px;
            }
        }
    }
    .chat-input {
        margin-top: 12px;
    }
}
</style>
