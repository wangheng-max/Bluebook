<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
    merchantGroupBuyListService,
    groupBuyAddService,
    groupBuyUpdateService,
    groupBuyCloseService
} from '@/api/groupBuy.js'
import { merchantProductListService } from '@/api/product.js'
import { money, formatTime, GROUP_BUY_STATUS_MAP, GROUP_BUY_STATUS_TAG, textOf } from '@/utils/mall.js'

const activities = ref([])
const products = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('创建团购活动')
const formRef = ref()

const form = reactive({
    id: null,
    productId: null,
    title: '',
    description: '',
    groupPrice: 0.01,
    groupSize: 3,
    maxGroupSize: null,
    timeRange: []
})

const rules = {
    productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
    title: [{ required: true, message: '请输入团购标题', trigger: 'blur' }],
    groupPrice: [{ required: true, message: '请输入团购价', trigger: 'blur' }],
    groupSize: [{ required: true, message: '请输入成团人数', trigger: 'blur' }],
    timeRange: [{ required: true, message: '请选择活动时间', trigger: 'change' }]
}

const productName = computed(() => {
    const map = {}
    products.value.forEach(p => { map[p.id] = p.name })
    return (id) => map[id] || `商品#${id}`
})

const loadList = async () => {
    loading.value = true
    try {
        const result = await merchantGroupBuyListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value === '' ? null : status.value
        })
        activities.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        activities.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const loadProducts = async () => {
    try {
        const result = await merchantProductListService({ pageNum: 1, pageSize: 200 })
        products.value = result.data.items || []
    } catch (e) {
        products.value = []
    }
}

const changeStatus = (val) => {
    status.value = val
    pageNum.value = 1
    loadList()
}

const onPageChange = (num) => {
    pageNum.value = num
    loadList()
}

const resetForm = () => {
    Object.assign(form, {
        id: null,
        productId: null,
        title: '',
        description: '',
        groupPrice: 0.01,
        groupSize: 3,
        maxGroupSize: null,
        timeRange: []
    })
}

const openAdd = () => {
    resetForm()
    dialogTitle.value = '创建团购活动'
    dialogVisible.value = true
}

const openEdit = (row) => {
    Object.assign(form, {
        id: row.id,
        productId: row.productId,
        title: row.title,
        description: row.description || '',
        groupPrice: row.groupPrice,
        groupSize: row.groupSize,
        maxGroupSize: row.maxGroupSize,
        timeRange: [row.startTime, row.endTime]
    })
    dialogTitle.value = '编辑团购活动'
    dialogVisible.value = true
}

const submit = async () => {
    await formRef.value.validate()
    if (!form.timeRange || form.timeRange.length !== 2) {
        ElMessage.warning('请选择活动开始与结束时间')
        return
    }

    const payload = {
        id: form.id || undefined,
        productId: form.productId,
        title: form.title,
        description: form.description,
        groupPrice: form.groupPrice,
        groupSize: form.groupSize,
        maxGroupSize: form.maxGroupSize,
        startTime: form.timeRange[0],
        endTime: form.timeRange[1]
    }

    saving.value = true
    try {
        if (form.id) {
            await groupBuyUpdateService(payload)
            ElMessage.success('团购活动已更新')
        } else {
            await groupBuyAddService(payload)
            ElMessage.success('团购活动已创建')
        }
        dialogVisible.value = false
        loadList()
    } finally {
        saving.value = false
    }
}

const close = (row) => {
    ElMessageBox.confirm(
        `确认关闭团购「${row.title}」吗？关闭后未成团的参团订单将按规则退款。`,
        '关闭团购',
        { confirmButtonText: '确认关闭', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
        await groupBuyCloseService(row.id)
        ElMessage.success('团购活动已关闭')
        loadList()
    }).catch(() => { })
}

loadProducts()
loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>👥 团购管理</span>
                <el-button type="primary" :icon="Plus" @click="openAdd">创建团购</el-button>
            </div>
        </template>

        <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button :label="0">未开始</el-radio-button>
            <el-radio-button :label="1">进行中</el-radio-button>
            <el-radio-button :label="2">已成团</el-radio-button>
            <el-radio-button :label="3">已关闭</el-radio-button>
        </el-radio-group>

        <el-table :data="activities" v-loading="loading" style="width: 100%">
            <el-table-column label="团购标题" prop="title" min-width="180" show-overflow-tooltip></el-table-column>
            <el-table-column label="商品" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">{{ productName(row.productId) }}</template>
            </el-table-column>
            <el-table-column label="团购价" width="110">
                <template #default="{ row }">
                    <span class="price">¥{{ money(row.groupPrice) }}</span>
                </template>
            </el-table-column>
            <el-table-column label="成团/上限" width="120">
                <template #default="{ row }">
                    {{ row.groupSize }} 人{{ row.maxGroupSize ? ` / 上限 ${row.maxGroupSize}` : '' }}
                </template>
            </el-table-column>
            <el-table-column label="已参团" width="100">
                <template #default="{ row }">
                    <span :class="{ done: row.progressCount >= row.groupSize }">{{ row.progressCount || 0 }} 人</span>
                </template>
            </el-table-column>
            <el-table-column label="活动时间" min-width="320">
                <template #default="{ row }">
                    {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
                </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="textOf(GROUP_BUY_STATUS_TAG, row.status, 'info')" size="small">
                        {{ textOf(GROUP_BUY_STATUS_MAP, row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                    <el-button v-if="row.status === 0 || row.status === 1" type="primary" link size="small"
                        @click="openEdit(row)">编辑</el-button>
                    <el-button v-if="row.status === 0 || row.status === 1" type="danger" link size="small"
                        @click="close(row)">关闭</el-button>
                    <span v-else class="no-op">—</span>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="还没有团购活动，点击右上角创建一个吧" />
            </template>
        </el-table>

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center" />

        <!-- 创建/编辑团购 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
                <el-form-item label="团购商品" prop="productId">
                    <el-select v-model="form.productId" placeholder="请选择本店商品" filterable style="width: 100%">
                        <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id">
                            <span>{{ p.name }}</span>
                            <span class="option-price">¥{{ money(p.price) }}</span>
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="团购标题" prop="title">
                    <el-input v-model="form.title" maxlength="60" placeholder="如：蓝牙耳机 3 人团" />
                </el-form-item>
                <el-form-item label="团购价" prop="groupPrice">
                    <el-input-number v-model="form.groupPrice" :min="0.01" :precision="2" :step="1" />
                    <span class="tip">建议低于商品原价</span>
                </el-form-item>
                <el-form-item label="成团人数" prop="groupSize">
                    <el-input-number v-model="form.groupSize" :min="1" :step="1" />
                    <span class="tip">参团人数达到该值即成团</span>
                </el-form-item>
                <el-form-item label="活动人数上限">
                    <el-input-number v-model="form.maxGroupSize" :min="1" :step="1" />
                    <span class="tip">留空表示不限制</span>
                </el-form-item>
                <el-form-item label="活动时间" prop="timeRange">
                    <el-date-picker v-model="form.timeRange" type="datetimerange" range-separator="至"
                        start-placeholder="开始时间" end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
                </el-form-item>
                <el-form-item label="活动说明">
                    <el-input v-model="form.description" type="textarea" :rows="3" maxlength="300" show-word-limit
                        placeholder="团购规则说明" />
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

    .status-filter {
        margin-bottom: 20px;
    }

    .price {
        color: #f56c6c;
        font-weight: bold;
    }

    .done {
        color: #67c23a;
        font-weight: bold;
    }

    .no-op {
        color: #ccc;
    }

    .tip {
        margin-left: 10px;
        color: #999;
        font-size: 12px;
    }

    .option-price {
        float: right;
        color: #f56c6c;
    }
}
</style>
