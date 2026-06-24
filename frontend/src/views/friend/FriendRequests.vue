<script setup>
import { ref, onMounted } from 'vue'
import { friendPendingListService, friendConfirmService, friendRejectService } from '@/api/friend.js'
import { ElMessage, ElMessageBox } from 'element-plus'

const requests = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const fetchPending = async () => {
    let result = await friendPendingListService({ page: pageNum.value, size: pageSize.value })
    requests.value = result.data.items
    total.value = result.data.total
}

const handleConfirm = async (relationId, nickname) => {
    await ElMessageBox.confirm(`确认添加「${nickname}」为好友？`, '确认好友', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'info'
    })
    await friendConfirmService(relationId)
    ElMessage.success('已添加为好友')
    fetchPending()
}

const handleReject = async (relationId) => {
    await ElMessageBox.confirm('确定要拒绝该好友请求吗？', '拒绝请求', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    })
    await friendRejectService(relationId)
    ElMessage.success('已拒绝好友请求')
    fetchPending()
}

onMounted(fetchPending)
</script>

<template>
    <el-card>
        <template #header>
            <span>📨 待确认的好友请求</span>
        </template>

        <el-table :data="requests" v-if="requests.length > 0">
            <el-table-column label="头像" width="80">
                <template #default="{ row }">
                    <el-avatar :size="40" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
                </template>
            </el-table-column>
            <el-table-column label="用户名" prop="username" />
            <el-table-column label="昵称" prop="nickname" />
            <el-table-column label="请求时间" prop="createTime" />
            <el-table-column label="操作" width="200">
                <template #default="{ row }">
                    <el-button type="success" size="small" @click="handleConfirm(row.relationId, row.nickname)">同意</el-button>
                    <el-button type="danger" size="small" plain @click="handleReject(row.relationId)">拒绝</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-empty v-else description="暂无待处理的好友请求" />

        <el-pagination
            v-if="total > pageSize"
            v-model:current-page="pageNum"
            :page-size="pageSize"
            layout="total, prev, pager, next"
            :total="total"
            @current-change="() => fetchPending()"
            style="margin-top: 20px; justify-content: center"
        />
    </el-card>
</template>
