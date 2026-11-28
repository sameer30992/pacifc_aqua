<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PaAvatar from '@/components/PaAvatar.vue'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  /** { value, label, sub?, photo?, image?, kind?: 'user'|'customer' } */
  options: { type: Array, default: () => [] },
  ariaLabel: { type: String, default: 'Select' },
  placeholder: { type: String, default: 'Select' },
  searchable: { type: Boolean, default: false },
  /** When searchable: typing updates modelValue with the query (text filter). */
  freeText: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'change'])

const open = ref(false)
const query = ref('')
/** After pick/focus, show full list until the user types again. */
const suppressFilter = ref(false)
const rootEl = ref(null)
const inputEl = ref(null)
const panelEl = ref(null)
const panelPos = ref({ top: 0, left: 0, width: 0 })

const selectedOption = computed(() =>
  (props.options || []).find((o) => String(o.value) === String(props.modelValue) && o.value !== ''),
)

const displayLabel = computed(() => {
  if (props.freeText && props.modelValue !== '' && props.modelValue != null) {
    return String(props.modelValue)
  }
  return selectedOption.value?.label || props.placeholder
})

const filteredOptions = computed(() => {
  const list = props.options || []
  if (!props.searchable) return list
  if (suppressFilter.value) return list
  const q = query.value.trim().toLowerCase()
  if (!q) return list
  return list.filter((o) => {
    const hay = `${o.label || ''} ${o.sub || ''} ${o.value ?? ''}`.toLowerCase()
    return hay.includes(q)
  })
})

function placePanel() {
  const anchor =
    rootEl.value?.querySelector?.('.ios-select-trigger, .ios-select-input') || rootEl.value
  if (!anchor) return
  const r = anchor.getBoundingClientRect()
  const width = Math.max(r.width, props.searchable ? 240 : 140)
  const gap = 8
  let left = r.left
  if (left + width > window.innerWidth - 8) {
    left = Math.max(8, window.innerWidth - width - 8)
  }
  if (left < 8) left = 8
  let top = r.bottom + gap
  const estH = Math.min(280, 48 + Math.max(filteredOptions.value.length, 1) * 40)
  if (top + estH > window.innerHeight - 8) {
    top = Math.max(8, r.top - estH - gap)
  }
  panelPos.value = { top, left, width }
}

function openPanel() {
  if (!open.value) {
    open.value = true
    if (props.searchable) {
      syncQueryFromValue()
      suppressFilter.value = true
    }
  }
  nextTick(() => {
    placePanel()
    if (props.searchable) {
      inputEl.value?.focus?.()
      inputEl.value?.select?.()
    } else {
      panelEl.value?.focus?.()
    }
  })
}

function syncQueryFromValue() {
  if (props.freeText) {
    query.value = String(props.modelValue ?? '')
    return
  }
  query.value = selectedOption.value?.label || ''
}

function close() {
  open.value = false
  suppressFilter.value = false
  syncQueryFromValue()
}

function toggle() {
  if (open.value) close()
  else openPanel()
}

function pick(opt) {
  emit('update:modelValue', opt.value)
  emit('change', opt.value)
  query.value = opt.label || ''
  suppressFilter.value = true
  close()
}

function clearValue(e) {
  e?.stopPropagation?.()
  e?.preventDefault?.()
  emit('update:modelValue', '')
  emit('change', '')
  query.value = ''
  suppressFilter.value = true
  if (props.searchable) openPanel()
}

function onSearchInput() {
  suppressFilter.value = false
  if (!open.value) open.value = true
  placePanel()
  if (props.freeText) {
    emit('update:modelValue', query.value)
    emit('change', query.value)
  }
}

function onSearchFocus() {
  openPanel()
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
    class="ios-select"
    :class="{ 'is-open': open, 'is-searchable': searchable }"
  >
    <!-- Searchable combobox -->
    <div v-if="searchable" class="ios-select-combo">
      <input
        ref="inputEl"
        v-model="query"
        type="search"
        class="ios-select-input"
        :placeholder="placeholder"
        :aria-label="ariaLabel"
        :aria-expanded="open ? 'true' : 'false'"
        aria-autocomplete="list"
        aria-haspopup="listbox"
        autocomplete="off"
        @focus="onSearchFocus"
        @click="onSearchFocus"
        @input="onSearchInput"
      />
      <button
        v-if="modelValue !== '' && modelValue != null"
        type="button"
        class="ios-select-clear"
        aria-label="Clear"
        @click="clearValue"
      >
        ×
      </button>
      <span v-else class="ios-select-caret" aria-hidden="true">
        <svg viewBox="0 0 12 8" fill="none">
          <path d="M1.2 1.4 6 6.2l4.8-4.8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
    </div>

    <!-- Plain select trigger -->
    <button
      v-else
      type="button"
      class="ios-select-trigger"
      :aria-label="ariaLabel"
      :aria-expanded="open ? 'true' : 'false'"
      aria-haspopup="listbox"
      @click="toggle"
    >
      <span class="ios-select-label" :class="{ 'is-placeholder': !selectedOption }">
        {{ displayLabel }}
      </span>
      <span class="ios-select-caret" aria-hidden="true">
        <svg viewBox="0 0 12 8" fill="none">
          <path d="M1.2 1.4 6 6.2l4.8-4.8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </span>
    </button>

    <Teleport to="body">
      <div
        v-if="open"
        ref="panelEl"
        class="ios-select-panel ios-select-panel-float"
        role="listbox"
        :aria-label="ariaLabel"
        tabindex="-1"
        :style="{
          top: `${panelPos.top}px`,
          left: `${panelPos.left}px`,
          width: `${panelPos.width}px`,
        }"
      >
        <p v-if="!filteredOptions.length" class="ios-select-empty">No matches</p>
        <button
          v-for="opt in filteredOptions"
          :key="`${opt.value}-${opt.label}`"
          type="button"
          role="option"
          class="ios-select-option"
          :class="{
            'is-active': String(opt.value) === String(modelValue),
            'has-media': !!(opt.photo || opt.image || opt.kind),
          }"
          :aria-selected="String(opt.value) === String(modelValue) ? 'true' : 'false'"
          @mousedown.prevent="pick(opt)"
        >
          <img
            v-if="opt.image"
            class="ios-select-option-img"
            :src="opt.image"
            alt=""
          />
          <PaAvatar
            v-else-if="opt.kind || opt.photo || (opt.value !== '' && opt.value != null && !opt.image)"
            class="ios-select-option-av"
            :name="opt.label"
            :photo="opt.photo"
            :kind="opt.kind || 'initials'"
            size="sm"
          />
          <span class="ios-select-option-text">
            <span class="ios-select-option-main">{{ opt.label }}</span>
            <small v-if="opt.sub" class="ios-select-option-sub">{{ opt.sub }}</small>
          </span>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.ios-select {
  position: relative;
  display: inline-flex;
  vertical-align: middle;
  min-width: 0;
  width: 100%;
}

.ios-select-combo {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.ios-select-trigger,
.ios-select-input {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.45rem;
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
  transition:
    box-shadow 0.16s ease,
    border-color 0.16s ease,
    filter 0.16s ease;
}

.ios-select-trigger {
  cursor: pointer;
  transition:
    transform 0.16s cubic-bezier(0.2, 0.8, 0.2, 1),
    box-shadow 0.16s ease,
    border-color 0.16s ease,
    filter 0.16s ease;
}

.ios-select-input {
  appearance: none;
  -webkit-appearance: none;
  outline: none;
}

.ios-select-input::-webkit-search-cancel-button {
  display: none;
}

.ios-select.is-open .ios-select-trigger,
.ios-select.is-open .ios-select-input {
  border-color: rgba(124, 108, 240, 0.45);
  box-shadow: 0 0 0 3px rgba(124, 108, 240, 0.14), var(--shadow-control,
    0 1px 2px rgba(76, 58, 160, 0.06));
}

.ios-select-trigger:active {
  transform: scale(0.98) translateY(1px);
  box-shadow: var(--shadow-btn-press,
    inset 0 2px 5px rgba(40, 30, 80, 0.16));
  filter: brightness(0.97);
  transition-duration: 0.05s;
}

.ios-select-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.ios-select-label.is-placeholder {
  color: var(--muted, #6b6280);
}

.ios-select-caret,
.ios-select-clear {
  position: absolute;
  right: 0.7rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.1rem;
  height: 1.1rem;
  color: var(--muted, #6b6280);
  flex-shrink: 0;
  display: grid;
  place-items: center;
  pointer-events: none;
}

.ios-select-trigger .ios-select-caret {
  position: static;
  transform: none;
  width: 0.7rem;
  height: 0.7rem;
  pointer-events: none;
  transition: transform 0.16s ease;
}

.ios-select.is-open .ios-select-trigger .ios-select-caret,
.ios-select.is-open .ios-select-combo .ios-select-caret {
  transform: translateY(-50%) rotate(180deg);
  color: var(--primary, #7c6cf0);
}

.ios-select.is-open .ios-select-trigger .ios-select-caret {
  transform: rotate(180deg);
}

.ios-select-clear {
  pointer-events: auto;
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 1.05rem;
  line-height: 1;
  border-radius: 999px;
  color: var(--muted, #6b6280);
  padding: 0;
}

.ios-select-clear:hover {
  color: var(--ink, #1e1a2e);
  background: rgba(124, 108, 240, 0.1);
}

.ios-select-caret svg {
  width: 0.7rem;
  height: 0.45rem;
  display: block;
}

.ios-select-panel {
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
  animation: iosSelectIn 0.18s cubic-bezier(0.22, 1, 0.36, 1);
  display: grid;
  gap: 0.12rem;
}

.ios-select-panel-float {
  position: fixed;
}

@keyframes iosSelectIn {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.ios-select-empty {
  margin: 0;
  padding: 0.65rem 0.7rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--muted, #6b6280);
}

.ios-select-option {
  border: 0;
  background: transparent;
  text-align: left;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: var(--ink, #1e1a2e);
  padding: 0.45rem 0.65rem;
  border-radius: 0.65rem;
  cursor: pointer;
  min-height: 2.35rem;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  transition: background 0.12s ease, transform 0.12s ease;
}

.ios-select-option-av,
.ios-select-option-img {
  flex-shrink: 0;
}

.ios-select-option-img {
  width: 1.85rem;
  height: 1.85rem;
  border-radius: 0.4rem;
  object-fit: cover;
  background: #f3f0ff;
  border: 1px solid rgba(124, 108, 240, 0.12);
}

.ios-select-option-text {
  min-width: 0;
  display: grid;
  gap: 0.08rem;
}

.ios-select-option-main {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ios-select-option-sub {
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--muted, #6b6280);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ios-select-option:hover {
  background: rgba(124, 108, 240, 0.1);
}

.ios-select-option.is-active {
  background: rgba(124, 108, 240, 0.14);
  color: var(--primary, #7c6cf0);
}

.ios-select-option.is-active .ios-select-option-sub {
  color: rgba(124, 108, 240, 0.75);
}

.ios-select-option:active {
  transform: scale(0.98);
  background: rgba(124, 108, 240, 0.16);
}
</style>
