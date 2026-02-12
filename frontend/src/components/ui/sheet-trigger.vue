<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const sheet = inject<{ isOpen: { value: boolean } }>('sheet');

const handleClick = () => {
  if (sheet) {
    sheet.isOpen.value = true;
  }
};
</script>

<template>
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="sheet-trigger"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






