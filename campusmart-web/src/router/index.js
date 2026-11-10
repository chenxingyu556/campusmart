import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, bare: true }
  },
  { path: '/home', component: () => import('@/views/Home.vue'), meta: { public: true } },
  { path: '/product/:id', component: () => import('@/views/ProductDetail.vue'), meta: { public: true } },
  { path: '/cart', component: () => import('@/views/Cart.vue') },
  { path: '/checkout', component: () => import('@/views/Checkout.vue') },
  { path: '/orders', component: () => import('@/views/OrderList.vue') },
  { path: '/orders/:id', component: () => import('@/views/OrderDetail.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：没 token 一律踢回登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/home'
})

export default router
