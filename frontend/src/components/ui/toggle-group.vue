<script setup lang="ts">
import { ref, provide, computed } from 'vue';
import { type VariantProps } from 'class-variance-authority';
import { cn } from './utils';
import { toggleVariants } from './toggle-variants';

interface Props {
  type?: 'single' | 'multiple';
  value?: string | string[];
  defaultValue?: string | string[];
  variant?: 'default' | 'outline';
  size?: 'default' | 'sm' | 'lg';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'single',
  variant: 'default',
  size: 'default',
});

const emit = defineEmits<{
  'update:value': [value: string | string[]];
}>();

const internalValue = ref<string | string[]>(
  props.value || props.defaultValue || (props.type === 'multiple' ? [] : '')
);

const setValue = (value: string) => {
  if (props.type === 'multiple') {
    const current = Array.isArray(internalValue.value) ? internalValue.value : [];
    if (current.includes(value)) {
      internalValue.value = current.filter((v) => v !== value);
    } else {
      internalValue.value = [...current, value];
    }
  } else {
    internalValue.value = value;
  }
  emit('update:value', internalValue.value);
};

provide('toggleGroup', {
  value: internalValue,
  setValue,
  variant: props.variant,
  size: props.size,
});

const classes = computed(() =>
  cn(
    'group/toggle-group flex w-fit items-center rounded-md data-[variant=outline]:shadow-xs',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="toggle-group"
    :data-variant="variant"
    :data-size="size"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


