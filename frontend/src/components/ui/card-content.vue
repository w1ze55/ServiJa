<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn("px-6 [&:last-child]:pb-6", props.class)
);
</script>

<template>
  <div
    data-slot="card-content"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

