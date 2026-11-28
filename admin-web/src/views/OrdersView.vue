<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import http from '@/api/http'
import PaAvatar from '@/components/PaAvatar.vue'
import AppConfirmDialog from '@/components/AppConfirmDialog.vue'
import CustomerSuggestField from '@/components/CustomerSuggestField.vue'
import HelperSuggestField from '@/components/HelperSuggestField.vue'
import IosDateRangeField from '@/components/IosDateRangeField.vue'
import IosSelectField from '@/components/IosSelectField.vue'
import VisitEntryModal from '@/components/VisitEntryModal.vue'
import { useCustomersStore } from '@/stores/customers'

const customersStore = useCustomersStore()
const allRows = ref([])
const loadingOrders = ref(true)
const helpers = ref([])
const products = ref([])
const error = ref('')
const fieldErrors = reactive({ customer: '', helper: '', products: '' })
const showForm = ref(false)
const editingOrderId = ref(null)
const openMenuId = ref(null)
const menuPos = ref({ top: 0, left: 0 })
const showDeliver = ref(false)
const deliverOrder = ref(null)
const deliverLines = ref([])
const deliverError = ref('')
const deliverForm = reactive({
  amountPaid: 0,
  paymentMode: 'CASH',
  remarks: '',
})
const filters = reactive({
  status: 'PENDING',
  dateFrom: '',
  dateTo: '',
  customer: '',
  helperId: '',
  productId: '',
})

const customerQ = ref('')
const selectedCustomer = ref(null)

const selectedHelper = ref(null)

const orderLines = ref([])
const showPicker = ref(false)
const pickSelected = ref(new Set())
let lineUid = 0

const form = reactive({
  priority: 'NORMAL',
  expectedHours: 2,
  notes: '',
})

const etaOptions = [
  { value: 1, label: '1 hour' },
  { value: 2, label: '2 hours' },
  { value: 3, label: '3 hours' },
  { value: 4, label: '4 hours' },
  { value: 6, label: '6 hours' },
  { value: 8, label: '8 hours' },
  { value: 12, label: '12 hours' },
  { value: 24, label: '24 hours' },
]

function inr(n) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(n || 0)
}

function productById(id) {
  return products.value.find((p) => p.id === id)
}

function productIcon(p) {
  if (!p?.imagePath) return '/assets/products/icon-jar-20l.png'
  return p.imagePath.startsWith('/') ? p.imagePath : `/${p.imagePath}`
}

const availableProducts = computed(() =>
  products.value.filter((p) => !orderLines.value.some((l) => l.productId === p.id)),
)

const addProductLabel = computed(() =>
  orderLines.value.length ? '＋ Add another product' : '＋ Add product',
)

const formTitle = computed(() => (editingOrderId.value ? 'Edit order' : 'New order'))
const formSaveLabel = computed(() => (editingOrderId.value ? 'Save changes' : 'Send to helper'))

const statusOptions = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'DELAYED', label: 'Delayed' },
  { value: '', label: 'All orders' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
]

const productOptions = computed(() => [
  { value: '', label: 'All products' },
  ...products.value.map((p) => ({
    value: p.id,
    label: p.name,
    sub: p.defaultRate != null ? `₹${p.defaultRate}` : '',
    image: productIcon(p),
  })),
])

const deliverCustomer = computed(() => {
  const o = deliverOrder.value
  if (!o) return null
  const c = customersStore.byId(o.customerId)
  return {
    name: c?.name || o.customerName,
    avatarPath: c?.avatarPath,
    due: o.customerDue,
    area: o.customerArea || c?.cityArea,
  }
})

const deliverHelper = computed(() => {
  const o = deliverOrder.value
  if (!o) return null
  const h = helpers.value.find((x) => x.id === o.helperId)
  return {
    name: h?.name || o.helperName,
    avatarPath: h?.avatarPath,
  }
})

const deliverMetaLine = computed(() => {
  const o = deliverOrder.value
  if (!o) return ''
  const when = formatWhen(o)
  return `Ordered · ${when.day} ${when.time}`
})

function orderDueAt(o) {
  const created = new Date(o.createdAt)
  const hrs = Math.max(1, Number(o.expectedHours) || 2)
  return new Date(created.getTime() + hrs * 3600000)
}

function isOrderDelayed(o) {
  if (o.status !== 'PENDING') return false
  return Date.now() > orderDueAt(o).getTime()
}

function formatClock(d) {
  return d.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true }).toLowerCase()
}

function formatWhen(o) {
  const d = new Date(o.createdAt)
  const day = d.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' })
  const time = formatClock(d)
  return { day, time }
}

function formatDelayOverdue(ms) {
  const mins = Math.max(1, Math.round(ms / 60000))
  if (mins < 60) return `${mins}m late`
  const hrs = Math.floor(mins / 60)
  const rem = mins % 60
  return rem ? `${hrs}h ${rem}m late` : `${hrs}h late`
}

function etaParts(o) {
  const hrs = Math.max(1, Number(o.expectedHours) || 2)
  const due = orderDueAt(o)
  const hrsLabel = hrs === 1 ? '1 hr' : `${hrs} hrs`
  if (o.status === 'PENDING' && isOrderDelayed(o)) {
    return { main: hrsLabel, sub: formatDelayOverdue(Date.now() - due.getTime()), delayed: true }
  }
  return { main: hrsLabel, sub: `by ${formatClock(due)}`, delayed: false }
}

function statusLabel(o) {
  if (o.status === 'DELIVERED') return { text: 'Delivered', cls: 'badge-paid' }
  if (o.status === 'CANCELLED') return { text: 'Cancelled', cls: 'badge-partial' }
  if (o.status === 'DELAYED' || isOrderDelayed(o)) return { text: 'Delayed', cls: 'badge-delayed' }
  return { text: 'Pending', cls: 'badge-pending' }
}

function lineIcon(line) {
  const p = products.value.find((x) => x.id === line.productId)
    || products.value.find((x) => x.name?.toLowerCase() === line.productName?.toLowerCase())
  return productIcon(p)
}

const filteredRows = computed(() => {
  const custQ = filters.customer.trim().toLowerCase()
  return allRows.value.filter((o) => {
    if (filters.status === 'DELAYED') {
      if (!isOrderDelayed(o)) return false
    } else if (filters.status && o.status !== filters.status) {
      return false
    }
    if (filters.helperId && String(o.helperId) !== String(filters.helperId)) return false
    if (filters.productId) {
      const pid = Number(filters.productId)
      if (!o.lines?.some((l) => l.productId === pid)) return false
    }
    if (custQ) {
      const hay = `${o.customerName || ''} ${o.customerPhone || ''}`.toLowerCase()
      if (!hay.includes(custQ)) return false
    }
    return true
  })
})

const ordersHint = computed(() => {
  if (loadingOrders.value) return 'Loading orders…'
  const n = filteredRows.value.length
  const map = {
    PENDING: 'pending orders',
    DELAYED: 'delayed orders',
    DELIVERED: 'delivered orders',
    CANCELLED: 'cancelled orders',
    '': 'all orders',
  }
  const label = map[filters.status] || 'orders'
  return n ? `Showing ${n} ${label}` : `No ${label} match filters`
})

async function load() {
  openMenuId.value = null
  loadingOrders.value = true
  const params = {}
  if (filters.status === 'DELAYED') {
    params.status = 'PENDING'
  } else if (filters.status) {
    params.status = filters.status
  }
  if (filters.dateFrom) params.from = filters.dateFrom
  if (filters.dateTo) params.to = filters.dateTo
  if (filters.helperId) params.helperId = filters.helperId
  try {
    const { data } = await http.get('/orders', { params })
    allRows.value = data || []
  } finally {
    loadingOrders.value = false
  }
}

const openMenuOrder = computed(() => {
  if (openMenuId.value == null) return null
  return allRows.value.find((o) => o.id === openMenuId.value) || null
})

function placeMenu(event) {
  const btn = event?.currentTarget
  if (!btn) return
  const r = btn.getBoundingClientRect()
  const panelW = 182
  const panelH = 148
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

async function loadCatalog() {
  const [h, p] = await Promise.all([
    http.get('/employees/helpers'),
    http.get('/products', { params: { activeOnly: true } }),
  ])
  helpers.value = h.data || []
  products.value = p.data || []
}

function selectCustomer(c) {
  selectedCustomer.value = c
  customerQ.value = ''
  fieldErrors.customer = ''
}

function onFilterCustomerSelect(c) {
  filters.customer = c.name || c.phone || ''
}

function clearCustomer() {
  selectedCustomer.value = null
  customerQ.value = ''
}

function selectHelper(h) {
  selectedHelper.value = h
  fieldErrors.helper = ''
}

function onHelperIdChange(id) {
  fieldErrors.helper = ''
  if (id === '' || id == null) {
    selectedHelper.value = null
    return
  }
  selectedHelper.value =
    helpers.value.find((x) => String(x.id) === String(id)) || selectedHelper.value
}

function clearHelper() {
  selectedHelper.value = null
}

function stepQty(line, delta) {
  line.qty = Math.max(1, Number(line.qty || 1) + delta)
}

function removeLine(uid) {
  orderLines.value = orderLines.value.filter((l) => l.uid !== uid)
}

function togglePicker() {
  if (!availableProducts.value.length) return
  showPicker.value = !showPicker.value
  if (showPicker.value) pickSelected.value = new Set()
}

function togglePick(id) {
  if (orderLines.value.some((l) => l.productId === id)) return
  const next = new Set(pickSelected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  pickSelected.value = next
}

function cancelPick() {
  pickSelected.value = new Set()
  showPicker.value = false
}

function confirmPick() {
  ;[...pickSelected.value].forEach((productId) => {
    if (orderLines.value.some((l) => l.productId === productId)) return
    orderLines.value.push({
      uid: `ol${++lineUid}`,
      productId,
      qty: 1,
    })
  })
  pickSelected.value = new Set()
  showPicker.value = false
  fieldErrors.products = ''
}

function resetOrderForm() {
  form.priority = 'NORMAL'
  form.expectedHours = 2
  form.notes = ''
  orderLines.value = []
  showPicker.value = false
  pickSelected.value = new Set()
  selectedCustomer.value = null
  selectedHelper.value = null
  customerQ.value = ''
  error.value = ''
  fieldErrors.customer = ''
  fieldErrors.helper = ''
  fieldErrors.products = ''
}

function openCreate() {
  closeMenus()
  editingOrderId.value = null
  resetOrderForm()
  showForm.value = true
}

function openEdit(o) {
  closeMenus()
  if (o.status !== 'PENDING') return
  editingOrderId.value = o.id
  resetOrderForm()

  const cust = customersStore.byId(o.customerId)
  selectedCustomer.value = cust || {
    id: o.customerId,
    name: o.customerName,
    phone: o.customerPhone,
    cityArea: o.customerArea,
    outstanding: o.customerDue,
  }
  customersStore.ensureLoaded().then(() => {
    const fresh = customersStore.byId(o.customerId)
    if (fresh && selectedCustomer.value?.id === o.customerId) selectedCustomer.value = fresh
  })
  customerQ.value = ''

  const helper = helpers.value.find((h) => h.id === o.helperId)
  selectedHelper.value = helper || {
    id: o.helperId,
    name: o.helperName,
  }

  form.priority = o.priority || 'NORMAL'
  form.expectedHours = o.expectedHours || 2
  form.notes = o.notes || ''
  orderLines.value = (o.lines || []).map((l) => ({
    uid: `ol${++lineUid}`,
    productId: l.productId,
    qty: l.qty,
  }))
  showForm.value = true
}

function closeForm() {
  showForm.value = false
  editingOrderId.value = null
  showPicker.value = false
}

async function save() {
  error.value = ''
  fieldErrors.customer = ''
  fieldErrors.helper = ''
  fieldErrors.products = ''

  let ok = true
  if (!selectedCustomer.value?.id) {
    fieldErrors.customer = 'Select a customer'
    ok = false
  }
  if (!selectedHelper.value?.id) {
    fieldErrors.helper = 'Choose a helper'
    ok = false
  }
  const lines = orderLines.value
    .filter((l) => l.productId && l.qty > 0)
    .map((l) => ({ productId: l.productId, qty: l.qty }))
  if (!lines.length) {
    fieldErrors.products = 'Add at least one product'
    ok = false
  }
  if (!ok) {
    error.value = 'Fix the highlighted fields'
    return
  }

  const payload = {
    customerId: selectedCustomer.value.id,
    helperId: selectedHelper.value.id,
    priority: form.priority,
    expectedHours: form.expectedHours,
    notes: form.notes,
    lines,
  }

  try {
    if (editingOrderId.value) {
      await http.put(`/orders/${editingOrderId.value}`, payload)
    } else {
      await http.post('/orders', payload)
    }
    closeForm()
    await load()
  } catch (e) {
    error.value = e.response?.data?.message
      || (editingOrderId.value ? 'Failed to update order' : 'Failed to create order')
  }
}

const confirmState = reactive({
  open: false,
  title: 'Confirm',
  message: '',
  confirmLabel: 'Confirm',
  cancelLabel: 'Cancel',
  danger: false,
})
let confirmResolve = null
let pendingCancelOrder = null

function askConfirm(opts) {
  confirmState.title = opts.title || 'Confirm'
  confirmState.message = opts.message || ''
  confirmState.confirmLabel = opts.confirmLabel || 'Confirm'
  confirmState.cancelLabel = opts.cancelLabel || 'Cancel'
  confirmState.danger = !!opts.danger
  confirmState.open = true
  return new Promise((resolve) => {
    confirmResolve = resolve
  })
}

function closeConfirm(result) {
  confirmState.open = false
  const resolve = confirmResolve
  confirmResolve = null
  if (resolve) resolve(!!result)
}

async function cancel(o) {
  closeMenus()
  pendingCancelOrder = o
  const ok = await askConfirm({
    title: 'Cancel order?',
    message: `Cancel order for ${o.customerName}?\nThis cannot be undone.`,
    confirmLabel: 'Cancel order',
    cancelLabel: 'Keep order',
    danger: true,
  })
  const target = pendingCancelOrder
  pendingCancelOrder = null
  if (!ok || !target) return
  await http.post(`/orders/${target.id}/cancel`)
  if (editingOrderId.value === target.id) closeForm()
  if (deliverOrder.value?.id === target.id) closeDeliver()
  await load()
}

function openDeliver(o) {
  closeMenus()
  if (o.status !== 'PENDING') return
  deliverOrder.value = o
  deliverError.value = ''
  deliverForm.paymentMode = 'CASH'
  deliverForm.remarks = ''
  deliverLines.value = (o.lines || []).map((l) => ({
    uid: `dv${++lineUid}`,
    productId: l.productId,
    qtyDiya: l.qty,
    qtyMila: 0,
  }))
  deliverForm.amountPaid = deliverLines.value.reduce((sum, line) => {
    const rate = Number(productById(line.productId)?.defaultRate) || 0
    return sum + rate * Math.max(0, Number(line.qtyDiya) || 0)
  }, 0)
  showDeliver.value = true
}

function closeDeliver() {
  showDeliver.value = false
  deliverOrder.value = null
  deliverLines.value = []
  deliverError.value = ''
}

async function saveDeliver() {
  deliverError.value = ''
  const o = deliverOrder.value
  if (!o) return
  const lines = deliverLines.value
    .filter((l) => l.productId && ((Number(l.qtyDiya) || 0) > 0 || (Number(l.qtyMila) || 0) > 0))
    .map((l) => ({
      productId: l.productId,
      qtyDiya: Math.max(0, Number(l.qtyDiya) || 0),
      qtyMila: Math.max(0, Number(l.qtyMila) || 0),
    }))
  if (!lines.length) {
    deliverError.value = 'Add at least one product with Given or Empty qty'
    return
  }
  try {
    await http.post(`/orders/${o.id}/deliver`, {
      lines,
      amountPaid: Number(deliverForm.amountPaid) || 0,
      paymentMode: deliverForm.paymentMode,
      remarks: deliverForm.remarks || null,
    })
    closeDeliver()
    customersStore.refreshIfStale({ force: true }).catch(() => {})
    await load()
  } catch (e) {
    deliverError.value = e.response?.data?.message || 'Failed to mark delivered'
  }
}

function onDocClick(e) {
  if (e?.target?.closest?.('.order-menu, .order-menu-panel')) return
  closeMenus()
}

onMounted(async () => {
  document.addEventListener('click', onDocClick)
  window.addEventListener('scroll', closeMenus, true)
  window.addEventListener('resize', closeMenus)
  // Orders list should not wait on the heavy /customers catalog (only needed for New order suggest).
  await Promise.all([loadCatalog(), load()])
})

onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  window.removeEventListener('scroll', closeMenus, true)
  window.removeEventListener('resize', closeMenus)
})
</script>

<template>
  <div class="screen active orders-page">
    <AppConfirmDialog
      :open="confirmState.open"
      :title="confirmState.title"
      :message="confirmState.message"
      :confirm-label="confirmState.confirmLabel"
      :cancel-label="confirmState.cancelLabel"
      :danger="confirmState.danger"
      @confirm="closeConfirm(true)"
      @cancel="closeConfirm(false)"
    />

    <div class="ledger-head orders-head">
      <div>
        <h2>Orders</h2>
        <p class="muted">Pending first · notify helper on mobile</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">＋ New order</button>
    </div>

    <div class="toolbar orders-toolbar">
      <IosSelectField
        v-model="filters.status"
        class="orders-filter-select"
        :options="statusOptions"
        aria-label="Status filter"
        placeholder="Status"
        @change="load"
      />
      <IosDateRangeField
        v-model:from="filters.dateFrom"
        v-model:to="filters.dateTo"
        class="orders-filter-dates"
        aria-label="Filter by date range"
        placeholder="Select dates"
        @change="load"
      />
      <CustomerSuggestField
        v-model="filters.customer"
        wrap-class="orders-filter-customer-wrap"
        placeholder="Customer…"
        aria-label="Filter by customer"
        @select="onFilterCustomerSelect"
      />
      <HelperSuggestField
        v-model="filters.helperId"
        class="orders-filter-select"
        wrap-class="orders-filter-helper-wrap"
        :helpers="helpers"
        placeholder="All helpers"
        aria-label="Helper filter"
        @change="load"
      />
      <IosSelectField
        v-model="filters.productId"
        class="orders-filter-select"
        searchable
        :options="productOptions"
        aria-label="Product filter"
        placeholder="All products"
      />
    </div>
    <p class="muted orders-hint">{{ ordersHint }}</p>

    <div class="table-wrap">
      <table class="data-table orders-table">
        <thead>
          <tr>
            <th>When</th>
            <th>Customer</th>
            <th>Products</th>
            <th>Helper</th>
            <th>ETA</th>
            <th>Priority</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <template v-if="loadingOrders">
            <tr v-for="n in 6" :key="`order-skel-${n}`" class="orders-skel-row" aria-hidden="true">
              <td><span class="skel-line skel-line-sm" /><span class="skel-line skel-line-xs" /></td>
              <td>
                <span class="person-cell">
                  <span class="skel-av" />
                  <span class="skel-text">
                    <span class="skel-line skel-line-lg" />
                    <span class="skel-line skel-line-sm" />
                  </span>
                </span>
              </td>
              <td>
                <span class="order-prods">
                  <span class="skel-chip" />
                  <span class="skel-chip" />
                </span>
              </td>
              <td>
                <span class="person-cell">
                  <span class="skel-av skel-av-xs" />
                  <span class="skel-line skel-line-sm" />
                </span>
              </td>
              <td><span class="skel-line skel-line-sm" /><span class="skel-line skel-line-xs" /></td>
              <td><span class="skel-pill" /></td>
              <td><span class="skel-pill" /></td>
              <td><span class="skel-dot" /></td>
            </tr>
          </template>
          <template v-else>
          <tr
            v-for="o in filteredRows"
            :key="o.id"
            class="tap-row"
            :class="{
              'is-urgent': o.priority === 'URGENT',
              'is-delayed': isOrderDelayed(o),
            }"
          >
            <td>
              <span class="cell-stack">
                <strong>{{ formatWhen(o).day }}</strong>
                <small>{{ formatWhen(o).time }}</small>
              </span>
            </td>
            <td>
              <span
                class="person-cell person-cell-stack"
                :class="{
                  'is-solo-name': !o.customerPhone && !o.customerArea && !o.notes,
                }"
              >
                <PaAvatar :name="o.customerName" size="sm" />
                <span class="cell-stack">
                  <strong>{{ o.customerName }}</strong>
                  <small v-if="o.customerPhone">{{ o.customerPhone }}</small>
                  <small v-else-if="o.customerArea" class="muted">{{ o.customerArea }}</small>
                  <small v-if="o.notes" class="muted">{{ o.notes }}</small>
                </span>
              </span>
            </td>
            <td>
              <span class="order-prods">
                <span
                  v-for="l in o.lines"
                  :key="`${o.id}-${l.productId}`"
                  class="prod-cell prod-cell-tip"
                  :data-tip="l.productName || 'Product'"
                  :title="l.productName || 'Product'"
                  :aria-label="`${l.productName || 'Product'} ×${l.qty}`"
                >
                  <img :src="lineIcon(l)" :alt="l.productName || ''" />
                  <b>×{{ l.qty }}</b>
                </span>
                <span v-if="!o.lines?.length" class="muted">—</span>
              </span>
            </td>
            <td>
              <span class="person-cell">
                <PaAvatar :name="o.helperName" kind="user" size="sm" />
                <span>{{ o.helperName }}</span>
              </span>
            </td>
            <td>
              <span class="cell-stack">
                <strong :class="{ 'eta-delayed-text': etaParts(o).delayed }">{{ etaParts(o).main }}</strong>
                <small :class="{ 'eta-delayed-text': etaParts(o).delayed }">{{ etaParts(o).sub }}</small>
              </span>
            </td>
            <td>
              <span
                class="badge"
                :class="o.priority === 'URGENT' ? 'badge-urgent' : 'badge-normal'"
              >
                {{ o.priority === 'URGENT' ? 'Urgent' : 'Normal' }}
              </span>
            </td>
            <td>
              <span class="badge" :class="statusLabel(o).cls">{{ statusLabel(o).text }}</span>
            </td>
            <td class="order-menu-cell">
              <div v-if="o.status === 'PENDING'" class="order-menu" @click.stop>
                <button
                  type="button"
                  class="order-menu-btn"
                  aria-label="Order actions"
                  :aria-expanded="openMenuId === o.id ? 'true' : 'false'"
                  @click="toggleMenu(o.id, $event)"
                >
                  <span class="order-menu-dots" aria-hidden="true" />
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!filteredRows.length">
            <td colspan="8" class="muted" style="text-align:center;padding:1.2rem">No orders match</td>
          </tr>
          </template>
        </tbody>
      </table>
    </div>

    <Teleport to="body">
      <div v-if="showForm" class="bill-modal new-order-modal">
        <div class="bill-modal-backdrop" @click="closeForm" />
        <div
          class="bill-modal-panel new-order-panel"
          role="dialog"
          aria-modal="true"
          aria-labelledby="newOrderTitle"
        >
          <div class="admin-form new-order-form">
            <div class="new-order-modal-head">
              <h2 id="newOrderTitle" class="screen-title">{{ formTitle }}</h2>
              <button type="button" class="modal-close-btn" aria-label="Close" @click="closeForm">×</button>
            </div>
            <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>

      <!-- Customer search + selected details (same box) -->
      <div
        class="sale-meta-row order-customer-row"
        :class="{ 'has-selected': !!selectedCustomer }"
      >
        <div
          class="sale-field grow form-field"
          :class="{ 'has-error': !!fieldErrors.customer }"
        >
          <CustomerSuggestField
            v-model="customerQ"
            label="Customer *"
            placeholder="Search name, phone, or area…"
            aria-label="Search customer"
            @select="selectCustomer"
            @update:model-value="fieldErrors.customer = ''"
          />
          <p v-if="fieldErrors.customer" class="field-error">{{ fieldErrors.customer }}</p>

          <div
            v-if="selectedCustomer"
            class="cust-profile order-cust-profile"
          >
            <PaAvatar :name="selectedCustomer.name" :photo="selectedCustomer.avatarPath" size="lg" />
            <div class="cust-profile-main">
              <div class="cust-profile-top">
                <strong>{{ selectedCustomer.name }}</strong>
                <span class="cust-chip">Card · {{ selectedCustomer.cardNo || '—' }}</span>
                <button type="button" class="btn btn-ghost btn-sm" @click="clearCustomer">Change</button>
              </div>
              <div class="cust-profile-lines">
                <a
                  v-if="selectedCustomer.phone"
                  class="cust-line cust-phone"
                  :href="`tel:${selectedCustomer.phone}`"
                >
                  <span class="cust-ico" aria-hidden="true">☎</span>
                  <span>{{ selectedCustomer.phone }}</span>
                </a>
                <span v-else class="cust-line muted">
                  <span class="cust-ico" aria-hidden="true">☎</span>
                  <span>No contact</span>
                </span>
                <span class="cust-line" :class="{ muted: !selectedCustomer.cityArea }">
                  <span class="cust-ico" aria-hidden="true">⌂</span>
                  <span>{{ selectedCustomer.cityArea || 'No address' }}</span>
                </span>
              </div>
            </div>
            <div class="cust-profile-due">
              <span>Due</span>
              <strong>{{ inr(selectedCustomer.outstanding) }}</strong>
            </div>
          </div>
          <p v-else class="muted order-cust-hint">Search and select a customer</p>
        </div>
      </div>

      <!-- Products · mockup multi-picker -->
      <div class="field field-tight order-products-block">
        <div class="dm-head">
          <label style="margin:0">Products</label>
          <span class="dm-bill">Lines <span>{{ orderLines.length }}</span></span>
        </div>

        <div v-if="orderLines.length" class="dm-cols order-dm-cols">
          <span class="dm-col-label dm-col-prod"><strong>Product</strong></span>
          <span class="dm-col-label"><strong>Qty</strong></span>
          <span class="dm-col-del" aria-hidden="true" />
        </div>

        <div
          v-for="line in orderLines"
          :key="line.uid"
          class="dm-row order-prod-line"
        >
          <div class="dm-prod">
            <span class="dm-prod-media">
              <img class="mobile-prod-icon" :src="productIcon(productById(line.productId))" alt="" />
            </span>
            <span class="dm-prod-name">
              {{ productById(line.productId)?.name || '—' }}
              <small>₹{{ productById(line.productId)?.defaultRate ?? 0 }}</small>
            </span>
          </div>
          <div class="dm-qty">
            <div class="stepper">
              <button type="button" @click="stepQty(line, -1)">−</button>
              <input v-model.number="line.qty" type="number" min="1" :aria-label="`${productById(line.productId)?.name || 'Product'} qty`" />
              <button type="button" @click="stepQty(line, 1)">+</button>
            </div>
          </div>
          <button
            type="button"
            class="line-remove"
            title="Remove product"
            @click="removeLine(line.uid)"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"
              />
            </svg>
          </button>
        </div>

        <p v-if="!orderLines.length" class="add-bill-empty">No products yet · tap below to add</p>
        <p v-if="fieldErrors.products" class="field-error">{{ fieldErrors.products }}</p>

        <button
          v-if="availableProducts.length"
          type="button"
          class="btn btn-ghost btn-sm add-prod-btn"
          @click="togglePicker"
        >
          {{ addProductLabel }}
        </button>

        <div v-if="showPicker" class="prod-picker">
          <div class="prod-picker-head">
            <p class="prod-picker-label">Select products</p>
            <span class="prod-picker-count">{{ pickSelected.size }} selected</span>
          </div>
          <div class="prod-picker-grid">
            <button
              v-for="p in products"
              :key="p.id"
              type="button"
              class="prod-pick"
              :class="{
                'is-selected': pickSelected.has(p.id),
                'is-used': orderLines.some((l) => l.productId === p.id),
              }"
              :disabled="orderLines.some((l) => l.productId === p.id)"
              @click="togglePick(p.id)"
            >
              <span class="prod-pick-check" aria-hidden="true">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    fill="none"
                    stroke="currentColor"
                    stroke-width="3"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              </span>
              <img class="mobile-prod-icon" :src="productIcon(p)" alt="" />
              <strong>{{ p.name }}</strong>
              <span>₹{{ p.defaultRate }}</span>
            </button>
          </div>
          <div class="prod-picker-actions">
            <button type="button" class="btn btn-ghost btn-sm" @click="cancelPick">Cancel</button>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              :disabled="!pickSelected.size"
              @click="confirmPick"
            >
              Add selected
            </button>
          </div>
        </div>
      </div>

            <!-- Priority · Helper · ETA · Notes -->
            <div class="form-grid two order-form-grid">
              <div class="form-field">
                <label>Priority</label>
                <select v-model="form.priority">
                  <option value="NORMAL">Normal</option>
                  <option value="URGENT">Urgent</option>
                </select>
              </div>

              <div class="form-field">
                <label>Expected delivery</label>
                <select v-model.number="form.expectedHours">
                  <option v-for="o in etaOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
                </select>
              </div>

              <div
                class="form-field span-2"
                :class="{ 'has-error': !!fieldErrors.helper }"
              >
                <HelperSuggestField
                  :model-value="selectedHelper?.id ?? ''"
                  :helpers="helpers"
                  :include-all="false"
                  label="Helper *"
                  placeholder="Search helper…"
                  aria-label="Helper"
                  wrap-class="order-form-helper-wrap"
                  @update:model-value="onHelperIdChange"
                  @select="(h) => h && selectHelper(h)"
                  @clear="clearHelper"
                />
                <p v-if="fieldErrors.helper" class="field-error">{{ fieldErrors.helper }}</p>
              </div>

              <div class="form-field span-2">
                <label>Notes</label>
                <input
                  v-model="form.notes"
                  type="text"
                  placeholder="Optional · deliver today, gate code…"
                />
              </div>
            </div>

            <div class="form-actions">
              <button type="button" class="btn btn-ghost" @click="closeForm">Cancel</button>
              <button type="button" class="btn btn-primary" @click="save">{{ formSaveLabel }}</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="openMenuOrder"
        class="order-menu-panel order-menu-panel-float"
        role="menu"
        :style="{ top: `${menuPos.top}px`, left: `${menuPos.left}px` }"
        @click.stop
      >
        <button type="button" role="menuitem" @click="openEdit(openMenuOrder)">
          <span class="omi omi-edit" aria-hidden="true" />
          Edit order
        </button>
        <button type="button" role="menuitem" class="is-danger" @click="cancel(openMenuOrder)">
          <span class="omi omi-cancel" aria-hidden="true" />
          Cancel order
        </button>
        <button type="button" role="menuitem" @click="openDeliver(openMenuOrder)">
          <span class="omi omi-delivered" aria-hidden="true" />
          Mark delivered
        </button>
      </div>
    </Teleport>

    <VisitEntryModal
      :open="showDeliver"
      title="Mark delivered"
      title-id="deliverOrderTitle"
      submit-label="Mark delivered"
      products-label="Delivered · Given / Empty"
      hint="Ordered qty pre-filled · add extra products if customer bought more"
      :products="products"
      :customer="deliverCustomer"
      :helper="deliverHelper"
      :meta-line="deliverMetaLine"
      :error="deliverError"
      v-model:lines="deliverLines"
      v-model:amount-paid="deliverForm.amountPaid"
      v-model:payment-mode="deliverForm.paymentMode"
      v-model:remarks="deliverForm.remarks"
      @close="closeDeliver"
      @submit="saveDeliver"
    />
  </div>
</template>
