<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  as?: string
  size?: 'xs' | 'sm' | 'md'
  tone?: 'default' | 'muted' | 'danger'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  as: 'p',
  size: 'sm',
  tone: 'muted',
})

const classes = computed(() =>
  cn(
    {
      'text-xs': props.size === 'xs',
      'text-sm': props.size === 'sm',
      'text-base': props.size === 'md',
    },
    {
      'text-slate-700': props.tone === 'default',
      'text-slate-500': props.tone === 'muted',
      'text-red-700': props.tone === 'danger',
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
