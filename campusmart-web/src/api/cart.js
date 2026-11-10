import request from './request'

// 加入购物车  data: { skuId, quantity }
export const addCart = (data) => request.post('/cart', data)

// 购物车列表
export const getCartList = () => request.get('/cart')

// 修改数量
export const updateCartQuantity = (id, quantity) =>
  request.put(`/cart/${id}`, { quantity })

// 删除
export const deleteCart = (id) => request.delete(`/cart/${id}`)
