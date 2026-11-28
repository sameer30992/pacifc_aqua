<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import html2pdf from 'html2pdf.js'
import http from '@/api/http'
import AppConfirmDialog from '@/components/AppConfirmDialog.vue'
import CustomerSuggestField from '@/components/CustomerSuggestField.vue'
import PaAvatar from '@/components/PaAvatar.vue'
import VisitEntryModal from '@/components/VisitEntryModal.vue'
import { useCustomersStore } from '@/stores/customers'

const customersStore = useCustomersStore()

const GST_RATE = 0.05 // packaged drinking water HSN 2201 · 5%
const HSN_CODE = '2201'

const route = useRoute()
const q = ref('')
const selected = ref(null)
const ledger = ref(null)
const bill = ref(null)
const products = ref([])
const helpers = ref([])
const error = ref('')
const showAdd = ref(false)
const showBillPreview = ref(false)
const billWithSeal = ref(false)
const billPreviewBusy = ref(false)
const viewAllDues = ref(false)

const period = reactive({
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear(),
})

const confirmState = reactive({
  open: false,
  title: 'Confirm',
  message: '',
  confirmLabel: 'OK',
  cancelLabel: 'Close',
  danger: false,
})
let confirmResolve = null

const visitForm = reactive({
  helperId: null,
  lines: [],
  amountPaid: 0,
  paymentMode: 'CASH',
  remarks: '',
})

const monthNames = [
  '', 'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

/** Short header for ledger columns (e.g. "20 Ltr Jar" → "Jar", "500 ml" → "500"). */
function shortProductHeader(name) {
  const n = String(name || '').trim()
  if (!n) return '—'
  const lower = n.toLowerCase()
  if (lower.includes('chilled')) return 'Chilled'
  if (lower.includes('pouch')) return 'Pouch'
  if (lower.includes('dispenser')) return 'Dispenser'
  if (/\b20\b/.test(lower) && lower.includes('jar')) return 'Jar'
  if (lower.includes('1 ltr') || lower.includes('1ltr')) return '1 Ltr'
  if (lower.includes('2 ltr') || lower.includes('2ltr')) return '2 Ltr'
  if (lower.includes('500')) return '500'
  if (lower.includes('250')) return '250'
  return n.length > 10 ? n.slice(0, 9) + '…' : n
}

/** Dynamic product columns from catalog + any names that appear on ledger lines. */
const productCols = computed(() => {
  const cols = []
  const seen = new Set()
  for (const p of products.value || []) {
    const name = String(p.name || '').trim()
    if (!name) continue
    const key = name.toLowerCase()
    if (seen.has(key)) continue
    seen.add(key)
    cols.push({
      key: `p${p.id}`,
      name,
      header: shortProductHeader(name),
    })
  }
  for (const line of ledger.value?.lines || []) {
    for (const name of Object.keys(line.givenByProduct || {})) {
      const key = String(name).toLowerCase()
      if (!name || seen.has(key)) continue
      seen.add(key)
      cols.push({
        key: `x-${key}`,
        name,
        header: shortProductHeader(name),
      })
    }
  }
  return cols
})

const ledgerColCount = computed(() => 5 + productCols.value.length)

const periodLabel = computed(() =>
  viewAllDues.value
    ? 'Customer DUES · all unpaid visits'
    : `Customer BILL · ${monthNames[period.month]} ${period.year}`,
)

/** Lines with running previous-due (balance before that visit). */
const ledgerRows = computed(() => {
  const lines = ledger.value?.lines || []
  let running = 0
  return lines.map((line) => {
    const prevDue = running
    running += Number(line.balance || 0)
    return { ...line, prevDue }
  })
})

const visitCustomer = computed(() => {
  if (!selected.value) return null
  return {
    name: selected.value.name,
    avatarPath: selected.value.avatarPath,
    due: selected.value.outstanding,
    area: selected.value.cityArea,
  }
})

function inr(n) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(n || 0)
}

function qtyFor(line, col) {
  const map = line.givenByProduct || {}
  if (!col?.name) return 0
  if (map[col.name] != null) return map[col.name]
  const want = col.name.toLowerCase()
  for (const [name, qty] of Object.entries(map)) {
    if (String(name).toLowerCase() === want) return qty
  }
  return 0
}

async function selectCustomer(c) {
  q.value = ''
  selected.value = c
  // If customer has outstanding, show All dues so unpaid rows are visible
  viewAllDues.value = Number(c.outstanding || 0) > 0
  await loadLedger()
}

async function loadLedger() {
  if (!selected.value) return
  const { data } = await http.get(`/billing/customers/${selected.value.id}/ledger`, {
    params: {
      year: period.year,
      month: viewAllDues.value ? 0 : period.month,
    },
  })
  ledger.value = data
  bill.value = null
}

function askConfirm(opts) {
  confirmState.title = opts.title || 'Confirm'
  confirmState.message = opts.message || ''
  confirmState.confirmLabel = opts.confirmLabel || 'OK'
  confirmState.cancelLabel = opts.cancelLabel || 'Close'
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

function openAdd() {
  visitForm.lines = []
  visitForm.amountPaid = 0
  visitForm.paymentMode = 'CASH'
  visitForm.remarks = ''
  visitForm.helperId = helpers.value[0]?.id || null
  error.value = ''
  showAdd.value = true
}

function closeAdd() {
  showAdd.value = false
}

/** Aggregate product qty for Indian tax-invoice lines (payments stay in summary). */
const billSheetLines = computed(() => {
  if (!bill.value?.ledger?.lines) return []
  const byName = new Map()
  for (const line of bill.value.ledger.lines) {
    const map = line.givenByProduct || {}
    for (const [name, qty] of Object.entries(map)) {
      const q = Number(qty) || 0
      if (!q) continue
      const product = products.value.find(
        (p) => p.name.toLowerCase() === String(name).toLowerCase(),
      )
      const rate = Number(product?.defaultRate ?? 0)
      const prev = byName.get(name) || { description: name, qty: 0, rate, hsn: HSN_CODE }
      prev.qty += q
      prev.rate = rate
      byName.set(name, prev)
    }
  }
  return [...byName.values()].map((row, i) => {
    const amount = row.rate * row.qty
    // Rates treated as GST-inclusive MRP (common for packaged water retail)
    const taxable = amount / (1 + GST_RATE)
    return {
      nos: i + 1,
      hsn: row.hsn,
      description: row.description,
      qty: row.qty,
      rate: row.rate,
      amount,
      taxable,
      cgst: taxable * (GST_RATE / 2),
      sgst: taxable * (GST_RATE / 2),
    }
  })
})

const billPaidTotal = computed(() =>
  Number(bill.value?.ledger?.totalPaid || 0),
)

const billTaxableTotal = computed(() =>
  billSheetLines.value.reduce((s, r) => s + r.taxable, 0),
)

const billCgstTotal = computed(() =>
  billSheetLines.value.reduce((s, r) => s + r.cgst, 0),
)

const billSgstTotal = computed(() =>
  billSheetLines.value.reduce((s, r) => s + r.sgst, 0),
)

const billGrossTotal = computed(() =>
  billSheetLines.value.reduce((s, r) => s + r.amount, 0),
)

const billBalanceDue = computed(() =>
  Math.max(0, billGrossTotal.value - billPaidTotal.value),
)

const billDateLabel = computed(() =>
  new Date().toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }),
)

const billPreviewTitle = computed(() =>
  billWithSeal.value ? 'Download bill' : 'Print bill',
)

const billSealHint = computed(() =>
  billWithSeal.value
    ? 'Download · PDF with company seal'
    : 'Print · A4 tax invoice',
)

const billActionLabel = computed(() =>
  billWithSeal.value ? 'Download PDF' : 'Print now',
)

function money(n) {
  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(n) || 0)
}

async function ensureBillPreview() {
  if (!selected.value) return null
  billPreviewBusy.value = true
  try {
    const { data } = await http.post('/billing/generate', {
      customerId: selected.value.id,
      year: period.year,
      month: period.month,
    })
    bill.value = data
    return data
  } finally {
    billPreviewBusy.value = false
  }
}

async function openBillPreview(withSeal) {
  if (!selected.value) return
  if (viewAllDues.value) {
    const ok = await askConfirm({
      title: 'Select a month',
      message:
        'Print and download need a calendar month.\nTurn off the All dues switch, pick Month and Year, then try again.',
      confirmLabel: 'OK',
      cancelLabel: 'Close',
    })
    if (ok) viewAllDues.value = false
    return
  }
  error.value = ''
  billWithSeal.value = !!withSeal
  await ensureBillPreview()
  showBillPreview.value = true
}

function closeBillPreview() {
  showBillPreview.value = false
}

function runBillAction() {
  if (billWithSeal.value) {
    downloadBillPdf()
    return
  }
  document.body.classList.add('printing-bill')
  // Defer so print CSS applies before dialog opens
  requestAnimationFrame(() => {
    window.print()
    setTimeout(() => document.body.classList.remove('printing-bill'), 500)
  })
}

async function downloadBillPdf() {
  const el = document.getElementById('billSheet')
  if (!el || !bill.value) return
  billWithSeal.value = true
  await nextTick()
  // Wait for logo / QR images so PDF is not blank
  const imgs = [...el.querySelectorAll('img')]
  await Promise.all(
    imgs.map(
      (img) =>
        img.complete
          ? Promise.resolve()
          : new Promise((resolve) => {
              img.onload = resolve
              img.onerror = resolve
            }),
    ),
  )
  const safeNo = String(bill.value.billNo || 'bill').replace(/[^\w.-]+/g, '-')
  await html2pdf()
    .set({
      margin: [8, 8, 8, 8],
      filename: `Pacific-Aqua-${safeNo}.pdf`,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
        logging: false,
      },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['avoid-all'] },
    })
    .from(el)
    .save()
}

async function addVisit() {
  error.value = ''
  try {
    const lines = visitForm.lines
      .filter((l) => l.productId && (l.qtyDiya > 0 || l.qtyMila > 0))
      .map((l) => ({
        productId: l.productId,
        qtyDiya: l.qtyDiya,
        qtyMila: l.qtyMila,
      }))
    if (!lines.length && !(visitForm.amountPaid > 0)) {
      error.value = 'Add at least one product or payment'
      return
    }
    if (!lines.length) {
      error.value = 'Add at least one product qty'
      return
    }
    await http.post('/visits', {
      customerId: selected.value.id,
      helperId: visitForm.helperId,
      lines,
      amountPaid: visitForm.amountPaid,
      paymentMode: visitForm.paymentMode,
      remarks: visitForm.remarks,
    })
    closeAdd()
    await loadLedger()
    const { data } = await http.get(`/customers/${selected.value.id}`)
    selected.value = data
    customersStore.upsert(data)
    customersStore.refreshIfStale({ force: true }).catch(() => {})
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to add bill line'
  }
}

watch(
  () => [period.month, period.year],
  () => {
    if (viewAllDues.value) viewAllDues.value = false
    else loadLedger()
  },
)
watch(viewAllDues, loadLedger)

onMounted(async () => {
  const [p, h] = await Promise.all([
    http.get('/products', { params: { activeOnly: true } }),
    http.get('/employees/helpers'),
    customersStore.ensureLoaded(),
  ])
  products.value = p.data
  helpers.value = h.data
  if (route.query.customerId) {
    const { data } = await http.get(`/customers/${route.query.customerId}`)
    await selectCustomer(data)
  }
})
</script>

<template>
  <div class="screen active sale-edit-dense bill-page">
    <div class="sale-edit-top">
      <h2>Billing</h2>
      <div v-if="selected" class="bill-top-actions">
        <button
          type="button"
          class="btn btn-ghost btn-sm"
          :disabled="billPreviewBusy"
          @click="openBillPreview(false)"
        >
          Print
        </button>
        <button
          type="button"
          class="btn btn-primary btn-sm"
          :disabled="billPreviewBusy"
          @click="openBillPreview(true)"
        >
          Download
        </button>
      </div>
    </div>

    <div class="sale-meta-row">
      <div class="sale-field grow">
        <CustomerSuggestField
          v-model="q"
          label="Search customer"
          placeholder="Type name, phone or address…"
          aria-label="Search customer"
          @select="selectCustomer"
        />
      </div>
    </div>

    <p v-if="!selected" class="bill-empty-hint">Search and select a customer to view billing.</p>

    <div v-if="selected" id="billSelectedPanel">
      <div class="cust-profile">
        <PaAvatar :name="selected.name" :photo="selected.avatarPath" size="lg" class="cust-avatar" />
        <div class="cust-profile-main">
          <div class="cust-profile-top">
            <strong>{{ selected.name }}</strong>
            <span class="cust-chip">Card · {{ selected.cardNo || '—' }}</span>
          </div>
          <div class="cust-profile-lines">
            <a v-if="selected.phone" class="cust-line cust-phone" :href="`tel:${selected.phone}`">
              <span class="cust-ico" aria-hidden="true">☎</span>
              <span>{{ selected.phone }}</span>
            </a>
            <span v-else class="cust-line muted">
              <span class="cust-ico" aria-hidden="true">☎</span>
              <span>No contact</span>
            </span>
            <span class="cust-line" :class="{ muted: !selected.cityArea }">
              <span class="cust-ico" aria-hidden="true">⌂</span>
              <span>{{ selected.cityArea || 'No address' }}</span>
            </span>
          </div>
        </div>
        <div class="cust-profile-due">
          <span>Due</span>
          <strong>{{ inr(selected.outstanding) }}</strong>
        </div>
      </div>

      <div class="bill-ledger-panel">
        <div class="sale-qty-head bill-ledger-toolbar">
          <span>{{ periodLabel }}</span>
          <div class="bill-ledger-actions">
            <label class="bill-period-field" :class="{ 'is-disabled': viewAllDues }">
              <span>Month</span>
              <select
                v-model.number="period.month"
                aria-label="Bill month"
                :disabled="viewAllDues"
              >
                <option v-for="m in 12" :key="m" :value="m">{{ monthNames[m] }}</option>
              </select>
            </label>
            <label class="bill-period-field" :class="{ 'is-disabled': viewAllDues }">
              <span>Year</span>
              <select
                v-model.number="period.year"
                aria-label="Bill year"
                :disabled="viewAllDues"
              >
                <option :value="2026">2026</option>
                <option :value="2025">2025</option>
                <option :value="2024">2024</option>
              </select>
            </label>
            <label
              class="bill-dues-switch"
              :class="{ 'is-on': viewAllDues }"
              title="Turn off to pick a month and year"
            >
              <span>All dues</span>
              <input
                v-model="viewAllDues"
                type="checkbox"
                role="switch"
                aria-label="Show all unpaid dues"
              />
              <span class="bill-dues-track" aria-hidden="true">
                <span class="bill-dues-knob" />
              </span>
            </label>
            <button type="button" class="btn btn-primary btn-sm" @click="openAdd">＋ Add New Bill</button>
          </div>
        </div>

        <div class="table-wrap bill-ledger-wrap">
          <table class="data-table bill-ledger-table">
            <thead>
              <tr>
                <th>Date / time</th>
                <th
                  v-for="col in productCols"
                  :key="col.key"
                  :title="col.name"
                >
                  {{ col.header }}
                </th>
                <th>Amount</th>
                <th>Paid</th>
                <th>Balance</th>
                <th>Prev. due</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="line in ledgerRows" :key="line.visitId">
                <td>{{ new Date(line.visitAt).toLocaleString('en-IN') }}</td>
                <td v-for="col in productCols" :key="col.key">{{ qtyFor(line, col) || '—' }}</td>
                <td>{{ inr(line.amount) }}</td>
                <td>{{ inr(line.paid) }}</td>
                <td>{{ inr(line.balance) }}</td>
                <td>{{ line.prevDue ? inr(line.prevDue) : '—' }}</td>
              </tr>
              <tr v-if="!ledgerRows.length">
                <td :colspan="ledgerColCount">{{ viewAllDues ? 'No unpaid visits' : 'No lines this month' }}</td>
              </tr>
            </tbody>
            <tfoot v-if="ledger">
              <tr>
                <td :colspan="1 + productCols.length"><strong>Totals</strong></td>
                <td>{{ inr(ledger.totalAmount) }}</td>
                <td>{{ inr(ledger.totalPaid) }}</td>
                <td>{{ inr(ledger.totalBalance) }}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

    </div>

    <!-- Print / Download bill preview · mockup -->
    <Teleport to="body">
      <div v-if="showBillPreview && bill" class="bill-modal">
        <div class="bill-modal-backdrop" @click="closeBillPreview" />
        <div class="bill-modal-panel" role="dialog" aria-modal="true" :aria-labelledby="'billModalTitle'">
          <div class="bill-modal-toolbar">
            <strong id="billModalTitle">{{ billPreviewTitle }}</strong>
            <span class="muted">{{ billSealHint }}</span>
            <div class="row-actions form-actions" style="margin-top:0.5rem;border-top:0;padding-top:0">
              <button type="button" class="btn btn-ghost btn-sm" @click="closeBillPreview">Close</button>
              <button type="button" class="btn btn-primary btn-sm" @click="runBillAction">
                {{ billActionLabel }}
              </button>
            </div>
          </div>
          <div class="bill-sheet-scroll">
            <article class="bill-sheet" :class="{ 'with-seal': billWithSeal }" id="billSheet">
              <div class="bill-doc-badge">TAX INVOICE</div>
              <header class="bill-sheet-head">
                <img
                  class="bill-logo"
                  src="/assets/brand/pacific-aqua-logo.png"
                  alt="Pacific Aqua logo"
                  width="72"
                  height="72"
                />
                <div class="bill-supplier">
                  <h1>Pacific Aqua</h1>
                  <p>(R.O.+U.V.+Ozonised) Packaged Drinking Water</p>
                  <p>C-4, Shankarnagar, near TV tower, Raipur (C.G.) 492001</p>
                  <p>
                    <strong>GSTIN:</strong> 22BVPPA3543R1Z9
                    · State: Chhattisgarh (22)
                  </p>
                  <p>Ph: 7974484450, 8319679407 · www.pacificaqua.in</p>
                </div>
                <div class="bill-qr-block">
                  <img
                    src="/assets/bill/phonepe-qr.jpeg"
                    alt="PhonePe UPI QR"
                    width="88"
                    height="88"
                  />
                  <small>Scan to pay · PhonePe</small>
                </div>
              </header>

              <div class="bill-sheet-meta">
                <div class="bill-party-box">
                  <h3>Bill To</h3>
                  <strong>{{ bill.ledger.customerName }}</strong>
                  <p>{{ bill.ledger.address || selected?.cityArea || '—' }}</p>
                  <p>Contact: {{ bill.ledger.phone || '—' }}</p>
                  <p>Card No.: {{ bill.ledger.cardNo || selected?.cardNo || '—' }}</p>
                  <p>Place of Supply: Chhattisgarh (22)</p>
                </div>
                <div class="bill-party-box">
                  <h3>Invoice Details</h3>
                  <div class="bill-kv"><span>Invoice No.</span><strong>{{ bill.billNo }}</strong></div>
                  <div class="bill-kv"><span>Date</span><strong>{{ billDateLabel }}</strong></div>
                  <div class="bill-kv"><span>Period</span><strong>{{ periodLabel.replace('Customer BILL · ', '') }}</strong></div>
                  <div class="bill-kv"><span>Reverse Charge</span><strong>No</strong></div>
                  <div class="bill-kv"><span>HSN</span><strong>{{ HSN_CODE }} (Packaged drinking water)</strong></div>
                </div>
              </div>

              <table class="bill-lines">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>HSN</th>
                    <th>Description</th>
                    <th>Qty</th>
                    <th>Rate (₹)</th>
                    <th>Amount (₹)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in billSheetLines" :key="`${row.nos}-${row.description}`">
                    <td>{{ row.nos }}</td>
                    <td>{{ row.hsn }}</td>
                    <td>{{ row.description }}</td>
                    <td>{{ row.qty }}</td>
                    <td>{{ money(row.rate) }}</td>
                    <td>{{ money(row.amount) }}</td>
                  </tr>
                  <tr v-if="!billSheetLines.length">
                    <td colspan="6">No supply lines this period</td>
                  </tr>
                </tbody>
              </table>

              <div class="bill-summary-grid">
                <div class="bill-words">
                  <span>Amount in words</span>
                  <strong>{{ bill.amountInWords }}</strong>
                  <p class="bill-tax-note">Rates are GST-inclusive (5% · CGST 2.5% + SGST 2.5%).</p>
                </div>
                <table class="bill-totals">
                  <tbody>
                    <tr>
                      <td>Taxable value</td>
                      <td>₹{{ money(billTaxableTotal) }}</td>
                    </tr>
                    <tr>
                      <td>CGST @ 2.5%</td>
                      <td>₹{{ money(billCgstTotal) }}</td>
                    </tr>
                    <tr>
                      <td>SGST @ 2.5%</td>
                      <td>₹{{ money(billSgstTotal) }}</td>
                    </tr>
                    <tr>
                      <td>Invoice total</td>
                      <td>₹{{ money(billGrossTotal) }}</td>
                    </tr>
                    <tr>
                      <td>Amount received</td>
                      <td>₹{{ money(billPaidTotal) }}</td>
                    </tr>
                    <tr class="bill-due-row">
                      <td>Balance due</td>
                      <td>₹{{ money(billBalanceDue) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <footer class="bill-sheet-foot">
                <div class="bill-foot-note">
                  <p>This is a computer-generated tax invoice.</p>
                  <p>Thank you for choosing Pacific Aqua.</p>
                </div>
                <div class="bill-sign">
                  <img
                    class="bill-seal-img"
                    src="/assets/brand/pacific-aqua-logo.png"
                    alt="Pacific Aqua seal"
                    width="88"
                    height="88"
                  />
                  <span>Authorized Signatory</span>
                  <small>For Pacific Aqua</small>
                </div>
              </footer>
            </article>
          </div>
        </div>
      </div>
    </Teleport>

    <VisitEntryModal
      :open="showAdd"
      title="Add New Bill"
      title-id="addBillTitle"
      submit-label="Add line to BILL"
      products-label="Products · Given / Empty"
      :products="products"
      :customer="visitCustomer"
      :error="error"
      v-model:lines="visitForm.lines"
      v-model:amount-paid="visitForm.amountPaid"
      v-model:payment-mode="visitForm.paymentMode"
      v-model:remarks="visitForm.remarks"
      @close="closeAdd"
      @submit="addVisit"
    />

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
  </div>
</template>

<style scoped>
.cust-avatar {
  flex-shrink: 0;
}
</style>
