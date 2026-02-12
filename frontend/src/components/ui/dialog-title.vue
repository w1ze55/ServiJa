<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('text-lg leading-none font-semibold', props.class)
);
</script>

<template>
  <h2
    data-slot="dialog-title"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </h2>
</template>






