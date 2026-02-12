<script setup lang="ts">
import { computed, inject } from 'vue';
import { ChevronDownIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const accordion = inject<{ isOpen: (value: string) => boolean; toggle: (value: string) => void }>('accordion');
const itemValue = inject<string>('accordionItemValue');

const isOpen = computed(() => accordion?.isOpen(itemValue || '') || false);

const classes = computed(() =>
  cn(
    'focus-visible:border-ring focus-visible:ring-ring/50 flex flex-1 items-start justify-between gap-4 rounded-md py-4 text-left text-sm font-medium transition-all outline-none hover:underline focus-visible:ring-[3px] disabled:pointer-events-none disabled:opacity-50',
    isOpen.value && '[&>svg]:rotate-180',
    props.class
  )
);

const handleClick = () => {
  if (itemValue) {
    accordion?.toggle(itemValue);
  }
};
</script>

<template>
  <div class="flex">
    <button
      data-slot="accordion-trigger"
      :class="classes"
      @click="handleClick"
      v-bind="$attrs"
    >
      <slot />
      <ChevronDownIcon class="text-muted-foreground pointer-events-none size-4 shrink-0 translate-y-0.5 transition-transform duration-200" />
    </button>
  </div>
</template>






