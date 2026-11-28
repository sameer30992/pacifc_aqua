<script setup>
import { computed, ref } from 'vue'
import PaAvatar from '@/components/PaAvatar.vue'
import { useCustomersStore } from '@/stores/customers'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: 'Search name, phone, or area…' },
  ariaLabel: { type: String, default: 'Search customer' },
  label: { type: String, default: '' },
  clearable: { type: Boolean, default: true },
  limit: { type: Number, default: 40 },
  /** Extra class on outer wrap (e.g. sales-search-wrap). */
  wrapClass: { type: [String, Object, Array], default: '' },
  /** Extra class on the input. */
  inputClass: { type: [String, Object, Array], default: '' },
  /**
   * Optional external list. When omitted, uses customers store.
   * Items: { id, name, phone, cityArea, avatarPath, cardNo? }
   */
  items: { type: Array, default: null },
  itemsLoading: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'select', 'clear', 'enter', 'open', 'close'])

const customersStore = useCustomersStore()
const open = ref(false)
const picking = ref(false)

const loading = computed(() => {
  if (props.items) return props.itemsLoading
  return customersStore.loading && !customersStore.list.length
})

const matches = computed(() => {
  const q = (props.modelValue || '').trim()
  if (props.items) {
    const list = props.items || []
    if (!q) return list.slice(0, props.limit)
    const term = q.toLowerCase()
    return list
      .filter((c) => {
        const hay = [c.name, c.phone, c.cityArea, c.cardNo]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        return hay.includes(term)
      })
      .slice(0, props.limit)
  }
  return customersStore.searchLocal(props.modelValue, props.limit)
})

function setQuery(v) {
  emit('update:modelValue', v)
}

async function openSuggest() {
  open.value = true
  emit('open')
  if (!props.items) await customersStore.ensureLoaded()
}

function onInput(e) {
  setQuery(e.target.value)
  open.value = true
}

function closeSuggest() {
  setTimeout(() => {
    if (picking.value) return
    open.value = false
    emit('close')
  }, 180)
}

function selectCustomer(c) {
  picking.value = true
  open.value = false
  emit('select', c)
  setTimeout(() => {
    picking.value = false
  }, 0)
}

function clearSearch(e) {
  e?.preventDefault?.()
  setQuery('')
  open.value = false
  emit('clear')
}

function onEnter() {
  open.value = false
  emit('enter')
}

function secondaryLine(c) {
  return c.cityArea || c.cardNo || 'No address'
}
</script>

<template>
  <div
    class="cust-search-field customer-suggest-field"
    :class="[wrapClass, { 'is-open': open }]"
  >
    <label v-if="label">{{ label }}</label>
    <div class="cust-search">
      <input
        :value="modelValue"
        type="search"
        :placeholder="placeholder"
        :class="inputClass"
        autocomplete="off"
        aria-autocomplete="list"
        :aria-label="ariaLabel"
        :aria-expanded="open ? 'true' : 'false'"
        @focus="openSuggest"
        @click="openSuggest"
        @input="onInput"
        @blur="closeSuggest"
        @keyup.enter="onEnter"
      />
      <button
        v-if="clearable && modelValue"
        type="button"
        class="customer-suggest-clear"
        aria-label="Clear search"
        @mousedown.prevent="clearSearch"
      >
        ×
      </button>
      <ul v-if="open" class="cust-suggest" role="listbox">
        <template v-if="loading">
          <li class="cust-suggest-empty cust-suggest-loading" aria-live="polite">
            <span class="cust-suggest-spinner" aria-hidden="true" />
            Loading customers…
          </li>
          <li
            v-for="n in 4"
            :key="`cskel-${n}`"
            class="cust-suggest-skel"
            aria-hidden="true"
          >
            <span class="skel-av" />
            <span class="skel-text">
              <span class="skel-line skel-line-lg" />
              <span class="skel-line skel-line-sm" />
            </span>
          </li>
        </template>
        <li v-else-if="!matches.length" class="cust-suggest-empty">No customer match</li>
        <template v-else>
          <li
            v-for="c in matches"
            :key="c.id"
            class="cust-suggest-item"
            role="option"
            @pointerdown.prevent="selectCustomer(c)"
          >
            <PaAvatar :name="c.name" :photo="c.avatarPath" size="sm" class="cust-suggest-av" />
            <span class="cust-suggest-text">
              <strong>{{ c.name || '(blank)' }}</strong>
              <span v-if="c.phone">{{ c.phone }}</span>
              <span>{{ secondaryLine(c) }}</span>
            </span>
          </li>
        </template>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.customer-suggest-field {
  position: relative;
  min-width: 0;
}

.customer-suggest-field .cust-search {
  position: relative;
  width: 100%;
}

.customer-suggest-field .cust-search input {
  width: 100%;
  padding-right: 2rem;
}

.customer-suggest-clear {
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

.customer-suggest-clear:hover {
  color: var(--ink, #1e1a2e);
  background: rgba(124, 108, 240, 0.1);
}
</style>
