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
    'relative flex size-10 shrink-0 overflow-hidden rounded-full',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="avatar"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

