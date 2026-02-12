<script setup lang="ts">
import { computed } from 'vue';
import { SearchIcon } from 'lucide-vue-next';
import { cn } from './utils';
import Input from './input.vue';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const inputClasses = computed(() =>
  cn(
    'placeholder:text-muted-foreground flex h-10 w-full rounded-md bg-transparent py-3 text-sm outline-hidden disabled:cursor-not-allowed disabled:opacity-50',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="command-input-wrapper"
    class="flex h-9 items-center gap-2 border-b px-3"
  >
    <SearchIcon class="size-4 shrink-0 opacity-50" />
    <Input
      data-slot="command-input"
      :class="inputClasses"
      v-bind="$attrs"
    />
  </div>
</template>

