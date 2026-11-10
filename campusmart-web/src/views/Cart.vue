<template>
  <div class="page">
    <div class="container">
      <h2 class="page-title">我的购物车</h2>

      <div v-if="loading" class="hint">加载中…</div>

      <el-empty v-else-if="items.length === 0" description="购物车还是空的">
        <el-button type="primary" @click="router.push('/home')">去逛逛</el-button>
      </el-empty>

      <template v-else>
        <div class="list">
          <div
            v-for="item in items"
            :key="item.id"
            class="row"
            :class="{ invalid: item.invalid }"
          >
            <el-image :src="item.mainImage" fit="cover" class="thumb">
              <template #error>
                <div class="img-ph">无图</div>
              </template>
            </el-image>

            <div class="meta">
              <div class="name" @click="goProduct(item.spuId)">{{ item.spuName }}</div>
              <div class="spec">{{ item.skuName }}</div>
              <div class="tags">
                <el-tag v-if="item.invalid" type="info" size="small">已失效</el-tag>
                <span v-else class="stock">库存 {{ item.stock }} 件</span>
              </div>
            </div>

            <div class="price">¥{{ item.price }}</div>

            <div class="qty">
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="Math.min(item.stock, 99)"
                :disabled="item.invalid"
                size="small"
                @change="(val) => changeQty(item, val)"
              />
            </div>

            <div class="subtotal">¥{{ subtotal(item) }}</div>

            <el-button text type="primary" size="small" @click="handleDelete(item)">
              删除
            </el-button>
          </div>
        </div>

        <div class="footer">
          <div class="summary">
            <span class="label">共 {{ totalQuantity }} 件，合计</span>
            <span class="amount">¥{{ selectedAmount }}</span>
          </div>
          <el-button type="primary" size="large" @click="goCheckout">去结算</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList, updateCartQuantity, deleteCart } from '@/api/cart'

const router = useRouter()

const items = ref([])
const totalQuantity = ref(0)
const selectedAmount = ref(0)
const loading = ref(false)

const subtotal = (item) => (Number(item.price) * item.quantity).toFixed(2)

const load = async () => {
  loading.value = true
  try {
    const data = await getCartList()
    items.value = data.items
    totalQuantity.value = data.totalQuantity
    selectedAmount.value = data.selectedAmount
  } finally {
    loading.value = false
  }
}

const changeQty = async (item, val) => {
  try {
    await updateCartQuantity(item.id, val)
    await load()
  } catch (e) {
    await load()
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm(`确定删除「${item.spuName}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await deleteCart(item.id)
  ElMessage.success('已删除')
  await load()
}

const goProduct = (spuId) => router.push(`/product/${spuId}`)

const goCheckout = () => {
  if (Number(selectedAmount.value) <= 0) {
    ElMessage.warning('没有可结算的商品')
    return
  }
  router.push('/checkout')
}

onMounted(load)
</script>

<style scoped>
.page {
  padding: 24px 0 48px;
}
.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 16px;
}
.hint {
  padding: 60px 0;
  text-align: center;
  color: #909399;
}
.list {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}
.row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #f2f3f5;
}
.row:last-child {
  border-bottom: none;
}
.row.invalid {
  opacity: 0.6;
}
.thumb {
  width: 72px;
  height: 72px;
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
  font-size: 12px;
}
.meta {
  flex: 1;
  min-width: 0;
}
.name {
  font-size: 14px;
  color: #303133;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.name:hover {
  color: #e1251b;
}
.spec {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.tags {
  margin-top: 6px;
}
.stock {
  font-size: 12px;
  color: #c0c4cc;
}
.price {
  width: 80px;
  text-align: right;
  font-size: 14px;
  color: #606266;
}
.qty {
  width: 120px;
  display: flex;
  justify-content: center;
}
.subtotal {
  width: 90px;
  text-align: right;
  font-size: 15px;
  font-weight: 600;
  color: #e1251b;
}
.footer {
  margin-top: 16px;
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
  font-size: 22px;
  font-weight: 600;
  color: #e1251b;
}
</style>
