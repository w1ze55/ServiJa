<script setup lang="ts">
import { computed } from "vue";
import { cva } from "class-variance-authority";
import { cn } from "./utils";

const alertVariants = cva(
  "relative w-full rounded-lg border px-4 py-3 text-sm grid has-[>svg]:grid-cols-[calc(var(--spacing)*4)_1fr] grid-cols-[0_1fr] has-[>svg]:gap-x-3 gap-y-0.5 items-start [&>svg]:size-4 [&>svg]:translate-y-0.5 [&>svg]:text-current",
  {
    variants: {
      variant: {
        default: "bg-card text-card-foreground",
        destructive:
          "text-destructive bg-card [&>svg]:text-current *:data-[slot=alert-description]:text-destructive/90",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

interface Props {
  variant?: "default" | "destructive";
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  variant: "default",
});

const classes = computed(() => cn(alertVariants({ variant: props.variant }), props.class));
</script>

<template>
  <div
    data-slot="alert"
    role="alert"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

