import { adminClient } from './client'

export function listCoachSchedules(params) {
  return adminClient.get('/admin/coach-schedule/list', { params })
}

export function listCoachScheduleCoachOptions() {
  return adminClient.get('/admin/coach-schedule/coach-options')
}

export function createCoachSchedule(payload) {
  return adminClient.post('/admin/coach-schedule', payload)
}

export function updateCoachSchedule(id, payload) {
  return adminClient.put(`/admin/coach-schedule/${id}`, payload)
}

export function updateCoachScheduleStatus(id, status) {
  return adminClient.put(`/admin/coach-schedule/${id}/status`, null, {
    params: { status }
  })
}

export function deleteCoachSchedule(id) {
  return adminClient.delete(`/admin/coach-schedule/${id}`)
}
