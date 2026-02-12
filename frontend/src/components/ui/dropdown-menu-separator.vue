<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('bg-border -mx-1 my-1 h-px', props.class)
);
</script>

<template>
  <div
    data-slot="dropdown-menu-separator"
    :class="classes"
    v-bind="$attrs"
  />
</template>

