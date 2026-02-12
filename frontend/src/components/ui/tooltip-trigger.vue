<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const tooltip = inject<{ show: () => void; hide: () => void }>('tooltip');
</script>

<template>
  <component
    :is="asChild ? 'span' : 'div'"
    data-slot="tooltip-trigger"
    @mouseenter="tooltip?.show"
    @mouseleave="tooltip?.hide"
    @focus="tooltip?.show"
    @blur="tooltip?.hide"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






