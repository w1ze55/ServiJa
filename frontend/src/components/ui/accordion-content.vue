<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const accordion = inject<{ isOpen: (value: string) => boolean }>('accordion');
const itemValue = inject<string>('accordionItemValue');

const isOpen = computed(() => accordion?.isOpen(itemValue || '') || false);

const contentClasses = computed(() =>
  cn('overflow-hidden text-sm', props.class)
);

const innerClasses = computed(() =>
  cn('pt-0 pb-4', props.class)
);
</script>

<template>
  <div
    v-if="isOpen"
    data-slot="accordion-content"
    :class="contentClasses"
    :data-state="isOpen ? 'open' : 'closed'"
    v-bind="$attrs"
  >
    <div :class="innerClasses">
      <slot />
    </div>
  </div>
</template>

