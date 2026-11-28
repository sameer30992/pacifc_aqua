<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import PaAvatar from '@/components/PaAvatar.vue'
import { customerInitials, avatarTone } from '@/utils/avatar'
import { useCustomersStore } from '@/stores/customers'

const router = useRouter()
const customersStore = useCustomersStore()
const rows = ref([])
const loading = ref(true)
const total = ref(0)
const totalPages = ref(0)
const error = ref('')
const showForm = ref(false)
const photoPreview = ref('')
const openMenuId = ref(null)
const menuPos = ref({ top: 0, left: 0 })
let searchTimer = null

const filters = reactive({
  q: '',
  balance: 'all',
  page: 0,
  sortBy: 'name',
  sortDir: 'asc',
})
const pageSize = ref(10)
const pageSizeOptions = [10, 20, 50, 100]

const form = reactive({
  id: null,
  name: '',
  phone: '',
  cityArea: '',
  type: 'RETAIL',
  avatarPath: '',
})

const pagerLabel = computed(() => {
  if (!total.value) return '0 customers'
  const from = filters.page * pageSize.value + 1
  const to = Math.min((filters.page + 1) * pageSize.value, total.value)
  return `${from}–${to} of ${total.value}`
})

function inr(n) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(n || 0)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/customers', {
      params: {
        q: filters.q.trim() || undefined,
        balance: filters.balance === 'all' ? undefined : filters.balance,
        page: filters.page,
        size: pageSize.value,
        sortBy: filters.sortBy,
        sortDir: filters.sortDir,
      },
    })
    rows.value = data.content || []
    total.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to load customers'
    rows.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

function applySearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filters.page = 0
    load()
  }, 280)
}

function onBalanceChange() {
  filters.page = 0
  load()
}

function onPageSizeChange() {
  filters.page = 0
  load()
}

function toggleOutstandingSort() {
  if (filters.sortBy !== 'outstanding') {
    filters.sortBy = 'outstanding'
    filters.sortDir = 'desc'
  } else {
    filters.sortDir = filters.sortDir === 'asc' ? 'desc' : 'asc'
  }
  filters.page = 0
  load()
}

function prevPage() {
  if (filters.page <= 0) return
  filters.page -= 1
  load()
}

function nextPage() {
  if (filters.page + 1 >= totalPages.value) return
  filters.page += 1
  load()
}

const openMenuCustomer = computed(() => {
  if (openMenuId.value == null) return null
  return rows.value.find((c) => c.id === openMenuId.value) || null
})

function placeMenu(event) {
  const btn = event?.currentTarget
  if (!btn) return
  const r = btn.getBoundingClientRect()
  const panelW = 168
  const panelH = 140
  let left = r.right - panelW
  if (left < 8) left = 8
  if (left + panelW > window.innerWidth - 8) left = window.innerWidth - panelW - 8
  let top = r.bottom + 6
  if (top + panelH > window.innerHeight - 8) {
    top = Math.max(8, r.top - panelH - 6)
  }
  menuPos.value = { top, left }
}

function toggleMenu(id, event) {
  event?.stopPropagation()
  if (openMenuId.value === id) {
    openMenuId.value = null
    return
  }
  placeMenu(event)
  openMenuId.value = id
}

function closeMenus() {
  openMenuId.value = null
}

function onDocPointer(e) {
  if (!openMenuId.value) return
  const t = e.target
  if (t?.closest?.('.order-menu, .order-menu-panel')) return
  closeMenus()
}

function openCreate() {
  closeMenus()
  Object.assign(form, {
    id: null,
    name: '',
    phone: '',
    cityArea: '',
    type: 'RETAIL',
    avatarPath: '',
  })
  photoPreview.value = ''
  showForm.value = true
}

function openEdit(c) {
  closeMenus()
  Object.assign(form, {
    id: c.id,
    name: c.name,
    phone: c.phone || '',
    cityArea: c.cityArea || '',
    type: c.type,
    avatarPath: c.avatarPath || '',
  })
  photoPreview.value = c.avatarPath || ''
  showForm.value = true
}

function openBilling(c) {
  closeMenus()
  router.push({ name: 'billing', query: { customerId: c.id } })
}

function onPhotoFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    photoPreview.value = String(reader.result || '')
    form.avatarPath = photoPreview.value
  }
  reader.readAsDataURL(file)
}

function clearPhoto() {
  photoPreview.value = ''
  form.avatarPath = ''
}

async function save() {
  error.value = ''
  try {
    const payload = {
      name: form.name,
      phone: form.phone,
      cityArea: form.cityArea,
      type: form.type,
      avatarPath: form.avatarPath || null,
    }
    if (form.id) {
      const { data } = await http.put(`/customers/${form.id}`, payload)
      customersStore.upsert(data)
    } else {
      const { data } = await http.post('/customers', payload)
      customersStore.upsert(data)
    }
    showForm.value = false
    await load()
    customersStore.refreshIfStale({ force: true }).catch(() => {})
  } catch (e) {
    error.value = e.response?.data?.message || 'Save failed'
  }
}

async function remove(c) {
  closeMenus()
  if (!confirm(`Remove ${c.name}?`)) return
  await http.delete(`/customers/${c.id}`)
  customersStore.remove(c.id)
  await load()
  customersStore.refreshIfStale({ force: true }).catch(() => {})
}

watch(() => filters.q, applySearch)

onMounted(() => {
  document.addEventListener('pointerdown', onDocPointer, true)
  window.addEventListener('scroll', closeMenus, true)
  window.addEventListener('resize', closeMenus)
  load()
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocPointer, true)
  window.removeEventListener('scroll', closeMenus, true)
  window.removeEventListener('resize', closeMenus)
})
</script>

<template>
  <div class="screen active customers-dense">
    <div class="ledger-head">
      <div>
        <h2>Customers</h2>
        <p class="muted">Masters · outstanding from ledger</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">+ Add customer</button>
    </div>

    <div class="toolbar customers-toolbar">
      <div class="cust-search customers-search">
        <input
          v-model="filters.q"
          type="search"
          placeholder="Search by name or mobile…"
          aria-label="Search customers"
          @keyup.enter="filters.page = 0; load()"
        />
      </div>
      <select
        v-model="filters.balance"
        aria-label="Balance filter"
        @change="onBalanceChange"
      >
        <option value="all">All customers</option>
        <option value="with">With balance</option>
        <option value="none">No balance</option>
      </select>
    </div>

    <div v-if="showForm" class="admin-form form-panel" style="margin-bottom:0.65rem">
      <div class="form-panel-head">
        <h3>{{ form.id ? 'Edit customer' : 'Add customer' }}</h3>
      </div>
      <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>
      <div class="form-grid two">
        <div class="form-field">
          <label>Name <span class="req">*</span></label>
          <input v-model="form.name" type="text" placeholder="Shop / farm name" />
        </div>
        <div class="form-field">
          <label>Phone</label>
          <input v-model="form.phone" type="tel" placeholder="10-digit" />
        </div>
        <div class="form-field">
          <label>City / Area</label>
          <input v-model="form.cityArea" type="text" placeholder="Raipur" />
        </div>
        <div class="form-field">
          <label>Type</label>
          <select v-model="form.type">
            <option value="DEALER">Dealer</option>
            <option value="RETAIL">Retail</option>
            <option value="INSTITUTION">Institution</option>
          </select>
        </div>
      </div>
      <div class="profile-photo-field">
        <label>Profile picture</label>
        <div class="profile-photo-row">
          <div
            class="profile-photo-preview"
            :class="photoPreview ? 'has-img' : `pa-tone-${avatarTone(form.name)}`"
            aria-hidden="true"
          >
            <img v-if="photoPreview" :src="photoPreview" alt="" />
            <template v-else>{{ customerInitials(form.name) }}</template>
          </div>
          <div class="profile-photo-actions">
            <p class="muted">Optional · billing, orders &amp; lists</p>
            <input ref="custPhotoFile" type="file" accept="image/*" hidden @change="onPhotoFile" />
            <button type="button" class="btn btn-ghost btn-sm" @click="$refs.custPhotoFile.click()">Choose photo</button>
            <button v-if="photoPreview" type="button" class="btn btn-ghost btn-sm" @click="clearPhoto">Remove</button>
          </div>
        </div>
      </div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" @click="showForm = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="save">Save customer</button>
      </div>
    </div>

    <p v-if="error && !showForm" class="form-banner-error" role="alert">{{ error }}</p>
    <p class="muted customers-hint">{{ loading ? 'Loading…' : pagerLabel }}</p>
    <div class="table-wrap">
      <table class="data-table" id="customerTable">
        <thead>
          <tr>
            <th>Name</th>
            <th>Phone</th>
            <th>Area / City</th>
            <th>Type</th>
            <th>
              <button
                type="button"
                class="th-sort"
                :class="{
                  'is-active': filters.sortBy === 'outstanding',
                  'is-asc': filters.sortBy === 'outstanding' && filters.sortDir === 'asc',
                  'is-desc': filters.sortBy === 'outstanding' && filters.sortDir === 'desc',
                }"
                aria-label="Sort by outstanding"
                @click="toggleOutstandingSort"
              >
                <span>Outstanding</span>
                <span class="th-sort-arrows" aria-hidden="true">
                  <span class="th-sort-up">▲</span>
                  <span class="th-sort-down">▼</span>
                </span>
              </button>
            </th>
            <th class="order-menu-cell" aria-label="Actions"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="6">Loading customers…</td>
          </tr>
          <template v-else>
            <tr v-for="c in rows" :key="c.id">
              <td>
                <span class="person-cell">
                  <PaAvatar :name="c.name" :photo="c.avatarPath" size="sm" />
                  <span>{{ c.name }}</span>
                </span>
              </td>
              <td>{{ c.phone || '—' }}</td>
              <td class="cust-area-cell">
                <span class="cust-area-text" :title="c.cityArea || ''">
                  {{ c.cityArea || '—' }}
                </span>
              </td>
              <td>{{ c.type }}</td>
              <td>{{ inr(c.outstanding) }}</td>
              <td class="order-menu-cell">
                <div class="order-menu" @click.stop>
                  <button
                    type="button"
                    class="order-menu-btn"
                    aria-label="Customer actions"
                    :aria-expanded="openMenuId === c.id ? 'true' : 'false'"
                    @click="toggleMenu(c.id, $event)"
                  >
                    <span class="order-menu-dots" aria-hidden="true" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="6">No customers yet</td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <div class="ledger-pager customers-pager">
      <span class="customers-pager-count">{{ pagerLabel }}</span>
      <div class="customers-pager-controls">
        <label class="customers-pager-size">
          <span>Rows</span>
          <select
            v-model.number="pageSize"
            aria-label="Page size"
            :disabled="loading"
            @change="onPageSizeChange"
          >
            <option v-for="n in pageSizeOptions" :key="n" :value="n">{{ n }}</option>
          </select>
        </label>
        <button
          type="button"
          class="btn btn-ghost btn-sm"
          :disabled="loading || filters.page <= 0"
          @click="prevPage"
        >
          ← Prev
        </button>
        <button
          type="button"
          class="btn btn-ghost btn-sm"
          :disabled="loading || filters.page + 1 >= totalPages"
          @click="nextPage"
        >
          Next →
        </button>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="openMenuCustomer"
        class="order-menu-panel order-menu-panel-float"
        role="menu"
        :style="{ top: `${menuPos.top}px`, left: `${menuPos.left}px` }"
        @click.stop
      >
        <button type="button" role="menuitem" @click="openEdit(openMenuCustomer)">
          <span class="omi omi-edit" aria-hidden="true" />
          Edit
        </button>
        <button type="button" role="menuitem" @click="openBilling(openMenuCustomer)">
          <span class="omi omi-billing" aria-hidden="true" />
          Billing
        </button>
        <button type="button" role="menuitem" class="is-danger" @click="remove(openMenuCustomer)">
          <span class="omi omi-trash" aria-hidden="true" />
          Remove
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.profile-photo-preview.has-img {
  padding: 0;
  overflow: hidden;
}
.profile-photo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.th-sort {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  font-weight: inherit;
  letter-spacing: inherit;
  text-transform: inherit;
  cursor: pointer;
  padding: 0;
}

.th-sort-arrows {
  display: inline-flex;
  flex-direction: column;
  line-height: 0.65;
  font-size: 0.55rem;
  opacity: 0.35;
}

.th-sort-up,
.th-sort-down {
  display: block;
}

.th-sort.is-active .th-sort-arrows {
  opacity: 1;
  color: var(--primary, #7c6cf0);
}

.th-sort.is-asc .th-sort-up,
.th-sort.is-desc .th-sort-down {
  opacity: 1;
}

.th-sort.is-asc .th-sort-down,
.th-sort.is-desc .th-sort-up {
  opacity: 0.3;
}

.cust-area-cell {
  max-width: 14rem;
}

.cust-area-text {
  display: block;
  max-width: 14rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.customers-pager {
  margin-top: 0.55rem;
  padding: 0.55rem 0.7rem;
  border-radius: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.58);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.88));
  box-shadow: var(--shadow-control, 0 1px 2px rgba(76, 58, 160, 0.06));
  gap: 0.75rem;
}

.customers-pager-count {
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--muted, #6b6280);
}

.customers-pager-controls {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin: 0;
  padding: 0;
  background: none;
  border: 0;
  box-shadow: none;
}

.customers-pager-size {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0;
  height: var(--control-h, 2.6rem);
  color: var(--muted, #6b6280);
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.customers-pager-size select {
  box-sizing: border-box;
  height: var(--control-h, 2.6rem);
  min-height: var(--control-h, 2.6rem);
  max-height: var(--control-h, 2.6rem);
  min-width: 4.25rem;
  padding: 0 1.85rem 0 0.85rem;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.62);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(237, 231, 255, 0.42));
  background-image:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(237, 231, 255, 0.42)),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%237c6cf0' d='M1.2 1.4 6 6.2l4.8-4.8'/%3E%3C/svg%3E");
  background-repeat: no-repeat, no-repeat;
  background-position: 0 0, right 0.7rem center;
  background-size: auto, 0.65rem;
  box-shadow: var(--shadow-control, 0 1px 2px rgba(76, 58, 160, 0.06));
  -webkit-backdrop-filter: blur(12px) saturate(1.5);
  backdrop-filter: blur(12px) saturate(1.5);
  font: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--primary, #7c6cf0);
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
}

.customers-pager-controls .btn {
  margin: 0;
}

</style>
