import { get, post } from '../../utils/request'

export const applyCoach = (payload) => post('/app/coach/apply', payload)

export const getMyCoachApplication = () => get('/app/coach/my-application')
