<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const popover = inject<{ isOpen: { value: boolean } }>('popover');

const handleClick = () => {
  if (popover) {
    popover.isOpen.value = !popover.isOpen.value;
  }
};
</script>

<template>
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="popover-trigger"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






