<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('mt-auto flex flex-col gap-2 p-4', props.class)
);
</script>

<template>
  <div
    data-slot="sheet-footer"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






