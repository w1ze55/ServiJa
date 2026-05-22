<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDownRight, ArrowUpRight } from 'lucide-vue-next'
import { cn } from './utils'

interface Props {
  changePercent: number
  class?: string
}

const props = defineProps<Props>()

const classes = computed(() =>
  cn(
    'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium',
    props.changePercent >= 0
      ? 'bg-green-100 text-green-700'
      : 'bg-red-100 text-red-700',
    props.class
  )
)

const formattedPercent = computed(() =>
  `${Math.abs(props.changePercent || 0).toLocaleString('pt-BR', {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
  })}%`
)
</script>

<template>
  <span :class="classes" v-bind="$attrs">
    <ArrowUpRight v-if="changePercent >= 0" class="h-3.5 w-3.5" />
    <ArrowDownRight v-else class="h-3.5 w-3.5" />
    {{ formattedPercent }}
  </span>
</template>
