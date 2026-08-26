<script setup>
import { ref, onMounted } from 'vue'
import { friendListService, friendDeleteService } from '@/api/friend.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import ChatWindow from '@/components/ChatWindow.vue'

const friends = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

// 聊天抽屉
const chatVisible = ref(false)
const chatFriend = ref({})

const openChat = (friend) => {
    chatFriend.value = friend
    chatVisible.value = true
}

const fetchFriends = async () => {
    let params = {
        page: pageNum.value,
        size: pageSize.value,
        keyword: keyword.value || null
    }
    let result = await friendListService(params)
    friends.value = result.data.items
    total.value = result.data.total
}

const handleDelete = async (friendId, nickname) => {
    await ElMessageBox.confirm(`确定要删除好友「${nickname}」吗？`, '删除好友', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    })
    await friendDeleteService(friendId)
    ElMessage.success('好友已删除')
    fetchFriends()
}

const onSearch = () => {
    pageNum.value = 1
    fetchFriends()
}

onMounted(fetchFriends)
</script>

<template>
    <el-card>
        <template #header>
            <div class="friend-header">
                <span>👥 我的好友</span>
                <el-input
                    v-model="keyword"
                    placeholder="搜索好友..."
                    size="small"
                    clearable
                    style="width: 200px"
                    @keyup.enter="onSearch"
                >
                    <template #append>
                        <el-button @click="onSearch">搜索</el-button>
                    </template>
                </el-input>
            </div>
        </template>

        <div class="friend-grid" v-if="friends.length > 0">
            <div v-for="f in friends" :key="f.friendId" class="friend-card">
                <el-avatar :size="60" :src="f.avatar">{{ f.nickname?.charAt(0) }}</el-avatar>
                <div class="friend-info">
                    <div class="friend-name">{{ f.nickname }}</div>
                    <div class="friend-username">@{{ f.username }}</div>
                    <div class="friend-notes">📒 {{ f.noteCount || 0 }} 篇笔记</div>
                </div>
                <el-button type="primary" size="small" plain @click="openChat(f)">
                    <el-icon><ChatDotRound /></el-icon> 聊天
                </el-button>
                <el-button type="danger" size="small" plain @click="handleDelete(f.friendId, f.nickname)">删除</el-button>
            </div>
        </div>

        <!-- 聊天抽屉（WebSocket 实时聊天） -->
        <el-drawer
            v-model="chatVisible"
            :title="'与 ' + (chatFriend.nickname || chatFriend.username) + ' 聊天'"
            size="420px"
        >
            <ChatWindow v-if="chatVisible" :friend-id="chatFriend.friendId" :friend-name="chatFriend.nickname || chatFriend.username" />
        </el-drawer>

        <el-empty v-if="friends.length === 0" description="还没有好友，去发现页面添加吧~" />

        <el-pagination
            v-if="total > pageSize"
            v-model:current-page="pageNum"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total"
            @current-change="() => fetchFriends()"
            style="margin-top: 20px; justify-content: center"
        />
    </el-card>
</template>

<style lang="scss" scoped>
.friend-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.friend-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;
}
.friend-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    transition: box-shadow 0.2s;
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
    .friend-info {
        flex: 1;
        .friend-name { font-size: 15px; font-weight: 500; }
        .friend-username { font-size: 12px; color: #999; }
        .friend-notes { font-size: 12px; color: #666; margin-top: 4px; }
    }
}
</style>
