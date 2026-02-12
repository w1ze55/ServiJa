<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';
import { buttonVariants } from './button-variants';

interface Props {
  isActive?: boolean;
  size?: 'default' | 'sm' | 'lg' | 'icon';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  isActive: false,
  size: 'icon',
});

const classes = computed(() =>
  cn(
    buttonVariants({
      variant: props.isActive ? 'outline' : 'ghost',
      size: props.size,
    }),
    props.class
  )
);
</script>

<template>
  <a
    :aria-current="isActive ? 'page' : undefined"
    data-slot="pagination-link"
    :data-active="isActive"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </a>
</template>

