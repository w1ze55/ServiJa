<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('relative', props.class)
);
</script>

<template>
  <div
    data-slot="scroll-area"
    :class="classes"
    v-bind="$attrs"
  >
    <div
      data-slot="scroll-area-viewport"
      class="focus-visible:ring-ring/50 size-full rounded-[inherit] transition-[color,box-shadow] outline-none focus-visible:ring-[3px] focus-visible:outline-1 overflow-auto"
    >
      <slot />
    </div>
    <ScrollBar />
  </div>
</template>


