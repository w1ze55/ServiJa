<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('flex flex-row items-center gap-1', props.class)
);
</script>

<template>
  <ul
    data-slot="pagination-content"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </ul>
</template>






