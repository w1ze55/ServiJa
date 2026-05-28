<script setup lang="ts">
import { computed, inject } from 'vue'
import { cn } from './utils'

interface Props {
  class?: string
  [key: string]: unknown
}

const props = defineProps<Props>()
const dropdownMenu = inject<{ close: () => void }>('dropdownMenu')

const classes = computed(() =>
  cn(
    'relative flex cursor-default select-none items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none hover:bg-slate-100 hover:text-slate-900',
    props.class
  )
)

const handleClick = () => {
  dropdownMenu?.close()
}
</script>

<template>
  <div :class="classes" data-slot="dropdown-menu-item" v-bind="$attrs" @click="handleClick">
    <slot />
  </div>
</template>
