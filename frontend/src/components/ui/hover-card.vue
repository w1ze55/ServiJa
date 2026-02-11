<script setup lang="ts">
import { ref, provide } from 'vue';

interface Props {
  open?: boolean;
  defaultOpen?: boolean;
  openDelay?: number;
  closeDelay?: number;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  openDelay: 700,
  closeDelay: 300,
});

const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = ref(props.open ?? props.defaultOpen ?? false);
let openTimeout: ReturnType<typeof setTimeout> | null = null;
let closeTimeout: ReturnType<typeof setTimeout> | null = null;

const handleMouseEnter = () => {
  if (closeTimeout) {
    clearTimeout(closeTimeout);
    closeTimeout = null;
  }
  openTimeout = setTimeout(() => {
    isOpen.value = true;
    emit('update:open', true);
  }, props.openDelay);
};

const handleMouseLeave = () => {
  if (openTimeout) {
    clearTimeout(openTimeout);
    openTimeout = null;
  }
  closeTimeout = setTimeout(() => {
    isOpen.value = false;
    emit('update:open', false);
  }, props.closeDelay);
};

provide('hoverCard', {
  isOpen,
  handleMouseEnter,
  handleMouseLeave,
});
</script>

<template>
  <div
    data-slot="hover-card"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


