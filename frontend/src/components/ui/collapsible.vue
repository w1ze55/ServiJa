<script setup lang="ts">
import { ref, provide, computed } from 'vue';

interface Props {
  open?: boolean;
  defaultOpen?: boolean;
  disabled?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = computed({
  get: () => props.open ?? props.defaultOpen ?? false,
  set: (value) => emit('update:open', value),
});

provide('collapsible', {
  isOpen,
});
</script>

<template>
  <div
    data-slot="collapsible"
    v-bind="$attrs"
  >
    <slot :isOpen="isOpen" />
  </div>
</template>

