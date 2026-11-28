<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCustomersStore } from '@/stores/customers'
import PaAvatar from '@/components/PaAvatar.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const customersStore = useCustomersStore()
const collapsed = ref(false)
const navOpen = ref(false)

let tipEl = null
let tipBtn = null

function ensureTip() {
  if (tipEl) return tipEl
  tipEl = document.createElement('div')
  tipEl.className = 'pa-float-tip'
  tipEl.setAttribute('role', 'tooltip')
  document.body.appendChild(tipEl)
  return tipEl
}

function hideNavTip() {
  tipBtn = null
  if (!tipEl) return
  tipEl.classList.remove('is-on', 'is-left')
}

function placeNavTip(btn) {
  const text = (btn.getAttribute('data-tip') || '').trim()
  if (!text) {
    hideNavTip()
    return
  }
  const el = ensureTip()
  tipBtn = btn
  el.textContent = text
  el.classList.add('is-on')
  el.classList.remove('is-left')
  const r = btn.getBoundingClientRect()
  const tipW = el.offsetWidth
  const tipH = el.offsetHeight
  let left = r.right + 12
  let top = r.top + r.height / 2 - tipH / 2
  if (left + tipW > window.innerWidth - 8) {
    left = Math.max(8, r.left - tipW - 12)
    el.classList.add('is-left')
  }
  top = Math.max(8, Math.min(top, window.innerHeight - tipH - 8))
  el.style.left = `${left}px`
  el.style.top = `${top}px`
}

function onTipPointerOver(e) {
  const btn = e.target?.closest?.('.admin-side .has-tip')
  if (!btn || btn === tipBtn) return
  placeNavTip(btn)
}

function onTipPointerOut(e) {
  const btn = e.target?.closest?.('.admin-side .has-tip')
  if (!btn || btn !== tipBtn) return
  const to = e.relatedTarget
  if (to && btn.contains(to)) return
  hideNavTip()
}

function onTipFocusIn(e) {
  const btn = e.target?.closest?.('.admin-side .has-tip')
  if (btn) placeNavTip(btn)
}

function onTipFocusOut(e) {
  const btn = e.target?.closest?.('.admin-side .has-tip')
  if (btn && btn === tipBtn) hideNavTip()
}

onMounted(() => {
  // Warm browser customer cache; sync-meta keeps it fresh when DB changes.
  customersStore.ensureLoaded().catch(() => {})
  document.addEventListener('pointerover', onTipPointerOver, true)
  document.addEventListener('pointerout', onTipPointerOut, true)
  document.addEventListener('focusin', onTipFocusIn)
  document.addEventListener('focusout', onTipFocusOut)
  window.addEventListener('scroll', hideNavTip, true)
  window.addEventListener('resize', hideNavTip)
})

onUnmounted(() => {
  document.removeEventListener('pointerover', onTipPointerOver, true)
  document.removeEventListener('pointerout', onTipPointerOut, true)
  document.removeEventListener('focusin', onTipFocusIn)
  document.removeEventListener('focusout', onTipFocusOut)
  window.removeEventListener('scroll', hideNavTip, true)
  window.removeEventListener('resize', hideNavTip)
  hideNavTip()
  tipEl?.remove()
  tipEl = null
})

const nav = [
  {
    name: 'dashboard',
    code: 'SUM UP',
    desc: 'Dashboard',
    tip: 'Dashboard',
    icon: '<path d="M4.5 4.5h6.2v6.2H4.5zM13.3 4.5h6.2v4H13.3zM13.3 10.8h6.2v8.7H13.3zM4.5 13h6.2v6.5H4.5z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>',
  },
  {
    name: 'sales',
    code: 'P&S',
    desc: 'Purchase & sales',
    tip: 'Purchase & sales',
    icon: '<path d="M4 19V5M4 19h16M8 16V9m4 7V7m4 9v-5" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"/>',
  },
  {
    name: 'billing',
    code: 'BILL',
    desc: 'Billing / print',
    tip: 'Billing / print',
    icon: '<path d="M7 3.8h10a1.2 1.2 0 0 1 1.2 1.2v14L12 16.2 5.8 19V5a1.2 1.2 0 0 1 1.2-1.2Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M9 8h6M9 11h6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  {
    name: 'orders',
    code: 'Orders',
    desc: 'Notify helper',
    tip: 'Orders',
    icon: '<path d="M7 8.5h10l-.7 9.5a1.2 1.2 0 0 1-1.2 1.1H8.9a1.2 1.2 0 0 1-1.2-1.1L7 8.5Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M9 8.5V7a3 3 0 0 1 6 0v1.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  {
    name: 'customers',
    code: 'Customers',
    desc: 'List & rates',
    tip: 'Customers',
    icon: '<circle cx="9" cy="8" r="2.4" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M4.8 18.2c.4-2.6 2.3-4 4.2-4s3.8 1.4 4.2 4M16.2 9.2a2.1 2.1 0 1 0 0-4.2M19.5 18.2c-.3-2-1.6-3.2-3.3-3.2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  {
    name: 'plant-trips',
    code: 'Plant trips',
    desc: 'Load / return',
    tip: 'Plant trips',
    icon: '<path d="M3.8 16.5h12.4V8.2H8.6L6.4 5.8H3.8v10.7Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M16.2 11.2h2.5l1.5 2.3v3H16.2M7.2 18.8a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Zm10.4 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>',
  },
  {
    name: 'products',
    code: 'Products',
    desc: 'Jar / chilled / …',
    tip: 'Products',
    icon: '<path d="M8.2 4.8h7.6L18 9.2v9.2a1.2 1.2 0 0 1-1.2 1.2H7.2A1.2 1.2 0 0 1 6 18.4V9.2L8.2 4.8Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M6.2 9.2h11.6M12 4.8v4.4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  {
    name: 'employees',
    code: 'Employees',
    desc: 'Helper / admin / …',
    tip: 'Employees',
    icon: '<circle cx="12" cy="8" r="2.6" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M6.2 18.5c.6-3 2.8-4.6 5.8-4.6s5.2 1.6 5.8 4.6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M17.5 7.2h2.8M18.9 5.8v2.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
]

const later = [
  {
    code: 'Daily Expenses',
    desc: 'Issue / receive',
    tip: 'Daily Expenses',
    icon: '<path d="M6.5 7.2h11M6.5 12h11M6.5 16.8h7.2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M4.5 4.8h15v14.4h-15z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>',
  },
  {
    code: 'ATT.',
    desc: 'Attendance',
    tip: 'Attendance',
    icon: '<rect x="4.5" y="5.5" width="15" height="14" rx="1.4" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M8 3.8v3.2M16 3.8v3.2M4.5 10h15" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="m9.2 14.2 1.8 1.8 3.8-3.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  },
  {
    code: 'ATT. · PAY',
    desc: 'Salary / wages',
    tip: 'Salary / wages',
    icon: '<circle cx="12" cy="12" r="7.2" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M12 8.2v7.6M9.6 9.8c.6-.7 1.4-1 2.4-1 1.5 0 2.5.7 2.5 1.9s-1 1.8-2.6 2.2c-1.5.4-2.5 1-2.5 2.2s1.1 2 2.7 2c1.1 0 1.9-.4 2.5-1.1" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  {
    code: 'OTH, PURCH',
    desc: 'Outside purchase',
    tip: 'Outside purchase',
    icon: '<path d="M5 8.5h14l-1.2 9.2a1.2 1.2 0 0 1-1.2 1H7.4a1.2 1.2 0 0 1-1.2-1L5 8.5Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M9 8.5 10.2 5h3.6L15 8.5M12 12v4M10 14h4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  {
    code: 'Print',
    desc: 'Export / print',
    tip: 'Print',
    icon: '<path d="M7.2 9V4.8h9.6V9M7.2 15.5H5.5A1.5 1.5 0 0 1 4 14v-3.2A1.5 1.5 0 0 1 5.5 9.3h13A1.5 1.5 0 0 1 20 10.8V14a1.5 1.5 0 0 1-1.5 1.5H16.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M7.2 13.5h9.6v5.7H7.2z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>',
  },
  {
    code: 'Outstanding',
    desc: 'Dues + empties',
    tip: 'Outstanding',
    icon: '<circle cx="12" cy="12" r="7.2" fill="none" stroke="currentColor" stroke-width="1.8"/><path d="M12 8v4.5M12 16.2h.01" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"/>',
  },
  {
    code: 'Dues',
    desc: 'Collect payments',
    tip: 'Dues',
    icon: '<path d="M12 3.8v16.4M16.2 8.2c-.7-1.2-2-2-4.2-2-2.8 0-4.5 1.4-4.5 3.4S9.5 13 12.4 13.6c2.6.6 4 1.5 4 3.4s-1.8 3.4-4.6 3.4c-2.2 0-3.7-.8-4.5-2.1" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
]

function closeNav() {
  navOpen.value = false
  hideNavTip()
}

function go(name) {
  hideNavTip()
  closeNav()
  router.push({ name })
}

async function logout() {
  closeNav()
  await auth.logout()
  router.push({ name: 'login' })
}

watch(
  () => route.fullPath,
  () => closeNav(),
)

watch(collapsed, hideNavTip)
</script>

<template>
  <div
    class="admin admin-dense admin-fullscreen"
    :class="{ 'nav-collapsed': collapsed, 'nav-drawer-open': navOpen }"
    id="adminShell"
  >
    <div class="admin-top">
      <div class="admin-top-left">
        <button
          type="button"
          class="admin-menu-btn"
          aria-label="Open menu"
          :aria-expanded="navOpen.toString()"
          aria-controls="sideNav"
          @click="navOpen = !navOpen"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M4 7h16M4 12h16M4 17h16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            />
          </svg>
        </button>
        <button type="button" class="admin-brand" title="Home · SUM UP" @click="go('dashboard')">
          <img
            class="admin-brand-logo"
            src="/assets/brand/pacific-aqua-logo.png"
            alt="Pacific Aqua"
            width="36"
            height="36"
          />
          <span class="admin-brand-text">
            <small>Pacific Aqua</small>
            <strong>Admin Control</strong>
          </span>
        </button>
      </div>
      <div class="admin-top-right">
        <div class="admin-user-chip" :title="`Logged in as ${auth.displayName}`">
          <PaAvatar :name="auth.displayName" kind="user" size="sm" />
          <span class="admin-user-text">
            <small>Welcome</small>
            <strong>{{ auth.displayName }}</strong>
          </span>
        </div>
        <button type="button" class="btn-on-dark" @click="logout">Logout</button>
      </div>
    </div>

    <div
      class="admin-nav-backdrop"
      aria-hidden="true"
      @click="closeNav"
    />

    <div class="admin-grid" id="adminGrid">
      <aside class="admin-side" id="sideNav" aria-label="Admin menu">
        <div class="side-rail-top">
          <div class="side-drawer-title">Menu</div>
          <button
            type="button"
            class="side-menu-toggle has-tip"
            :data-tip="collapsed ? 'Expand sidebar' : 'Collapse sidebar'"
            :aria-label="collapsed ? 'Expand sidebar' : 'Collapse sidebar'"
            :aria-expanded="(!collapsed).toString()"
            aria-controls="sideNav"
            @click="collapsed = !collapsed"
          >
            <span class="menu-toggle-ico menu-ico-collapse" aria-hidden="true">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3.5" y="4.5" width="17" height="15" rx="2.2" fill="none" stroke="currentColor" stroke-width="1.9" />
                <path d="M9 4.5v15" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" />
              </svg>
            </span>
            <span class="menu-toggle-ico menu-ico-expand" aria-hidden="true">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3.5" y="4.5" width="17" height="15" rx="2.2" fill="none" stroke="currentColor" stroke-width="1.9" />
                <path d="M9 4.5v15" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" />
                <path d="M13.2 12h5.2M16.2 9.5 18.5 12 16.2 14.5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
          </button>
          <button
            type="button"
            class="side-drawer-close"
            aria-label="Close menu"
            @click="closeNav"
          >
            ×
          </button>
        </div>

        <button
          v-for="item in nav"
          :key="item.name"
          type="button"
          class="has-tip"
          :class="{ active: route.name === item.name || (item.name === 'dashboard' && route.name === 'dashboard') }"
          :data-tip="item.tip"
          :data-tab="item.name"
          @click="go(item.name)"
        >
          <span class="nav-ico" aria-hidden="true">
            <svg viewBox="0 0 24 24" v-html="item.icon" />
          </span>
          <span class="nav-text">
            <span class="nav-code">{{ item.code }}</span>
            <span class="nav-desc">{{ item.desc }}</span>
          </span>
        </button>

        <div class="nav-section">Later</div>
        <button
          v-for="item in later"
          :key="item.code"
          type="button"
          class="nav-later has-tip"
          disabled
          aria-disabled="true"
          :data-tip="item.tip"
        >
          <span class="nav-ico" aria-hidden="true">
            <svg viewBox="0 0 24 24" v-html="item.icon" />
          </span>
          <span class="nav-text">
            <span class="nav-code">{{ item.code }}</span>
            <span class="nav-desc">{{ item.desc }} <em>Later</em></span>
          </span>
        </button>
      </aside>

      <div class="admin-main">
        <router-view />
      </div>
    </div>
  </div>
</template>
