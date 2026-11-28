<script setup>
import { computed } from 'vue'
import { avatarTone, customerInitials, resolvePhotoUrl } from '@/utils/avatar'

const props = defineProps({
  name: { type: String, default: '' },
  photo: { type: String, default: '' },
  size: { type: String, default: 'sm' },
  /** initials | user — user falls back to brand silhouette when no photo */
  kind: { type: String, default: 'initials' },
})

const photoUrl = computed(() => resolvePhotoUrl(props.photo))
const initials = computed(() => customerInitials(props.name))
const tone = computed(() => avatarTone(props.name))
const showUserFallback = computed(() => !photoUrl.value && props.kind === 'user')
</script>

<template>
  <span
    class="pa-avatar"
    :class="[
      `pa-av-${size}`,
      photoUrl || showUserFallback ? 'has-img' : `pa-tone-${tone}`,
      showUserFallback ? 'pa-user-fallback' : null,
    ]"
    :title="name"
  >
    <img v-if="photoUrl" :src="photoUrl" alt="" />
    <img v-else-if="showUserFallback" src="/assets/brand/user-avatar.svg" alt="" />
    <template v-else>{{ initials }}</template>
  </span>
</template>
