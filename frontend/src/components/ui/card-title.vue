<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn("leading-none", props.class)
);
</script>

<template>
  <h4
    data-slot="card-title"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </h4>
</template>

