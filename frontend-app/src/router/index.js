import { createRouter, createWebHistory } from 'vue-router'
import { useAppAuthStore } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'
import Login from '../pages/Login.vue'
import Home from '../pages/Home.vue'
import Plans from '../pages/Plans.vue'
import Courses from '../pages/Courses.vue'
import Booking from '../pages/Booking.vue'
import Training from '../pages/Training.vue'
import Orders from '../pages/Orders.vue'
import Profile from '../pages/Profile.vue'
import CoachApply from '../pages/CoachApply.vue'
import VideoPlayer from '../pages/VideoPlayer.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: Login },
  { path: '/video-player', component: VideoPlayer, meta: { requiresAuth: true } },
  {
    path: '/',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      { path: 'home', component: Home },
      { path: 'plans', component: Plans },
      { path: 'courses', component: Courses },
      { path: 'booking', component: Booking },
      { path: 'training', component: Training },
      { path: 'orders', component: Orders },
      { path: 'profile', component: Profile },
      { path: 'coach-apply', component: CoachApply }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to) {
    if (to.path !== '/login' && to.path !== '/video-player') {
      return { el: '.app-nav', top: 0, left: 0 }
    }
    return { top: 0, left: 0 }
  }
})

const publicPaths = new Set(['/login', '/register'])

router.beforeEach((to, from, next) => {
  const store = useAppAuthStore()
  const hasToken = Boolean(store.accessToken)

  if (!publicPaths.has(to.path) && !hasToken) {
    next('/login')
  } else if (to.path === '/login' && hasToken) {
    next('/home')
  } else {
    next()
  }
})

export default router
