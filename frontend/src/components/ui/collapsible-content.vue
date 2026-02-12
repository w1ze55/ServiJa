<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const collapsible = inject<{ isOpen: { value: boolean } }>('collapsible');

const isOpen = computed(() => collapsible?.isOpen.value || false);

const classes = computed(() =>
  cn(
    'overflow-hidden text-sm transition-all',
    isOpen.value ? 'animate-accordion-down' : 'animate-accordion-up',
    props.class
  )
);
</script>

<template>
  <div
    v-if="isOpen"
    data-slot="collapsible-content"
    :class="classes"
    :data-state="isOpen ? 'open' : 'closed'"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






