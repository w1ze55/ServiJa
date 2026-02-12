<script setup lang="ts">
import { computed } from 'vue';
import { SearchIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn(
    'bg-popover text-popover-foreground flex h-full w-full flex-col overflow-hidden rounded-md',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="command"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


