<script setup lang="ts">
import { computed, provide, ref } from 'vue'

interface Props {
  open?: boolean
  [key: string]: unknown
}

const props = defineProps<Props>()
const emit = defineEmits<{ 'update:open': [value: boolean] }>()

const internalOpen = ref(false)
const isOpen = computed({
  get: () => (props.open !== undefined ? props.open : internalOpen.value),
  set: (value: boolean) => {
    if (props.open !== undefined) {
      emit('update:open', value)
    } else {
      internalOpen.value = value
    }
  },
})
const triggerRef = ref<HTMLElement | null>(null)

provide('dropdownMenu', {
  isOpen,
  triggerRef,
  toggle: () => {
    isOpen.value = !isOpen.value
  },
  close: () => {
    isOpen.value = false
  },
})
</script>

<template>
  <div ref="triggerRef" class="relative" v-bind="$attrs">
    <slot />
  </div>
</template>
