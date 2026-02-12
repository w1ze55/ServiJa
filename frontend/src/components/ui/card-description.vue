<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn("text-muted-foreground", props.class)
);
</script>

<template>
  <p
    data-slot="card-description"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </p>
</template>

