<script setup lang="ts">
import { inject, computed } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const dropdownMenu = inject<{ close: () => void }>('dropdownMenu');

const classes = computed(() =>
  cn(
    'relative flex cursor-default items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none select-none hover:bg-accent hover:text-accent-foreground',
    props.class
  )
);

const handleClick = () => {
  dropdownMenu?.close();
};
</script>

<template>
  <div
    data-slot="dropdown-menu-item"
    :class="classes"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

