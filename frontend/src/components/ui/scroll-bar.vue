<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  orientation?: 'vertical' | 'horizontal';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  orientation: 'vertical',
});

const classes = computed(() =>
  cn(
    'flex touch-none p-px transition-colors select-none',
    props.orientation === 'vertical' &&
      'h-full w-2.5 border-l border-l-transparent',
    props.orientation === 'horizontal' &&
      'h-2.5 flex-col border-t border-t-transparent',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="scroll-area-scrollbar"
    :class="classes"
    v-bind="$attrs"
  >
    <div
      data-slot="scroll-area-thumb"
      class="bg-border relative flex-1 rounded-full"
    />
  </div>
</template>






