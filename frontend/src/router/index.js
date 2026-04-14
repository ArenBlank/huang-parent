import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AdminLayout from '../layouts/AdminLayout.vue'
import Login from '../pages/Login.vue'
import Dashboard from '../pages/Dashboard.vue'
import Courses from '../pages/Courses.vue'
import CourseSchedule from '../pages/CourseSchedule.vue'
import Orders from '../pages/Orders.vue'
import BookingOps from '../pages/BookingOps.vue'
import Videos from '../pages/Videos.vue'
import TrainingPlans from '../pages/TrainingPlans.vue'
import CheckInCenter from '../pages/CheckInCenter.vue'
import PermissionMatrix from '../pages/PermissionMatrix.vue'
import ContentOps from '../pages/ContentOps.vue'
import OperationLogs from '../pages/OperationLogs.vue'
import CoachApply from '../pages/CoachApply.vue'
import UserRoleManagement from '../pages/UserRoleManagement.vue'
import RoleScope from '../pages/RoleScope.vue'
import RolePermission from '../pages/RolePermission.vue'
import PermissionCenter from '../pages/PermissionCenter.vue'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: Login },
  {
    path: '/',
    component: AdminLayout,
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'courses', component: Courses },
      { path: 'check-in-center', component: CheckInCenter },
      { path: 'schedules', component: CourseSchedule },
      { path: 'orders', component: Orders },
      { path: 'booking-ops', component: BookingOps },
      { path: 'videos', component: Videos },
      { path: 'training-plans', component: TrainingPlans },
      { path: 'permission-matrix', component: PermissionMatrix },
      { path: 'content', component: ContentOps },
      { path: 'operation-logs', component: OperationLogs },
      { path: 'coach-apply', component: CoachApply },
      { path: 'user-roles', component: UserRoleManagement },
      { path: 'role-scope', component: RoleScope },
      { path: 'role-permission', component: RolePermission },
      { path: 'permission-center', component: PermissionCenter }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const store = useAuthStore()
  if (to.meta.requiresAuth && !store.accessToken) {
    next('/login')
  } else if (to.path === '/login' && store.accessToken) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
