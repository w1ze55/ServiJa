<script setup lang="ts">
import { computed, inject } from 'vue';
import { CheckIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  value: string;
  disabled?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const select = inject<{ 
  value: { value: string }; 
  setValue: (value: string) => void;
}>('select');

const isSelected = computed(() => select?.value.value === props.value);

const classes = computed(() =>
  cn(
    'relative flex w-full cursor-pointer items-center gap-2 rounded-sm py-2 px-3 text-sm select-none',
    'hover:bg-blue-50 focus:bg-blue-50 text-gray-900',
    isSelected.value && 'bg-blue-100 font-medium',
    props.disabled && 'pointer-events-none opacity-50',
    props.class
  )
);

const handleClick = () => {
  if (props.disabled) return;
  
  if (select) {
    select.setValue(props.value);
  }
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
