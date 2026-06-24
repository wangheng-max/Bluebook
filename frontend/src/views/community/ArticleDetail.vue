<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const noteId = route.params.noteId

// Simplified - in a real app this would fetch from an API
const article = ref({})

onMounted(async () => {
    // 这里可以调用后端接口获取文章详情
    // 暂时展示提示
})
</script>

<template>
    <el-card>
        <template #header>
            <el-button type="text" @click="router.back()">← 返回</el-button>
        </template>
        <el-empty description="文章详情页（可扩展：查看完整内容、评论、点赞等）" />
    </el-card>
</template>
