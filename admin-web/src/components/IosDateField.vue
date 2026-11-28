<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  ariaLabel: { type: String, default: 'Date' },
  placeholder: { type: String, default: 'Select date' },
})

const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)
const rootEl = ref(null)
const panelEl = ref(null)
const panelPos = ref({ top: 0, left: 0 })
const viewYear = ref(new Date().getFullYear())
const viewMonth = ref(new Date().getMonth()) // 0-11

const weekdays = ['S', 'M', 'T', 'W', 'T', 'F', 'S']
const monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]

const selected = computed(() => parseIso(props.modelValue))

const displayLabel = computed(() => {
  const d = selected.value
  if (!d) return props.placeholder
  return d.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  })
})

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

function sameDay(a, b) {
  return (
    a &&
    b &&
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

function isToday(date) {
  return sameDay(date, new Date())
}

function syncViewToValue() {
  const d = selected.value || new Date()
  viewYear.value = d.getFullYear()
  viewMonth.value = d.getMonth()
}

function placePanel() {
  const btn = rootEl.value?.querySelector?.('.ios-date-trigger') || rootEl.value
  if (!btn) return
  const r = btn.getBoundingClientRect()
  const panelW = 280
  const gap = 8
  let left = r.left
  if (left + panelW > window.innerWidth - 8) {
    left = Math.max(8, r.right - panelW)
  }
  if (left < 8) left = 8
  let top = r.bottom + gap
  const estH = 320
  if (top + estH > window.innerHeight - 8) {
    top = Math.max(8, r.top - estH - gap)
  }
  panelPos.value = { top, left }
}

function toggle() {
  open.value = !open.value
  if (open.value) {
    syncViewToValue()
    placePanel()
  }
}

function close() {
  open.value = false
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

function pick(date) {
  const iso = toIso(date)
  emit('update:modelValue', iso)
  emit('change', iso)
  close()
}

function pickToday() {
  pick(new Date())
}

function clearDate() {
  emit('update:modelValue', '')
  emit('change', '')
  close()
}

function onDocPointer(e) {
  if (!open.value) return
  const t = e.target
  if (rootEl.value?.contains(t)) return
  if (panelEl.value?.contains(t)) return
  close()
}

function onKey(e) {
  if (e.key === 'Escape' && open.value) close()
}

function onReposition() {
  if (!open.value) return
  placePanel()
}

watch(
  () => props.modelValue,
  () => {
    if (open.value) syncViewToValue()
  },
)

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

async function focusPanel() {
  await nextTick()
  placePanel()
  panelEl.value?.focus?.()
}

watch(open, (v) => {
  if (v) focusPanel()
})
</script>

<template>
  <div ref="rootEl" class="ios-date" :class="{ 'is-open': open }">
    <button
      type="button"
      class="ios-date-trigger"
      :aria-label="ariaLabel"
      :aria-expanded="open ? 'true' : 'false'"
      aria-haspopup="dialog"
      @click="toggle"
    >
      <span class="ios-date-label" :class="{ 'is-placeholder': !selected }">
        {{ displayLabel }}
      </span>
      <span class="ios-date-ico" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <rect x="3" y="5" width="18" height="16" rx="2.5" stroke="currentColor" stroke-width="1.8" />
          <path d="M8 3v4M16 3v4M3 11h18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
        </svg>
      </span>
    </button>

    <Teleport to="body">
      <div
        v-if="open"
        ref="panelEl"
        class="ios-date-panel ios-date-panel-float"
        role="dialog"
        aria-modal="false"
        :aria-label="ariaLabel"
        tabindex="-1"
        :style="{ top: `${panelPos.top}px`, left: `${panelPos.left}px` }"
      >
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
              'is-selected': sameDay(cell.date, selected),
            }"
            :aria-label="cell.date.toLocaleDateString('en-IN')"
            :aria-pressed="sameDay(cell.date, selected) ? 'true' : 'false'"
            @click="pick(cell.date)"
          >
            {{ cell.day }}
          </button>
        </div>

        <div class="ios-date-foot ios-date-foot-split">
          <button
            type="button"
            class="ios-date-clear-btn"
            :disabled="!selected"
            @click="clearDate"
          >
            Clear
          </button>
          <button type="button" class="ios-date-today" @click="pickToday">Today</button>
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
  min-width: 9.5rem;
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
  box-shadow: 0 0 0 3px rgba(124, 108, 240, 0.14), var(--shadow-control,
    0 1px 2px rgba(76, 58, 160, 0.06));
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

.ios-date-panel {
  position: absolute;
  top: calc(100% + 0.4rem);
  left: 0;
  z-index: 60;
  width: 17.5rem;
  padding: 0.7rem 0.7rem 0.55rem;
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

.ios-date-panel-float {
  position: fixed;
  top: auto;
  left: auto;
  z-index: 1600;
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
  gap: 0.15rem;
}

.ios-date-week {
  margin-bottom: 0.2rem;
}

.ios-date-week span {
  text-align: center;
  font-size: 0.68rem;
  font-weight: 650;
  color: var(--muted, #6b6280);
  letter-spacing: -0.01em;
  padding: 0.2rem 0;
}

.ios-date-day {
  aspect-ratio: 1;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--ink, #1e1a2e);
  font: inherit;
  font-size: 0.84rem;
  font-weight: 500;
  letter-spacing: -0.02em;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: background 0.15s ease, color 0.15s ease, transform 0.15s ease;
}

.ios-date-day:hover {
  background: rgba(124, 108, 240, 0.1);
}

.ios-date-day.is-out {
  color: rgba(107, 98, 128, 0.4);
}

.ios-date-day.is-today:not(.is-selected) {
  box-shadow: inset 0 0 0 1.5px rgba(124, 108, 240, 0.55);
  color: var(--primary, #7c6cf0);
  font-weight: 650;
}

.ios-date-day.is-selected {
  background: var(--primary, #7c6cf0);
  color: #fff;
  font-weight: 650;
  box-shadow:
    0 4px 12px rgba(124, 108, 240, 0.35),
    inset 0 0.5px 0 rgba(255, 255, 255, 0.35);
}

.ios-date-day:active {
  transform: scale(0.92);
}

.ios-date-foot {
  display: flex;
  justify-content: center;
  padding-top: 0.35rem;
}

.ios-date-foot-split {
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
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
</style>
