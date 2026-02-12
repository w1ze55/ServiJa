<script setup lang="ts">
import { computed, inject, provide } from 'vue';
import { cn } from './utils';

interface Props {
  value: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const accordion = inject<{ isOpen: (value: string) => boolean }>('accordion');

provide('accordionItemValue', props.value);

const classes = computed(() =>
  cn('border-b last:border-b-0', props.class)
);
</script>

<template>
  <div
    data-slot="accordion-item"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

