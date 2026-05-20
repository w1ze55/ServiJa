<script setup lang="ts">
import { computed, inject } from 'vue';
import { CheckIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  value: string;
  disabled?: boolean;
  class?: string;
  [key: string]: unknown;
}

const props = defineProps<Props>();

const select = inject<{
  value: { value: string };
  setValue: (value: string) => void;
}>('select');

const isSelected = computed(() => select?.value.value === props.value);

const classes = computed(() =>
  cn(
    'relative flex w-full cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-sm text-slate-900 select-none',
    'hover:bg-blue-50 focus:bg-blue-50',
    isSelected.value && 'bg-blue-100 font-medium',
    props.disabled && 'pointer-events-none opacity-50',
    props.class
  )
);

const handleClick = () => {
  if (props.disabled) {
    return;
  }

  select?.setValue(props.value);
};
</script>

<template>
  <div
    data-slot="select-item"
    :class="classes"
    :data-disabled="disabled || undefined"
    :data-selected="isSelected || undefined"
    @click="handleClick"
    v-bind="$attrs"
  >
    <span class="flex-1">
      <slot>{{ value }}</slot>
    </span>
    <CheckIcon v-if="isSelected" class="h-4 w-4 text-blue-600" />
  </div>
</template>
