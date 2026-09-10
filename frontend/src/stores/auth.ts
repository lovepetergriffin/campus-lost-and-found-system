import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api'
import type { User } from '@/types'

const USER_KEY = 'lost-found-user'
const TOKEN_KEY = 'lost-found-token'

export const useAuthStore = defineStore('auth', () => {
  const saved = localStorage.getItem(USER_KEY)
  const user = ref<User | null>(saved ? JSON.parse(saved) : null)
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const isLoggedIn = computed(() => Boolean(token.value && user.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(username: string, password: string) {
    const result = await authApi.login({ username, password })
    token.value = result.token
    user.value = result.user
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  async function register(username: string, password: string, contact: string) {
    await authApi.register({ username, password, contact })
    await login(username, password)
  }

  function logout() {
    user.value = null
    token.value = ''
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(TOKEN_KEY)
  }

  return { user, token, isLoggedIn, isAdmin, login, register, logout }
})
