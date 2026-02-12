<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('px-2 py-1.5 text-sm font-semibold', props.class)
);
</script>

<template>
  <div
    data-slot="dropdown-menu-label"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

