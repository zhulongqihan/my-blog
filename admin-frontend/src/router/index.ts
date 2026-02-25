import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '管理员登录', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' },
      },
      {
        path: 'articles',
        name: 'Articles',
        component: () => import('@/views/ArticleList.vue'),
        meta: { title: '文章管理', icon: 'Document' },
      },
      {
        path: 'articles/create',
        name: 'ArticleCreate',
        component: () => import('@/views/ArticleEdit.vue'),
        meta: { title: '新建文章', icon: 'Edit', activeMenu: '/articles' },
      },
      {
        path: 'articles/edit/:id',
        name: 'ArticleEditById',
        component: () => import('@/views/ArticleEdit.vue'),
        meta: { title: '编辑文章', icon: 'Edit', activeMenu: '/articles' },
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/CategoryManage.vue'),
        meta: { title: '分类管理', icon: 'Folder' },
      },
      {
        path: 'tags',
        name: 'Tags',
        component: () => import('@/views/TagManage.vue'),
        meta: { title: '标签管理', icon: 'PriceTag' },
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/CommentManage.vue'),
        meta: { title: '评论管理', icon: 'ChatDotRound' },
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/LogList.vue'),
        meta: { title: '操作日志', icon: 'Tickets' },
      },
      {
        path: 'rate-limit',
        name: 'RateLimit',
        component: () => import('@/views/RateLimitMonitor.vue'),
        meta: { title: '限流监控', icon: 'Odometer' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes,
})

// 全局前置守卫
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '后台管理'} - 博客管理系统`

  const token = localStorage.getItem('admin_token')

  if (to.meta.requiresAuth === false) {
    // 登录页：如果已登录则跳转到仪表盘
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    // 需要认证的页面
    if (!token) {
      next('/login')
    } else {
      next()
    }
  }
})

export default router
