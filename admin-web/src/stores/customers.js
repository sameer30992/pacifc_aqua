import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import http from '@/api/http'

const CACHE_KEY = 'pa_customers_cache_v1'

function readCache() {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed?.items)) return null
    return parsed
  } catch {
    return null
  }
}

function writeCache(payload) {
  localStorage.setItem(CACHE_KEY, JSON.stringify(payload))
}

function clearCacheStorage() {
  localStorage.removeItem(CACHE_KEY)
}

export const useCustomersStore = defineStore('customers', () => {
  const cached = readCache()
  const items = ref(cached?.items || [])
  const customersRevision = ref(cached?.customersRevision || null)
  const balancesRevision = ref(cached?.balancesRevision || null)
  const activeCount = ref(cached?.activeCount || 0)
  const loading = ref(false)
  const syncing = ref(false)
  let refreshPromise = null

  const list = computed(() => items.value)

  function persist() {
    writeCache({
      items: items.value,
      customersRevision: customersRevision.value,
      balancesRevision: balancesRevision.value,
      activeCount: activeCount.value,
      savedAt: Date.now(),
    })
  }

  function replaceAll(nextItems, meta = {}) {
    items.value = nextItems || []
    if (meta.customersRevision != null) customersRevision.value = meta.customersRevision
    if (meta.balancesRevision != null) balancesRevision.value = meta.balancesRevision
    if (meta.activeCount != null) activeCount.value = meta.activeCount
    else activeCount.value = items.value.length
    persist()
  }

  function upsert(customer) {
    if (!customer?.id) return
    const idx = items.value.findIndex((c) => c.id === customer.id)
    if (idx >= 0) {
      const next = items.value.slice()
      next[idx] = { ...next[idx], ...customer }
      items.value = next
    } else {
      items.value = [...items.value, customer].sort((a, b) =>
        String(a.name || '').localeCompare(String(b.name || '')),
      )
    }
    activeCount.value = items.value.length
    persist()
  }

  function remove(id) {
    items.value = items.value.filter((c) => c.id !== id)
    activeCount.value = items.value.length
    persist()
  }

  function clear() {
    items.value = []
    customersRevision.value = null
    balancesRevision.value = null
    activeCount.value = 0
    clearCacheStorage()
  }

  async function fetchAll() {
    loading.value = true
    try {
      const [listRes, metaRes] = await Promise.all([
        http.get('/customers'),
        http.get('/customers/sync-meta'),
      ])
      replaceAll(listRes.data || [], {
        customersRevision: metaRes.data?.customersRevision,
        balancesRevision: metaRes.data?.balancesRevision,
        activeCount: metaRes.data?.activeCount,
      })
      return items.value
    } finally {
      loading.value = false
    }
  }

  async function refreshIfStale({ force = false } = {}) {
    if (refreshPromise) return refreshPromise
    refreshPromise = (async () => {
      syncing.value = true
      try {
        if (force || !items.value.length) {
          await fetchAll()
          return items.value
        }
        const { data: meta } = await http.get('/customers/sync-meta')
        const sameCustomers = String(meta.customersRevision) === String(customersRevision.value)
        const sameBalances = String(meta.balancesRevision) === String(balancesRevision.value)
        const sameCount = Number(meta.activeCount) === Number(activeCount.value)
        if (sameCustomers && sameBalances && sameCount) {
          return items.value
        }
        await fetchAll()
        return items.value
      } finally {
        syncing.value = false
        refreshPromise = null
      }
    })()
    return refreshPromise
  }

  /** Use cached list immediately; refresh from DB when revision changes. */
  async function ensureLoaded() {
    if (!items.value.length) {
      await fetchAll()
      return items.value
    }
    // Non-blocking freshness check so UI can use cache right away.
    refreshIfStale().catch(() => {})
    return items.value
  }

  function searchLocal(term, limit = 40) {
    const q = String(term || '').trim().toLowerCase()
    const source = items.value
    if (!q) return source.slice(0, limit)
    return source
      .filter((c) => {
        const hay = [c.name, c.phone, c.cityArea, c.cardNo].filter(Boolean).join(' ').toLowerCase()
        return hay.includes(q)
      })
      .slice(0, limit)
  }

  function byId(id) {
    return items.value.find((c) => String(c.id) === String(id)) || null
  }

  return {
    items,
    list,
    loading,
    syncing,
    customersRevision,
    balancesRevision,
    activeCount,
    ensureLoaded,
    refreshIfStale,
    fetchAll,
    upsert,
    remove,
    clear,
    searchLocal,
    byId,
  }
})
