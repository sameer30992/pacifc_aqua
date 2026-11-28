<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import PaAvatar from '@/components/PaAvatar.vue'

const router = useRouter()
const summary = ref(null)
const error = ref('')

function inr(n) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(n || 0)
}

onMounted(async () => {
  try {
    const { data } = await http.get('/dashboard/summary')
    summary.value = data
  } catch (e) {
    error.value = e.response?.data?.message || 'Failed to load dashboard'
  }
})
</script>

<template>
  <div class="screen active">
    <div class="ledger-head">
      <div>
        <h2>SUM UP</h2>
        <p class="muted">Month overview</p>
      </div>
      <div class="row-actions">
        <button type="button" class="btn btn-primary" @click="router.push({ name: 'orders' })">＋ New order</button>
        <button type="button" class="btn btn-ghost" @click="router.push({ name: 'billing' })">＋ New visit</button>
      </div>
    </div>

    <p v-if="error" class="form-banner-error">{{ error }}</p>

    <template v-if="summary">
      <div class="kpi-grid">
        <div class="kpi"><span>Revenue</span><strong>{{ inr(summary.kpis.revenue) }}</strong></div>
        <div class="kpi"><span>Collected</span><strong>{{ inr(summary.kpis.collected) }}</strong></div>
        <div class="kpi"><span>Pending dues</span><strong>{{ inr(summary.kpis.pendingDues) }}</strong></div>
        <div class="kpi"><span>P&amp;S entries</span><strong>{{ summary.kpis.visitCount }}</strong></div>
        <div class="kpi"><span>Top product</span><strong>{{ summary.kpis.topProduct }}</strong></div>
        <div class="kpi"><span>Helpers active</span><strong>{{ summary.kpis.staffPresent }}</strong></div>
      </div>

      <div class="ledger-head" style="margin-top:0.85rem">
        <div>
          <h2 style="font-size:1rem">Recent sales</h2>
        </div>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Visit</th>
              <th>Customer</th>
              <th>Helper</th>
              <th>Amount</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in summary.recentSales" :key="row.id">
              <td>{{ row.visitNo }}</td>
              <td>
                <span class="person-cell">
                  <PaAvatar :name="row.customerName" size="sm" />
                  <span>{{ row.customerName }}</span>
                </span>
              </td>
              <td>
                <span class="person-cell">
                  <PaAvatar :name="row.helperName" kind="user" size="sm" />
                  <span>{{ row.helperName }}</span>
                </span>
              </td>
              <td>{{ inr(row.amount) }}</td>
              <td>{{ row.status }}</td>
            </tr>
            <tr v-if="!summary.recentSales?.length">
              <td colspan="5">No sales yet</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="muted" style="margin-top:0.65rem">
        <a href="#" @click.prevent="router.push({ name: 'sales' })">Open full sales ledger →</a>
      </p>
    </template>
  </div>
</template>

<style scoped>
.person-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}
</style>
