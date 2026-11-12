import request from './request'

// 地址列表（默认地址排第一）
export const getAddressList = () => request.get('/address/list')

// 新增地址  data: { receiverName, receiverPhone, province, city, district, detail, isDefault }
export const addAddress = (data) => request.post('/address', data)

// 修改地址
export const updateAddress = (id, data) => request.put(`/address/${id}`, data)

// 删除地址
export const deleteAddress = (id) => request.delete(`/address/${id}`)

// 设为默认
export const setDefaultAddress = (id) => request.put(`/address/${id}/default`)
