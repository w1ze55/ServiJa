<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const alertDialog = inject<{ isOpen: { value: boolean } }>('alertDialog');

const classes = computed(() =>
  cn(
    'bg-background fixed top-[50%] left-[50%] z-50 grid w-full max-w-[calc(100%-2rem)] translate-x-[-50%] translate-y-[-50%] gap-4 rounded-lg border p-6 shadow-lg duration-200 sm:max-w-lg',
    props.class
  )
);
</script>

<template>
  <Teleport to="body">
    <div
      v-if="alertDialog?.isOpen.value"
      class="fixed inset-0 z-50 bg-black/50"
      @click="alertDialog.isOpen.value = false"
    />
    <div
      v-if="alertDialog?.isOpen.value"
      data-slot="alert-dialog-content"
      :class="classes"
      v-bind="$attrs"
      @click.stop
    >
      <slot />
    </div>
  </Teleport>
</template>

