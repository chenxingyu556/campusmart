<template>
  <div class="page">
    <div v-if="loading" class="hint">加载中…</div>

    <div v-else-if="product" class="container">
      <div class="left">
        <el-image :src="product.mainImage" fit="cover" class="main-img">
          <template #error>
            <div class="img-ph">暂无图片</div>
          </template>
        </el-image>
      </div>

      <div class="right">
        <h1 class="name">{{ product.name }}</h1>
        <p class="sub">{{ product.subTitle }}</p>

        <div class="price-box">
          <span class="price-label">校园价</span>
          <span class="price">¥{{ currentSku ? currentSku.price : product.priceLow }}</span>
          <span class="sales">已售 {{ product.sales || 0 }}</span>
        </div>

        <div class="row">
          <span class="label">规格</span>
          <div class="skus">
            <div
              v-for="sku in product.skus"
              :key="sku.id"
              class="sku"
              :class="{ active: skuId === sku.id, disabled: sku.stock === 0 }"
              @click="pickSku(sku)"
            >
              {{ sku.skuName }}
              <span v-if="sku.stock === 0" class="out">缺货</span>
            </div>
          </div>
        </div>

        <div class="row">
          <span class="label">数量</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQty" size="small" />
          <span v-if="currentSku" class="stock-hint">库存 {{ currentSku.stock }} 件</span>
        </div>

        <div class="actions">
          <el-button
            color="#ff6700"
            size="large"
            :disabled="!canBuy"
            @click="handleAddCart"
          >
            加入购物车
          </el-button>
          <el-button
            type="primary"
            size="large"
            :disabled="!canBuy"
            @click="handleBuyNow"
          >
            立即购买
          </el-button>
        </div>

        <div v-if="product.detail" class="detail">
          <div class="detail-title">商品详情</div>
          <p class="detail-text">{{ product.detail }}</p>
        </div>
      </div>
    </div>

    <el-empty v-else description="商品不存在" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail } from '@/api/product'
import { addCart } from '@/api/cart'

const route = useRoute()
const router = useRouter()

const product = ref(null)
const loading = ref(false)
const skuId = ref(null)
const quantity = ref(1)

const currentSku = computed(
  () => product.value?.skus.find((s) => s.id === skuId.value) || null
)
const maxQty = computed(() =>
  currentSku.value ? Math.max(1, Math.min(currentSku.value.stock, 99)) : 99
)
const canBuy = computed(() => !!currentSku.value && currentSku.value.stock > 0)

const pickSku = (sku) => {
  if (sku.stock === 0) return
  skuId.value = sku.id
  quantity.value = 1
}

const load = async () => {
  loading.value = true
  try {
    product.value = await getProductDetail(route.params.id)
    const first = product.value.skus.find((s) => s.stock > 0)
    if (first) skuId.value = first.id
  } finally {
    loading.value = false
  }
}

const handleAddCart = async () => {
  await addCart({ skuId: skuId.value, quantity: quantity.value })
  ElMessage.success('已加入购物车')
}

const handleBuyNow = () => {
  router.push({
    path: '/checkout',
    query: {
      spuId: product.value.id,
      skuId: skuId.value,
      quantity: quantity.value
    }
  })
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 24px 0 48px;
}
.hint {
  padding: 80px 0;
  text-align: center;
  color: #909399;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  gap: 40px;
  background: #fff;
  border-radius: 10px;
  padding-top: 30px;
  padding-bottom: 30px;
}
.left {
  width: 420px;
  flex: none;
}
.main-img {
  width: 100%;
  height: 420px;
  border-radius: 8px;
}
.img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
}
.right {
  flex: 1;
  min-width: 0;
}
.name {
  font-size: 22px;
  color: #303133;
  font-weight: 500;
}
.sub {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}
.price-box {
  margin-top: 18px;
  padding: 16px 20px;
  background: #f7f7f7;
  display: flex;
  align-items: baseline;
  gap: 14px;
}
.price-label {
  font-size: 12px;
  color: #999;
  flex: none;
}
.price {
  color: #e1251b;
  font-size: 28px;
  font-weight: 600;
}
.sales {
  font-size: 13px;
  color: #909399;
}
.row {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.label {
  width: 48px;
  font-size: 14px;
  color: #606266;
  flex: none;
}
.skus {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.sku {
  padding: 6px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
}
.sku:hover {
  border-color: #e1251b;
  color: #e1251b;
}
.sku.active {
  border-color: #e1251b;
  background: #fdeceb;
  color: #e1251b;
}
.sku.disabled {
  color: #c0c4cc;
  background: #f5f7fa;
  cursor: not-allowed;
  border-color: #e4e7ed;
}
.out {
  margin-left: 4px;
  font-size: 11px;
}
.stock-hint {
  font-size: 13px;
  color: #909399;
}
.actions {
  margin-top: 32px;
  display: flex;
  gap: 12px;
}
.detail {
  margin-top: 36px;
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}
.detail-title {
  font-size: 15px;
  color: #303133;
  margin-bottom: 10px;
}
.detail-text {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
}
</style>
