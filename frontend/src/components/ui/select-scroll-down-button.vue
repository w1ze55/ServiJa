<script setup lang="ts">
import { computed } from 'vue';
import { ChevronDownIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('flex cursor-default items-center justify-center py-1', props.class)
);
</script>

<template>
  <div
    data-slot="select-scroll-down-button"
    :class="classes"
    v-bind="$attrs"
  >
    <ChevronDownIcon class="size-4" />
  </div>
</template>






