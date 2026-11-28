import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

/**
 * Same-origin client — Vite/nginx proxy forwards to Spring.
 * Auth is HttpOnly cookies (`pa_access` / `pa_refresh`); JS never stores JWT.
 */
const http = axios.create({
  baseURL: '',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    const auth = useAuthStore()
    const status = error.response?.status
    const url = error.config?.url || ''
    if (
      status === 401 &&
      !error.config._retry &&
      !url.includes('/auth/login') &&
      !url.includes('/auth/refresh') &&
      !url.includes('/auth/me')
    ) {
      error.config._retry = true
      try {
        const { data } = await axios.post('/auth/refresh', {}, { withCredentials: true })
        if (data?.user) auth.setUser(data.user)
        return http(error.config)
      } catch {
        auth.clearSession()
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
    }
    return Promise.reject(error)
  },
)

export default http
