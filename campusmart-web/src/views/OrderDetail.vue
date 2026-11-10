<template>
  <div class="page">
    <div v-if="loading" class="hint">加载中…</div>

    <div v-else-if="order" class="container">
      <div class="status-bar" :class="`s-${order.status}`">
        <div class="status-main">
          <span class="status-text">{{ order.statusDesc }}</span>
          <span v-if="order.status === 10" class="status-tip">
            请在 {{ fmt(order.expireTime) }} 前完成支付，超时订单将自动关闭
          </span>
          <span v-else-if="order.status === 20" class="status-tip">
            支付时间 {{ fmt(order.payTime) }}
          </span>
          <span v-else-if="order.closeTime" class="status-tip">
            关闭时间 {{ fmt(order.closeTime) }}
          </span>
        </div>

        <div v-if="order.status === 10" class="status-actions">
          <el-button size="small" @click="handleCancel">取消订单</el-button>
          <el-button type="primary" size="small" @click="handlePay">去支付</el-button>
        </div>
      </div>

      <div class="block">
        <div class="block-title">订单信息</div>
        <div class="kv-row">
          <span class="k">订单号</span>
          <span class="v">{{ order.orderNo }}</span>
        </div>
        <div class="kv-row">
          <span class="k">下单时间</span>
          <span class="v">{{ fmt(order.createdAt) }}</span>
        </div>
        <div class="kv-row">
          <span class="k">收货人</span>
          <span class="v">{{ order.receiverName }} {{ order.receiverPhone }}</span>
        </div>
        <div class="kv-row">
          <span class="k">收货地址</span>
          <span class="v">{{ order.receiverAddress }}</span>
        </div>
      </div>

      <div class="block">
        <div class="block-title">商品清单</div>
        <div v-for="item in order.items" :key="item.skuId" class="goods-row">
          <el-image :src="item.image" fit="cover" class="thumb">
            <template #error>
              <div class="img-ph">无图</div>
            </template>
          </el-image>
          <div class="g-meta">
            <div class="g-name">{{ item.spuName }}</div>
            <div class="g-spec">{{ item.skuName }}</div>
          </div>
          <div class="g-price">¥{{ item.price }}</div>
          <div class="g-qty">× {{ item.quantity }}</div>
          <div class="g-sub">¥{{ item.totalAmount }}</div>
        </div>

        <div class="total-row">
          <span class="total-label">订单总额</span>
          <span class="total-num">¥{{ order.totalAmount }}</span>
        </div>
        <div class="total-row">
          <span class="total-label">实付金额</span>
          <span class="pay-num">¥{{ order.payAmount }}</span>
        </div>
      </div>

      <div class="bottom">
        <el-button @click="router.push('/orders')">返回订单列表</el-button>
      </div>
    </div>

    <el-empty v-else description="订单不存在" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderDetail, payOrder, cancelOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()

const order = ref(null)
const loading = ref(false)

const fmt = (t) => (t ? t.replace('T', ' ').slice(0, 19) : '-')

const load = async () => {
  loading.value = true
  try {
    order.value = await getOrderDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${order.value.payAmount} 吗？`, '模拟支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await payOrder(order.value.orderId)
  ElMessage.success('支付成功')
  await load()
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('取消后库存将返还，确定取消吗？', '取消订单', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想'
    })
  } catch (e) {
    return
  }
  await cancelOrder(order.value.orderId)
  ElMessage.success('订单已取消')
  await load()
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
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.status-bar {
  background: #fff;
  border-radius: 10px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-left: 4px solid #909399;
}
.status-bar.s-10 {
  border-left-color: #e6a23c;
}
.status-bar.s-20 {
  border-left-color: #67c23a;
}
.status-bar.s-50,
.status-bar.s-60 {
  border-left-color: #909399;
}
.status-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.status-text {
  font-size: 17px;
  font-weight: 500;
  color: #303133;
}
.status-tip {
  font-size: 12px;
  color: #909399;
}
.status-actions {
  display: flex;
  gap: 10px;
}
.block {
  background: #fff;
  border-radius: 10px;
  padding: 20px 22px;
}
.block-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 14px;
}
.kv-row {
  display: flex;
  font-size: 13px;
  padding: 5px 0;
}
.k {
  width: 80px;
  color: #909399;
  flex: none;
}
.v {
  color: #303133;
  word-break: break-all;
}
.goods-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
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
  color: #e1251b;
  width: 90px;
  text-align: right;
}
.total-row {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  gap: 10px;
  margin-top: 12px;
}
.total-label {
  font-size: 13px;
  color: #909399;
}
.total-num {
  font-size: 15px;
  color: #606266;
}
.pay-num {
  font-size: 22px;
  font-weight: 600;
  color: #e1251b;
}
.bottom {
  display: flex;
  justify-content: center;
}
</style>
