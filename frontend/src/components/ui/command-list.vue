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
    'max-h-[300px] scroll-py-1 overflow-x-hidden overflow-y-auto',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="command-list"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






