import { createRouter, createWebHistory } from 'vue-router'
import AdminDashboardPage from '@/pages/AdminDashboard/index.vue'
import AdminLoginPage from '@/pages/AdminLogin/index.vue'
import AdminUsersPage from '@/pages/AdminUsers/index.vue'
import AdminCategoriesPage from '@/pages/AdminCategories/index.vue'
import AdminSupportPage from '@/pages/AdminSupport/AdminSupport.vue'
import AdminAuditLogsPage from '@/pages/AdminAuditLogs/index.vue'
import AdminSettingsPage from '@/pages/AdminSettings/index.vue'
import AdminWalletPage from '@/pages/AdminWallet/index.vue'
import NotFoundPage from '@/pages/NotFound/index.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/dashboard',
    name: 'admin-dashboard',
    component: AdminDashboardPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/login',
    name: 'admin-login',
    component: AdminLoginPage,
    meta: { requiresAuth: false },
  },
  {
    path: '/users',
    name: 'admin-users',
    component: AdminUsersPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/categories',
    name: 'admin-categories',
    component: AdminCategoriesPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/support',
    name: 'admin-support',
    component: AdminSupportPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/audit-logs',
    name: 'admin-audit-logs',
    component: AdminAuditLogsPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/wallet',
    name: 'admin-wallet',
    component: AdminWalletPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/settings/security',
    name: 'admin-settings-security',
    component: AdminSettingsPage,
    meta: { requiresAuth: true, userType: 'admin' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundPage,
    meta: { requiresAuth: false },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to, from, next) => {
  const userString = localStorage.getItem('user')
  let user: { type?: string } | null = null
  try {
    user = userString ? (JSON.parse(userString) as { type?: string }) : null
  } catch {
    user = null
  }

  if (to.name === 'admin-login' && user?.type === 'admin') {
    next('/dashboard')
    return
  }

  if (to.meta.requiresAuth && !user) {
    next('/login')
    return
  }

  if (to.meta.requiresAuth && user && user.type !== 'admin') {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    next({ path: '/login', query: { denied: 'true' } })
    return
  }

  next()
})

export default router
