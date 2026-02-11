<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const collapsible = inject<{ isOpen: { value: boolean } }>('collapsible');

const toggle = () => {
  if (collapsible) {
    collapsible.isOpen.value = !collapsible.isOpen.value;
  }
};
</script>

<template>
  <component
    :is="asChild ? 'span' : 'button'"
    data-slot="collapsible-trigger"
    @click="toggle"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






