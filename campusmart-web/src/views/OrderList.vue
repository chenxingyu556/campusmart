<template>
  <div class="page">
    <div class="container">
      <h2 class="page-title">我的订单</h2>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待支付" name="10" />
        <el-tab-pane label="已支付" name="20" />
        <el-tab-pane label="已取消" name="50" />
        <el-tab-pane label="已关闭" name="60" />
      </el-tabs>

      <div v-if="loading" class="hint">加载中…</div>

      <el-empty v-else-if="list.length === 0" description="还没有订单">
        <el-button type="primary" @click="router.push('/home')">去逛逛</el-button>
      </el-empty>

      <template v-else>
        <div class="list">
          <div v-for="o in list" :key="o.orderId" class="card">
            <div class="card-head">
              <span class="order-no">订单号 {{ o.orderNo }}</span>
              <span class="time">{{ fmt(o.createdAt) }}</span>
              <el-tag :type="tagType(o.status)" size="small" effect="plain">
                {{ o.statusDesc }}
              </el-tag>
            </div>

            <div class="card-body" @click="goDetail(o.orderId)">
              <el-image :src="o.firstImage" fit="cover" class="thumb">
                <template #error>
                  <div class="img-ph">无图</div>
                </template>
              </el-image>

              <div class="meta">
                <div class="name">{{ o.firstSpuName || '商品' }}</div>
                <div class="count">共 {{ o.totalCount }} 件</div>
              </div>

              <div class="amount">
                <span class="label">实付</span>
                <span class="num">¥{{ o.payAmount }}</span>
              </div>
            </div>

            <div class="card-foot">
              <el-button text size="small" @click="goDetail(o.orderId)">查看详情</el-button>
              <template v-if="o.status === 10">
                <el-button size="small" @click="handleCancel(o)">取消订单</el-button>
                <el-button type="primary" size="small" @click="handlePay(o)">去支付</el-button>
              </template>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > size"
          class="pager"
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="load"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, payOrder, cancelOrder } from '@/api/order'

const router = useRouter()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 5
const loading = ref(false)
const activeTab = ref('all')

const fmt = (t) => (t ? t.replace('T', ' ').slice(0, 19) : '')

const tagType = (status) => {
  if (status === 10) return 'warning'
  if (status === 20) return 'success'
  if (status === 50) return 'info'
  if (status === 60) return 'info'
  return 'primary'
}

const load = async (p = 1) => {
  page.value = p
  loading.value = true
  try {
    const params = { page: p, size }
    if (activeTab.value !== 'all') params.status = Number(activeTab.value)
    const data = await getOrderList(params)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const onTabChange = () => load(1)

const goDetail = (id) => router.push(`/orders/${id}`)

const handlePay = async (o) => {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${o.payAmount} 吗？`, '模拟支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  await payOrder(o.orderId)
  ElMessage.success('支付成功')
  await load(page.value)
}

const handleCancel = async (o) => {
  try {
    await ElMessageBox.confirm('取消后库存将返还，确定取消吗？', '取消订单', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想'
    })
  } catch (e) {
    return
  }
  await cancelOrder(o.orderId)
  ElMessage.success('订单已取消')
  await load(page.value)
}

onMounted(() => load(1))
</script>

<style scoped>
.page {
  padding: 24px 0 48px;
}
.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}
.hint {
  padding: 60px 0;
  text-align: center;
  color: #909399;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f2f3f5;
}
.order-no {
  font-size: 13px;
  color: #606266;
}
.time {
  font-size: 12px;
  color: #c0c4cc;
  flex: 1;
}
.card-body {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  cursor: pointer;
}
.thumb {
  width: 64px;
  height: 64px;
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
}
.count {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}
.amount {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.label {
  font-size: 12px;
  color: #909399;
}
.num {
  font-size: 18px;
  font-weight: 600;
  color: #e1251b;
}
.card-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 20px 14px;
}
.pager {
  margin-top: 20px;
  justify-content: center;
}
</style>
