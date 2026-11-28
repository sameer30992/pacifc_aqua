import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

/** Browser refresh on Vue routes must get index.html, not the Spring API. */
function apiProxy(target = 'http://127.0.0.1:8080') {
  return {
    target,
    changeOrigin: true,
    // Forward cookies both ways (HttpOnly JWT session)
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq, req) => {
        if (req.headers.cookie) {
          proxyReq.setHeader('cookie', req.headers.cookie)
        }
      })
    },
    bypass(req) {
      const accept = req.headers.accept || ''
      // Full page navigation / refresh
      if (req.method === 'GET' && accept.includes('text/html')) {
        return '/index.html'
      }
    },
  }
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    proxy: {
      '/auth': apiProxy(),
      '/dashboard': apiProxy(),
      '/visits': apiProxy(),
      '/billing': apiProxy(),
      '/orders': apiProxy(),
      '/customers': apiProxy(),
      '/products': apiProxy(),
      '/employees': apiProxy(),
      '/plant-trips': apiProxy(),
      '/ws': {
        target: 'http://127.0.0.1:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
