<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const sheet = inject<{ isOpen: { value: boolean } }>('sheet');

const handleClick = () => {
  if (sheet) {
    sheet.isOpen.value = false;
  }
};
</script>

<template>
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="sheet-close"
    :class="props.class"
    @click="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






