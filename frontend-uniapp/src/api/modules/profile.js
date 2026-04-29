import { get, put } from '../../utils/request'

export const getProfile = () => get('/app/profile/info')

export const updateProfile = (payload) => put('/app/profile/info', payload)

export const updatePassword = (payload) => put('/app/profile/password', payload)
