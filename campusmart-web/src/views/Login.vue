<template>
  <div class="login-page">
    <div class="brand-bar">
      <div class="jd-wrap">
        <span class="brand" @click="goHome">校园商城</span>
        <span class="brand-sub">CampusMart</span>
      </div>
    </div>

    <div class="login-body">
      <el-card class="login-card" shadow="never">
        <div class="card-head">
          <span class="head-title">账号登录</span>
          <span class="head-tip">扫码登录暂未开放</span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号"
              size="large"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              @keyup.enter="submit"
            />
          </el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            size="large"
            class="submit-btn"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="card-foot">
          测试账号：13800138005 / 123456
        </div>
      </el-card>
    </div>

    <div class="login-footer">
      校园商城 · CampusMart &nbsp;|&nbsp; 本项目为学习用途，非真实交易
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  phone: '',
  password: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const goHome = () => router.push('/home')

const submit = async () => {
  await formRef.value.validate()

  loading.value = true
  try {
    const data = await login({ phone: form.phone, password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('nickname', data.nickname)
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f4f4;
}

/* 顶部品牌条 */
.brand-bar {
  height: 70px;
  background: #fff;
  border-bottom: 1px solid #e3e4e5;
}
.brand-bar .jd-wrap {
  height: 70px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand {
  font-size: 26px;
  font-weight: 700;
  color: #e1251b;
  letter-spacing: 1px;
  cursor: pointer;
}
.brand-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
  letter-spacing: 1px;
}

/* 登录卡片 */
.login-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.login-card {
  width: 380px;
  border-radius: 4px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
}
.head-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  border-bottom: 2px solid #e1251b;
  padding-bottom: 6px;
}
.head-tip {
  font-size: 12px;
  color: #c0c4cc;
}
.submit-btn {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 4px;
}
.card-foot {
  margin-top: 18px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

/* 底部 */
.login-footer {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #999;
}
</style>
