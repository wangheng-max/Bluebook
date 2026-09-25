<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Goods, Sell, Discount, List, RefreshLeft, Shop } from '@element-plus/icons-vue'
import { merchantShopInfoService, merchantShopInfoUpdateService } from '@/api/merchant.js'
import { merchantCouponStatisticsService } from '@/api/coupon.js'
import { formatTime } from '@/utils/mall.js'
import ImageUploader from '@/components/ImageUploader.vue'

const router = useRouter()

const shop = ref({})
const statistics = ref({})
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
    shopName: '',
    shopLogo: '',
    shopDescription: ''
})

const rules = {
    shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }]
}

const loadData = async () => {
    loading.value = true
    try {
        const [shopResult, statResult] = await Promise.all([
            merchantShopInfoService(),
            merchantCouponStatisticsService()
        ])
        shop.value = shopResult.data || {}
        statistics.value = statResult.data || {}
    } catch (e) {
        // 统一提示
    } finally {
        loading.value = false
    }
}

const openEdit = () => {
    Object.assign(form, {
        shopName: shop.value.shopName || '',
        shopLogo: shop.value.shopLogo || '',
        shopDescription: shop.value.shopDescription || ''
    })
    dialogVisible.value = true
}

const submit = async () => {
    await formRef.value.validate()
    saving.value = true
    try {
        await merchantShopInfoUpdateService({ ...form })
        ElMessage.success('店铺信息已更新')
        dialogVisible.value = false
        loadData()
    } finally {
        saving.value = false
    }
}

const entries = [
    { title: '商品管理', desc: '发布 / 编辑 / 上下架', icon: Goods, path: '/merchant/products', color: '#409eff' },
    { title: '团购管理', desc: '创建团购活动、查看进度', icon: Sell, path: '/merchant/group-buys', color: '#67c23a' },
    { title: '优惠券管理', desc: '券类型、发放活动、定向发放', icon: Discount, path: '/merchant/coupons', color: '#e6a23c' },
    { title: '订单管理', desc: '查看订单、发货', icon: List, path: '/merchant/orders', color: '#909399' },
    { title: '退款审核', desc: '处理买家退款申请', icon: RefreshLeft, path: '/merchant/refunds', color: '#f56c6c' }
]

loadData()
</script>

<template>
    <el-card class="page-container" v-loading="loading">
        <template #header>
            <div class="header">
                <span>🏪 商家中心</span>
                <el-button type="primary" plain :icon="Edit" @click="openEdit">编辑店铺信息</el-button>
            </div>
        </template>

        <!-- 店铺信息 -->
        <div class="shop-card">
            <img :src="shop.shopLogo" class="logo" />
            <div class="shop-info">
                <h2>{{ shop.shopName }}</h2>
                <p class="desc">{{ shop.shopDescription || '还没有店铺简介' }}</p>
                <div class="meta">
                    <span>联系人：{{ shop.contactName || '—' }}</span>
                    <span>电话：{{ shop.contactPhone || '—' }}</span>
                </div>
                <div class="meta">
                    <span>地址：{{ shop.province }}{{ shop.city }}{{ shop.district }}{{ shop.address }}</span>
                </div>
                <div class="meta">
                    <span>最近更新时间：{{ formatTime(shop.updateTime) }}</span>
                </div>
            </div>
        </div>

        <!-- 优惠券统计 -->
        <div class="stat-row">
            <div class="stat-item">
                <div class="num">{{ statistics.typeCount || 0 }}</div>
                <div class="label">券类型</div>
            </div>
            <div class="stat-item">
                <div class="num">{{ statistics.stockCount || 0 }}</div>
                <div class="label">发放活动</div>
            </div>
            <div class="stat-item">
                <div class="num">{{ statistics.grantedCount || 0 }}</div>
                <div class="label">已领取张数</div>
            </div>
            <div class="stat-item">
                <div class="num">{{ statistics.usedCount || 0 }}</div>
                <div class="label">已使用张数</div>
            </div>
        </div>

        <!-- 快捷入口 -->
        <div class="entry-grid">
            <div v-for="e in entries" :key="e.path" class="entry-item" @click="router.push(e.path)">
                <el-icon :size="26" :color="e.color"><component :is="e.icon" /></el-icon>
                <div class="entry-text">
                    <div class="entry-title">{{ e.title }}</div>
                    <div class="entry-desc">{{ e.desc }}</div>
                </div>
            </div>
        </div>

        <!-- 编辑店铺信息 -->
        <el-dialog v-model="dialogVisible" title="编辑店铺信息" width="520px">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="店铺名称" prop="shopName">
                    <el-input v-model="form.shopName" maxlength="50" />
                </el-form-item>
                <el-form-item label="店铺 Logo">
                    <ImageUploader v-model="form.shopLogo" :width="100" :height="100" />
                </el-form-item>
                <el-form-item label="店铺简介">
                    <el-input v-model="form.shopDescription" type="textarea" :rows="3" maxlength="200"
                        show-word-limit />
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

    .shop-card {
        display: flex;
        gap: 20px;
        padding: 20px;
        border-radius: 6px;
        background: linear-gradient(135deg, #ecf5ff, #f5f7fa);

        .logo {
            width: 90px;
            height: 90px;
            border-radius: 6px;
            object-fit: cover;
            background-color: #fff;
        }

        .shop-info {
            flex: 1;

            h2 {
                margin: 0 0 8px;
                font-size: 20px;
            }

            .desc {
                color: #666;
                font-size: 13px;
                margin: 0 0 10px;
            }

            .meta {
                display: flex;
                gap: 24px;
                color: #999;
                font-size: 12px;
                line-height: 2;
            }
        }
    }

    .stat-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 16px;
        margin: 20px 0;

        .stat-item {
            border: 1px solid #ebeef5;
            border-radius: 6px;
            padding: 18px;
            text-align: center;

            .num {
                font-size: 26px;
                font-weight: bold;
                color: #409eff;
            }

            .label {
                color: #999;
                font-size: 13px;
                margin-top: 6px;
            }
        }
    }

    .entry-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
        gap: 16px;

        .entry-item {
            display: flex;
            align-items: center;
            gap: 14px;
            border: 1px solid #ebeef5;
            border-radius: 6px;
            padding: 16px;
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
                border-color: #c6e2ff;
                background-color: #f5f9ff;
            }

            .entry-title {
                font-weight: bold;
            }

            .entry-desc {
                color: #999;
                font-size: 12px;
                margin-top: 4px;
            }
        }
    }
}
</style>
