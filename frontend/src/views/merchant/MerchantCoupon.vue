<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
    couponTypeListService,
    couponTypeAddService,
    couponTypeUpdateService,
    couponStockAddService,
    merchantCouponStockListService,
    couponGrantService,
    merchantCouponStatisticsService
} from '@/api/coupon.js'
import { merchantShopInfoService } from '@/api/merchant.js'
import { money, formatTime, couponValueText, couponThresholdText, STOCK_STATUS_MAP, STOCK_STATUS_TAG, textOf } from '@/utils/mall.js'

const activeTab = ref('types')
const myUserId = ref(null)
const statistics = ref({})

// ---------- 券类型 ----------
const types = ref([])
const typeLoading = ref(false)
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('创建券类型')
const typeSaving = ref(false)
const typeFormRef = ref()

const typeForm = reactive({
    id: null,
    name: '',
    couponKind: 'amount', // amount=满减券 rate=折扣券
    minSpend: 0,
    discountAmount: 0,
    discountRate: 0.9,
    validDays: 7,
    maxUses: 1
})

const typeRules = {
    name: [{ required: true, message: '请输入券类型名称', trigger: 'blur' }],
    validDays: [{ required: true, message: '请输入有效期天数', trigger: 'blur' }]
}

// 只展示本店的券类型（公开接口返回全部，按 createUserId 过滤）
const loadTypes = async () => {
    typeLoading.value = true
    try {
        const result = await couponTypeListService()
        const all = result.data || []
        types.value = myUserId.value === null ? all : all.filter(t => t.createUserId === myUserId.value)
    } catch (e) {
        types.value = []
    } finally {
        typeLoading.value = false
    }
}

const typeName = computed(() => {
    const map = {}
    types.value.forEach(t => { map[t.id] = t.name })
    return (id) => map[id] || `券类型#${id}`
})

const openTypeAdd = () => {
    Object.assign(typeForm, {
        id: null,
        name: '',
        couponKind: 'amount',
        minSpend: 0,
        discountAmount: 0,
        discountRate: 0.9,
        validDays: 7,
        maxUses: 1
    })
    typeDialogTitle.value = '创建券类型'
    typeDialogVisible.value = true
}

const openTypeEdit = (row) => {
    const isRate = Number(row.discountRate || 0) > 0
    Object.assign(typeForm, {
        id: row.id,
        name: row.name,
        couponKind: isRate ? 'rate' : 'amount',
        minSpend: Number(row.minSpend || 0),
        discountAmount: Number(row.discountAmount || 0),
        discountRate: Number(row.discountRate || 0.9),
        validDays: row.validDays,
        maxUses: row.maxUses || 0
    })
    typeDialogTitle.value = '修改券类型'
    typeDialogVisible.value = true
}

const submitType = async () => {
    await typeFormRef.value.validate()

    const payload = {
        id: typeForm.id || undefined,
        name: typeForm.name,
        minSpend: Number(typeForm.minSpend || 0),
        discountAmount: typeForm.couponKind === 'amount' ? Number(typeForm.discountAmount) : 0,
        discountRate: typeForm.couponKind === 'rate' ? Number(typeForm.discountRate) : 0,
        validDays: typeForm.validDays,
        maxUses: Number(typeForm.maxUses || 0)
    }

    if (typeForm.couponKind === 'amount' && !(payload.discountAmount > 0)) {
        ElMessage.warning('满减券的减免金额必须大于 0')
        return
    }
    if (typeForm.couponKind === 'rate' && !(payload.discountRate > 0 && payload.discountRate < 1)) {
        ElMessage.warning('折扣率需在 0~1 之间，如 0.85 表示 85 折')
        return
    }

    typeSaving.value = true
    try {
        if (typeForm.id) {
            await couponTypeUpdateService(payload)
            ElMessage.success('券类型已更新')
        } else {
            await couponTypeAddService(payload)
            ElMessage.success('券类型已创建')
        }
        typeDialogVisible.value = false
        loadTypes()
        loadStatistics()
    } finally {
        typeSaving.value = false
    }
}

// ---------- 发放活动 ----------
const stocks = ref([])
const stockTotal = ref(0)
const stockPageNum = ref(1)
const stockPageSize = ref(10)
const stockStatus = ref('')
const stockLoading = ref(false)
const stockDialogVisible = ref(false)
const stockSaving = ref(false)
const stockFormRef = ref()

const stockForm = reactive({
    couponTypeId: null,
    totalCount: 100,
    timeRange: []
})

const stockRules = {
    couponTypeId: [{ required: true, message: '请选择券类型', trigger: 'change' }],
    totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
    timeRange: [{ required: true, message: '请选择抢购时间', trigger: 'change' }]
}

const loadStocks = async () => {
    stockLoading.value = true
    try {
        const result = await merchantCouponStockListService({
            pageNum: stockPageNum.value,
            pageSize: stockPageSize.value,
            status: stockStatus.value === '' ? null : stockStatus.value
        })
        stocks.value = result.data.items || []
        stockTotal.value = result.data.total || 0
    } catch (e) {
        stocks.value = []
        stockTotal.value = 0
    } finally {
        stockLoading.value = false
    }
}

const changeStockStatus = (val) => {
    stockStatus.value = val
    stockPageNum.value = 1
    loadStocks()
}

const onStockPageChange = (num) => {
    stockPageNum.value = num
    loadStocks()
}

const openStockAdd = () => {
    Object.assign(stockForm, { couponTypeId: null, totalCount: 100, timeRange: [] })
    stockDialogVisible.value = true
}

const submitStock = async () => {
    await stockFormRef.value.validate()
    if (!stockForm.timeRange || stockForm.timeRange.length !== 2) {
        ElMessage.warning('请选择抢购开始与结束时间')
        return
    }

    stockSaving.value = true
    try {
        await couponStockAddService({
            couponTypeId: stockForm.couponTypeId,
            totalCount: stockForm.totalCount,
            startTime: stockForm.timeRange[0],
            endTime: stockForm.timeRange[1]
        })
        ElMessage.success('发放活动已创建，库存已同步到 Redis')
        stockDialogVisible.value = false
        loadStocks()
        loadStatistics()
    } finally {
        stockSaving.value = false
    }
}

// 定向发放
const grantVisible = ref(false)
const grantSaving = ref(false)
const grantTarget = ref({})
const grantUserId = ref(null)

const openGrant = (row) => {
    grantTarget.value = row
    grantUserId.value = null
    grantVisible.value = true
}

const submitGrant = async () => {
    if (!grantUserId.value) {
        ElMessage.warning('请输入接收用户 ID')
        return
    }
    grantSaving.value = true
    try {
        await couponGrantService(grantTarget.value.id, grantUserId.value)
        ElMessage.success('已定向发放')
        grantVisible.value = false
        loadStocks()
        loadStatistics()
    } finally {
        grantSaving.value = false
    }
}

// ---------- 统计 ----------
const loadStatistics = async () => {
    try {
        const result = await merchantCouponStatisticsService()
        statistics.value = result.data || {}
    } catch (e) {
        statistics.value = {}
    }
}

const onTabChange = (name) => {
    if (name === 'stocks') {
        loadStocks()
    } else {
        loadTypes()
    }
}

// 先拿本店 userId，再按 createUserId 过滤券类型
const init = async () => {
    try {
        const result = await merchantShopInfoService()
        myUserId.value = (result.data || {}).userId ?? null
    } catch (e) {
        myUserId.value = null
    }
    await Promise.all([loadTypes(), loadStocks(), loadStatistics()])
}

init()
</script>

<template>
    <el-card class="page-container">
        <template #header>
            <div class="header">
                <span>🎫 优惠券管理</span>
                <el-button v-if="activeTab === 'types'" type="primary" :icon="Plus" @click="openTypeAdd">
                    创建券类型
                </el-button>
                <el-button v-else type="primary" :icon="Plus" @click="openStockAdd">创建发放活动</el-button>
            </div>
        </template>

        <!-- 统计概览 -->
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

        <el-tabs v-model="activeTab" @tab-change="onTabChange">
            <!-- 券类型 -->
            <el-tab-pane label="券类型" name="types">
                <el-table :data="types" v-loading="typeLoading" style="width: 100%">
                    <el-table-column label="名称" prop="name" min-width="160" show-overflow-tooltip></el-table-column>
                    <el-table-column label="类型" width="100">
                        <template #default="{ row }">
                            <el-tag size="small" :type="Number(row.discountRate || 0) > 0 ? 'warning' : 'primary'">
                                {{ Number(row.discountRate || 0) > 0 ? '折扣券' : '满减券' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="优惠" width="110">
                        <template #default="{ row }">
                            <span class="discount">{{ couponValueText(row) }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="门槛" width="130">
                        <template #default="{ row }">{{ couponThresholdText(row) }}</template>
                    </el-table-column>
                    <el-table-column label="有效期" width="110">
                        <template #default="{ row }">领取后 {{ row.validDays }} 天</template>
                    </el-table-column>
                    <el-table-column label="限用次数" width="110">
                        <template #default="{ row }">{{ row.maxUses ? `${row.maxUses} 次` : '不限' }}</template>
                    </el-table-column>
                    <el-table-column label="创建时间" width="170">
                        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="100" fixed="right">
                        <template #default="{ row }">
                            <el-button type="primary" link size="small" @click="openTypeEdit(row)">编辑</el-button>
                        </template>
                    </el-table-column>
                    <template #empty>
                        <el-empty description="还没有券类型，先创建一个吧" />
                    </template>
                </el-table>
            </el-tab-pane>

            <!-- 发放活动 -->
            <el-tab-pane label="发放活动" name="stocks">
                <el-radio-group v-model="stockStatus" @change="changeStockStatus" class="status-filter">
                    <el-radio-button label="">全部</el-radio-button>
                    <el-radio-button :label="0">未开始</el-radio-button>
                    <el-radio-button :label="1">进行中</el-radio-button>
                    <el-radio-button :label="2">已结束</el-radio-button>
                    <el-radio-button :label="3">已售罄</el-radio-button>
                </el-radio-group>

                <el-table :data="stocks" v-loading="stockLoading" style="width: 100%">
                    <el-table-column label="券类型" min-width="150">
                        <template #default="{ row }">{{ typeName(row.couponTypeId) }}</template>
                    </el-table-column>
                    <el-table-column label="总量" prop="totalCount" width="90"></el-table-column>
                    <el-table-column label="剩余" width="90">
                        <template #default="{ row }">
                            <span :class="{ low: Number(row.remainCount) <= 0 }">{{ row.remainCount }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="已核销" prop="usedCount" width="90"></el-table-column>
                    <el-table-column label="抢购时间" min-width="320">
                        <template #default="{ row }">
                            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="100">
                        <template #default="{ row }">
                            <el-tag :type="textOf(STOCK_STATUS_TAG, row.status, 'info')" size="small">
                                {{ textOf(STOCK_STATUS_MAP, row.status) }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="110" fixed="right">
                        <template #default="{ row }">
                            <el-button type="primary" link size="small" @click="openGrant(row)">定向发放</el-button>
                        </template>
                    </el-table-column>
                    <template #empty>
                        <el-empty description="还没有发放活动，点击右上角创建" />
                    </template>
                </el-table>

                <el-pagination v-if="stockTotal > 0" v-model:current-page="stockPageNum"
                    v-model:page-size="stockPageSize" layout="total, prev, pager, next" background
                    :total="stockTotal" @current-change="onStockPageChange"
                    style="margin-top: 20px; justify-content: center" />
            </el-tab-pane>
        </el-tabs>

        <!-- 创建/修改券类型 -->
        <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="560px">
            <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="110px">
                <el-form-item label="券类型名称" prop="name">
                    <el-input v-model="typeForm.name" maxlength="40" placeholder="如：满 100 减 20" />
                </el-form-item>
                <el-form-item label="优惠方式">
                    <el-radio-group v-model="typeForm.couponKind">
                        <el-radio-button label="amount">满减券</el-radio-button>
                        <el-radio-button label="rate">折扣券</el-radio-button>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="最低消费">
                    <el-input-number v-model="typeForm.minSpend" :min="0" :precision="2" :step="10" />
                    <span class="tip">0 表示无门槛</span>
                </el-form-item>
                <el-form-item v-if="typeForm.couponKind === 'amount'" label="减免金额">
                    <el-input-number v-model="typeForm.discountAmount" :min="0" :precision="2" :step="5" />
                </el-form-item>
                <el-form-item v-else label="折扣率">
                    <el-input-number v-model="typeForm.discountRate" :min="0.01" :max="0.99" :precision="2"
                        :step="0.05" />
                    <span class="tip">0.85 表示 85 折</span>
                </el-form-item>
                <el-form-item label="有效期天数" prop="validDays">
                    <el-input-number v-model="typeForm.validDays" :min="1" :step="1" />
                    <span class="tip">领取后 N 天内有效</span>
                </el-form-item>
                <el-form-item label="限用次数">
                    <el-input-number v-model="typeForm.maxUses" :min="0" :step="1" />
                    <span class="tip">0 表示不限</span>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="typeDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="typeSaving" @click="submitType">保存</el-button>
            </template>
        </el-dialog>

        <!-- 创建发放活动 -->
        <el-dialog v-model="stockDialogVisible" title="创建优惠券发放活动" width="560px">
            <el-form ref="stockFormRef" :model="stockForm" :rules="stockRules" label-width="110px">
                <el-form-item label="券类型" prop="couponTypeId">
                    <el-select v-model="stockForm.couponTypeId" placeholder="请选择本店券类型" style="width: 100%">
                        <el-option v-for="t in types" :key="t.id" :label="t.name" :value="t.id">
                            <span>{{ t.name }}</span>
                            <span class="option-tip">{{ couponValueText(t) }}</span>
                        </el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="发放总量" prop="totalCount">
                    <el-input-number v-model="stockForm.totalCount" :min="1" :step="10" />
                    <span class="tip">创建后同步写入 Redis 库存</span>
                </el-form-item>
                <el-form-item label="抢购时间" prop="timeRange">
                    <el-date-picker v-model="stockForm.timeRange" type="datetimerange" range-separator="至"
                        start-placeholder="开始时间" end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
                </el-form-item>
                <el-alert type="info" :closable="false" show-icon
                    title="用户需在抢购时间内抢券，先到先得；同一用户同一活动只能抢一次" />
            </el-form>
            <template #footer>
                <el-button @click="stockDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="stockSaving" @click="submitStock">创建</el-button>
            </template>
        </el-dialog>

        <!-- 定向发放 -->
        <el-dialog v-model="grantVisible" title="定向发放优惠券" width="460px">
            <el-form label-width="100px">
                <el-form-item label="发放活动">
                    <span>{{ typeName(grantTarget.couponTypeId) }}（活动 #{{ grantTarget.id }}）</span>
                </el-form-item>
                <el-form-item label="接收用户 ID">
                    <el-input-number v-model="grantUserId" :min="1" :step="1" />
                </el-form-item>
                <el-alert type="warning" :closable="false" show-icon
                    title="定向发放不占用抢购库存，同一用户同一活动只能领取一次" />
            </el-form>
            <template #footer>
                <el-button @click="grantVisible = false">取消</el-button>
                <el-button type="primary" :loading="grantSaving" @click="submitGrant">发放</el-button>
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

    .stat-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
        gap: 14px;
        margin-bottom: 10px;

        .stat-item {
            border: 1px solid #ebeef5;
            border-radius: 6px;
            padding: 14px;
            text-align: center;

            .num {
                font-size: 22px;
                font-weight: bold;
                color: #409eff;
            }

            .label {
                color: #999;
                font-size: 12px;
                margin-top: 4px;
            }
        }
    }

    .status-filter {
        margin-bottom: 16px;
    }

    .discount {
        color: #f56c6c;
        font-weight: bold;
    }

    .low {
        color: #f56c6c;
        font-weight: bold;
    }

    .tip {
        margin-left: 10px;
        color: #999;
        font-size: 12px;
    }

    .option-tip {
        float: right;
        color: #f56c6c;
    }
}
</style>
