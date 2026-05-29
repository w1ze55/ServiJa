<script setup lang="ts">
import type { Component } from 'vue'
import Card from './card.vue'
import HeadingSubtitle from './heading-subtitle.vue'
import HeadingTitle from './heading-title.vue'
import TrendBadge from './trend-badge.vue'
import { cn } from './utils'

interface Props {
  title: string
  subtitle?: string
  value: string
  supportingText?: string
  changePercent?: number | null
  icon?: Component
  iconWrapperClass?: string
  iconClass?: string
  compact?: boolean
  badgePosition?: 'absolute' | 'inline'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  subtitle: '',
  supportingText: '',
  changePercent: null,
  compact: false,
  badgePosition: 'absolute',
  iconWrapperClass: 'bg-slate-100',
  iconClass: 'text-slate-600',
})
</script>

<template>
  <Card
    radius="3xl"
    :padding="compact ? 'sm' : 'md'"
    :class="cn('relative', compact ? '' : 'overflow-hidden', props.class)"
    v-bind="$attrs"
  >
    <div :class="compact ? 'mb-4 flex items-center gap-3' : 'mb-5 flex items-center gap-3'">
      <div
        v-if="icon"
        :class="[
          'flex items-center justify-center rounded-2xl',
          compact ? 'h-11 w-11' : 'h-12 w-12',
          iconWrapperClass,
        ]"
      >
        <component :is="icon" :class="[compact ? 'h-5 w-5' : 'h-6 w-6', iconClass]" />
      </div>

      <div>
        <HeadingSubtitle as="p" size="sm" tone="default" class="font-medium text-[#435574]">
          {{ title }}
        </HeadingSubtitle>
        <HeadingSubtitle v-if="subtitle" size="xs">
          {{ subtitle }}
        </HeadingSubtitle>
      </div>
    </div>

    <p :class="compact ? 'text-2xl font-semibold text-[#263238]' : 'text-3xl font-semibold text-[#263238]'">
      {{ value }}
    </p>

    <HeadingSubtitle
      v-if="supportingText"
      size="xs"
      :class="compact ? 'mt-2' : 'mt-2 pr-24 text-[#6B7A90]'"
    >
      {{ supportingText }}
    </HeadingSubtitle>

    <TrendBadge
      v-if="typeof changePercent === 'number'"
      :change-percent="changePercent"
      :class="badgePosition === 'absolute' ? 'absolute bottom-6 right-6' : 'mt-4'"
    />
  </Card>
</template>
