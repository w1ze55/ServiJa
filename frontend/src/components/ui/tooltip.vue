<script setup lang="ts">
import { ref, provide } from 'vue';

interface Props {
  open?: boolean;
  defaultOpen?: boolean;
  delayDuration?: number;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  delayDuration: 0,
});

const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = ref(props.open ?? props.defaultOpen ?? false);
let timeoutId: ReturnType<typeof setTimeout> | null = null;

const show = () => {
  if (timeoutId) clearTimeout(timeoutId);
  timeoutId = setTimeout(() => {
    isOpen.value = true;
    emit('update:open', true);
  }, props.delayDuration);
};

const hide = () => {
  if (timeoutId) clearTimeout(timeoutId);
  isOpen.value = false;
  emit('update:open', false);
};

provide('tooltip', {
  isOpen,
  show,
  hide,
});
</script>

<template>
  <div v-bind="$attrs">
    <slot />
  </div>
</template>


