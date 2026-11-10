<template>
  <div class="home">
    <div class="jd-wrap">
      <!-- 首屏：分类菜单 + banner -->
      <div class="hero">
        <aside class="cat-menu">
          <div
            class="cat-item all"
            :class="{ active: categoryId === null }"
            @click="selectCategory(null)"
          >
            全部商品
          </div>

          <template v-for="top in categories" :key="top.id">
            <div
              class="cat-item parent"
              :class="{ active: categoryId === top.id }"
              @click="selectCategory(top.id)"
            >
              <span>{{ top.name }}</span>
              <span class="arrow">›</span>
            </div>
            <div
              v-for="child in top.children"
              :key="child.id"
              class="cat-item child"
              :class="{ active: categoryId === child.id }"
              @click="selectCategory(child.id)"
            >
              {{ child.name }}
            </div>
          </template>
        </aside>

        <div class="banner">
          <div class="banner-inner">
            <div class="banner-tag">开学季</div>
            <h1 class="banner-title">校园好物 一站购齐</h1>
            <p class="banner-sub">零食饮料 · 文具办公 · 数码配件</p>
            <div class="banner-line"></div>
            <p class="banner-note">满 50 元免配送费 · 30 分钟内下单当日送达</p>
          </div>
        </div>
      </div>

      <!-- 商品楼层 -->
      <section class="floor">
        <div class="floor-head">
          <div class="floor-title">
            <span class="bar"></span>
            <span>{{ currentCatName }}</span>
            <span v-if="keyword" class="clear-kw" @click="clearKeyword">清除搜索</span>
          </div>
          <div class="floor-tools">
            <span class="count">共 {{ total }} 件</span>
            <el-radio-group v-model="sort" size="small" @change="load(1)">
              <el-radio-button value="">综合</el-radio-button>
              <el-radio-button value="sales">销量</el-radio-button>
              <el-radio-button value="price_asc">价格↑</el-radio-button>
              <el-radio-button value="price_desc">价格↓</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div v-if="loading" class="hint">加载中…</div>

        <el-empty v-else-if="list.length === 0" description="该分类下暂无商品" />

        <div v-else class="grid">
          <div v-for="p in list" :key="p.id" class="goods" @click="goDetail(p.id)">
            <div class="goods-img-wrap">
              <el-image :src="p.mainImage" fit="cover" class="goods-img">
                <template #error>
                  <div class="img-ph">暂无图片</div>
                </template>
              </el-image>
            </div>
            <div class="goods-info">
              <div class="goods-price">
                <span class="sym">¥</span>
                <span class="num">{{ p.priceLow }}</span>
              </div>
              <div class="goods-name">{{ p.name }}</div>
              <div class="goods-sub">{{ p.subTitle }}</div>
              <div class="goods-foot">
                <span class="sales">已售 {{ p.sales || 0 }}</span>
              </div>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > size"
          class="pager"
          layout="prev, pager, next, jumper"
          :total="total"
          :page-size="size"
          :current-page="page"
          @current-change="load"
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategoryTree, getProductList } from '@/api/product'

const router = useRouter()
const route = useRoute()

const categories = ref([])
const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = 20
const categoryId = ref(null)
const sort = ref('')
const keyword = ref(route.query.keyword || '')

const currentCatName = computed(() => {
  if (keyword.value) return `搜索：${keyword.value}`
  if (categoryId.value === null) return '猜你喜欢'
  for (const top of categories.value) {
    if (top.id === categoryId.value) return top.name
    const child = (top.children || []).find((c) => c.id === categoryId.value)
    if (child) return child.name
  }
  return '猜你喜欢'
})

const load = async (p = 1) => {
  page.value = p
  loading.value = true
  try {
    const params = {
      page: p,
      size,
      categoryId: categoryId.value,
      sort: sort.value
    }
    if (keyword.value) params.keyword = keyword.value

    const data = await getProductList(params)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const selectCategory = (id) => {
  categoryId.value = id
  if (keyword.value) {
    keyword.value = ''
    router.replace({ path: '/home' })
  }
  load(1)
}

const clearKeyword = () => selectCategory(null)

const goDetail = (id) => router.push(`/product/${id}`)

// 顶栏搜索只改变 query，组件不会重建，所以必须监听
watch(
  () => route.query.keyword,
  (kw) => {
    const next = kw || ''
    if (next === keyword.value) return
    keyword.value = next
    if (next) categoryId.value = null
    load(1)
  }
)

onMounted(async () => {
  try {
    categories.value = await getCategoryTree()
  } catch (e) {
    categories.value = []
  }
  load(1)
})
</script>

<style scoped>
.home {
  padding-bottom: 30px;
}

/* ===== 首屏 ===== */
.hero {
  display: flex;
  gap: 10px;
  padding-top: 10px;
  height: 470px;
}
.cat-menu {
  width: 190px;
  flex: none;
  background: #fff;
  overflow-y: auto;
  padding: 8px 0;
}
.cat-item {
  padding: 8px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 3px solid transparent;
}
.cat-item:hover {
  background: #f4f4f4;
  color: #e1251b;
}
.cat-item.active {
  color: #e1251b;
  background: #fdeceb;
  border-left-color: #e1251b;
  font-weight: 500;
}
.cat-item.all {
  font-weight: 500;
}
.cat-item.parent {
  font-weight: 500;
}
.cat-item.child {
  padding-left: 30px;
  font-size: 13px;
  color: #666;
}
.arrow {
  color: #c0c4cc;
  font-size: 14px;
}

.banner {
  flex: 1;
  background: linear-gradient(115deg, #e1251b 0%, #f4511e 55%, #ff7a45 100%);
  display: flex;
  align-items: center;
  padding: 0 60px;
  overflow: hidden;
  position: relative;
}
.banner::after {
  content: "";
  position: absolute;
  right: -80px;
  top: -80px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.banner-inner {
  color: #fff;
  position: relative;
  z-index: 1;
}
.banner-tag {
  display: inline-block;
  padding: 3px 12px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  font-size: 12px;
  margin-bottom: 18px;
}
.banner-title {
  font-size: 40px;
  font-weight: 700;
  letter-spacing: 2px;
  line-height: 1.2;
}
.banner-sub {
  margin-top: 14px;
  font-size: 17px;
  opacity: 0.95;
  letter-spacing: 1px;
}
.banner-line {
  width: 60px;
  height: 3px;
  background: #fff;
  margin: 22px 0 16px;
  opacity: 0.8;
}
.banner-note {
  font-size: 13px;
  opacity: 0.85;
}

/* ===== 商品楼层 ===== */
.floor {
  margin-top: 20px;
  background: #fff;
  padding: 18px 16px 24px;
}
.floor-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid #eee;
  margin-bottom: 16px;
}
.floor-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.bar {
  width: 4px;
  height: 20px;
  background: #e1251b;
  display: inline-block;
  border-radius: 2px;
}
.clear-kw {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  cursor: pointer;
  margin-left: 6px;
}
.clear-kw:hover {
  color: #e1251b;
}
.floor-tools {
  display: flex;
  align-items: center;
  gap: 16px;
}
.count {
  font-size: 13px;
  color: #999;
}
.hint {
  padding: 70px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}
.goods {
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  padding: 6px;
  border: 1px solid transparent;
}
.goods:hover {
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
  border-color: #f5b3af;
}
.goods-img-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
}
.goods-img {
  width: 100%;
  height: 100%;
}
.img-ph {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f7f7;
  color: #c0c4cc;
  font-size: 12px;
}
.goods-info {
  padding: 10px 6px 6px;
}
.goods-price {
  color: #e1251b;
  font-weight: 600;
  line-height: 1;
}
.goods-price .sym {
  font-size: 13px;
  margin-right: 1px;
}
.goods-price .num {
  font-size: 22px;
}
.goods-name {
  margin-top: 8px;
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.goods-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  height: 17px;
}
.goods-foot {
  margin-top: 8px;
}
.sales {
  font-size: 12px;
  color: #c0c4cc;
}
.pager {
  margin-top: 24px;
  justify-content: center;
}

@media (max-width: 1200px) {
  .grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
@media (max-width: 960px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .search-box {
    width: 100%;
  }
}
</style>
