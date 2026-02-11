<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const dialog = inject<{ isOpen: { value: boolean } }>('dialog');

const handleClick = () => {
  if (dialog) {
    dialog.isOpen.value = true;
  }
};
</script>

<template>
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="dialog-trigger"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






