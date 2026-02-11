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
    'flex flex-col-reverse gap-2 sm:flex-row sm:justify-end',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="dialog-footer"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






