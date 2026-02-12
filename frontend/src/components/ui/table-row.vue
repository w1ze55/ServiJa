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
    'hover:bg-muted/50 data-[state=selected]:bg-muted border-b transition-colors',
    props.class
  )
);
</script>

<template>
  <tr
    data-slot="table-row"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </tr>
</template>






