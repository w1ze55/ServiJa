<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn("bg-accent animate-pulse rounded-md", props.class)
);
</script>

<template>
  <div
    data-slot="skeleton"
    :class="classes"
    v-bind="$attrs"
  />
</template>

