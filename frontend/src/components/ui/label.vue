<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  for?: string;
  htmlFor?: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn(
    "flex items-center gap-2 text-sm leading-none font-medium select-none group-data-[disabled=true]:pointer-events-none group-data-[disabled=true]:opacity-50 peer-disabled:cursor-not-allowed peer-disabled:opacity-50",
    props.class
  )
);

const forAttr = computed(() => props.for || props.htmlFor);
</script>

<template>
  <label
    :for="forAttr"
    data-slot="label"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </label>
</template>

