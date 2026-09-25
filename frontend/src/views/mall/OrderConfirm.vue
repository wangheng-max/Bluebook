<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { productDetailService, productSkuListService } from '@/api/product.js'
import { groupBuyDetailService } from '@/api/groupBuy.js'
import { addressListService } from '@/api/address.js'
import { availableCouponService } from '@/api/coupon.js'
import { orderCreateService } from '@/api/order.js'
import { money, couponValueText, couponThresholdText, genIdempotencyKey } from '@/utils/mall.js'

const route = useRoute()
const router = useRouter()

const orderType = Number(route.query.orderType || 1)
const productId = Number(route.query.productId)
const skuId = route.query.skuId ? Number(route.query.skuId) : null
const groupBuyId = route.query.groupBuyId ? Number(route.query.groupBuyId) : null
const quantity = ref(Number(route.query.quantity || 1))

// 幂等键：进入页面生成一次，重复点击提交/网络重试都由服务端去重
const idempotencyKey = ref(genIdempotencyKey())

const product = ref({})
const sku = ref(null)
const groupBuy = ref({})
const addresses = ref([])
const selectedAddressId = ref(null)
const coupons = ref([])
const selectedCouponId = ref(null)
const remark = ref('')
const loading = ref(false)
const submitting = ref(false)
const showCouponDialog = ref(false)

// ---------- 金额计算（与后端 OrderServiceImpl 规则一致） ----------
const unitPrice = computed(() => {
    if (orderType === 2) return Number(groupBuy.value.groupPrice || 0)
    if (sku.value) return Number(sku.value.price || 0)
    return Number(product.value.price || 0)
})
const totalAmount = computed(() => unitPrice.value * quantity.value)

const selectedCoupon = computed(() => coupons.value.find(c => c.id === selectedCouponId.value) || null)

const couponDiscount = computed(() => {
    const c = selectedCoupon.value
    if (!c) return 0
    const amount = Number(c.discountAmount || 0)
    if (amount > 0) return Math.min(amount, totalAmount.value)
    const rate = Number(c.discountRate || 0)
    if (rate > 0 && rate < 1) {
        return Number((totalAmount.value * (1 - rate)).toFixed(2))
    }
    return 0
})

const payAmount = computed(() => Math.max(0, Number((totalAmount.value - couponDiscount.value).toFixed(2))))

const availableStock = computed(() => {
    if (orderType === 2) return Number(product.value.stock || 0)
    if (sku.value) return Number(sku.value.stock || 0)
    return Number(product.value.stock || 0)
})

// ---------- 数据加载 ----------
const loadBase = async () => {
    loading.value = true
    try {
        const productResult = await productDetailService(productId)
        product.value = productResult.data || {}

        if (orderType === 2 && groupBuyId) {
            const gbResult = await groupBuyDetailService(groupBuyId)
            groupBuy.value = gbResult.data || {}
        } else if (skuId) {
            const skuResult = await productSkuListService(productId)
            sku.value = (skuResult.data || []).find(s => s.id === skuId) || null
        }

        const addressResult = await addressListService()
        addresses.value = addressResult.data || []
        const defaultAddress = addresses.value.find(a => a.isDefault === 1)
        selectedAddressId.value = defaultAddress ? defaultAddress.id : (addresses.value[0]?.id || null)

        await loadCoupons()
    } catch (e) {
        // 错误提示由响应拦截器统一处理
    } finally {
        loading.value = false
    }
}

// 可用券：按当前订单金额过滤（门槛）
const loadCoupons = async () => {
    try {
        const result = await availableCouponService({ minSpend: totalAmount.value })
        coupons.value = result.data || []
        // 已选券不再满足门槛时清空
        if (selectedCouponId.value && !coupons.value.some(c => c.id === selectedCouponId.value)) {
            selectedCouponId.value = null
        }
    } catch (e) {
        coupons.value = []
    }
}

watch(quantity, () => loadCoupons())

const chooseCoupon = (id) => {
    selectedCouponId.value = id
    showCouponDialog.value = false
}

// ---------- 提交订单 ----------
const submit = async () => {
    if (!selectedAddressId.value) {
        ElMessage.warning('请先选择收货地址')
        return
    }
    if (quantity.value < 1) {
        ElMessage.warning('购买数量至少为 1')
        return
    }
    if (availableStock.value > 0 && quantity.value > availableStock.value) {
        ElMessage.warning(`库存不足，最多可购买 ${availableStock.value} 件`)
        return
    }
    if (orderType === 2 && groupBuy.value.joined) {
        ElMessage.warning('您已参与过该团购')
        return
    }

    const payload = {
        orderType: orderType,
        groupBuyId: orderType === 2 ? groupBuyId : null,
        addressId: selectedAddressId.value,
        userCouponId: selectedCouponId.value || null,
        remark: remark.value,
        productItems: [
            {
                productId: productId,
                skuId: orderType === 2 ? null : skuId,
                quantity: quantity.value
            }
        ]
    }

    submitting.value = true
    try {
        const result = await orderCreateService(payload, idempotencyKey.value)
        ElMessage.success(`下单成功，订单号 ${result.data.orderNo}`)
        router.push('/mall/my-order')
    } catch (e) {
        // 失败原因由拦截器提示；幂等键可继续复用（后端失败时会释放占坑）
    } finally {
        submitting.value = false
    }
}

onMounted(loadBase)
</script>

<template>
    <el-card class="page-container" v-loading="loading">
        <template #header>
            <div class="header">
                <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
                <span>确认订单</span>
                <el-tag size="small" :type="orderType === 2 ? 'danger' : 'primary'">
                    {{ orderType === 2 ? '团购订单' : '普通订单' }}
                </el-tag>
            </div>
        </template>

        <div class="content">
            <!-- 收货地址 -->
            <div class="block">
                <div class="block-title">
                    <span>收货地址</span>
                    <el-button text type="primary" :icon="Plus" @click="router.push('/user/address')">
                        管理地址
                    </el-button>
                </div>
                <div v-if="addresses.length > 0" class="address-list">
                    <div v-for="a in addresses" :key="a.id" class="address-item"
                        :class="{ active: selectedAddressId === a.id }" @click="selectedAddressId = a.id">
                        <div class="receiver">
                            {{ a.receiverName }} <span class="phone">{{ a.receiverPhone }}</span>
                            <el-tag v-if="a.isDefault === 1" size="small" type="success">默认</el-tag>
                        </div>
                        <div class="detail">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detailAddress }}</div>
                    </div>
                </div>
                <el-empty v-else description="还没有收货地址，请先添加" :image-size="60">
                    <el-button type="primary" @click="router.push('/user/address')">去添加地址</el-button>
                </el-empty>
            </div>

            <!-- 商品信息 -->
            <div class="block">
                <div class="block-title">商品信息</div>
                <div class="goods">
                    <img :src="orderType === 2 ? groupBuy.productCover : product.coverImg" class="goods-img" />
                    <div class="goods-info">
                        <div class="goods-name">{{ orderType === 2 ? groupBuy.title : product.name }}</div>
                        <div class="goods-spec">
                            <span v-if="orderType === 2">团购商品：{{ groupBuy.productName }}</span>
                            <span v-else-if="sku">规格：{{ sku.skuName }}</span>
                            <span v-else>默认规格</span>
                        </div>
                        <div class="goods-price">单价 ¥{{ money(unitPrice) }}</div>
                    </div>
                    <el-input-number v-model="quantity" :min="1"
                        :max="availableStock > 0 ? availableStock : 1" />
                </div>
            </div>

            <!-- 优惠券 -->
            <div class="block">
                <div class="block-title">优惠券</div>
                <div class="coupon-row">
                    <span v-if="selectedCoupon" class="coupon-selected">
                        {{ selectedCoupon.typeName }}（{{ couponValueText(selectedCoupon) }}，{{ couponThresholdText(selectedCoupon) }}）
                        <el-button text type="primary" size="small" @click="selectedCouponId = null">不使用</el-button>
                    </span>
                    <span v-else class="coupon-empty">
                        未使用优惠券（可用 {{ coupons.length }} 张）
                    </span>
                    <el-button v-if="coupons.length > 0" type="primary" plain size="small"
                        @click="showCouponDialog = true">选择优惠券</el-button>
                </div>
            </div>

            <!-- 备注 -->
            <div class="block">
                <div class="block-title">订单备注</div>
                <el-input v-model="remark" maxlength="100" show-word-limit placeholder="选填，如：尽快发货" />
            </div>

            <!-- 金额汇总 -->
            <div class="summary">
                <div class="line"><span>商品金额</span><span>¥{{ money(totalAmount) }}</span></div>
                <div class="line"><span>优惠券抵扣</span><span class="discount">-¥{{ money(couponDiscount) }}</span></div>
                <div class="line total"><span>应付金额</span><span class="pay">¥{{ money(payAmount) }}</span></div>
                <el-button type="danger" size="large" :loading="submitting" @click="submit">
                    提交订单
                </el-button>
            </div>
        </div>

        <!-- 选择优惠券 -->
        <el-dialog v-model="showCouponDialog" title="选择优惠券" width="520px">
            <div v-if="coupons.length > 0" class="dialog-coupon-list">
                <div v-for="c in coupons" :key="c.id" class="dialog-coupon-item"
                    :class="{ active: selectedCouponId === c.id }" @click="chooseCoupon(c.id)">
                    <div class="left">{{ couponValueText(c) }}</div>
                    <div class="right">
                        <div class="name">{{ c.typeName }}</div>
                        <div class="threshold">{{ couponThresholdText(c) }}</div>
                    </div>
                    <el-icon v-if="selectedCouponId === c.id" color="#409eff"><Plus /></el-icon>
                </div>
            </div>
            <el-empty v-else description="当前订单无可用优惠券" :image-size="60" />
            <template #footer>
                <el-button @click="chooseCoupon(null)">不使用优惠券</el-button>
                <el-button type="primary" @click="showCouponDialog = false">确定</el-button>
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
        align-items: center;
        gap: 12px;
        font-size: 18px;
        font-weight: bold;
    }

    .content {
        max-width: 820px;
        margin: 0 auto;

        .block {
            margin-bottom: 24px;

            .block-title {
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-weight: bold;
                margin-bottom: 12px;
            }
        }

        .address-list {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
            gap: 12px;

            .address-item {
                border: 1px solid #ebeef5;
                border-radius: 4px;
                padding: 12px;
                cursor: pointer;
                transition: all 0.2s;

                &:hover {
                    border-color: #c6e2ff;
                }

                &.active {
                    border-color: #409eff;
                    background-color: #ecf5ff;
                }

                .receiver {
                    font-weight: bold;
                    margin-bottom: 6px;

                    .phone {
                        color: #666;
                        font-weight: normal;
                        margin: 0 8px;
                    }
                }

                .detail {
                    color: #999;
                    font-size: 12px;
                }
            }
        }

        .goods {
            display: flex;
            align-items: center;
            gap: 16px;
            border: 1px solid #ebeef5;
            border-radius: 4px;
            padding: 14px;

            .goods-img {
                width: 90px;
                height: 90px;
                object-fit: cover;
                border-radius: 4px;
                background-color: #f5f7fa;
            }

            .goods-info {
                flex: 1;

                .goods-name {
                    font-size: 15px;
                    font-weight: bold;
                }

                .goods-spec {
                    color: #999;
                    font-size: 12px;
                    margin: 6px 0;
                }

                .goods-price {
                    color: #f56c6c;
                    font-weight: bold;
                }
            }
        }

        .coupon-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            border: 1px solid #ebeef5;
            border-radius: 4px;
            padding: 12px 14px;

            .coupon-selected {
                color: #f56c6c;
            }

            .coupon-empty {
                color: #999;
            }
        }

        .summary {
            border-top: 1px solid #ebeef5;
            padding-top: 16px;
            text-align: right;

            .line {
                display: flex;
                justify-content: flex-end;
                gap: 30px;
                margin-bottom: 8px;
                color: #666;

                &.total {
                    font-size: 16px;
                    font-weight: bold;
                    color: #333;
                    margin: 12px 0 16px;

                    .pay {
                        color: #f56c6c;
                        font-size: 22px;
                    }
                }

                .discount {
                    color: #f56c6c;
                }
            }
        }
    }

    .dialog-coupon-list {
        max-height: 360px;
        overflow-y: auto;

        .dialog-coupon-item {
            display: flex;
            align-items: center;
            gap: 16px;
            border: 1px solid #ebeef5;
            border-radius: 4px;
            padding: 12px;
            margin-bottom: 10px;
            cursor: pointer;

            &.active {
                border-color: #409eff;
                background-color: #ecf5ff;
            }

            .left {
                color: #f56c6c;
                font-size: 20px;
                font-weight: bold;
                width: 90px;
            }

            .right {
                flex: 1;

                .name {
                    font-weight: bold;
                }

                .threshold {
                    color: #999;
                    font-size: 12px;
                    margin-top: 4px;
                }
            }
        }
    }
}
</style>
