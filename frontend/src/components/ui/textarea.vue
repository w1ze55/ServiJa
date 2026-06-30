<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { cn } from "./utils";

interface Props {
  class?: string;
  modelValue?: string;
  autosize?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [value: string]
}>();

const textareaRef = ref<HTMLTextAreaElement | null>(null);

const classes = computed(() =>
  cn(
    "resize-none border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:ring-destructive/20 dark:aria-invalid:ring-destructive/40 aria-invalid:border-destructive dark:bg-input/30 flex w-full rounded-md border bg-input-background px-3 py-2 text-base transition-[color,box-shadow] outline-none focus-visible:ring-[3px] disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
    // No modo autosize a altura é controlada por JS; nos demais mantém o comportamento atual.
    props.autosize ? "overflow-y-auto" : "field-sizing-content min-h-16",
    props.class
  )
);

const resize = () => {
  if (!props.autosize) return;
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = `${el.scrollHeight}px`;
};

const onInput = (event: Event) => {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value);
  resize();
};

watch(() => props.modelValue, () => nextTick(resize));
onMounted(() => nextTick(resize));
</script>

<template>
  <textarea
    ref="textareaRef"
    data-slot="textarea"
    :class="classes"
    :value="modelValue"
    @input="onInput"
    v-bind="$attrs"
  />
</template>
