<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('[&_tr:last-child]:border-0', props.class)
);
</script>

<template>
  <tbody
    data-slot="table-body"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </tbody>
</template>






