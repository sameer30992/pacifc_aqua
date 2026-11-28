<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import CustomerSuggestField from '@/components/CustomerSuggestField.vue'
import HelperSuggestField from '@/components/HelperSuggestField.vue'
import IosDateRangeField from '@/components/IosDateRangeField.vue'
import PaAvatar from '@/components/PaAvatar.vue'
import { useCustomersStore } from '@/stores/customers'

const router = useRouter()
const customersStore = useCustomersStore()
const rows = ref([])
const total = ref(0)
const totalPages = ref(0)
const loading = ref(true)
const error = ref('')
const helpers = ref([])
const products = ref([])
let searchTimer = null

function todayIso() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function monthStartIso() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}-01`
}

const filters = reactive({
  from: monthStartIso(),
  to: todayIso(),
  q: '',
  status: '',
  helperId: '',
  page: 0,
})

const pageSize = 20

function inr(n) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(n || 0)
}

function productIcon(p) {
  if (!p?.imagePath) return '/assets/products/icon-jar-20l.png'
  return p.imagePath.startsWith('/') ? p.imagePath : `/${p.imagePath}`
}

function lineIcon(line) {
  const p = products.value.find((x) => x.id === line.productId)
    || products.value.find((x) => x.name?.toLowerCase() === line.productName?.toLowerCase())
  return productIcon(p)
}

function formatClock(d) {
  return d.toLocaleTimeString('en-IN', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  }).toLowerCase()
}

function formatWhen(v) {
  const d = new Date(v.visitAt)
  const day = d.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
  return { day, time: formatClock(d) }
}

function modeLabel(mode) {
  const map = {
    CASH: 'Cash',
    UPI: 'UPI',
    BANK: 'Bank',
    CREDIT: 'Credit',
  }
  return map[mode] || mode || '—'
}

function statusBadge(status) {
  if (status === 'PAID') return { text: 'Paid', cls: 'badge-paid' }
  if (status === 'PARTIAL') return { text: 'Partial', cls: 'badge-partial' }
  return { text: 'Pending', cls: 'badge-pending' }
}

function rowTone(status) {
  if (status === 'PARTIAL') return 'is-partial'
  if (status === 'PENDING') return 'is-pending'
  return ''
}

const pagerLabel = computed(() => {
  if (loading.value) return 'Loading P&S entries…'
  if (!total.value) return 'No P&S entries match filters'
  const start = filters.page * pageSize + 1
  const end = Math.min((filters.page + 1) * pageSize, total.value)
  return `Showing ${start}–${end} of ${total.value} P&S entries`
})

async function loadCatalog() {
  const [h, p] = await Promise.all([
    http.get('/employees/helpers'),
    http.get('/products', { params: { activeOnly: true } }),
  ])
  helpers.value = h.data || []
  products.value = p.data || []
}

function onCustomerQueryUpdate(v) {
  filters.q = v
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filters.page = 0
    load()
  }, 280)
}

function selectCustomer(c) {
  filters.q = c.name || c.phone || ''
  filters.page = 0
  load()
}

function clearCustomerSearch() {
  filters.q = ''
  filters.page = 0
  load()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await http.get('/visits', {
      params: {
        from: filters.from || undefined,
        to: filters.to || undefined,
        q: filters.q || undefined,
        status: filters.status || undefined,
        helperId: filters.helperId || undefined,
        page: filters.page,
        size: pageSize,
      },
    })
    rows.value = data.content || []
    total.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to load sales'
    rows.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
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

function openBilling(v) {
  router.push({ name: 'billing', query: { customerId: v.customerId } })
}

onMounted(async () => {
  await Promise.all([loadCatalog(), load()])
})
</script>

<template>
  <div class="screen active sales-page">
    <div class="ledger-head">
      <div>
        <h2>P&amp;S · Purchase &amp; sales</h2>
        <p class="muted">Replaces Excel P&amp;S · Given / Empty back · collections</p>
      </div>
      <div class="row-actions">
        <button type="button" class="btn btn-primary" @click="router.push({ name: 'billing' })">
          ＋ New visit
        </button>
      </div>
    </div>

    <div class="toolbar sales-toolbar">
      <label class="muted sales-toolbar-label">Find in big list</label>
      <IosDateRangeField
        v-model:from="filters.from"
        v-model:to="filters.to"
        aria-label="Date range"
        @change="applyFilters"
      />
      <CustomerSuggestField
        :model-value="filters.q"
        wrap-class="sales-search-wrap"
        input-class="sales-search"
        placeholder="Search name, phone, or area…"
        aria-label="Search customer"
        @update:model-value="onCustomerQueryUpdate"
        @select="selectCustomer"
        @clear="clearCustomerSearch"
        @enter="applyFilters"
      />
      <select v-model="filters.status" aria-label="Status" @change="applyFilters">
        <option value="">All statuses</option>
        <option value="PAID">Paid</option>
        <option value="PARTIAL">Partial</option>
        <option value="PENDING">Pending</option>
      </select>
      <HelperSuggestField
        v-model="filters.helperId"
        class="sales-helper-filter"
        wrap-class="sales-helper-filter-wrap"
        :helpers="helpers"
        placeholder="All helpers"
        aria-label="Helper"
        @change="applyFilters"
      />
    </div>

    <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>

    <div class="ledger-table-wrap">
      <table>
        <thead>
          <tr>
            <th>Date / Time</th>
            <th>Customer / Mobile</th>
            <th>Helper</th>
            <th>Products</th>
            <th>Amount</th>
            <th>Paid</th>
            <th>Balance</th>
            <th>Mode</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <template v-if="loading">
            <tr v-for="n in 6" :key="`ps-skel-${n}`" class="orders-skel-row" aria-hidden="true">
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
                <span class="person-cell">
                  <span class="skel-av skel-av-xs" />
                  <span class="skel-line skel-line-sm" />
                </span>
              </td>
              <td>
                <span class="order-prods">
                  <span class="skel-chip" />
                  <span class="skel-chip" />
                </span>
              </td>
              <td><span class="skel-line skel-line-sm" /></td>
              <td><span class="skel-line skel-line-sm" /></td>
              <td><span class="skel-line skel-line-sm" /></td>
              <td><span class="skel-line skel-line-xs" /></td>
              <td><span class="skel-pill" /></td>
              <td><span class="skel-line skel-line-sm" /></td>
            </tr>
          </template>
          <template v-else>
            <tr
              v-for="v in rows"
              :key="v.id"
              class="tap-row"
              :class="rowTone(v.paymentStatus)"
              @click="openBilling(v)"
            >
              <td>
                <span class="cell-stack">
                  <strong>{{ formatWhen(v).day }}</strong>
                  <small>{{ formatWhen(v).time }}</small>
                </span>
              </td>
              <td>
                <span
                  class="person-cell person-cell-stack"
                  :class="{
                    'is-solo-name': !v.customerPhone && !v.customerArea,
                  }"
                >
                  <PaAvatar :name="v.customerName" size="sm" />
                  <span class="cell-stack">
                    <strong>{{ v.customerName }}</strong>
                    <small v-if="v.customerPhone">{{ v.customerPhone }}</small>
                    <small v-else-if="v.customerArea" class="muted">{{ v.customerArea }}</small>
                  </span>
                </span>
              </td>
              <td>
                <span class="person-cell">
                  <PaAvatar :name="v.helperName || '—'" kind="user" size="sm" />
                  <span>{{ v.helperName || '—' }}</span>
                </span>
              </td>
              <td>
                <span class="order-prods">
                  <span
                    v-for="l in v.lines"
                    :key="`${v.id}-${l.productId}`"
                    class="prod-cell prod-cell-tip"
                    :data-tip="l.productName || 'Product'"
                    :title="l.productName || 'Product'"
                  >
                    <img :src="lineIcon(l)" :alt="l.productName || ''" />
                    <b>×{{ l.qtyDiya }}</b>
                  </span>
                  <span v-if="!v.lines?.length" class="muted">—</span>
                </span>
              </td>
              <td>{{ inr(v.amountTotal) }}</td>
              <td>{{ inr(v.amountPaid) }}</td>
              <td>{{ inr(v.balance) }}</td>
              <td>{{ modeLabel(v.paymentMode) }}</td>
              <td>
                <span class="badge" :class="statusBadge(v.paymentStatus).cls">
                  {{ statusBadge(v.paymentStatus).text }}
                </span>
              </td>
              <td>
                <button
                  type="button"
                  class="link-btn"
                  @click.stop="openBilling(v)"
                >
                  Billing
                </button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="10" class="muted" style="text-align:center;padding:1.2rem">
                No visits match filters
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <div class="ledger-pager">
      <span>{{ pagerLabel }}</span>
      <div class="row-actions">
        <button
          type="button"
          class="btn btn-ghost"
          :disabled="loading || filters.page <= 0"
          @click="prevPage"
        >
          ← Prev
        </button>
        <button
          type="button"
          class="btn btn-ghost"
          :disabled="loading || filters.page + 1 >= totalPages"
          @click="nextPage"
        >
          Next →
        </button>
      </div>
    </div>
  </div>
</template>
