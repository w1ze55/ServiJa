<script setup lang="ts">
import { computed } from 'vue';
import { type VariantProps } from 'class-variance-authority';
import { cn } from './utils';
import { toggleVariants } from './toggle-variants';

interface Props {
  pressed?: boolean;
  defaultPressed?: boolean;
  variant?: 'default' | 'outline';
  size?: 'default' | 'sm' | 'lg';
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:pressed': [value: boolean];
}>();

const isPressed = computed({
  get: () => props.pressed ?? props.defaultPressed ?? false,
  set: (value) => emit('update:pressed', value),
});

const classes = computed(() =>
  cn(toggleVariants({ variant: props.variant, size: props.size }), props.class)
);

const toggle = () => {
  isPressed.value = !isPressed.value;
};
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-pressed="isPressed"
    data-slot="toggle"
    :data-state="isPressed ? 'on' : 'off'"
    :class="classes"
    @click="toggle"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>


