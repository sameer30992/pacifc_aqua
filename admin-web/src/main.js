import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './styles/admin.css'
import './styles/vue-shell.css'

// Ensure body keeps mockup admin class after mount
document.body.classList.add('admin-app')

/** Open native date picker when clicking anywhere on the field (not only the icon). */
document.addEventListener(
  'click',
  (e) => {
    const el = e.target
    if (!(el instanceof HTMLInputElement) || el.type !== 'date' || el.disabled || el.readOnly) {
      return
    }
    if (typeof el.showPicker !== 'function') return
    try {
      el.showPicker()
    } catch {
      /* Browser may reject if not a direct user gesture; ignore. */
    }
  },
  true,
)

createApp(App).use(createPinia()).use(router).mount('#app')
