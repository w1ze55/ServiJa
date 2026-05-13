<script setup lang="ts">
import { computed, inject, ref, onMounted } from 'vue';
import { ChevronDownIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  size?: 'sm' | 'default';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default',
});

const select = inject<{ 
  isOpen: { value: boolean }; 
  triggerRef: { value: HTMLElement | null };
  toggleOpen: () => void;
}>('select');

const triggerElementRef = ref<HTMLElement | null>(null);

const classes = computed(() =>
  cn(
    'border-input data-[placeholder]:text-muted-foreground [&_svg:not([class*="text-"])]:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 dark:hover:bg-input/50 flex w-full min-w-0 items-center justify-between gap-2 rounded-md border bg-white px-3 py-2 text-sm whitespace-nowrap transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 data-[size=default]:h-9 data-[size=sm]:h-8 *:data-[slot=select-value]:min-w-0 *:data-[slot=select-value]:flex-1 *:data-[slot=select-value]:truncate [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*="size-"])]:size-4 cursor-pointer hover:bg-gray-50',
    props.class
  )
);

const handleClick = () => {
  if (select) {
    select.toggleOpen();
  }
};

onMounted(() => {
  if (triggerElementRef.value && select?.triggerRef) {
    select.triggerRef.value = triggerElementRef.value;
  }
});
</script>

<template>
  <button
    ref="triggerElementRef"
    type="button"
    data-slot="select-trigger"
    :data-size="size"
    :class="classes"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
    <ChevronDownIcon class="size-4 opacity-50" />
  </button>
</template>
