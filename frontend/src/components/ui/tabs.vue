<script setup lang="ts">
import { ref, provide, computed } from 'vue';
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


