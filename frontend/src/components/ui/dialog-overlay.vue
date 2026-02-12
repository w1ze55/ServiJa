<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const dialog = inject<{ isOpen: { value: boolean } }>('dialog');

const classes = computed(() =>
  cn(
    'data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 fixed inset-0 z-50 bg-black/50',
    props.class
  )
);

const handleClick = () => {
  if (dialog) {
    dialog.isOpen.value = false;
  }
};
</script>

<template>
  <div
    v-if="dialog?.isOpen.value"
    data-slot="dialog-overlay"
    :class="classes"
    @click="handleClick"
    v-bind="$attrs"
  />
</template>






