<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { regionData, codeToText } from 'element-china-area-data'
import { addressListService, addressAddService, addressUpdateService, addressDeleteService } from '@/api/address.js'
import { formatTime } from '@/utils/mall.js'

const addresses = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
    id: null,
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false
})

// 省市区级联选择（静态数据，支持输入关键字过滤）
const selectedRegion = ref([])

// 选择后把行政码转成中文写入表单（后端按省/市/区三个文本列存储）
const onRegionChange = (codes) => {
    if (codes && codes.length === 3) {
        form.province = codeToText[codes[0]]
        form.city = codeToText[codes[1]]
        form.district = codeToText[codes[2]]
    }
}

// 编辑回显：按中文名逐级反查行政码；老数据手填匹配不上时留空让用户重选
const findRegionCodes = (province, city, district) => {
    const p = regionData.find(x => x.label === province)
    if (!p) return []
    if (!city) return [p.value]
    const c = (p.children || []).find(x => x.label === city)
    if (!c) return [p.value]
    if (!district) return [p.value, c.value]
    const d = (c.children || []).find(x => x.label === district)
    return d ? [p.value, c.value, d.value] : [p.value, c.value]
}

const rules = {
    receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
    receiverPhone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
    ],
    province: [{ required: true, message: '请选择省市区', trigger: 'change' }],
    detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const loadList = async () => {
    loading.value = true
    try {
        const result = await addressListService()
        addresses.value = result.data || []
    } catch (e) {
        addresses.value = []
    } finally {
        loading.value = false
    }
}

const openAdd = () => {
    Object.assign(form, {
        id: null,
        receiverName: '',
        receiverPhone: '',
        province: '',
        city: '',
        district: '',
        detailAddress: '',
        isDefault: addresses.value.length === 0
    })
    selectedRegion.value = []
    dialogVisible.value = true
}

const openEdit = (row) => {
    Object.assign(form, {
        id: row.id,
        receiverName: row.receiverName,
        receiverPhone: row.receiverPhone,
        province: row.province,
        city: row.city,
        district: row.district,
        detailAddress: row.detailAddress,
        isDefault: row.isDefault === 1
    })
    selectedRegion.value = findRegionCodes(row.province, row.city, row.district)
    dialogVisible.value = true
}

const submit = async () => {
    await formRef.value.validate()
    saving.value = true
    try {
        if (form.id) {
            await addressUpdateService({ ...form })
            ElMessage.success('地址已更新')
        } else {
            await addressAddService({ ...form })
            ElMessage.success('地址已添加')
        }
        dialogVisible.value = false
        loadList()
    } finally {
        saving.value = false
    }
}

const remove = (row) => {
    ElMessageBox.confirm(`确认删除「${row.receiverName} ${row.receiverPhone}」这条地址吗？`, '删除地址', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await addressDeleteService(row.id)
        ElMessage.success('地址已删除')
        loadList()
    }).catch(() => { })
}

loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>📍 收货地址</span>
                <el-button type="primary" :icon="Plus" @click="openAdd">新增地址</el-button>
            </div>
        </template>

        <el-table :data="addresses" v-loading="loading" style="width: 100%">
            <el-table-column label="收货人" prop="receiverName" width="120"></el-table-column>
            <el-table-column label="手机号" prop="receiverPhone" width="140"></el-table-column>
            <el-table-column label="收货地址" min-width="280">
                <template #default="{ row }">
                    {{ row.province }}{{ row.city }}{{ row.district }} {{ row.detailAddress }}
                </template>
            </el-table-column>
            <el-table-column label="默认" width="90">
                <template #default="{ row }">
                    <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
                    <span v-else>—</span>
                </template>
            </el-table-column>
            <el-table-column label="更新时间" width="170">
                <template #default="{ row }">{{ formatTime(row.updateTime || row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                    <el-button type="primary" link size="small" :icon="Edit"
                        @click="openEdit(row)">编辑</el-button>
                    <el-button type="danger" link size="small" :icon="Delete"
                        @click="remove(row)">删除</el-button>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="还没有收货地址，先添加一条吧" />
            </template>
        </el-table>

        <!-- 新增/编辑地址 -->
        <el-dialog v-model="dialogVisible" :title="form.id ? '编辑地址' : '新增地址'" width="520px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="收货人" prop="receiverName">
                    <el-input v-model="form.receiverName" maxlength="20" placeholder="请输入收货人姓名" />
                </el-form-item>
                <el-form-item label="手机号" prop="receiverPhone">
                    <el-input v-model="form.receiverPhone" maxlength="11" placeholder="请输入 11 位手机号" />
                </el-form-item>
                <el-form-item label="省市区" prop="province">
                    <el-cascader v-model="selectedRegion" :options="regionData" filterable
                        placeholder="输入关键字搜索，如：杭州" style="width: 100%" @change="onRegionChange" />
                </el-form-item>
                <el-form-item label="详细地址" prop="detailAddress">
                    <el-input v-model="form.detailAddress" type="textarea" :rows="2" maxlength="100"
                        show-word-limit placeholder="街道、门牌号等" />
                </el-form-item>
                <el-form-item label="默认地址">
                    <el-switch v-model="form.isDefault" />
                    <span class="tip">下单时默认选中该地址</span>
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
