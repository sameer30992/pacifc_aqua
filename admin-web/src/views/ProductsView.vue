<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/http'

const rows = ref([])
const showForm = ref(false)
const error = ref('')
const form = reactive({
  id: null,
  code: '',
  name: '',
  category: 'Jar',
  unit: 'jar',
  defaultRate: 0,
  stock: 0,
  status: 'ACTIVE',
  imagePath: '',
})

function inr(n) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n || 0)
}

async function load() {
  const { data } = await http.get('/products')
  rows.value = data
}

function openCreate() {
  Object.assign(form, {
    id: null, code: '', name: '', category: 'Jar', unit: 'jar',
    defaultRate: 40, stock: 0, status: 'ACTIVE', imagePath: '',
  })
  error.value = ''
  showForm.value = true
}

function openEdit(p) {
  Object.assign(form, {
    id: p.id, code: p.code, name: p.name, category: p.category, unit: p.unit,
    defaultRate: p.defaultRate, stock: p.stock, status: p.status, imagePath: p.imagePath || '',
  })
  error.value = ''
  showForm.value = true
}

async function save() {
  error.value = ''
  try {
    const payload = { ...form }
    if (form.id) await http.put(`/products/${form.id}`, payload)
    else await http.post('/products', payload)
    showForm.value = false
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Save failed'
  }
}

async function bumpStock(p, delta) {
  const stock = Math.max(0, (p.stock || 0) + delta)
  await http.patch(`/products/${p.id}/stock`, { stock })
  await load()
}

async function remove(p) {
  if (!confirm(`Deactivate ${p.name}?`)) return
  await http.delete(`/products/${p.id}`)
  await load()
}

function imgSrc(p) {
  if (!p.imagePath) return '/assets/products/icon-jar-20l.png'
  return p.imagePath.startsWith('/') ? p.imagePath : `/${p.imagePath}`
}

onMounted(load)
</script>

<template>
  <div class="screen active">
    <div class="ledger-head">
      <div>
        <h2>Products</h2>
        <p class="muted">Jar / chilled / bottles</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">＋ Add product</button>
    </div>

    <div v-if="showForm" class="admin-form form-panel">
      <div class="form-panel-head"><h3>{{ form.id ? 'Edit product' : 'Add product' }}</h3></div>
      <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>
      <div class="form-grid two">
        <div class="form-field">
          <label>Name <span class="req">*</span></label>
          <input v-model="form.name" type="text" />
        </div>
        <div class="form-field">
          <label>Category</label>
          <input v-model="form.category" type="text" />
        </div>
        <div class="form-field">
          <label>Unit</label>
          <input v-model="form.unit" type="text" />
        </div>
        <div class="form-field">
          <label>Default rate <span class="req">*</span></label>
          <input v-model.number="form.defaultRate" type="number" min="0" step="0.01" />
        </div>
        <div class="form-field">
          <label>Stock</label>
          <input v-model.number="form.stock" type="number" min="0" />
        </div>
        <div class="form-field">
          <label>Status</label>
          <select v-model="form.status">
            <option value="ACTIVE">Active</option>
            <option value="DEACTIVATED">Deactivated</option>
          </select>
        </div>
      </div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" @click="showForm = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="save">Save</button>
      </div>
    </div>

    <div class="prod-grid">
      <div v-for="p in rows" :key="p.id" class="prod-card">
        <img :src="imgSrc(p)" :alt="p.name" width="48" height="48" />
        <strong>{{ p.name }}</strong>
        <small class="muted">{{ p.category }} · {{ p.unit }}</small>
        <div>{{ inr(p.defaultRate) }}</div>
        <div class="stock-row">
          <button type="button" class="btn btn-ghost btn-sm" @click="bumpStock(p, -1)">−</button>
          <span>{{ p.stock }}</span>
          <button type="button" class="btn btn-ghost btn-sm" @click="bumpStock(p, 1)">+</button>
        </div>
        <div class="row-actions">
          <button type="button" class="btn btn-ghost btn-sm" @click="openEdit(p)">Edit</button>
          <button type="button" class="btn btn-ghost btn-sm" @click="remove(p)">Remove</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.prod-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
  margin-top: 0.75rem;
}
.prod-card {
  border: 1px solid rgba(124, 108, 240, 0.14);
  border-radius: 0.85rem;
  padding: 0.85rem;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.stock-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}
</style>
