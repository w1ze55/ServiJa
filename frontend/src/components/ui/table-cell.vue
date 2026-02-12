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
    'p-2 align-middle whitespace-nowrap [&:has([role=checkbox])]:pr-0 [&>[role=checkbox]]:translate-y-[2px]',
    props.class
  )
);
</script>

<template>
  <td
    data-slot="table-cell"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </td>
</template>






