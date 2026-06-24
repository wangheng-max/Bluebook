<script setup>
import { ref } from 'vue'
import { searchUsersService } from '@/api/search.js'
import { friendSendRequestService } from '@/api/friend.js'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const searchUsers = async () => {
    if (!keyword.value.trim()) return
    loading.value = true
    try {
        let result = await searchUsersService({ keyword: keyword.value, page: pageNum.value, size: pageSize.value })
        users.value = result.data.items
        total.value = result.data.total
    } finally {
        loading.value = false
    }
}

const handleAddFriend = async (userId) => {
    try {
        await friendSendRequestService(userId)
        ElMessage.success('好友请求已发送')
        // 更新该用户状态
        const user = users.value.find(u => u.userId === userId)
        if (user) user.hasRequested = true
    } catch (err) {
        // 错误由拦截器统一提示
    }
}

const onPageChange = (num) => {
    pageNum.value = num
    searchUsers()
}
</script>

<template>
    <el-card>
        <template #header>
            <div class="discover-header">
                <span>🔍 发现用户</span>
                <span class="subtitle">搜索用户名或昵称，添加好友</span>
            </div>
        </template>

        <div class="search-bar">
            <el-input
                v-model="keyword"
                placeholder="输入用户名或昵称查找..."
                size="large"
                clearable
                @keyup.enter="searchUsers"
            >
                <template #append>
                    <el-button type="primary" @click="searchUsers" :loading="loading">搜索</el-button>
                </template>
            </el-input>
        </div>

        <div class="user-list" v-if="users.length > 0">
            <div v-for="user in users" :key="user.userId" class="user-card">
                <el-avatar :size="48" :src="user.avatar">{{ user.nickname?.charAt(0) }}</el-avatar>
                <div class="user-info">
                    <div class="user-name">{{ user.nickname }} <span class="username">@{{ user.username }}</span></div>
                </div>
                <div class="user-action">
                    <el-tag v-if="user.isFriend" type="success" size="small">已是好友</el-tag>
                    <el-button
                        v-else-if="user.hasRequested"
                        size="small"
                        disabled
                    >已发送请求</el-button>
                    <el-button
                        v-else
                        type="primary"
                        size="small"
                        @click="handleAddFriend(user.userId)"
                    >添加好友</el-button>
                </div>
            </div>
        </div>

        <el-empty v-else-if="keyword" description="未找到用户" />
        <el-empty v-else description="输入关键词搜索用户" />

        <el-pagination
            v-if="total > 0"
            v-model:current-page="pageNum"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total"
            @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center"
        />
    </el-card>
</template>

<style lang="scss" scoped>
.discover-header {
    display: flex;
    align-items: baseline;
    gap: 12px;
    .subtitle { color: #999; font-size: 13px; }
}
.search-bar {
    max-width: 500px;
    margin: 0 auto 20px;
}
.user-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}
.user-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    .user-info {
        flex: 1;
        .user-name { font-size: 15px; font-weight: 500; }
        .username { color: #999; font-size: 12px; }
    }
    .user-action { flex-shrink: 0; }
}
</style>
