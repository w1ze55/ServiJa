<script setup lang="ts">
import { ref, provide } from 'vue';

interface Props {
  open?: boolean;
  defaultOpen?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = ref(props.open ?? props.defaultOpen ?? false);

provide('popover', {
  isOpen,
});
</script>

<template>
  <div v-bind="$attrs">
    <slot />
  </div>
</template>


