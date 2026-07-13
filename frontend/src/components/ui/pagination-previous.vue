<script setup lang="ts">
import { computed } from 'vue';
import { ChevronLeftIcon } from 'lucide-vue-next';
import { cn } from './utils';
import PaginationLink from './pagination-link.vue';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('gap-1 px-2.5 sm:pl-2.5', props.class)
);
</script>

<template>
  <PaginationLink
    aria-label="Ir para a página anterior"
    size="default"
    :class="classes"
    v-bind="$attrs"
  >
    <ChevronLeftIcon />
    <span class="hidden sm:block">Anterior</span>
  </PaginationLink>
</template>






