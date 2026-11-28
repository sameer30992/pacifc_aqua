import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
      { path: 'sales', name: 'sales', component: () => import('@/views/SalesView.vue') },
      { path: 'billing', name: 'billing', component: () => import('@/views/BillingView.vue') },
      { path: 'orders', name: 'orders', component: () => import('@/views/OrdersView.vue') },
      { path: 'customers', name: 'customers', component: () => import('@/views/CustomersView.vue') },
      { path: 'plant-trips', name: 'plant-trips', component: () => import('@/views/PlantTripsView.vue') },
      { path: 'products', name: 'products', component: () => import('@/views/ProductsView.vue') },
      { path: 'employees', name: 'employees', component: () => import('@/views/EmployeesView.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.bootstrapped) {
    await auth.bootstrap()
  }
  if (!to.meta.public && !auth.isLoggedIn) return { name: 'login' }
  if (to.name === 'login' && auth.isLoggedIn) return { name: 'dashboard' }
  return true
})

export default router
