<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  variant?: 'default' | 'soft' | 'gradient' | 'danger'
  padding?: 'none' | 'sm' | 'md' | 'lg'
  radius?: 'xl' | '2xl' | '3xl'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  padding: 'md',
  radius: '3xl',
})

const classes = computed(() =>
  cn(
    'border shadow-sm',
    {
      'rounded-xl': props.radius === 'xl',
      'rounded-2xl': props.radius === '2xl',
      'rounded-3xl': props.radius === '3xl',
    },
    {
      'p-0': props.padding === 'none',
      'p-4': props.padding === 'sm',
      'p-6': props.padding === 'md',
      'p-8': props.padding === 'lg',
    },
    {
      'border-slate-200 bg-white': props.variant === 'default',
      'border-slate-200 bg-slate-50': props.variant === 'soft',
      'border-[#D9E7FF] bg-gradient-to-br from-white via-[#F8FBFF] to-[#EEF4FF]':
        props.variant === 'gradient',
      'border-red-200 bg-red-50': props.variant === 'danger',
    },
    props.class
  )
)
</script>

<template>
  <div :class="classes" v-bind="$attrs">
    <slot />
  </div>
</template>
