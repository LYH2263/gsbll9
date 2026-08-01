import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import Login from '../views/Login.vue'
import Contest from '../views/Contest.vue'
import Rankings from '../views/Rankings.vue'
import Admin from '../views/Admin.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/contest',
    component: Contest,
    meta: { requiresAuth: true }
  },
  {
    path: '/rankings',
    component: Rankings,
    meta: { requiresAuth: false }
  },
  {
    path: '/admin',
    component: Admin,
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 未认证用户访问需要认证的页面
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
    return
  }

  // 非管理员访问管理页面
  if (to.meta.requiresAdmin && !authStore.isAdmin()) {
    next('/contest')
    return
  }

  // 已认证的普通用户访问登录页，重定向到答题页面
  if (to.path === '/login' && authStore.isAuthenticated && !authStore.isAdmin()) {
    next('/contest')
    return
  }

  // 已认证的管理员访问登录页，重定向到管理页面
  if (to.path === '/login' && authStore.isAuthenticated && authStore.isAdmin()) {
    next('/admin')
    return
  }

  next()
})

export default router
