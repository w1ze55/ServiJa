<script setup lang="ts">
import type { Component } from 'vue'
import { computed } from 'vue'
import Card from './card.vue'
import HeadingTitle from './heading-title.vue'
import HeadingSubtitle from './heading-subtitle.vue'
import { cn } from './utils'

interface Props {
  title: string
  description?: string
  icon?: Component
  tone?: 'default' | 'danger'
  compact?: boolean
  surface?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  tone: 'default',
  compact: false,
  surface: true,
})

const wrapperClasses = computed(() =>
  cn(
    props.compact
      ? 'flex items-center justify-center text-center'
      : 'text-center',
    props.class
  )
)

const iconWrapperClasses = computed(() =>
  props.tone === 'danger'
    ? 'bg-red-100 text-red-600'
    : 'bg-slate-100 text-slate-500'
)

const cardVariant = computed(() => (props.tone === 'danger' ? 'danger' : 'default'))
</script>

<template>
  <Card
    v-if="surface"
    :variant="cardVariant"
    :padding="compact ? 'sm' : 'lg'"
    radius="3xl"
    :class="wrapperClasses"
    v-bind="$attrs"
  >
    <div class="mx-auto max-w-2xl">
      <div
        v-if="icon"
        :class="[
          'mx-auto flex items-center justify-center',
          compact ? 'mb-4 h-14 w-14 rounded-2xl' : 'mb-5 h-16 w-16 rounded-3xl',
          iconWrapperClasses,
        ]"
      >
        <component :is="icon" :class="compact ? 'h-7 w-7' : 'h-8 w-8'" />
      </div>

      <HeadingTitle :size="compact ? 'md' : 'lg'">{{ title }}</HeadingTitle>
      <HeadingSubtitle
        v-if="description"
        :tone="tone === 'danger' ? 'danger' : 'muted'"
        class="mx-auto mt-2 max-w-2xl"
      >
        {{ description }}
      </HeadingSubtitle>

      <div v-if="$slots.actions" class="mt-5">
        <slot name="actions" />
      </div>
    </div>
  </Card>

  <div v-else :class="wrapperClasses" v-bind="$attrs">
    <div class="mx-auto max-w-2xl">
      <div
        v-if="icon"
        :class="[
          'mx-auto flex items-center justify-center',
          compact ? 'mb-4 h-14 w-14 rounded-2xl' : 'mb-5 h-16 w-16 rounded-3xl',
          iconWrapperClasses,
        ]"
      >
        <component :is="icon" :class="compact ? 'h-7 w-7' : 'h-8 w-8'" />
      </div>

      <HeadingTitle :size="compact ? 'md' : 'lg'">{{ title }}</HeadingTitle>
      <HeadingSubtitle
        v-if="description"
        :tone="tone === 'danger' ? 'danger' : 'muted'"
        class="mx-auto mt-2 max-w-2xl"
      >
        {{ description }}
      </HeadingSubtitle>

      <div v-if="$slots.actions" class="mt-5">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>
