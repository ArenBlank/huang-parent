import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AdminLayout from '../layouts/AdminLayout.vue'
import Login from '../pages/Login.vue'
import Dashboard from '../pages/Dashboard.vue'
import Courses from '../pages/Courses.vue'
import CourseSchedule from '../pages/CourseSchedule.vue'
import Orders from '../pages/Orders.vue'
import Videos from '../pages/Videos.vue'
import PermissionMatrix from '../pages/PermissionMatrix.vue'

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
      { path: 'schedules', component: CourseSchedule },
      { path: 'orders', component: Orders },
      { path: 'videos', component: Videos },
      { path: 'permission-matrix', component: PermissionMatrix }
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
