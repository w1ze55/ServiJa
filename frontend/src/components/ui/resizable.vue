<script setup lang="ts">
import { computed, provide } from 'vue';
import { cn } from './utils';

interface Props {
  direction?: 'horizontal' | 'vertical';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  direction: 'horizontal',
});

provide('resizableGroup', {
  direction: props.direction,
});

const classes = computed(() =>
  cn(
    'flex h-full w-full',
    props.direction === 'vertical' && 'flex-col',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="resizable-panel-group"
    :class="classes"
    :data-panel-group-direction="direction"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


