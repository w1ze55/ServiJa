<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';
import { toggleVariants } from './toggle-variants';

interface Props {
  value: string;
  variant?: 'default' | 'outline';
  size?: 'default' | 'sm' | 'lg';
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const toggleGroup = inject<{
  value: { value: string | string[] };
  setValue: (value: string) => void;
  variant?: 'default' | 'outline';
  size?: 'default' | 'sm' | 'lg';
}>('toggleGroup');

const isPressed = computed(() => {
  if (!toggleGroup) return false;
  if (Array.isArray(toggleGroup.value.value)) {
    return toggleGroup.value.value.includes(props.value);
  }
  return toggleGroup.value.value === props.value;
});

const variant = computed(() => props.variant || toggleGroup?.variant || 'default');
const size = computed(() => props.size || toggleGroup?.size || 'default');

const classes = computed(() =>
  cn(
    toggleVariants({ variant: variant.value, size: size.value }),
    'min-w-0 flex-1 shrink-0 rounded-none shadow-none first:rounded-l-md last:rounded-r-md focus:z-10 focus-visible:z-10 data-[variant=outline]:border-l-0 data-[variant=outline]:first:border-l',
    props.class
  )
);

const handleClick = () => {
  toggleGroup?.setValue(props.value);
};
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-pressed="isPressed"
    data-slot="toggle-group-item"
    :data-variant="variant"
    :data-size="size"
    :data-state="isPressed ? 'on' : 'off'"
    :class="classes"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>

