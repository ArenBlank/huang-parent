import { get, post } from '../../utils/request'

export const login = (payload) => post('/app/auth/login', payload, { skipAuth: true })

export const register = (payload) => post('/app/auth/register', payload, { skipAuth: true })

export const refreshToken = (refreshTokenValue) =>
  post('/app/auth/refresh-token', { refreshToken: refreshTokenValue }, { skipAuth: true })

export const logout = () => post('/app/auth/logout')

export const getProfile = () => get('/app/profile/info')

export const getCaptcha = (payload) => post('/app/auth/captcha/get', payload, { skipAuth: true })

export const checkCaptcha = (payload) => post('/app/auth/captcha/check', payload, { skipAuth: true })
