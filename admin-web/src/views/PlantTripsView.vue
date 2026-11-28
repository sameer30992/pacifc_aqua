<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/http'
import PaAvatar from '@/components/PaAvatar.vue'

const rows = ref([])
const summary = ref(null)
const helpers = ref([])
const products = ref([])
const showForm = ref(false)
const error = ref('')
const form = reactive({
  helperId: null,
  productId: null,
  qtyLifted: 10,
  notes: '',
})

async function load() {
  const [s, t] = await Promise.all([
    http.get('/plant-trips/summary'),
    http.get('/plant-trips'),
  ])
  summary.value = s.data
  rows.value = t.data
}

async function loadMasters() {
  const [h, p] = await Promise.all([
    http.get('/employees/helpers'),
    http.get('/products', { params: { activeOnly: true } }),
  ])
  helpers.value = h.data
  products.value = p.data
  form.helperId = helpers.value[0]?.id || null
  form.productId = products.value[0]?.id || null
}

async function save() {
  error.value = ''
  try {
    await http.post('/plant-trips', {
      helperId: form.helperId,
      notes: form.notes,
      lines: [{ productId: form.productId, qtyLifted: form.qtyLifted }],
    })
    showForm.value = false
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to log lift'
  }
}

onMounted(async () => {
  await loadMasters()
  await load()
})
</script>

<template>
  <div class="screen active">
    <div class="ledger-head">
      <div>
        <h2>Plant trips</h2>
        <p class="muted">Load / return</p>
      </div>
      <button type="button" class="btn btn-primary" @click="showForm = true">＋ Log plant lift</button>
    </div>

    <div v-if="summary" class="kpi-grid">
      <div class="kpi"><span>20 L Jar in plant</span><strong>{{ summary.jarInPlant }}</strong></div>
      <div class="kpi"><span>Lifted today</span><strong>{{ summary.liftedToday }}</strong></div>
      <div class="kpi"><span>Delivered today</span><strong>{{ summary.deliveredToday }}</strong></div>
      <div class="kpi"><span>Empty returned</span><strong>{{ summary.emptyReturned }}</strong></div>
    </div>

    <div v-if="showForm" class="admin-form form-panel">
      <div class="form-panel-head"><h3>Log plant lift</h3></div>
      <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>
      <div class="form-grid two">
        <div class="form-field">
          <label>Helper <span class="req">*</span></label>
          <select v-model="form.helperId">
            <option v-for="h in helpers" :key="h.id" :value="h.id">{{ h.name }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>Product <span class="req">*</span></label>
          <select v-model="form.productId">
            <option v-for="p in products" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>Qty lifted</label>
          <input v-model.number="form.qtyLifted" type="number" min="1" />
        </div>
        <div class="form-field">
          <label>Notes</label>
          <input v-model="form.notes" type="text" />
        </div>
      </div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" @click="showForm = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="save">Save lift</button>
      </div>
    </div>

    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>When</th><th>Helper</th><th>Product</th><th>Lifted</th><th>Delivered</th><th>Empties</th></tr>
        </thead>
        <tbody>
          <template v-for="t in rows" :key="t.id">
            <tr v-for="line in t.lines" :key="t.id + '-' + line.productId">
              <td>{{ new Date(t.startedAt).toLocaleString('en-IN') }}</td>
              <td>
                <span class="person-cell">
                  <PaAvatar :name="t.helperName" kind="user" size="sm" />
                  <span>{{ t.helperName }}</span>
                </span>
              </td>
              <td>{{ line.productName }}</td>
              <td>{{ line.qtyLifted }}</td>
              <td>{{ line.qtyDelivered }}</td>
              <td>{{ line.qtyEmpties }}</td>
            </tr>
          </template>
          <tr v-if="!rows.length"><td colspan="6">No plant trips yet</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
