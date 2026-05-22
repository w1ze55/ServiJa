<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  as?: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  as: 'h2',
  size: 'md',
})

const classes = computed(() =>
  cn(
    'font-semibold text-slate-900',
    {
      'text-sm': props.size === 'sm',
      'text-xl': props.size === 'md',
      'text-2xl font-bold': props.size === 'lg',
      'text-3xl font-semibold': props.size === 'xl',
    },
    props.class
  )
)
</script>

<template>
  <component :is="as" :class="classes" v-bind="$attrs">
    <slot />
  </component>
</template>
