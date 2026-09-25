<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
    productCategoryTreeService,
    productCategoryAddService,
    productCategoryUpdateService,
    productCategoryDeleteService
} from '@/api/product.js'

const tree = ref([])
const flatList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const dialogTitle = ref('添加分类')
const formRef = ref()

const form = reactive({
    id: null,
    name: '',
    parentId: 0,
    icon: '',
    sortOrder: 0
})

const rules = {
    name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

// 顶级分类可选（编辑时排除自身，避免父分类指向自己）
const parentOptions = computed(() => {
    return [{ id: 0, name: '顶级分类' }, ...flatList.value.filter(c => c.id !== form.id)]
})

const flatten = (nodes, result = []) => {
    nodes.forEach(n => {
        result.push({ id: n.id, name: n.name })
        if (n.children && n.children.length > 0) {
            flatten(n.children, result)
        }
    })
    return result
}

const loadTree = async () => {
    loading.value = true
    try {
        const result = await productCategoryTreeService()
        tree.value = result.data || []
        flatList.value = flatten(tree.value)
    } catch (e) {
        tree.value = []
        flatList.value = []
    } finally {
        loading.value = false
    }
}

const openAdd = (parentId = 0) => {
    Object.assign(form, { id: null, name: '', parentId: parentId, icon: '', sortOrder: 0 })
    dialogTitle.value = parentId === 0 ? '添加顶级分类' : '添加子分类'
    dialogVisible.value = true
}

const openEdit = (row) => {
    Object.assign(form, {
        id: row.id,
        name: row.name,
        parentId: row.parentId || 0,
        icon: row.icon || '',
        sortOrder: row.sortOrder || 0
    })
    dialogTitle.value = '修改分类'
    dialogVisible.value = true
}

const submit = async () => {
    await formRef.value.validate()
    saving.value = true
    try {
        const payload = {
            id: form.id || undefined,
            name: form.name,
            parentId: form.parentId || 0,
            icon: form.icon,
            sortOrder: form.sortOrder
        }
        if (form.id) {
            await productCategoryUpdateService(payload)
            ElMessage.success('分类已更新')
        } else {
            await productCategoryAddService(payload)
            ElMessage.success('分类已添加')
        }
        dialogVisible.value = false
        loadTree()
    } finally {
        saving.value = false
    }
}

const remove = (row) => {
    ElMessageBox.confirm(
        `确认删除分类「${row.name}」吗？存在子分类或商品时无法删除。`,
        '删除分类',
        { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
        await productCategoryDeleteService(row.id)
        ElMessage.success('分类已删除')
        loadTree()
    }).catch(() => { })
}

loadTree()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>🗂 商品分类管理</span>
                <el-button type="primary" :icon="Plus" @click="openAdd(0)">添加顶级分类</el-button>
            </div>
        </template>

        <el-alert type="info" :closable="false" show-icon
            title="分类由平台统一维护，商家发布商品时只能选用；存在子分类或商品的分类不可删除"
            style="margin-bottom: 16px" />

        <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all
            :tree-props="{ children: 'children' }" style="width: 100%">
            <el-table-column label="分类名称" prop="name" min-width="220"></el-table-column>
            <el-table-column label="分类ID" prop="id" width="90"></el-table-column>
            <el-table-column label="父分类ID" width="110">
                <template #default="{ row }">{{ row.parentId === 0 ? '—' : row.parentId }}</template>
            </el-table-column>
            <el-table-column label="图标" prop="icon" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ row.icon || '—' }}</template>
            </el-table-column>
            <el-table-column label="排序" prop="sortOrder" width="90"></el-table-column>
            <el-table-column label="类型" width="100">
                <template #default="{ row }">
                    <el-tag size="small" :type="row.isSystem === 1 ? 'info' : 'primary'">
                        {{ row.isSystem === 1 ? '系统内置' : '自定义' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                    <el-button type="primary" link size="small" @click="openAdd(row.id)">添加子分类</el-button>
                    <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                    <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="还没有商品分类，先添加一个吧" />
            </template>
        </el-table>

        <!-- 添加/修改分类 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="分类名称" prop="name">
                    <el-input v-model="form.name" maxlength="30" placeholder="如：数码电器" />
                </el-form-item>
                <el-form-item label="父分类">
                    <el-select v-model="form.parentId" style="width: 100%">
                        <el-option v-for="c in parentOptions" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="图标">
                    <el-input v-model="form.icon" maxlength="100" placeholder="选填，图标名称或图片地址" />
                </el-form-item>
                <el-form-item label="排序值">
                    <el-input-number v-model="form.sortOrder" :min="0" :step="1" />
                    <span class="tip">数值越小越靠前</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
            </template>
        </el-dialog>
    </el-card>
</template>

<style lang="scss" scoped>
.page-container {
    min-height: 100%;
    box-sizing: border-box;

    .header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        span {
            font-size: 18px;
            font-weight: bold;
        }
    }

    .tip {
        margin-left: 10px;
        color: #999;
        font-size: 12px;
    }
}
</style>
