<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const phoneOrEmail = ref('9999999999')
const password = ref('admin123')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(phoneOrEmail.value.trim(), password.value)
    router.push({ name: 'dashboard' })
  } catch (e) {
    error.value = e.response?.data?.message || 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin admin-fullscreen login-screen">
    <form class="login-card" @submit.prevent="submit">
      <div class="login-brand">
        <img class="login-brand-logo" src="/assets/brand/pacific-aqua-logo.png" alt="Pacific Aqua" width="64" height="64" />
        <h1>Admin Control</h1>
        <p class="muted">Sign in to Pacific Aqua</p>
      </div>
      <div class="form-field">
        <label for="loginId">Phone or email</label>
        <input id="loginId" v-model="phoneOrEmail" type="text" required autocomplete="username" />
      </div>
      <div class="form-field">
        <label for="loginPass">Password</label>
        <input id="loginPass" v-model="password" type="password" required autocomplete="current-password" />
      </div>
      <p v-if="error" class="form-banner-error" role="alert">{{ error }}</p>
      <button type="submit" class="btn btn-primary" :disabled="loading" style="width:100%">
        {{ loading ? 'Signing in…' : 'Login' }}
      </button>
      <p class="muted login-hint">Demo · 9999999999 / admin123</p>
    </form>
  </div>
</template>

<style scoped>
.login-screen {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 1.5rem;
  background:
    radial-gradient(1200px 600px at 10% -10%, rgba(124, 108, 240, 0.18), transparent 55%),
    radial-gradient(900px 500px at 100% 0%, rgba(11, 110, 79, 0.12), transparent 50%),
    #f4f6fb;
}
.login-card {
  width: min(420px, 100%);
  background: #fff;
  border-radius: 1rem;
  padding: 1.6rem 1.5rem 1.4rem;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.12);
  border: 1px solid rgba(124, 108, 240, 0.12);
}
.login-brand {
  text-align: center;
  margin-bottom: 1.1rem;
}
.login-brand h1 {
  margin: 0.55rem 0 0.2rem;
  font-size: 1.35rem;
}
.login-hint {
  margin: 0.9rem 0 0;
  text-align: center;
  font-size: 0.78rem;
}
.form-field {
  margin-bottom: 0.75rem;
}
</style>
