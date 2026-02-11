<script setup lang="ts">
import { ref, provide, computed } from 'vue';
import { ChevronDownIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  type?: 'single' | 'multiple';
  defaultValue?: string | string[];
  value?: string | string[];
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'single',
});

const emit = defineEmits<{
  'update:value': [value: string | string[]];
}>();

const internalValue = ref<string | string[]>(props.value || props.defaultValue || (props.type === 'multiple' ? [] : ''));

const isOpen = (itemValue: string) => {
  if (props.type === 'multiple') {
    return Array.isArray(internalValue.value) && internalValue.value.includes(itemValue);
  }
  return internalValue.value === itemValue;
};

const toggle = (itemValue: string) => {
  if (props.type === 'multiple') {
    const current = Array.isArray(internalValue.value) ? internalValue.value : [];
    if (current.includes(itemValue)) {
      internalValue.value = current.filter(v => v !== itemValue);
    } else {
      internalValue.value = [...current, itemValue];
    }
  } else {
    internalValue.value = internalValue.value === itemValue ? '' : itemValue;
  }
  emit('update:value', internalValue.value);
};

provide('accordion', {
  type: props.type,
  isOpen,
  toggle,
});
</script>

<template>
  <div
    data-slot="accordion"
    :class="props.class"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


