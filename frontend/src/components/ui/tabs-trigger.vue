<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  value: string;
  disabled?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const tabs = inject<{ value: { value: string }; setValue: (value: string) => void }>('tabs');

const isActive = computed(() => tabs?.value.value === props.value);

const classes = computed(() =>
  cn(
    'relative z-10 data-[state=active]:text-foreground focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:outline-ring text-foreground dark:text-muted-foreground inline-flex h-[calc(100%-1px)] flex-1 items-center justify-center gap-1.5 rounded-xl border border-transparent bg-transparent px-2 py-1 text-sm font-medium whitespace-nowrap transition-[color,box-shadow] duration-150 ease-out focus-visible:ring-[3px] focus-visible:outline-1 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*="size-"])]:size-4',
    props.class
  )
);

const handleClick = () => {
  if (!props.disabled && tabs) {
    tabs.setValue(props.value);
  }
};
</script>

<template>
  <button
    type="button"
    role="tab"
    :aria-selected="isActive"
    data-slot="tabs-trigger"
    :data-state="isActive ? 'active' : 'inactive'"
    :class="classes"
    :disabled="disabled"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </button>
</template>





