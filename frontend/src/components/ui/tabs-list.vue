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
    'bg-muted text-muted-foreground inline-flex h-9 w-fit items-center justify-center rounded-xl p-[3px] flex',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="tabs-list"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






