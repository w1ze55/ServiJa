<script setup lang="ts">
import { ref, provide, computed } from 'vue';
import { CircleIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  value?: string;
  defaultValue?: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:value': [value: string];
}>();

const internalValue = ref(props.value || props.defaultValue || '');

const setValue = (value: string) => {
  internalValue.value = value;
  emit('update:value', value);
};

provide('radioGroup', {
  value: internalValue,
  setValue,
});
</script>

<template>
  <div
    data-slot="radio-group"
    :class="cn('grid gap-3', props.class)"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


