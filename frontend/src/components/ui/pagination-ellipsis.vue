<script setup lang="ts">
import { computed } from 'vue';
import { MoreHorizontalIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('flex size-9 items-center justify-center', props.class)
);
</script>

<template>
  <span
    aria-hidden
    data-slot="pagination-ellipsis"
    :class="classes"
    v-bind="$attrs"
  >
    <MoreHorizontalIcon class="size-4" />
    <span class="sr-only">More pages</span>
  </span>
</template>






