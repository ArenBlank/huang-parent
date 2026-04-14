import { appClient } from './client'

export const fetchMyCourseSchedules = () =>
  appClient.get('/app/course/my/schedules')

export const checkInCourseByCode = (data) =>
  appClient.post('/app/course/check-in', data)
