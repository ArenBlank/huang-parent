import { get, post } from '../../utils/request'

const appendQuery = (url, params = {}) => {
  const query = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== null && value !== '')
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&')
  return query ? `${url}?${query}` : url
}

export const listBookingSchedules = (params = {}) => get('/app/booking/schedule/list', params)

export const getBookingScheduleSummary = (params = {}) => get('/app/booking/schedule/summary', params)

export const listBookingCoachOptions = () => get('/app/booking/coach-options')

export const createBooking = (payload) => post('/app/booking/create', payload)

export const mockPayBooking = (bookingId) => post(appendQuery('/app/booking/pay-success', { bookingId }))

export const completeBooking = (bookingId) => post(appendQuery('/app/booking/complete', { bookingId }))

export const cancelUnpaidBooking = (bookingId) => post(appendQuery('/app/booking/cancel-unpaid', { bookingId }))

export const reviewBooking = (payload) => post('/app/booking/review', payload)

export const listMyBookings = () => get('/app/booking/my/list')
