import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/Login.vue'), meta: { public: true, register: true } },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/overview',
    meta: { admin: true },
    children: [
      { path: 'overview', name: 'admin-overview', component: () => import('@/views/admin/AdminOverview.vue'), meta: { title: '运营总览' } },
      { path: 'users', name: 'admin-users', component: () => import('@/views/admin/AdminUsers.vue'), meta: { title: '用户管理' } },
      { path: 'usage', name: 'admin-usage', component: () => import('@/views/admin/AdminUsage.vue'), meta: { title: '使用分析' } },
      { path: 'logs', name: 'admin-logs', component: () => import('@/views/admin/AdminLogs.vue'), meta: { title: '系统日志' } }
    ]
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'records', name: 'records', component: () => import('@/views/Records.vue'), meta: { title: '收支流水' } },
      { path: 'records/import', name: 'records-import', component: () => import('@/views/RecordsImport.vue'), meta: { title: 'CSV 导入' } },
      { path: 'accounts', name: 'accounts', component: () => import('@/views/Accounts.vue'), meta: { title: '账户与对账' } },
      { path: 'categories', name: 'categories', component: () => import('@/views/Categories.vue'), meta: { title: '分类管理' } },
      { path: 'budgets', name: 'budgets', component: () => import('@/views/Budgets.vue'), meta: { title: '预算管理' } },
      { path: 'stats', name: 'stats', component: () => import('@/views/Stats.vue'), meta: { title: '统计分析' } },
      { path: 'goals', name: 'goals', component: () => import('@/views/Goals.vue'), meta: { title: '理财目标' } },
      { path: 'reports', name: 'reports', component: () => import('@/views/Reports.vue'), meta: { title: '报表与备份' } },
      { path: 'brief', name: 'brief', component: () => import('@/views/Brief.vue'), meta: { title: '智能简报' } },
      { path: 'onboarding', name: 'onboarding', component: () => import('@/views/Onboarding.vue'), meta: { title: '新手引导' } },
      { path: 'settings', name: 'settings', component: () => import('@/views/Settings.vue'), meta: { title: '个人设置' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue'), meta: { public: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }
  const store = useUserStore()
  if (!store.user) {
    try {
      await store.fetchMe()
    } catch (e) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }
  const adminArea = to.path.startsWith('/admin')
  if (store.isAdmin) {
    return adminArea ? true : '/admin/overview'
  }
  if (adminArea || to.meta.admin) {
    return '/dashboard'
  }
  return true
})

router.afterEach((to) => {
  if (to.path.startsWith('/admin')) {
    document.title = to.meta.title ? `${to.meta.title} · 钱迹管理台` : '钱迹 · 管理控制台'
    return
  }
  document.title = to.meta.title ? `${to.meta.title} · 钱迹` : '钱迹 · 个人理财管理系统'
})

export default router
