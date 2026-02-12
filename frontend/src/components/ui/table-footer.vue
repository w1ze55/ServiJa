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
    'bg-muted/50 border-t font-medium [&>tr]:last:border-b-0',
    props.class
  )
);
</script>

<template>
  <tfoot
    data-slot="table-footer"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </tfoot>
</template>






