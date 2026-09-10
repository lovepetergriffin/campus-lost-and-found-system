import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/auth', name: 'auth', component: () => import('@/views/AuthView.vue') },
    { path: '/items/:id', name: 'item-detail', component: () => import('@/views/ItemDetailView.vue') },
    { path: '/publish/:id?', name: 'publish', component: () => import('@/views/PublishView.vue'), meta: { requiresAuth: true } },
    { path: '/center', name: 'center', component: () => import('@/views/CenterView.vue'), meta: { requiresAuth: true } },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { requiresAuth: true, admin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) return { name: 'auth', query: { redirect: to.fullPath } }
  if (to.meta.admin && !auth.isAdmin) return { name: 'home' }
})

export default router
