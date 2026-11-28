import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'
import { useCustomersStore } from '@/stores/customers'

const USER_KEY = 'pa_user'

function clearLegacyTokens() {
  localStorage.removeItem('pa_access')
  localStorage.removeItem('pa_refresh')
}

clearLegacyTokens()

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))
  const bootstrapped = ref(false)

  const isLoggedIn = computed(() => !!user.value)
  const displayName = computed(() => user.value?.name || 'Admin')

  function setUser(u) {
    user.value = u
    if (u) localStorage.setItem(USER_KEY, JSON.stringify(u))
    else localStorage.removeItem(USER_KEY)
  }

  function clearSession() {
    user.value = null
    localStorage.removeItem(USER_KEY)
    clearLegacyTokens()
    try {
      useCustomersStore().clear()
    } catch {
      /* pinia may not be ready during early boot */
    }
  }

  async function login(phoneOrEmail, password) {
    const { data } = await http.post('/auth/login', { phoneOrEmail, password })
    setUser(data.user)
    bootstrapped.value = true
    return data.user
  }

  async function logout() {
    try {
      await http.post('/auth/logout')
    } catch {
      /* ignore */
    }
    clearSession()
  }

  /** Restore session from HttpOnly cookie via /auth/me */
  async function bootstrap() {
    if (bootstrapped.value) return isLoggedIn.value
    bootstrapped.value = true
    try {
      const { data } = await http.get('/auth/me')
      setUser(data)
      return true
    } catch {
      clearSession()
      return false
    }
  }

  return {
    user,
    bootstrapped,
    isLoggedIn,
    displayName,
    setUser,
    clearSession,
    login,
    logout,
    bootstrap,
  }
})
