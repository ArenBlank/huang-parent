import { get, post } from '../../utils/request'

const appendQuery = (url, params = {}) => {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&')
  return query ? `${url}?${query}` : url
}

export const listCourses = (params = {}) => get('/app/course/list', params)

export const listCourseSchedules = (courseId) => get(`/app/course/${courseId}/schedule/list`)

export const enrollCourse = (payload) => post('/app/course/enroll', payload)

export const mockPayCourse = (enrollmentId) => post(appendQuery('/app/course/pay-success', { enrollmentId }))

export const cancelUnpaidCourse = (enrollmentId) => post(appendQuery('/app/course/cancel-unpaid', { enrollmentId }))

export const refundCourse = (payload) => post('/app/course/refund', payload)

export const listMyEnrollments = () => get('/app/course/my/enrollments')

export const listMyCourseSchedules = () => get('/app/course/my/schedules')

export const checkInCourseByCode = (payload) => post('/app/course/check-in', payload)
