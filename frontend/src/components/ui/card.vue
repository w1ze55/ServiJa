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
    "bg-card text-card-foreground flex flex-col gap-6 rounded-xl border",
    props.class
  )
);
</script>

<template>
  <div
    data-slot="card"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>


