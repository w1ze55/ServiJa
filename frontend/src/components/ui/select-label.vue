<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('text-muted-foreground px-2 py-1.5 text-xs', props.class)
);
</script>

<template>
  <div
    data-slot="select-label"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






