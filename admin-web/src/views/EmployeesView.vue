<script setup>
import { onMounted, reactive, ref } from 'vue'
import http from '@/api/http'
import PaAvatar from '@/components/PaAvatar.vue'

const rows = ref([])
const q = ref('')
const role = ref('')
const status = ref('ACTIVE')
const showForm = ref(false)
const error = ref('')
const photoPreview = ref('')
const form = reactive({
  id: null,
  name: '',
  phone: '',
  email: '',
  role: 'HELPER',
  password: '',
  avatarPath: '',
})

async function load() {
  const { data } = await http.get('/employees', {
    params: {
      q: q.value || undefined,
      role: role.value || undefined,
      status: status.value || undefined,
    },
  })
  rows.value = data
}

function openCreate() {
  Object.assign(form, { id: null, name: '', phone: '', email: '', role: 'HELPER', password: '', avatarPath: '' })
  photoPreview.value = ''
  showForm.value = true
}

function openEdit(e) {
  Object.assign(form, {
    id: e.id,
    name: e.name,
    phone: e.phone,
    email: e.email || '',
    role: e.role,
    password: '',
    avatarPath: e.avatarPath || '',
  })
  photoPreview.value = e.avatarPath || ''
  showForm.value = true
}

function onPhotoFile(ev) {
  const file = ev.target.files?.[0]
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
      email: form.email || null,
      role: form.role,
      password: form.password || undefined,
      avatarPath: form.avatarPath || null,
    }
    if (form.id) await http.put(`/employees/${form.id}`, payload)
    else await http.post('/employees', payload)
    showForm.value = false
    await load()
  } catch (e) {
    error.value = e.response?.data?.message || 'Save failed'
  }
}

async function toggle(e) {
  if (e.status === 'ACTIVE') await http.post(`/employees/${e.id}/deactivate`)
  else await http.post(`/employees/${e.id}/activate`)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="screen active">
    <div class="ledger-head">
      <div>
        <h2>Employees</h2>
        <p class="muted">Helpers, admins &amp; plant staff</p>
      </div>
      <button type="button" class="btn btn-primary" @click="openCreate">＋ Add employee</button>
    </div>

    <div class="toolbar">
      <input v-model="q" type="search" placeholder="Search name or phone…" @keyup.enter="load" />
      <select v-model="role" aria-label="Employee type filter" @change="load">
        <option value="">All types</option>
        <option value="HELPER">Helper</option>
        <option value="ADMIN">Admin</option>
        <option value="OWNER">Owner</option>
        <option value="PLANT">Plant</option>
      </select>
      <select v-model="status" aria-label="Status filter" @change="load">
        <option value="ACTIVE">Active</option>
        <option value="INACTIVE">Inactive</option>
        <option value="">All</option>
      </select>
    </div>

    <div v-if="showForm" class="admin-form form-panel new-order-form">
      <div class="form-panel-head">
        <h3>{{ form.id ? 'Edit employee' : 'Add employee' }}</h3>
      </div>
      <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>
      <div class="form-grid two">
        <div class="form-field">
          <label>Name <span class="req">*</span></label>
          <input v-model="form.name" type="text" placeholder="Full name" />
        </div>
        <div class="form-field">
          <label>Phone <span class="req">*</span></label>
          <input v-model="form.phone" type="tel" placeholder="10-digit" />
        </div>
        <div class="form-field">
          <label>Email <span class="muted">(optional · admin alerts)</span></label>
          <input v-model="form.email" type="email" placeholder="name@example.com" />
        </div>
        <div class="form-field">
          <label>Employee type <span class="req">*</span></label>
          <select v-model="form.role">
            <option value="HELPER">Helper</option>
            <option value="ADMIN">Admin</option>
            <option value="OWNER">Owner</option>
            <option value="PLANT">Plant</option>
          </select>
        </div>
        <div class="form-field">
          <label>{{ form.id ? 'New password (optional)' : 'Password' }}</label>
          <input v-model="form.password" type="password" :placeholder="form.id ? 'Leave blank to keep' : 'Set password'" />
        </div>
      </div>
      <div class="profile-photo-field">
        <label>Profile picture</label>
        <div class="profile-photo-row">
          <div class="profile-photo-preview pa-user-fallback" :class="{ 'has-img': true }" aria-hidden="true">
            <img v-if="photoPreview" :src="photoPreview" alt="" />
            <img v-else src="/assets/brand/user-avatar.svg" alt="" />
          </div>
          <div class="profile-photo-actions">
            <input ref="empPhotoFile" type="file" accept="image/*" hidden @change="onPhotoFile" />
            <button type="button" class="btn btn-ghost btn-sm" @click="$refs.empPhotoFile.click()">Choose photo</button>
            <button v-if="photoPreview" type="button" class="btn btn-ghost btn-sm" @click="clearPhoto">Remove</button>
          </div>
        </div>
      </div>
      <div class="form-actions">
        <button type="button" class="btn btn-ghost" @click="showForm = false">Cancel</button>
        <button type="button" class="btn btn-primary" @click="save">Save employee</button>
      </div>
    </div>

    <p class="muted orders-hint">Showing {{ status === 'ACTIVE' ? 'active' : status === 'INACTIVE' ? 'inactive' : 'all' }} employees</p>
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Phone</th>
            <th>Employee type</th>
            <th>App access</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in rows" :key="e.id">
            <td>
              <span class="person-cell">
                <PaAvatar :name="e.name" :photo="e.avatarPath" kind="user" size="sm" />
                <span>{{ e.name }}</span>
              </span>
            </td>
            <td>{{ e.phone }}</td>
            <td>{{ e.role }}</td>
            <td>{{ e.appAccess ? 'Yes' : 'No' }}</td>
            <td>{{ e.status }}</td>
            <td>
              <button type="button" class="btn btn-ghost btn-sm" @click="openEdit(e)">Edit</button>
              <button type="button" class="btn btn-ghost btn-sm" @click="toggle(e)">
                {{ e.status === 'ACTIVE' ? 'Deactivate' : 'Activate' }}
              </button>
            </td>
          </tr>
          <tr v-if="!rows.length">
            <td colspan="6">No employees</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.profile-photo-preview {
  width: 3.1rem;
  height: 3.1rem;
  border-radius: 50%;
  overflow: hidden;
}
.profile-photo-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>
