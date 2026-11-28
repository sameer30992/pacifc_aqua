<script setup>
defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: 'Confirm' },
  message: { type: String, default: '' },
  confirmLabel: { type: String, default: 'Confirm' },
  cancelLabel: { type: String, default: 'Cancel' },
  danger: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'cancel'])

function messageLines(message) {
  return String(message || '').split('\n')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="app-dialog">
      <div class="app-dialog-backdrop" @click="emit('cancel')" />
      <div
        class="app-dialog-panel"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="appConfirmTitle"
        aria-describedby="appConfirmMessage"
      >
        <div class="app-dialog-ico" :class="danger ? 'is-danger' : 'is-confirm'" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" stroke-width="1.8" />
            <path d="M12 7v6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            <circle cx="12" cy="16.2" r="1" fill="currentColor" />
          </svg>
        </div>
        <h3 id="appConfirmTitle">{{ title }}</h3>
        <p id="appConfirmMessage">
          <template v-for="(line, i) in messageLines(message)" :key="i">
            <span v-if="line">{{ line }}</span>
            <br v-else />
          </template>
        </p>
        <div class="app-dialog-actions">
          <button type="button" class="btn btn-ghost" @click="emit('cancel')">
            {{ cancelLabel }}
          </button>
          <button
            type="button"
            class="btn btn-primary"
            :class="{ 'app-dialog-danger': danger }"
            @click="emit('confirm')"
          >
            {{ confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
