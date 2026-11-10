<template>
  <footer class="jd-footer">
    <div class="jd-wrap">
      <div class="promise">
        <div class="promise-item">
          <span class="dot"></span>
          <span>正品保障</span>
        </div>
        <div class="promise-item">
          <span class="dot"></span>
          <span>极速配送</span>
        </div>
        <div class="promise-item">
          <span class="dot"></span>
          <span>无忧售后</span>
        </div>
        <div class="promise-item">
          <span class="dot"></span>
          <span>校园直送</span>
        </div>
      </div>

      <div class="links">
        <div class="col">
          <div class="col-title">购物指南</div>
          <span class="lnk" @click="go('/home')">购物流程</span>
          <span class="lnk">会员介绍</span>
          <span class="lnk">常见问题</span>
        </div>
        <div class="col">
          <div class="col-title">配送方式</div>
          <span class="lnk">上门自提</span>
          <span class="lnk">配送服务查询</span>
          <span class="lnk">配送费收取标准</span>
        </div>
        <div class="col">
          <div class="col-title">支付方式</div>
          <span class="lnk">在线支付</span>
          <span class="lnk">货到付款</span>
          <span class="lnk">分期付款</span>
        </div>
        <div class="col">
          <div class="col-title">售后服务</div>
          <span class="lnk" @click="go('/orders')">我的订单</span>
          <span class="lnk">退换货政策</span>
          <span class="lnk">退款说明</span>
        </div>
      </div>

      <div class="copyright">
        校园商城 · CampusMart &nbsp;|&nbsp; 本项目为学习用途，非真实交易
      </div>
    </div>
  </footer>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const go = (path) => router.push(path)
</script>

<style scoped>
.jd-footer {
  margin-top: 40px;
  background: #fff;
  border-top: 1px solid #e3e4e5;
  padding: 26px 0 20px;
}
.promise {
  display: flex;
  justify-content: space-around;
  padding: 10px 0 24px;
  border-bottom: 1px solid #eee;
}
.promise-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #333;
}
.dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #e1251b;
  opacity: 0.85;
  display: inline-block;
}
.links {
  display: flex;
  justify-content: space-around;
  padding: 24px 0;
  border-bottom: 1px solid #eee;
}
.col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: center;
}
.col-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
}
.lnk {
  font-size: 12px;
  color: #999;
  cursor: pointer;
}
.lnk:hover {
  color: #e1251b;
}
.copyright {
  text-align: center;
  font-size: 12px;
  color: #999;
  padding-top: 18px;
}
</style>
