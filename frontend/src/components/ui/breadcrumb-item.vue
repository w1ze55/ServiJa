<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('inline-flex items-center gap-1.5', props.class)
);
</script>

<template>
  <li
    data-slot="breadcrumb-item"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </li>
</template>






