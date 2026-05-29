<script setup lang="ts">
import { computed, provide, ref, watch } from 'vue';

interface Props {
  modelValue?: string;
  value?: string;
  defaultValue?: string;
  [key: string]: unknown;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: string];
  'update:value': [value: string];
}>();

const currentValue = computed(
  () => props.modelValue || props.value || props.defaultValue || ''
);
const internalValue = ref(currentValue.value);
const isOpen = ref(false);
const triggerRef = ref<HTMLElement | null>(null);

watch(
  currentValue,
  (newValue) => {
    if (newValue !== undefined) {
      internalValue.value = newValue;
    }
  },
  { immediate: true }
);

const setValue = (value: string) => {
  internalValue.value = value;
  emit('update:modelValue', value);
  emit('update:value', value);
  isOpen.value = false;
};

const toggleOpen = () => {
  isOpen.value = !isOpen.value;
};

const closeDropdown = () => {
  isOpen.value = false;
};

provide('select', {
  value: internalValue,
  setValue,
  isOpen,
  triggerRef,
  toggleOpen,
  closeDropdown,
});
</script>

<template>
  <div data-slot="select" class="relative" v-bind="$attrs">
    <slot />
  </div>
</template>
