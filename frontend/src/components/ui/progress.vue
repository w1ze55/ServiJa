<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  value?: number;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const rootClasses = computed(() =>
  cn(
    "bg-primary/20 relative h-2 w-full overflow-hidden rounded-full",
    props.class
  )
);

const indicatorStyle = computed(() => ({
  transform: `translateX(-${100 - (props.value || 0)}%)`,
}));
</script>

<template>
  <div
    data-slot="progress"
    :class="rootClasses"
    v-bind="$attrs"
  >
    <div
      data-slot="progress-indicator"
      class="bg-primary h-full w-full flex-1 transition-all"
      :style="indicatorStyle"
    />
  </div>
</template>

