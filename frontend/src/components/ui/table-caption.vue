<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('text-muted-foreground mt-4 text-sm', props.class)
);
</script>

<template>
  <caption
    data-slot="table-caption"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </caption>
</template>






