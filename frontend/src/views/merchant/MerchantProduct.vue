<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Upload } from '@element-plus/icons-vue'
import { useTokenStore } from '@/stores/token.js'
import {
    merchantProductListService,
    productDetailService,
    productSkuListService,
    productAddService,
    productUpdateService,
    productOffShelfService,
    productCategoryListService
} from '@/api/product.js'
import { productStatusText, formatTime, money } from '@/utils/mall.js'
import ImageUploader from '@/components/ImageUploader.vue'

const tokenStore = useTokenStore()

const products = ref([])
const categories = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref('')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('发布商品')
const formRef = ref()

const form = reactive({
    id: null,
    name: '',
    categoryId: null,
    coverImg: '',
    images: [],
    videoUrl: '',
    description: '',
    price: 0.01,
    marketPrice: null,
    stock: 0,
    status: 1,
    skuList: []
})

const rules = {
    name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
    coverImg: [{ required: true, message: '请上传商品封面', trigger: 'change' }],
    price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }]
}

const categoryName = computed(() => {
    const map = {}
    categories.value.forEach(c => { map[c.id] = c.name })
    return (id) => map[id] || '—'
})

// ---------- 列表 ----------
const loadList = async () => {
    loading.value = true
    try {
        const result = await merchantProductListService({
            pageNum: pageNum.value,
            pageSize: pageSize.value,
            status: status.value === '' ? null : status.value
        })
        products.value = result.data.items || []
        total.value = result.data.total || 0
    } catch (e) {
        products.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const loadCategories = async () => {
    try {
        const result = await productCategoryListService()
        categories.value = result.data || []
    } catch (e) {
        categories.value = []
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

// ---------- JSON 字段工具（后端存 JSON 字符串） ----------
const parseArray = (text) => {
    if (!text) return []
    try {
        const arr = JSON.parse(text)
        return Array.isArray(arr) ? arr : []
    } catch (e) {
        return []
    }
}

// ---------- 新增 / 编辑 ----------
const resetForm = () => {
    Object.assign(form, {
        id: null,
        name: '',
        categoryId: null,
        coverImg: '',
        images: [],
        videoUrl: '',
        description: '',
        price: 0.01,
        marketPrice: null,
        stock: 0,
        status: 1,
        skuList: []
    })
}

const openAdd = () => {
    resetForm()
    dialogTitle.value = '发布商品'
    dialogVisible.value = true
}

const openEdit = async (row) => {
    resetForm()
    dialogTitle.value = '编辑商品'
    try {
        const detailResult = await productDetailService(row.id)
        const detail = detailResult.data || {}
        const skuResult = await productSkuListService(row.id)
        const skuList = (skuResult.data || []).map(s => ({
            skuName: s.skuName,
            specText: parseArray(s.specValues).join(','),
            price: Number(s.price),
            stock: s.stock
        }))

        Object.assign(form, {
            id: detail.id,
            name: detail.name,
            categoryId: detail.categoryId,
            coverImg: detail.coverImg,
            images: parseArray(detail.images),
            videoUrl: detail.videoUrl || '',
            description: detail.description || '',
            price: detail.price,
            marketPrice: detail.marketPrice,
            stock: detail.stock,
            status: detail.status,
            skuList: skuList
        })
        dialogVisible.value = true
    } catch (e) {
        // 统一提示
    }
}

const addImage = () => {
    if (form.images.length >= 5) {
        ElMessage.warning('详情图最多 5 张')
        return
    }
    form.images.push('')
}

const removeImage = (index) => form.images.splice(index, 1)

// 视频上传：走项目既有 POST /upload（OSS），成功后回写直链
const videoUploadSuccess = (result) => {
    if (result && result.code === 0) {
        form.videoUrl = result.data
        ElMessage.success('视频上传成功')
    } else {
        ElMessage.error((result && result.message) || '视频上传失败')
    }
}

const videoUploadError = () => {
    ElMessage.error('视频上传失败，可手动粘贴视频直链')
}

const addSku = () => {
    form.skuList.push({ skuName: '', specText: '', price: Number(form.price) || 0.01, stock: 0 })
}

const removeSku = (index) => form.skuList.splice(index, 1)

const submit = async () => {
    await formRef.value.validate()

    for (const sku of form.skuList) {
        if (!sku.skuName || !sku.skuName.trim()) {
            ElMessage.warning('规格名称不能为空')
            return
        }
        if (!(Number(sku.price) > 0)) {
            ElMessage.warning(`规格「${sku.skuName}」价格必须大于 0`)
            return
        }
    }

    const payload = {
        id: form.id || undefined,
        name: form.name,
        categoryId: form.categoryId,
        coverImg: form.coverImg,
        images: form.images.filter(u => u && u.trim()),
        videoUrl: form.videoUrl,
        description: form.description,
        price: form.price,
        marketPrice: form.marketPrice,
        stock: form.stock,
        status: form.status,
        skuList: form.skuList.map(s => ({
            skuName: s.skuName,
            specValues: s.specText
                ? s.specText.split(/[,，]/).map(v => v.trim()).filter(v => v)
                : [],
            price: s.price,
            stock: s.stock
        }))
    }

    saving.value = true
    try {
        if (form.id) {
            await productUpdateService(payload)
            ElMessage.success('商品已更新')
        } else {
            await productAddService(payload)
            ElMessage.success('商品已发布')
        }
        dialogVisible.value = false
        loadList()
    } finally {
        saving.value = false
    }
}

const offShelf = (row) => {
    ElMessageBox.confirm(`确认下架「${row.name}」吗？下架后买家不可见。`, '下架商品', {
        confirmButtonText: '确认下架',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        await productOffShelfService(row.id)
        ElMessage.success('商品已下架')
        loadList()
    }).catch(() => { })
}

loadCategories()
loadList()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>📦 商品管理</span>
                <el-button type="primary" :icon="Plus" @click="openAdd">发布商品</el-button>
            </div>
        </template>

        <el-radio-group v-model="status" @change="changeStatus" class="status-filter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button :label="1">已上架</el-radio-button>
            <el-radio-button :label="0">已下架</el-radio-button>
        </el-radio-group>

        <el-table :data="products" v-loading="loading" style="width: 100%">
            <el-table-column label="封面" width="90">
                <template #default="{ row }">
                    <img :src="row.coverImg" class="cover" />
                </template>
            </el-table-column>
            <el-table-column label="商品名称" prop="name" min-width="180" show-overflow-tooltip></el-table-column>
            <el-table-column label="分类" width="120">
                <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
            </el-table-column>
            <el-table-column label="价格" width="110">
                <template #default="{ row }">¥{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column label="库存" prop="stock" width="90"></el-table-column>
            <el-table-column label="销量" prop="salesCount" width="90"></el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                        {{ productStatusText(row.status) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                    <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
                    <el-button v-if="row.status === 1" type="danger" link size="small"
                        @click="offShelf(row)">下架</el-button>
                </template>
            </el-table-column>
            <template #empty>
                <el-empty description="还没有商品，点击右上角发布一个吧" />
            </template>
        </el-table>

        <el-pagination v-if="total > 0" v-model:current-page="pageNum" v-model:page-size="pageSize"
            layout="total, prev, pager, next" background :total="total" @current-change="onPageChange"
            style="margin-top: 20px; justify-content: center" />

        <!-- 发布/编辑商品 -->
        <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" top="5vh">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
                <el-form-item label="商品名称" prop="name">
                    <el-input v-model="form.name" maxlength="80" placeholder="请输入商品名称" />
                </el-form-item>
                <el-form-item label="商品分类" prop="categoryId">
                    <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="商品封面" prop="coverImg">
                    <ImageUploader v-model="form.coverImg" :width="140" :height="140" tip="列表页展示的主图" />
                </el-form-item>
                <el-form-item label="详情图">
                    <div class="image-list">
                        <div v-for="(img, index) in form.images" :key="index" class="image-item">
                            <ImageUploader :model-value="img" :width="110" :height="110" tip=""
                                @update:model-value="val => form.images[index] = val" />
                            <el-button type="danger" link size="small" :icon="Delete"
                                @click="removeImage(index)">删除</el-button>
                        </div>
                        <el-button :icon="Plus" @click="addImage">添加详情图</el-button>
                    </div>
                </el-form-item>
                <el-form-item label="商品价格">
                    <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
                    <span class="tip">无规格时按下单价格计算</span>
                </el-form-item>
                <el-form-item label="市场价">
                    <el-input-number v-model="form.marketPrice" :min="0" :precision="2" :step="1" />
                </el-form-item>
                <el-form-item label="总库存">
                    <el-input-number v-model="form.stock" :min="0" />
                    <span class="tip">含全部规格的总库存</span>
                </el-form-item>
                <el-form-item label="上架状态">
                    <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
                    <span class="tip">{{ productStatusText(form.status) }}</span>
                </el-form-item>
                <el-form-item label="视频地址">
                    <div class="video-field">
                        <el-input v-model="form.videoUrl" placeholder="选填，视频文件直链（mp4/webm）" clearable />
                        <el-upload :auto-upload="true" :show-file-list="false" action="/api/upload" name="file"
                            :headers="{ Authorization: tokenStore.token }" accept="video/*"
                            :on-success="videoUploadSuccess" :on-error="videoUploadError">
                            <el-button :icon="Upload">上传视频</el-button>
                        </el-upload>
                    </div>
                    <div class="video-tip">需为视频文件直链才能播放；B站/抖音等分享页链接无法播放</div>
                </el-form-item>
                <el-form-item label="商品描述">
                    <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
                </el-form-item>

                <el-divider content-position="left">
                    商品规格
                    <span class="divider-tip">保存时按下述列表重建规格（见 Bug 清单 B-002）</span>
                </el-divider>
                <el-table :data="form.skuList" size="small" border>
                    <el-table-column label="规格名称" min-width="150">
                        <template #default="{ row }">
                            <el-input v-model="row.skuName" size="small" placeholder="如：黑色 256G" />
                        </template>
                    </el-table-column>
                    <el-table-column label="规格值（逗号分隔）" min-width="170">
                        <template #default="{ row }">
                            <el-input v-model="row.specText" size="small" placeholder="如：黑色,256G" />
                        </template>
                    </el-table-column>
                    <el-table-column label="价格" width="140">
                        <template #default="{ row }">
                            <el-input-number v-model="row.price" size="small" :min="0.01" :precision="2"
                                controls-position="right" style="width: 100%" />
                        </template>
                    </el-table-column>
                    <el-table-column label="库存" width="130">
                        <template #default="{ row }">
                            <el-input-number v-model="row.stock" size="small" :min="0" controls-position="right"
                                style="width: 100%" />
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="80">
                        <template #default="{ $index }">
                            <el-button type="danger" link size="small" @click="removeSku($index)">删除</el-button>
                        </template>
                    </el-table-column>
                    <template #empty>
                        <span class="empty-sku">未配置规格时，买家按商品价格直接下单</span>
                    </template>
                </el-table>
                <el-button class="add-sku" :icon="Plus" size="small" @click="addSku">添加规格</el-button>
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

    .cover {
        width: 56px;
        height: 56px;
        object-fit: cover;
        border-radius: 4px;
        background-color: #f5f7fa;
    }

    .image-list {
        display: flex;
        flex-wrap: wrap;
        gap: 14px;
        align-items: flex-start;

        .image-item {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
        }
    }

    .tip {
        margin-left: 10px;
        color: #999;
        font-size: 12px;
    }

    .video-field {
        display: flex;
        gap: 10px;
        width: 100%;
    }

    .video-tip {
        width: 100%;
        color: #999;
        font-size: 12px;
        line-height: 1.6;
    }

    .divider-tip {
        color: #e6a23c;
        font-size: 12px;
        margin-left: 8px;
    }

    .empty-sku {
        color: #999;
        font-size: 12px;
    }

    .add-sku {
        margin-top: 10px;
    }
}
</style>
