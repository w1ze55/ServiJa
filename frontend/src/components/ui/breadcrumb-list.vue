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
    'text-muted-foreground flex flex-wrap items-center gap-1.5 text-sm break-words sm:gap-2.5',
    props.class
  )
);
</script>

<template>
  <ol
    data-slot="breadcrumb-list"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </ol>
</template>






