<script setup>
import { computed, ref, watch } from 'vue'
import PaAvatar from '@/components/PaAvatar.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: 'Add visit' },
  titleId: { type: String, default: 'visitEntryTitle' },
  submitLabel: { type: String, default: 'Save' },
  products: { type: Array, default: () => [] },
  customer: { type: Object, default: null },
  helper: { type: Object, default: null },
  metaLine: { type: String, default: '' },
  productsLabel: { type: String, default: 'Products · Given / Empty' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
})

const lines = defineModel('lines', { type: Array, default: () => [] })
const amountPaid = defineModel('amountPaid', { type: Number, default: 0 })
const paymentMode = defineModel('paymentMode', { type: String, default: 'CASH' })
const remarks = defineModel('remarks', { type: String, default: '' })

const emit = defineEmits(['close', 'submit'])

const payModes = [
  { id: 'CASH', label: 'Cash' },
  { id: 'UPI', label: 'UPI' },
  { id: 'BANK', label: 'Bank' },
  { id: 'CREDIT', label: 'Credit' },
]

const showPicker = ref(false)
const pickSelected = ref(new Set())
const paidAuto = ref(true)
let lineUid = 0

const billTotal = computed(() =>
  (lines.value || []).reduce((sum, line) => {
    const p = productById(line.productId)
    return sum + (p ? Number(p.defaultRate || 0) * Number(line.qtyDiya || 0) : 0)
  }, 0),
)

const availableProducts = computed(() =>
  (props.products || []).filter((p) => !(lines.value || []).some((l) => l.productId === p.id)),
)

const addProductLabel = computed(() =>
  (lines.value || []).length ? '＋ Add another product' : '＋ Add product',
)

function inr(n) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(n || 0)
}

function productById(id) {
  return (props.products || []).find((p) => p.id === id)
}

function productIcon(p) {
  if (!p?.imagePath) return '/assets/products/icon-jar-20l.png'
  return p.imagePath.startsWith('/') ? p.imagePath : `/${p.imagePath}`
}

function syncPaidFromTotal() {
  if (paidAuto.value) amountPaid.value = billTotal.value
}

function stepQty(line, field, delta) {
  line[field] = Math.max(0, (Number(line[field]) || 0) + delta)
  if (field === 'qtyDiya') syncPaidFromTotal()
}

function removeLine(uid) {
  lines.value = (lines.value || []).filter((l) => l.uid !== uid)
  syncPaidFromTotal()
}

function selectPayMode(mode) {
  paymentMode.value = mode
  if (mode === 'CREDIT') {
    paidAuto.value = false
    amountPaid.value = 0
  } else if (!amountPaid.value && paidAuto.value) {
    amountPaid.value = billTotal.value
  }
}

function onPaidInput() {
  paidAuto.value = false
}

function togglePicker() {
  if (!availableProducts.value.length) return
  showPicker.value = !showPicker.value
  if (showPicker.value) pickSelected.value = new Set()
}

function togglePick(id) {
  if ((lines.value || []).some((l) => l.productId === id)) return
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
  const next = [...(lines.value || [])]
  ;[...pickSelected.value].forEach((productId) => {
    if (next.some((l) => l.productId === productId)) return
    next.push({
      uid: `ve${++lineUid}`,
      productId,
      qtyDiya: 1,
      qtyMila: 0,
    })
  })
  lines.value = next
  pickSelected.value = new Set()
  showPicker.value = false
  syncPaidFromTotal()
}

function close() {
  showPicker.value = false
  pickSelected.value = new Set()
  emit('close')
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      showPicker.value = false
      pickSelected.value = new Set()
      return
    }
    paidAuto.value = true
    if (paymentMode.value !== 'CREDIT') syncPaidFromTotal()
  },
)

watch(billTotal, () => {
  if (props.open) syncPaidFromTotal()
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="bill-modal add-bill-modal">
      <div class="bill-modal-backdrop" @click="close" />
      <div
        class="bill-modal-panel add-bill-panel"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
      >
        <div class="visit-dense add-bill-body">
          <div class="visit-top">
            <button type="button" class="back-link" @click="close">←</button>
            <h2 :id="titleId" class="screen-title">{{ title }}</h2>
            <button type="button" class="modal-close-btn" aria-label="Close" @click="close">×</button>
          </div>

          <div v-if="customer" class="deliver-parties">
            <div class="deliver-party deliver-party-customer">
              <PaAvatar :name="customer.name" :photo="customer.avatarPath" size="md" />
              <div class="deliver-party-text">
                <strong>{{ customer.name || '—' }}</strong>
                <small>
                  Due <b class="warn">{{ inr(customer.due) }}</b>
                  <span v-if="customer.area"> · {{ customer.area }}</span>
                </small>
                <small v-if="metaLine" class="muted">{{ metaLine }}</small>
              </div>
            </div>
            <div v-if="helper" class="deliver-party deliver-party-helper">
              <div class="deliver-party-text deliver-party-text-end">
                <span class="deliver-party-label">Helper</span>
                <strong>{{ helper.name || '—' }}</strong>
              </div>
              <PaAvatar
                :name="helper.name"
                :photo="helper.avatarPath"
                kind="user"
                size="md"
              />
            </div>
          </div>

          <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>

          <div class="field field-tight">
            <div class="dm-head">
              <label style="margin:0">{{ productsLabel }}</label>
              <span class="dm-bill">Bill <span>{{ inr(billTotal) }}</span></span>
            </div>
            <p v-if="hint" class="deliver-hint muted">{{ hint }}</p>

            <div v-if="lines.length" class="dm-cols">
              <span class="dm-col-label dm-col-prod"><strong>Product</strong></span>
              <span class="dm-col-label"><strong>Given</strong><small>दिया</small></span>
              <span class="dm-col-label"><strong>Empty</strong><small>मिला</small></span>
              <span class="dm-col-del" aria-hidden="true" />
            </div>

            <div
              v-for="line in lines"
              :key="line.uid"
              class="dm-row add-bill-line"
            >
              <div class="dm-prod">
                <span class="dm-prod-media">
                  <img
                    class="mobile-prod-icon"
                    :src="productIcon(productById(line.productId))"
                    alt=""
                  />
                </span>
                <span class="dm-prod-name">
                  {{ productById(line.productId)?.name || '—' }}
                  <small>₹{{ productById(line.productId)?.defaultRate ?? 0 }}</small>
                </span>
              </div>
              <div class="dm-qty">
                <div class="stepper">
                  <button type="button" @click="stepQty(line, 'qtyDiya', -1)">−</button>
                  <input
                    v-model.number="line.qtyDiya"
                    type="number"
                    min="0"
                    :aria-label="`${productById(line.productId)?.name || 'Product'} given`"
                    @input="syncPaidFromTotal"
                  />
                  <button type="button" @click="stepQty(line, 'qtyDiya', 1)">+</button>
                </div>
              </div>
              <div class="dm-qty">
                <div class="stepper">
                  <button type="button" @click="stepQty(line, 'qtyMila', -1)">−</button>
                  <input
                    v-model.number="line.qtyMila"
                    type="number"
                    min="0"
                    :aria-label="`${productById(line.productId)?.name || 'Product'} empty`"
                  />
                  <button type="button" @click="stepQty(line, 'qtyMila', 1)">+</button>
                </div>
              </div>
              <button
                type="button"
                class="line-remove"
                title="Remove product"
                :aria-label="`Remove ${productById(line.productId)?.name || 'product'}`"
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

            <p v-if="!lines.length" class="add-bill-empty">
              No products yet · tap below to add
            </p>

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
                    'is-used': lines.some((l) => l.productId === p.id),
                  }"
                  :disabled="lines.some((l) => l.productId === p.id)"
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

          <div class="deliver-pay">
            <div class="deliver-pay-head">
              <div>
                <span class="deliver-pay-kicker">This bill</span>
                <strong class="deliver-pay-total">{{ inr(billTotal) }}</strong>
              </div>
              <div class="deliver-pay-due">
                <span class="deliver-pay-kicker">Prior due</span>
                <strong class="warn">{{ inr(customer?.due) }}</strong>
              </div>
            </div>

            <div class="deliver-pay-methods" role="radiogroup" aria-label="Payment method">
              <button
                v-for="m in payModes"
                :key="m.id"
                type="button"
                role="radio"
                class="deliver-pay-method"
                :class="{ active: paymentMode === m.id }"
                :aria-checked="paymentMode === m.id ? 'true' : 'false'"
                @click="selectPayMode(m.id)"
              >
                <span class="deliver-pay-method-ico" aria-hidden="true">
                  <template v-if="m.id === 'CASH'">₹</template>
                  <template v-else-if="m.id === 'UPI'">⌁</template>
                  <template v-else-if="m.id === 'BANK'">⌂</template>
                  <template v-else>◷</template>
                </span>
                <span>{{ m.label }}</span>
              </button>
            </div>

            <div class="deliver-tender">
              <label :for="`${titleId}-collected`">Amount collected</label>
              <div class="deliver-tender-input">
                <span aria-hidden="true">₹</span>
                <input
                  :id="`${titleId}-collected`"
                  v-model.number="amountPaid"
                  type="number"
                  min="0"
                  inputmode="numeric"
                  @input="onPaidInput"
                />
              </div>
            </div>
          </div>

          <div class="field field-tight">
            <label>Remarks</label>
            <input
              v-model="remarks"
              type="text"
              placeholder="Optional note · extras, gate code…"
            />
          </div>

          <div class="visit-save-bar form-actions">
            <button type="button" class="btn btn-ghost" @click="close">Cancel</button>
            <button type="button" class="btn btn-primary" @click="emit('submit')">
              {{ submitLabel }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
