<script setup lang="ts">
import { computed } from "vue";
import { cn } from "./utils";

interface Props {
  checked?: boolean;
  disabled?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  "update:checked": [value: boolean];
  change: [value: boolean];
}>();

const isChecked = computed({
  get: () => props.checked || false,
  set: (value) => {
    emit("update:checked", value);
    emit("change", value);
  },
});

const toggle = () => {
  if (props.disabled) return;
  isChecked.value = !isChecked.value;
};

const classes = computed(() =>
  cn(
    "peer data-[state=checked]:bg-primary data-[state=unchecked]:bg-switch-background focus-visible:border-ring focus-visible:ring-ring/50 dark:data-[state=unchecked]:bg-input/80 inline-flex h-[1.15rem] w-8 shrink-0 items-center rounded-full border border-transparent transition-all outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50",
    props.class
  )
);

const thumbClasses = computed(() =>
  cn(
    "bg-card dark:data-[state=unchecked]:bg-card-foreground dark:data-[state=checked]:bg-primary-foreground pointer-events-none block size-4 rounded-full ring-0 transition-transform",
    isChecked.value ? "translate-x-[calc(100%-2px)]" : "translate-x-0"
  )
);
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-checked="isChecked"
    data-slot="switch"
    :data-state="isChecked ? 'checked' : 'unchecked'"
    :class="classes"
    :disabled="disabled"
    @click="toggle"
    v-bind="$attrs"
  >
    <span
      data-slot="switch-thumb"
      :class="thumbClasses"
      :data-state="isChecked ? 'checked' : 'unchecked'"
    />
  </button>
</template>


