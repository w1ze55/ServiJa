<script setup lang="ts">
import { computed, inject } from 'vue';
import { CircleIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  value: string;
  disabled?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const radioGroup = inject<{ value: { value: string }; setValue: (value: string) => void }>('radioGroup');

const isChecked = computed(() => radioGroup?.value.value === props.value);

const classes = computed(() =>
  cn(
    'border-input text-primary focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 aspect-square size-4 shrink-0 rounded-full border shadow-xs transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50',
    props.class
  )
);

const handleClick = () => {
  if (!props.disabled && radioGroup) {
    radioGroup.setValue(props.value);
  }
};
</script>

<template>
  <button
    type="button"
    role="radio"
    :aria-checked="isChecked"
    data-slot="radio-group-item"
    :class="classes"
    :disabled="disabled"
    @click="handleClick"
    v-bind="$attrs"
  >
    <span
      v-if="isChecked"
      data-slot="radio-group-indicator"
      class="relative flex items-center justify-center"
    >
      <CircleIcon class="fill-primary absolute top-1/2 left-1/2 size-2 -translate-x-1/2 -translate-y-1/2" />
    </span>
  </button>
</template>






