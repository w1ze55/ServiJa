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
  <div class="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
    <div>
      <HeadingTitle as="h1" size="lg">{{ title }}</HeadingTitle>
      <HeadingSubtitle v-if="subtitle" class="mt-1">
        {{ subtitle }}
      </HeadingSubtitle>
    </div>

    <div v-if="$slots.actions" class="w-full xl:w-auto">
      <slot name="actions" />
    </div>
  </div>
</template>
