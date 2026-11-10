import request from './request'

// 立即购买  data: { skuId, quantity, receiverName, receiverPhone, receiverAddress }
export const submitOrder = (data) => request.post('/order', data)

// 从购物车下单  data: { receiverName, receiverPhone, receiverAddress }
export const submitFromCart = (data) => request.post('/order/from-cart', data)

// 模拟支付
export const payOrder = (orderId) => request.post(`/order/${orderId}/pay`)

// 取消订单
export const cancelOrder = (orderId) => request.post(`/order/${orderId}/cancel`)

// 订单列表  params: { status, page, size }
export const getOrderList = (params) => request.get('/order/list', { params })

// 订单详情
export const getOrderDetail = (orderId) => request.get(`/order/${orderId}`)
