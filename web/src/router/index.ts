import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'dishes',
          name: 'Dishes',
          component: () => import('@/views/dishes/index.vue'),
          meta: { title: '菜品管理' },
        },
        {
          path: 'dishes/create',
          name: 'DishCreate',
          component: () => import('@/views/dishes/form.vue'),
          meta: { title: '新增菜品' },
        },
        {
          path: 'dishes/:id',
          name: 'DishEdit',
          component: () => import('@/views/dishes/form.vue'),
          meta: { title: '编辑菜品' },
        },
        {
          path: 'categories',
          name: 'Categories',
          component: () => import('@/views/categories/index.vue'),
          meta: { title: '分类管理' },
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/orders/index.vue'),
          meta: { title: '订单管理' },
        },
        {
          path: 'orders/:id',
          name: 'OrderDetail',
          component: () => import('@/views/orders/detail.vue'),
          meta: { title: '订单详情' },
        },
        {
          path: 'stats',
          name: 'Stats',
          component: () => import('@/views/stats/index.vue'),
          meta: { title: '数据统计' },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/index.vue'),
          meta: { title: '设置' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/404.vue'),
      meta: { title: '404' },
    },
  ],
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  document.title = `${to.meta.title || '陈哥厨房'} - 管理后台`
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
