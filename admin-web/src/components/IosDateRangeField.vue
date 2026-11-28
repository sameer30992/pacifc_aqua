<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const from = defineModel('from', { type: String, default: '' })
const to = defineModel('to', { type: String, default: '' })

const props = defineProps({
  ariaLabel: { type: String, default: 'Date range' },
  placeholder: { type: String, default: 'Select dates' },
})

const emit = defineEmits(['change'])

const open = ref(false)
const rootEl = ref(null)
const panelEl = ref(null)
const panelPos = ref({ top: 0, left: 0 })
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth())
const draftFrom = ref(null)
const draftTo = ref(null)
const picking = ref('from') // 'from' | 'to'

const hasValue = computed(() => !!(from.value || to.value))

const weekdays = ['S', 'M', 'T', 'W', 'T', 'F', 'S']
const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const start = computed(() => draftFrom.value || parseIso(from.value))
const end = computed(() => draftTo.value || parseIso(to.value))

const displayLabel = computed(() => {
  const a = parseIso(from.value)
  const b = parseIso(to.value)
  if (!a && !b) return props.placeholder
  if (a && !b) return `${fmt(a)} – …`
  if (!a && b) return `… – ${fmt(b)}`
  return `${fmt(a)} – ${fmt(b)}`
})

const hint = computed(() =>
  picking.value === 'from' ? 'Select start date' : 'Select end date',
)

const monthTitle = computed(
  () => `${monthNames[viewMonth.value]} ${viewYear.value}`,
)

const cells = computed(() => {
  const first = new Date(viewYear.value, viewMonth.value, 1)
  const startPad = first.getDay()
  const daysInMonth = new Date(viewYear.value, viewMonth.value + 1, 0).getDate()
  const prevDays = new Date(viewYear.value, viewMonth.value, 0).getDate()
  const list = []

  for (let i = startPad - 1; i >= 0; i -= 1) {
    const day = prevDays - i
    const date = new Date(viewYear.value, viewMonth.value - 1, day)
    list.push({ day, date, inMonth: false, key: `p-${day}` })
  }
  for (let day = 1; day <= daysInMonth; day += 1) {
    const date = new Date(viewYear.value, viewMonth.value, day)
    list.push({ day, date, inMonth: true, key: `c-${day}` })
  }
  let nextDay = 1
  while (list.length % 7 !== 0) {
    const date = new Date(viewYear.value, viewMonth.value + 1, nextDay)
    list.push({ day: nextDay, date, inMonth: false, key: `n-${nextDay}` })
    nextDay += 1
  }
  return list
})

function parseIso(value) {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null
  const [y, m, d] = value.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  if (Number.isNaN(date.getTime())) return null
  return date
}

function toIso(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function fmt(date) {
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
}

function sameDay(a, b) {
  return (
    a &&
    b &&
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

function dayTime(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
}

function isToday(date) {
  return sameDay(date, new Date())
}

function isStart(date) {
  return sameDay(date, start.value)
}

function isEnd(date) {
  return sameDay(date, end.value)
}

function inRange(date) {
  if (!start.value || !end.value) return false
  const t = dayTime(date)
  return t > dayTime(start.value) && t < dayTime(end.value)
}

function syncView() {
  const d = start.value || end.value || new Date()
  viewYear.value = d.getFullYear()
  viewMonth.value = d.getMonth()
}

function resetDraft() {
  draftFrom.value = parseIso(from.value)
  draftTo.value = parseIso(to.value)
  picking.value = 'from'
}

function placePanel() {
  const btn = rootEl.value?.querySelector?.('.ios-date-trigger') || rootEl.value
  if (!btn) return
  const r = btn.getBoundingClientRect()
  const panelW = 296
  const gap = 8
  let left = r.left
  if (left + panelW > window.innerWidth - 8) {
    left = Math.max(8, r.right - panelW)
  }
  if (left < 8) left = 8
  let top = r.bottom + gap
  const estH = 360
  if (top + estH > window.innerHeight - 8) {
    top = Math.max(8, r.top - estH - gap)
  }
  panelPos.value = { top, left }
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    resetDraft()
    syncView()
    placePanel()
  }
}

function close() {
  open.value = false
}

function clearDates(e) {
  e?.stopPropagation?.()
  e?.preventDefault?.()
  from.value = ''
  to.value = ''
  draftFrom.value = null
  draftTo.value = null
  picking.value = 'from'
  emit('change', { from: '', to: '' })
  close()
}

function prevMonth() {
  if (viewMonth.value === 0) {
    viewMonth.value = 11
    viewYear.value -= 1
  } else {
    viewMonth.value -= 1
  }
}

function nextMonth() {
  if (viewMonth.value === 11) {
    viewMonth.value = 0
    viewYear.value += 1
  } else {
    viewMonth.value += 1
  }
}

function commitRange(a, b) {
  let startD = a
  let endD = b
  if (dayTime(endD) < dayTime(startD)) {
    startD = b
    endD = a
  }
  from.value = toIso(startD)
  to.value = toIso(endD)
  draftFrom.value = startD
  draftTo.value = endD
  emit('change', { from: from.value, to: to.value })
  close()
}

function pick(date) {
  if (picking.value === 'from' || !draftFrom.value || (draftFrom.value && draftTo.value)) {
    draftFrom.value = date
    draftTo.value = null
    picking.value = 'to'
    return
  }
  commitRange(draftFrom.value, date)
}

function pickThisMonth() {
  const now = new Date()
  const startD = new Date(now.getFullYear(), now.getMonth(), 1)
  const endD = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  commitRange(startD, endD)
}

function onDocPointer(e) {
  if (!open.value) return
  if (rootEl.value?.contains(e.target)) return
  if (panelEl.value?.contains(e.target)) return
  close()
}

function onKey(e) {
  if (e.key === 'Escape' && open.value) close()
}

function onReposition() {
  if (!open.value) return
  placePanel()
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocPointer, true)
  document.addEventListener('keydown', onKey)
  window.addEventListener('resize', onReposition)
  window.addEventListener('scroll', onReposition, true)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocPointer, true)
  document.removeEventListener('keydown', onKey)
  window.removeEventListener('resize', onReposition)
  window.removeEventListener('scroll', onReposition, true)
})

watch(open, async (v) => {
  if (!v) return
  await nextTick()
  placePanel()
  panelEl.value?.focus?.()
})
</script>

<template>
  <div ref="rootEl" class="ios-date ios-date-range" :class="{ 'is-open': open, 'has-value': hasValue }">
    <div class="ios-date-combo">
      <button
        type="button"
        class="ios-date-trigger"
        :aria-label="ariaLabel"
        :aria-expanded="open ? 'true' : 'false'"
        aria-haspopup="dialog"
        @click="toggle"
      >
        <span class="ios-date-label" :class="{ 'is-placeholder': !from && !to }">
          {{ displayLabel }}
        </span>
        <span v-if="!hasValue" class="ios-date-ico" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <rect x="3" y="5" width="18" height="16" rx="2.5" stroke="currentColor" stroke-width="1.8" />
            <path d="M8 3v4M16 3v4M3 11h18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </span>
      </button>
      <button
        v-if="hasValue"
        type="button"
        class="ios-date-clear"
        aria-label="Clear dates"
        @click="clearDates"
      >
        ×
      </button>
    </div>

    <Teleport to="body">
      <div
        v-if="open"
        ref="panelEl"
        class="ios-date-panel ios-range-panel ios-date-panel-float"
        role="dialog"
        aria-modal="false"
        :aria-label="ariaLabel"
        tabindex="-1"
        :style="{ top: `${panelPos.top}px`, left: `${panelPos.left}px` }"
      >
        <p class="ios-range-hint">{{ hint }}</p>

        <div class="ios-date-head">
          <button type="button" class="ios-date-nav" aria-label="Previous month" @click="prevMonth">
            ‹
          </button>
          <strong>{{ monthTitle }}</strong>
          <button type="button" class="ios-date-nav" aria-label="Next month" @click="nextMonth">
            ›
          </button>
        </div>

        <div class="ios-date-week" aria-hidden="true">
          <span v-for="(w, i) in weekdays" :key="`${w}-${i}`">{{ w }}</span>
        </div>

        <div class="ios-date-grid" role="grid">
          <button
            v-for="cell in cells"
            :key="cell.key"
            type="button"
            class="ios-date-day"
            :class="{
              'is-out': !cell.inMonth,
              'is-today': isToday(cell.date),
              'is-start': isStart(cell.date),
              'is-end': isEnd(cell.date),
              'is-in-range': inRange(cell.date),
              'is-selected': isStart(cell.date) || isEnd(cell.date),
            }"
            :aria-label="cell.date.toLocaleDateString('en-IN')"
            @click="pick(cell.date)"
          >
            <span class="ios-day-bg" aria-hidden="true" />
            <span class="ios-day-num">{{ cell.day }}</span>
          </button>
        </div>

        <div class="ios-date-foot ios-range-foot">
          <button type="button" class="ios-date-clear-btn" :disabled="!hasValue && !draftFrom" @click="clearDates">
            Clear
          </button>
          <button type="button" class="ios-date-today" @click="pickThisMonth">This month</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.ios-date {
  position: relative;
  display: inline-flex;
  vertical-align: middle;
  width: auto;
  min-width: 12.5rem;
  max-width: 15.5rem;
  flex: 0 0 auto;
}

.ios-date-combo {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.ios-date-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.55rem;
  width: 100%;
  height: var(--control-h, 2.6rem);
  min-height: var(--control-h, 2.6rem);
  padding: 0 0.75rem 0 0.9rem;
  border-radius: var(--radius-control, 0.95rem);
  border: 1px solid rgba(255, 255, 255, 0.55);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(255, 255, 255, 0.4));
  box-shadow: var(--shadow-control,
    0 1px 2px rgba(76, 58, 160, 0.06),
    0 8px 24px rgba(76, 58, 160, 0.06),
    0 1px 0 rgba(255, 255, 255, 0.7) inset);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  backdrop-filter: blur(16px) saturate(1.6);
  color: var(--ink, #1e1a2e);
  font: inherit;
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: -0.01em;
  cursor: pointer;
  box-sizing: border-box;
}

.ios-date.is-open .ios-date-trigger {
  border-color: rgba(124, 108, 240, 0.45);
  box-shadow: 0 0 0 3px rgba(124, 108, 240, 0.14);
}

.ios-date-label.is-placeholder {
  color: var(--muted, #6b6280);
}

.ios-date-ico {
  width: 1rem;
  height: 1rem;
  color: var(--muted, #6b6280);
  flex-shrink: 0;
  display: grid;
  place-items: center;
}

.ios-date-ico svg {
  width: 1rem;
  height: 1rem;
  display: block;
}

.ios-date-clear {
  position: absolute;
  right: 0.55rem;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 1.35rem;
  height: 1.35rem;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--muted, #6b6280);
  font-size: 1.05rem;
  line-height: 1;
  cursor: pointer;
  display: grid;
  place-items: center;
  padding: 0;
}

.ios-date-clear:hover {
  color: var(--ink, #1e1a2e);
  background: rgba(124, 108, 240, 0.1);
}

.ios-date.has-value .ios-date-trigger {
  padding-right: 2rem;
}

.ios-date-panel {
  position: absolute;
  top: calc(100% + 0.4rem);
  left: 0;
  z-index: 60;
  width: 18.5rem;
  padding: 0.65rem 0.7rem 0.55rem;
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: #ffffff;
  box-shadow:
    0 22px 50px rgba(76, 58, 160, 0.18),
    0 1px 0 rgba(255, 255, 255, 0.9) inset;
  -webkit-backdrop-filter: none;
  backdrop-filter: none;
  outline: none;
  animation: iosDateIn 0.2s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes iosDateIn {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.ios-range-hint {
  margin: 0 0 0.45rem;
  text-align: center;
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--muted, #6b6280);
}

.ios-date-head {
  display: grid;
  grid-template-columns: 2rem 1fr 2rem;
  align-items: center;
  gap: 0.25rem;
  margin-bottom: 0.45rem;
}

.ios-date-head strong {
  text-align: center;
  font-size: 0.95rem;
  font-weight: 650;
  letter-spacing: -0.02em;
  color: var(--ink, #1e1a2e);
}

.ios-date-nav {
  width: 2rem;
  height: 2rem;
  border: 0;
  border-radius: 999px;
  background: rgba(124, 108, 240, 0.1);
  color: var(--primary, #7c6cf0);
  font-size: 1.15rem;
  line-height: 1;
  cursor: pointer;
  display: grid;
  place-items: center;
}

.ios-date-nav:hover {
  background: rgba(124, 108, 240, 0.18);
}

.ios-date-week,
.ios-date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0.1rem 0;
}

.ios-date-week {
  margin-bottom: 0.15rem;
}

.ios-date-week span {
  text-align: center;
  font-size: 0.68rem;
  font-weight: 650;
  color: var(--muted, #6b6280);
  padding: 0.2rem 0;
}

.ios-date-day {
  position: relative;
  aspect-ratio: 1;
  border: 0;
  background: transparent;
  color: var(--ink, #1e1a2e);
  font: inherit;
  font-size: 0.84rem;
  font-weight: 500;
  letter-spacing: -0.02em;
  cursor: pointer;
  display: grid;
  place-items: center;
  padding: 0;
  border-radius: 0;
}

.ios-day-bg {
  position: absolute;
  inset: 0.12rem;
  border-radius: 999px;
  z-index: 0;
}

.ios-day-num {
  position: relative;
  z-index: 1;
}

.ios-date-day.is-out {
  color: rgba(107, 98, 128, 0.4);
}

.ios-date-day.is-in-range .ios-day-bg {
  inset: 0.12rem 0;
  border-radius: 0;
  background: rgba(124, 108, 240, 0.14);
}

.ios-date-day.is-start.is-in-range .ios-day-bg,
.ios-date-day.is-start.is-end .ios-day-bg,
.ios-date-day.is-end.is-in-range .ios-day-bg {
  /* ends handled below */
}

.ios-date-day.is-start:not(.is-end)::before,
.ios-date-day.is-end:not(.is-start)::before {
  content: "";
  position: absolute;
  top: 0.12rem;
  bottom: 0.12rem;
  background: rgba(124, 108, 240, 0.14);
  z-index: 0;
}

.ios-date-day.is-start:not(.is-end)::before {
  left: 50%;
  right: 0;
}

.ios-date-day.is-end:not(.is-start)::before {
  left: 0;
  right: 50%;
}

.ios-date-day.is-today:not(.is-selected) .ios-day-num {
  color: var(--primary, #7c6cf0);
  font-weight: 650;
}

.ios-date-day.is-today:not(.is-selected) .ios-day-bg {
  box-shadow: inset 0 0 0 1.5px rgba(124, 108, 240, 0.55);
}

.ios-date-day.is-selected .ios-day-bg {
  background: var(--primary, #7c6cf0);
  box-shadow: 0 4px 12px rgba(124, 108, 240, 0.3);
}

.ios-date-day.is-selected .ios-day-num {
  color: #fff;
  font-weight: 650;
}

.ios-date-day:hover:not(.is-selected) .ios-day-bg {
  background: rgba(124, 108, 240, 0.1);
}

.ios-date-foot,
.ios-range-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding-top: 0.4rem;
}

.ios-date-today,
.ios-date-clear-btn {
  border: 0;
  background: transparent;
  color: var(--primary, #7c6cf0);
  font: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  cursor: pointer;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
}

.ios-date-clear-btn {
  color: var(--muted, #6b6280);
}

.ios-date-clear-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.ios-date-today:hover,
.ios-date-clear-btn:hover:not(:disabled) {
  background: rgba(124, 108, 240, 0.1);
  color: var(--primary, #7c6cf0);
}

.ios-date-panel-float {
  position: fixed;
  top: auto;
  left: auto;
  z-index: 1600;
}
</style>
