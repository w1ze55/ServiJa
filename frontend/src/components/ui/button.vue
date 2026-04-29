<script setup lang="ts">
import { cn } from "./utils";
import { computed } from "vue";
import { buttonVariants } from "./button-variants";

interface Props {
  variant?: 'default' | 'destructive' | 'success' | 'warning' | 'outline' | 'secondary' | 'ghost' | 'link';
  size?: 'default' | 'sm' | 'lg' | 'xl' | 'icon';
  asChild?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits(["click"]);

const classes = computed(() =>
  cn(buttonVariants({ variant: props.variant, size: props.size }), props.class)
);
</script>

<template>
  <!-- asChild permite WRAPPER via slot -->
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="button"
    :class="classes"
    v-bind="props"
    @click="emit('click', $event)"
  >
    <slot />
  </component>
</template>

