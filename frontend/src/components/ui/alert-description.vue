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
    "text-muted-foreground col-start-2 grid justify-items-start gap-1 text-sm [&_p]:leading-relaxed",
    props.class
  )
);
</script>

<template>
  <div
    data-slot="alert-description"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

