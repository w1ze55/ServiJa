<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('w-full caption-bottom text-sm', props.class)
);
</script>

<template>
  <div
    data-slot="table-container"
    class="relative w-full overflow-x-auto"
  >
    <table
      data-slot="table"
      :class="classes"
      v-bind="$attrs"
    >
      <slot />
    </table>
  </div>
</template>


