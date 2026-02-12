<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('flex flex-col gap-2 text-center sm:text-left', props.class)
);
</script>

<template>
  <div
    data-slot="alert-dialog-header"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

