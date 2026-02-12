<script setup lang="ts">
import { computed } from 'vue';
import { ChevronRightIcon } from 'lucide-vue-next';
import { cn } from './utils';
import PaginationLink from './pagination-link.vue';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('gap-1 px-2.5 sm:pr-2.5', props.class)
);
</script>

<template>
  <PaginationLink
    aria-label="Go to next page"
    size="default"
    :class="classes"
    v-bind="$attrs"
  >
    <span class="hidden sm:block">Next</span>
    <ChevronRightIcon />
  </PaginationLink>
</template>






