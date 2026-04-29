import { get, post } from '../../utils/request'

export const checkIn = (payload) => post('/app/record/checkin', payload)

export const listMyRecords = (params = {}) => get('/app/record/my/list', params)

export const getWeeklyStat = () => get('/app/record/my/weekly-stat')
