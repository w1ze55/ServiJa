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
    'bg-muted flex size-full items-center justify-center rounded-full',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="avatar-fallback"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

