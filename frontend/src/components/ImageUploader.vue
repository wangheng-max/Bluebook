<script setup>
/**
 * 通用图片上传组件（商城团购模块复用：商品封面、营业执照、店铺 Logo）。
 * 上传接口沿用项目既有 POST /upload（OSS），上传成功后回写图片地址；
 * 同时支持直接粘贴图片地址，避免 OSS 不可用时阻塞业务流程。
 */
import { ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'

const props = defineProps({
    modelValue: { type: String, default: '' },
    width: { type: Number, default: 120 },
    height: { type: Number, default: 120 },
    tip: { type: String, default: 'jpg/png，不超过 5MB' }
})
const emit = defineEmits(['update:modelValue'])

const tokenStore = useTokenStore()
const showUrlInput = ref(false)

const uploadSuccess = (result) => {
    if (result && result.code === 0) {
        emit('update:modelValue', result.data)
        ElMessage.success('上传成功')
    } else {
        ElMessage.error((result && result.message) || '上传失败')
    }
}

const uploadError = () => {
    ElMessage.error('上传失败，可点击「填写图片地址」直接粘贴链接')
    showUrlInput.value = true
}

const clear = () => emit('update:modelValue', '')
</script>

<template>
    <div class="image-uploader">
        <el-upload class="uploader" :auto-upload="true" :show-file-list="false" action="/api/upload" name="file"
            :headers="{ Authorization: tokenStore.token }" :on-success="uploadSuccess" :on-error="uploadError">
            <img v-if="modelValue" :src="modelValue" class="preview" :style="{ width: width + 'px', height: height + 'px' }" />
            <el-icon v-else class="uploader-icon" :style="{ width: width + 'px', height: height + 'px' }">
                <Plus />
            </el-icon>
        </el-upload>

        <div class="ops">
            <el-button link type="primary" size="small" @click="showUrlInput = !showUrlInput">
                填写图片地址
            </el-button>
            <el-button v-if="modelValue" link type="danger" size="small" @click="clear">清除</el-button>
        </div>
        <div class="tip">{{ tip }}</div>

        <el-input v-if="showUrlInput" :model-value="modelValue" size="small" clearable
            placeholder="粘贴图片 URL 后回车" class="url-input"
            @update:model-value="emit('update:modelValue', $event)" />
    </div>
</template>

<style lang="scss" scoped>
.image-uploader {
    display: inline-block;

    .uploader {
        :deep(.el-upload) {
            border: 1px dashed var(--el-border-color);
            border-radius: 6px;
            cursor: pointer;
            position: relative;
            overflow: hidden;
            transition: var(--el-transition-duration-fast);

            &:hover {
                border-color: var(--el-color-primary);
            }
        }
    }

    .preview {
        display: block;
        object-fit: cover;
    }

    .uploader-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        color: #8c939d;
    }

    .ops {
        margin-top: 4px;
    }

    .tip {
        color: #999;
        font-size: 12px;
        line-height: 1.6;
    }

    .url-input {
        margin-top: 6px;
        width: 220px;
    }
}
</style>
