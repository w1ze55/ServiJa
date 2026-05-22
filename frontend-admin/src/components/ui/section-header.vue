<script setup lang="ts">
import HeadingTitle from './heading-title.vue'
import HeadingSubtitle from './heading-subtitle.vue'

interface Props {
  title: string
  subtitle?: string
}

defineProps<Props>()
</script>

<template>
  <div class="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
    <div>
      <div class="mb-2 flex items-center gap-3">
        <slot name="icon" />
        <HeadingTitle>{{ title }}</HeadingTitle>
      </div>
      <HeadingSubtitle v-if="subtitle" class="text-[#435574]">
        {{ subtitle }}
      </HeadingSubtitle>
    </div>

    <div v-if="$slots.actions">
      <slot name="actions" />
    </div>
  </div>
</template>
