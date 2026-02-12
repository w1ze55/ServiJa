<script setup lang="ts">
import { inject, computed } from 'vue';

interface Props {
  placeholder?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const select = inject<{ value: { value: string } }>('select');

const displayValue = computed(() => {
  if (select?.value.value) {
    return select.value.value;
  }
  return props.placeholder || '';
});
</script>

<template>
  <span
    data-slot="select-value"
    :data-placeholder="!select?.value.value"
    v-bind="$attrs"
  >
    {{ displayValue }}
  </span>
</template>






