<script setup lang="ts">
import { computed } from 'vue';
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  MoreHorizontalIcon,
} from 'lucide-vue-next';
import { cn } from './utils';
import { buttonVariants } from './button.vue';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('mx-auto flex w-full justify-center', props.class)
);
</script>

<template>
  <nav
    role="navigation"
    aria-label="pagination"
    data-slot="pagination"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </nav>
</template>


