<template>
  <div class="page">
    <div class="container">
      <h2 class="page-title">确认订单</h2>

      <!-- ---------- 收货信息 ---------- -->
      <div class="block">
        <div class="block-title">
          <span>收货信息</span>
          <el-button v-if="selectedAddress" link type="primary" @click="openPicker">
            更换地址
          </el-button>
        </div>

        <div v-if="selectedAddress" class="addr-selected">
          <div class="addr-line1">
            <span class="name">{{ selectedAddress.receiverName }}</span>
            <span class="phone">{{ selectedAddress.receiverPhone }}</span>
            <el-tag v-if="selectedAddress.isDefault" size="small" type="danger" effect="plain">
              默认
            </el-tag>
          </div>
          <div class="addr-line2">{{ fullAddress }}</div>
        </div>

        <div v-else class="addr-empty" @click="openPicker">
          <span class="plus">+</span>
          <span>还没有收货地址，点击添加</span>
        </div>
      </div>

      <!-- ---------- 商品清单 ---------- -->
      <div class="block">
        <div class="block-title">商品清单</div>
        <div v-if="loading" class="hint">加载中…</div>
        <div v-else class="goods">
          <div v-for="(g, i) in previewItems" :key="i" class="goods-row">
            <el-image :src="g.image" fit="cover" class="thumb">
              <template #error>
                <div class="img-ph">无图</div>
              </template>
            </el-image>
            <div class="g-meta">
              <div class="g-name">{{ g.name }}</div>
              <div class="g-spec">{{ g.spec }}</div>
            </div>
            <div class="g-price">¥{{ g.price }}</div>
            <div class="g-qty">× {{ g.quantity }}</div>
            <div class="g-sub">¥{{ (Number(g.price) * g.quantity).toFixed(2) }}</div>
          </div>
        </div>
      </div>

      <div class="footer">
        <div class="summary">
          <span class="label">应付金额</span>
          <span class="amount">¥{{ totalAmount }}</span>
        </div>
        <el-button type="primary" size="large" :loading="submitting" @click="submit">
          提交订单
        </el-button>
      </div>
    </div>

    <AddressPicker
      ref="pickerRef"
      v-model:visible="pickerVisible"
      :selected-id="selectedAddress ? selectedAddress.id : null"
      @select="onSelectAddress"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCartList } from '@/api/cart'
import { getProductDetail } from '@/api/product'
import { submitOrder, submitFromCart } from '@/api/order'
import { getAddressList } from '@/api/address'
import AddressPicker from '@/components/AddressPicker.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const previewItems = ref([])
const totalAmount = ref('0.00')

const pickerRef = ref()
const pickerVisible = ref(false)
const selectedAddress = ref(null)

const isBuyNow = computed(() => !!route.query.skuId)

const fullAddress = computed(() => {
  const a = selectedAddress.value
  if (!a) return ''
  return `${a.province}${a.city}${a.district} ${a.detail}`
})

const openPicker = () => {
  pickerVisible.value = true
  pickerRef.value?.open()
}

const onSelectAddress = (a) => {
  selectedAddress.value = a
}

const loadAddress = async () => {
  const list = await getAddressList()
  if (list && list.length > 0) {
    // 优先选默认地址，没有默认就选第一条
    selectedAddress.value = list.find((a) => a.isDefault) || list[0]
  }
}

const loadPreview = async () => {
  loading.value = true
  try {
    if (isBuyNow.value) {
      const detail = await getProductDetail(route.query.spuId)
      const sku = detail.skus.find((s) => s.id === Number(route.query.skuId))
      const qty = Number(route.query.quantity)
      if (!sku) {
        ElMessage.error('规格信息已失效')
        router.replace('/home')
        return
      }
      previewItems.value = [
        {
          name: detail.name,
          spec: sku.skuName,
          price: sku.price,
          quantity: qty,
          image: detail.mainImage
        }
      ]
      totalAmount.value = (Number(sku.price) * qty).toFixed(2)
    } else {
      const data = await getCartList()
      const picked = data.items.filter((i) => i.selected && !i.invalid)
      if (picked.length === 0) {
        ElMessage.warning('没有可结算的商品')
        router.replace('/cart')
        return
      }
      previewItems.value = picked.map((i) => ({
        name: i.spuName,
        spec: i.skuName,
        price: i.price,
        quantity: i.quantity,
        image: i.mainImage
      }))
      totalAmount.value = data.selectedAmount
    }
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!selectedAddress.value) {
    ElMessage.warning('请先选择收货地址')
    openPicker()
    return
  }

  submitting.value = true
  try {
    const payload = { addressId: selectedAddress.value.id }

    const res = isBuyNow.value
      ? await submitOrder({
          skuId: Number(route.query.skuId),
          quantity: Number(route.query.quantity),
          ...payload
        })
      : await submitFromCart(payload)

    ElMessage.success('下单成功，请在 30 分钟内完成支付')
    router.replace(`/orders/${res.orderId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadAddress(), loadPreview()])
})
</script>

<style scoped>
.page {
  padding: 24px 0 48px;
}
.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 16px;
}
.block {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 16px;
}
.block-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.addr-selected {
  padding: 14px 16px;
  border: 1px solid #f5b7b3;
  background: #fffafa;
  border-radius: 8px;
}
.addr-line1 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}
.phone {
  font-size: 13px;
  color: #606266;
}
.addr-line2 {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  word-break: break-all;
}
.addr-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 72px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  color: #909399;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.addr-empty:hover {
  border-color: #e1251b;
  color: #e1251b;
}
.plus {
  font-size: 18px;
  line-height: 1;
}
.hint {
  padding: 30px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.goods-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}
.goods-row:last-child {
  border-bottom: none;
}
.thumb {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex: none;
}
.img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 11px;
}
.g-meta {
  flex: 1;
  min-width: 0;
}
.g-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.g-spec {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}
.g-price,
.g-qty {
  font-size: 13px;
  color: #909399;
  width: 80px;
  text-align: right;
}
.g-sub {
  font-size: 14px;
  font-weight: 600;
  color: #e1251b;
  width: 90px;
  text-align: right;
}
.footer {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 20px;
}
.summary {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.label {
  font-size: 14px;
  color: #606266;
}
.amount {
  font-size: 24px;
  font-weight: 600;
  color: #e1251b;
}
</style>
