<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue';
import { ChevronDownIcon } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  size?: 'sm' | 'default';
  class?: string;
  [key: string]: unknown;
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
    'flex w-full min-w-0 cursor-pointer items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm whitespace-nowrap outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100 hover:bg-slate-50',
    props.size === 'default' ? 'h-10' : 'h-8',
    '*:data-[slot=select-value]:min-w-0 *:data-[slot=select-value]:flex-1 *:data-[slot=select-value]:truncate [&_svg]:pointer-events-none [&_svg]:shrink-0',
    props.class
  )
);

const handleClick = () => {
  select?.toggleOpen();
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
    <ChevronDownIcon class="h-4 w-4 opacity-50" />
  </button>
</template>
