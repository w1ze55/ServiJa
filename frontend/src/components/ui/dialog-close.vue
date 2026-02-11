<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const dialog = inject<{ isOpen: { value: boolean } }>('dialog');

const handleClick = () => {
  if (dialog) {
    dialog.isOpen.value = false;
  }
};
</script>

<template>
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="dialog-close"
    :class="props.class"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






