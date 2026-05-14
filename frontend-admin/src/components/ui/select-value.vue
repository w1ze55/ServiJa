<script setup lang="ts">
import { computed, inject } from 'vue';

interface Props {
  placeholder?: string;
  label?: string;
  [key: string]: unknown;
}

const props = defineProps<Props>();
const select = inject<{ value: { value: string } }>('select');

const displayValue = computed(() => {
  if (props.label) {
    return props.label;
  }

  if (select?.value.value) {
    return select.value.value;
  }

  return props.placeholder || '';
});
</script>

<template>
  <span
    class="block min-w-0 overflow-hidden text-ellipsis whitespace-nowrap"
    data-slot="select-value"
    :data-placeholder="!select?.value.value"
    v-bind="$attrs"
  >
    {{ displayValue }}
  </span>
</template>
