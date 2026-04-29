import { get } from '../../utils/request'

export const listBanners = () => get('/app/banner/list')

export const listNotices = (params = {}) => get('/app/notice/list', params)

export const getSystemConfigMap = () => get('/app/system-config/map')
