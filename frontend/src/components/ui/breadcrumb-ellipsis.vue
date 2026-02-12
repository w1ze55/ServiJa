<script setup lang="ts">
import { computed } from 'vue';
import { MoreHorizontal } from 'lucide-vue-next';
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
    data-slot="breadcrumb-ellipsis"
    role="presentation"
    aria-hidden="true"
    :class="classes"
    v-bind="$attrs"
  >
    <MoreHorizontal class="size-4" />
    <span class="sr-only">More</span>
  </span>
</template>






