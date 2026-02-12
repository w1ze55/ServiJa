<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  orientation?: "horizontal" | "vertical";
  decorative?: boolean;
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  orientation: "horizontal",
  decorative: true,
});

const classes = computed(() =>
  cn(
    "bg-border shrink-0",
    props.orientation === "horizontal" ? "h-px w-full" : "h-full w-px",
    props.class
  )
);
</script>

<template>
  <div
    data-slot="separator-root"
    :class="classes"
    :role="decorative ? 'none' : 'separator'"
    :aria-orientation="orientation"
    v-bind="$attrs"
  />
</template>

