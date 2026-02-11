<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  asChild?: boolean;
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  asChild: false,
});

const classes = computed(() =>
  cn('hover:text-foreground transition-colors', props.class)
);
</script>

<template>
  <component
    :is="asChild ? 'span' : 'a'"
    data-slot="breadcrumb-link"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






