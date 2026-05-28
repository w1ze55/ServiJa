<script setup lang="ts">
import { computed, inject, nextTick, onUnmounted, ref, watch } from 'vue'
import { cn } from './utils'

interface Props {
  align?: 'start' | 'end' | 'center'
  class?: string
  [key: string]: unknown
}

const props = withDefaults(defineProps<Props>(), {
  align: 'end',
})

const dropdownMenu = inject<{
  isOpen: { value: boolean }
  close: () => void
  triggerRef?: { value: HTMLElement | null }
}>('dropdownMenu')

const isOpen = computed(() => dropdownMenu?.isOpen.value || false)
const contentRef = ref<HTMLElement | null>(null)
const position = ref({ top: '0px', left: '0px', transform: 'none' })

const classes = computed(() =>
  cn(
    'z-50 min-w-[8rem] overflow-hidden rounded-md border border-slate-200 bg-white p-1 text-slate-900 shadow-md',
    props.class
  )
)

const updatePosition = () => {
  if (!contentRef.value || !dropdownMenu?.triggerRef?.value) return

  const trigger = dropdownMenu.triggerRef.value
  const content = contentRef.value
  const triggerRect = trigger.getBoundingClientRect()
  const contentRect = content.getBoundingClientRect()

  let top = triggerRect.bottom + 8
  let left = triggerRect.left

  if (props.align === 'end') {
    left = triggerRect.right - contentRect.width
  } else if (props.align === 'center') {
    left = triggerRect.left + (triggerRect.width - contentRect.width) / 2
  }

  const spaceBelow = window.innerHeight - triggerRect.bottom
  const spaceAbove = triggerRect.top

  if (spaceBelow < contentRect.height && spaceAbove > contentRect.height) {
    top = triggerRect.top - contentRect.height - 8
  } else if (spaceBelow < contentRect.height) {
    top = window.innerHeight - contentRect.height - 8
  }

  if (left + contentRect.width > window.innerWidth) {
    left = window.innerWidth - contentRect.width - 8
  }
  if (left < 8) {
    left = 8
  }

  position.value = {
    top: `${top}px`,
    left: `${left}px`,
    transform: 'none',
  }
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (contentRef.value && !contentRef.value.contains(target)) {
    const trigger = dropdownMenu?.triggerRef?.value
    if (trigger && !trigger.contains(target)) {
      dropdownMenu?.close()
    }
  }
}

let updateOnResize: (() => void) | null = null

watch(isOpen, async (value) => {
  if (value) {
    await nextTick()
    updatePosition()
    updateOnResize = () => updatePosition()
    window.addEventListener('resize', updateOnResize)
    window.addEventListener('scroll', updateOnResize, true)

    setTimeout(() => {
      document.addEventListener('click', handleClickOutside)
    }, 0)
  } else {
    if (updateOnResize) {
      window.removeEventListener('resize', updateOnResize)
      window.removeEventListener('scroll', updateOnResize, true)
      updateOnResize = null
    }
    document.removeEventListener('click', handleClickOutside)
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (updateOnResize) {
    window.removeEventListener('resize', updateOnResize)
    window.removeEventListener('scroll', updateOnResize, true)
  }
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      ref="contentRef"
      :class="classes"
      :style="{
        position: 'fixed',
        ...position,
      }"
      v-bind="$attrs"
    >
      <slot />
    </div>
  </Teleport>
</template>
