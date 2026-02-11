<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn(
    "col-start-2 row-span-2 row-start-1 self-start justify-self-end",
    props.class
  )
);
</script>

<template>
  <div
    data-slot="card-action"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

