<script setup lang="ts">
import { computed } from "vue";
import { CheckIcon } from "lucide-vue-next";
import { cn } from "./utils";

interface Props {
  checked?: boolean;
  disabled?: boolean;
  class?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  "update:checked": [value: boolean];
  change: [value: boolean];
}>();

const state = computed(() => (props.checked ? "checked" : "unchecked"));

function toggle(e: MouseEvent) {
  if (props.disabled) return;

  emit("update:checked", !props.checked);
  emit("change", !props.checked);
}
</script>

<template>
  <button
    type="button"
    data-slot="checkbox"
    :data-state="state"
    class="peer border bg-input-background dark:bg-input/30 data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground dark:data-[state=checked]:bg-primary data-[state=checked]:border-primary focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive size-4 shrink-0 rounded-[4px] border shadow-xs transition-shadow outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50"
    :class="props.class"
    :disabled="props.disabled"
    @click="toggle"
  >
    <span
      v-if="props.checked"
      data-slot="checkbox-indicator"
      class="flex items-center justify-center text-current transition-none"
    >
      <CheckIcon class="size-3.5" />
    </span>
  </button>
</template>

