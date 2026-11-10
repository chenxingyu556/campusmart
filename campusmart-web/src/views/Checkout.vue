<template>
  <div class="page">
    <div class="container">
      <h2 class="page-title">确认订单</h2>

      <div class="block">
        <div class="block-title">收货信息</div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="80px"
          class="addr-form"
        >
          <el-form-item label="收货人" prop="receiverName">
            <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="receiverPhone">
            <el-input v-model="form.receiverPhone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="收货地址" prop="receiverAddress">
            <el-input
              v-model="form.receiverAddress"
              type="textarea"
              :rows="2"
              placeholder="请输入详细地址"
            />
          </el-form-item>
        </el-form>
      </div>

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
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          @click="submit"
        >
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCartList } from '@/api/cart'
import { getProductDetail } from '@/api/product'
import { submitOrder, submitFromCart } from '@/api/order'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(true)
const submitting = ref(false)
const previewItems = ref([])
const totalAmount = ref('0.00')

const isBuyNow = computed(() => !!route.query.skuId)

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
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
  await formRef.value.validate()

  submitting.value = true
  try {
    const addr = {
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress
    }

    const res = isBuyNow.value
      ? await submitOrder({
          skuId: Number(route.query.skuId),
          quantity: Number(route.query.quantity),
          ...addr
        })
      : await submitFromCart(addr)

    ElMessage.success('下单成功，请在 30 分钟内完成支付')
    router.replace(`/orders/${res.orderId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(loadPreview)
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
}
.addr-form {
  max-width: 520px;
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
