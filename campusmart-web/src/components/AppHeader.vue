<template>
  <header class="jd-header">
    <!-- 一、顶部工具条 -->
    <div class="topbar">
      <div class="jd-wrap topbar-inner">
        <div class="tb-left">
          <span class="city">📍 校园站</span>
          <span class="tip">校园商城，欢迎您！</span>
        </div>
        <div class="tb-right">
          <span class="tb-link" @click="go('/orders')">我的订单</span>
          <i class="sep">|</i>
          <span class="tb-link" @click="go('/cart')">我的购物车</span>
          <i class="sep">|</i>
          <span class="tb-link" @click="logout">退出登录</span>
        </div>
      </div>
    </div>

    <!-- 二、搜索行 -->
    <div class="search-row">
      <div class="jd-wrap search-inner">
        <div class="logo" @click="go('/home')">校园商城</div>

        <div class="search-box">
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索商品，如：薯片"
            @keyup.enter="doSearch"
          />
          <button class="search-btn" @click="doSearch">搜索</button>
        </div>

        <div class="cart-entry" @click="go('/cart')">
          <span class="cart-text">我的购物车</span>
        </div>
      </div>
    </div>

    <!-- 三、导航条 -->
    <div class="navbar">
      <div class="jd-wrap navbar-inner">
        <div class="all-cats">全部商品分类</div>
        <nav class="nav-links">
          <span class="nav-link" @click="go('/home')">首页</span>
          <span class="nav-link" @click="go('/orders')">我的订单</span>
          <span class="nav-link" @click="go('/cart')">购物车</span>
        </nav>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const keyword = ref('')

const go = (path) => router.push(path)

const doSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  router.push({ path: '/home', query: { keyword: kw } })
}

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  router.push('/login')
}
</script>

<style scoped>
.jd-header {
  background: #fff;
}

/* ===== 顶部工具条 ===== */
.topbar {
  height: 30px;
  background: #e3e4e5;
  border-bottom: 1px solid #ddd;
  font-size: 12px;
  color: #999;
}
.topbar-inner {
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.tb-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.city {
  color: #999;
}
.tip {
  color: #999;
}
.tb-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tb-link {
  cursor: pointer;
  color: #999;
}
.tb-link:hover {
  color: #e1251b;
}
.sep {
  color: #ccc;
  font-style: normal;
}

/* ===== 搜索行 ===== */
.search-row {
  height: 100px;
  background: #fff;
}
.search-inner {
  height: 100px;
  display: flex;
  align-items: center;
  gap: 30px;
}
.logo {
  font-size: 30px;
  font-weight: 700;
  color: #e1251b;
  letter-spacing: 1px;
  cursor: pointer;
  width: 200px;
  flex: none;
  line-height: 1;
  padding-bottom: 8px;
}
.search-box {
  display: flex;
  width: 550px;
  height: 36px;
  border: 2px solid #e1251b;
  flex: none;
}
.search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 0 12px;
  font-size: 14px;
  color: #333;
}
.search-input::placeholder {
  color: #bbb;
}
.search-btn {
  width: 80px;
  border: none;
  background: #e1251b;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.search-btn:hover {
  background: #c81623;
}
.cart-entry {
  width: 140px;
  height: 36px;
  border: 1px solid #e3e4e5;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #e1251b;
  font-size: 13px;
  background: #fff;
}
.cart-entry:hover {
  border-color: #e1251b;
}

/* ===== 导航条 ===== */
.navbar {
  height: 40px;
  border-bottom: 2px solid #e1251b;
  background: #fff;
}
.navbar-inner {
  height: 40px;
  display: flex;
  align-items: stretch;
}
.all-cats {
  width: 190px;
  background: #e1251b;
  color: #fff;
  font-size: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  cursor: default;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 32px;
  padding-left: 28px;
}
.nav-link {
  font-size: 15px;
  color: #333;
  cursor: pointer;
  height: 40px;
  line-height: 40px;
}
.nav-link:hover {
  color: #e1251b;
}
</style>
