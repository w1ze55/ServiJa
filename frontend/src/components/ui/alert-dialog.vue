<script setup lang="ts">
import { computed, provide } from 'vue';

interface Props {
  open?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = computed({
  get: () => props.open || false,
  set: (value) => emit('update:open', value),
});

provide('alertDialog', {
  isOpen,
});
</script>

<template>
  <div v-bind="$attrs">
    <slot />
  </div>
</template>

