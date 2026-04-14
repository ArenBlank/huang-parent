import { appClient } from './client'

export const fetchPlanOverview = () =>
  appClient.get('/app/plan/overview')

export const subscribePlan = (data) =>
  appClient.post('/app/plan/subscribe', data)

export const unsubscribePlan = (data) =>
  appClient.post('/app/plan/unsubscribe', data)

export const fetchPlanDetail = (planId) =>
  appClient.get(`/app/plan/${planId}`)

export const generateAiPlan = (data) =>
  appClient.post('/app/plan/ai-generate', data, {
    timeout: 90000
  })
