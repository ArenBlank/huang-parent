import { get } from '../../utils/request'

export const listOrders = (params = {}) => get('/app/order/my/list', params)

export const getOrderDetail = (params) => get('/app/order/detail', params)
