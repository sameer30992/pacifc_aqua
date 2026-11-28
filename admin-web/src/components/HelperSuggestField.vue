<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PaAvatar from '@/components/PaAvatar.vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  helpers: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'All helpers' },
  ariaLabel: { type: String, default: 'Helper' },
  label: { type: String, default: '' },
  /** Show “All helpers” row (empty value). */
  includeAll: { type: Boolean, default: true },
  allLabel: { type: String, default: 'All helpers' },
  clearable: { type: Boolean, default: true },
  wrapClass: { type: [String, Object, Array], default: '' },
})

const emit = defineEmits(['update:modelValue', 'change', 'select', 'clear'])

const open = ref(false)
const query = ref('')
const suppressFilter = ref(false)
const rootEl = ref(null)
const inputEl = ref(null)
const panelEl = ref(null)
const panelPos = ref({ top: 0, left: 0, width: 0 })

const selectedHelper = computed(() =>
  (props.helpers || []).find((h) => String(h.id) === String(props.modelValue)),
)

const options = computed(() => {
  const list = (props.helpers || []).map((h) => ({
    value: h.id,
    label: h.name || '—',
    sub: h.phone || h.role || '',
    photo: h.avatarPath,
    raw: h,
  }))
  if (props.includeAll) {
    return [{ value: '', label: props.allLabel, sub: '', photo: '', raw: null }, ...list]
  }
  return list
})

const filteredOptions = computed(() => {
  if (suppressFilter.value) return options.value
  const q = query.value.trim().toLowerCase()
  if (!q) return options.value
  return options.value.filter((o) => {
    if (o.value === '' || o.value == null) return props.allLabel.toLowerCase().includes(q)
    const hay = `${o.label} ${o.sub}`.toLowerCase()
    return hay.includes(q)
  })
})

function syncQueryFromValue() {
  query.value = selectedHelper.value?.name || (props.modelValue === '' || props.modelValue == null ? '' : '')
}

function placePanel() {
  const anchor = rootEl.value?.querySelector?.('.helper-suggest-input') || rootEl.value
  if (!anchor) return
  const r = anchor.getBoundingClientRect()
  const width = Math.max(r.width, 220)
  const gap = 8
  let left = r.left
  if (left + width > window.innerWidth - 8) left = Math.max(8, window.innerWidth - width - 8)
  if (left < 8) left = 8
  let top = r.bottom + gap
  const estH = Math.min(280, 48 + Math.max(filteredOptions.value.length, 1) * 44)
  if (top + estH > window.innerHeight - 8) top = Math.max(8, r.top - estH - gap)
  panelPos.value = { top, left, width }
}

function openPanel() {
  if (!open.value) {
    open.value = true
    syncQueryFromValue()
    suppressFilter.value = true
  }
  nextTick(() => {
    placePanel()
    inputEl.value?.focus?.()
    inputEl.value?.select?.()
  })
}

function close() {
  open.value = false
  suppressFilter.value = false
  syncQueryFromValue()
}

function pick(opt) {
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  if (opt.raw) emit('select', opt.raw)
  else emit('select', null)
  query.value = opt.value === '' || opt.value == null ? '' : opt.label
  suppressFilter.value = true
  close()
}

function clearValue(e) {
  e?.stopPropagation?.()
  e?.preventDefault?.()
  emit('update:modelValue', '')
  emit('change', '')
  emit('clear')
  emit('select', null)
  query.value = ''
  suppressFilter.value = true
  openPanel()
}

function onSearchInput() {
  suppressFilter.value = false
  if (!open.value) open.value = true
  placePanel()
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
  if (open.value) placePanel()
}

onMounted(() => {
  syncQueryFromValue()
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

watch(
  () => props.modelValue,
  () => {
    if (!open.value) syncQueryFromValue()
  },
)
</script>

<template>
  <div
    ref="rootEl"
    class="helper-suggest-field"
    :class="[wrapClass, { 'is-open': open }]"
  >
    <label v-if="label">{{ label }}</label>
    <div class="helper-suggest-combo">
      <input
        ref="inputEl"
        v-model="query"
        type="search"
        class="helper-suggest-input"
        :placeholder="placeholder"
        :aria-label="ariaLabel"
        :aria-expanded="open ? 'true' : 'false'"
        aria-autocomplete="list"
        aria-haspopup="listbox"
        autocomplete="off"
        @focus="openPanel"
        @click="openPanel"
        @input="onSearchInput"
      />
      <button
        v-if="clearable && modelValue !== '' && modelValue != null"
        type="button"
        class="helper-suggest-clear"
        aria-label="Clear helper"
        @click="clearValue"
      >
        ×
      </button>
      <span v-else class="helper-suggest-caret" aria-hidden="true">
        <svg viewBox="0 0 12 8" fill="none">
          <path d="M1.2 1.4 6 6.2l4.8-4.8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
    </div>

    <Teleport to="body">
      <div
        v-if="open"
        ref="panelEl"
        class="helper-suggest-panel"
        role="listbox"
        :aria-label="ariaLabel"
        tabindex="-1"
        :style="{
          top: `${panelPos.top}px`,
          left: `${panelPos.left}px`,
          width: `${panelPos.width}px`,
        }"
      >
        <p v-if="!filteredOptions.length" class="helper-suggest-empty">No helper match</p>
        <button
          v-for="opt in filteredOptions"
          :key="`${opt.value}-${opt.label}`"
          type="button"
          role="option"
          class="helper-suggest-option"
          :class="{ 'is-active': String(opt.value) === String(modelValue) }"
          :aria-selected="String(opt.value) === String(modelValue) ? 'true' : 'false'"
          @mousedown.prevent="pick(opt)"
        >
          <PaAvatar
            v-if="opt.value !== '' && opt.value != null"
            class="helper-suggest-av"
            :name="opt.label"
            :photo="opt.photo"
            kind="user"
            size="sm"
          />
          <span v-else class="helper-suggest-all-ico" aria-hidden="true">☰</span>
          <span class="helper-suggest-text">
            <strong>{{ opt.label }}</strong>
            <small v-if="opt.sub">{{ opt.sub }}</small>
          </span>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.helper-suggest-field {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
  width: 100%;
  vertical-align: middle;
}

.helper-suggest-field > label {
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--muted, #6b6280);
}

.helper-suggest-combo {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.helper-suggest-input {
  width: 100%;
  height: var(--control-h, 2.6rem);
  min-height: var(--control-h, 2.6rem);
  padding: 0 2rem 0 0.9rem;
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
  box-sizing: border-box;
  outline: none;
  appearance: none;
  -webkit-appearance: none;
}

.helper-suggest-input::-webkit-search-cancel-button {
  display: none;
}

.helper-suggest-field.is-open .helper-suggest-input {
  border-color: rgba(124, 108, 240, 0.45);
  box-shadow: 0 0 0 3px rgba(124, 108, 240, 0.14), var(--shadow-control,
    0 1px 2px rgba(76, 58, 160, 0.06));
}

.helper-suggest-caret,
.helper-suggest-clear {
  position: absolute;
  right: 0.7rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.1rem;
  height: 1.1rem;
  color: var(--muted, #6b6280);
  display: grid;
  place-items: center;
  pointer-events: none;
}

.helper-suggest-clear {
  pointer-events: auto;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 1.05rem;
  line-height: 1;
  border-radius: 999px;
  padding: 0;
}

.helper-suggest-clear:hover {
  color: var(--ink, #1e1a2e);
  background: rgba(124, 108, 240, 0.1);
}

.helper-suggest-field.is-open .helper-suggest-caret {
  transform: translateY(-50%) rotate(180deg);
  color: var(--primary, #7c6cf0);
}

.helper-suggest-caret svg {
  width: 0.7rem;
  height: 0.45rem;
  display: block;
}

.helper-suggest-panel {
  position: fixed;
  z-index: 1600;
  max-height: 16rem;
  overflow: auto;
  padding: 0.35rem;
  border-radius: 0.95rem;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: #ffffff;
  box-shadow:
    0 18px 40px rgba(76, 58, 160, 0.16),
    0 1px 0 rgba(255, 255, 255, 0.9) inset;
  outline: none;
  display: grid;
  gap: 0.12rem;
  animation: helperSuggestIn 0.18s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes helperSuggestIn {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.helper-suggest-empty {
  margin: 0;
  padding: 0.65rem 0.7rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--muted, #6b6280);
}

.helper-suggest-option {
  border: 0;
  background: transparent;
  text-align: left;
  font: inherit;
  color: var(--ink, #1e1a2e);
  padding: 0.45rem 0.65rem;
  border-radius: 0.65rem;
  cursor: pointer;
  min-height: 2.45rem;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  transition: background 0.12s ease, transform 0.12s ease;
}

.helper-suggest-option:hover {
  background: rgba(124, 108, 240, 0.1);
}

.helper-suggest-option.is-active {
  background: rgba(124, 108, 240, 0.14);
  color: var(--primary, #7c6cf0);
}

.helper-suggest-option:active {
  transform: scale(0.98);
}

.helper-suggest-av {
  flex-shrink: 0;
}

.helper-suggest-all-ico {
  width: 1.85rem;
  height: 1.85rem;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  background: rgba(124, 108, 240, 0.1);
  color: var(--primary, #7c6cf0);
  font-size: 0.7rem;
}

.helper-suggest-text {
  min-width: 0;
  display: grid;
  gap: 0.08rem;
}

.helper-suggest-text strong {
  font-size: 0.84rem;
  font-weight: 650;
  letter-spacing: -0.015em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.helper-suggest-text small {
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--muted, #6b6280);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.helper-suggest-option.is-active .helper-suggest-text small {
  color: rgba(124, 108, 240, 0.75);
}
</style>
