<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('bg-border pointer-events-none -mx-1 my-1 h-px', props.class)
);
</script>

<template>
  <div
    data-slot="select-separator"
    :class="classes"
    v-bind="$attrs"
  />
</template>






