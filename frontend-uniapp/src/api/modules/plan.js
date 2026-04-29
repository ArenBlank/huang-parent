import { get, post } from '../../utils/request'

export const fetchPlanOverview = () => get('/app/plan/overview')

export const listPlans = () => get('/app/plan/list')

export const getPlanDetail = (planId) => get(`/app/plan/${planId}`)

export const subscribePlan = (payload) => post('/app/plan/subscribe', payload)

export const unsubscribePlan = (payload) => post('/app/plan/unsubscribe', payload)

export const generateAiPlan = (payload) => post('/app/plan/ai-generate', payload, { timeout: 90000 })
