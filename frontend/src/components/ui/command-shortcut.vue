<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn(
    'text-muted-foreground ml-auto text-xs tracking-widest',
    props.class
  )
);
</script>

<template>
  <span
    data-slot="command-shortcut"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </span>
</template>






