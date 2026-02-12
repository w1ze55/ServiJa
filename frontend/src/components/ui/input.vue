<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  type?: string;
  class?: string;
  modelValue?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>();

const classes = computed(() =>
  cn(
    "file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground dark:bg-input/30 border-input flex h-9 w-full min-w-0 rounded-md border px-3 py-1 text-base bg-input-background transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
    "focus-visible:border-ring focus-visible:ring-ring/50 focus-visible:ring-[3px]",
    "aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive",
    props.class
  )
);
</script>

<template>
  <input
    :type="type"
    data-slot="input"
    :class="classes"
    :value="modelValue"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    v-bind="$attrs"
  />
</template>

