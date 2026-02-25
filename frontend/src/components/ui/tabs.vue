<script setup lang="ts">
import { ref, provide, computed, watch } from 'vue';
import { cn } from './utils';

interface Props {
  modelValue?: string;
  value?: string;
  defaultValue?: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: string];
  'update:value': [value: string];
}>();

const internalValue = ref(props.modelValue || props.value || props.defaultValue || '');

watch(() => props.modelValue, (newValue: string | undefined) => {
  if (newValue !== undefined && newValue !== internalValue.value) {
    internalValue.value = newValue;
  }
});

const setValue = (value: string) => {
  internalValue.value = value;
  emit('update:modelValue', value);
  emit('update:value', value);
};

provide('tabs', {
  value: internalValue,
  setValue,
});

const classes = computed(() =>
  cn('flex flex-col gap-2', props.class)
);
</script>

<template>
  <div
    data-slot="tabs"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


