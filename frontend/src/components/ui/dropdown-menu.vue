<script setup lang="ts">
import { ref, provide, computed } from 'vue';

interface Props {
  [key: string]: any;
}

const props = defineProps<Props>();
const isOpen = ref(false);
const triggerRef = ref<HTMLElement | null>(null);

provide('dropdownMenu', {
  isOpen,
  triggerRef,
  toggle: () => {
    isOpen.value = !isOpen.value;
  },
  close: () => {
    isOpen.value = false;
  },
});
</script>

<template>
  <div class="relative" ref="triggerRef" v-bind="$attrs">
    <slot />
  </div>
</template>

<script lang="ts">
export { default as DropdownMenuTrigger } from './dropdown-menu-trigger.vue';
export { default as DropdownMenuContent } from './dropdown-menu-content.vue';
export { default as DropdownMenuItem } from './dropdown-menu-item.vue';
export { default as DropdownMenuSeparator } from './dropdown-menu-separator.vue';
</script>

