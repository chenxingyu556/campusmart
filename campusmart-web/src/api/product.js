import request from './request'

// 分类树
export const getCategoryTree = () => request.get('/category/tree')

// 商品列表  params: { page, size, categoryId, sort }
export const getProductList = (params) => request.get('/product/list', { params })

// 商品详情
export const getProductDetail = (spuId) => request.get(`/product/${spuId}`)
