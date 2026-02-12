<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('[&_tr]:border-b', props.class)
);
</script>

<template>
  <thead
    data-slot="table-header"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </thead>
</template>






