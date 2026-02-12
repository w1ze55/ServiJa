<script setup lang="ts">
import { inject } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const hoverCard = inject<{ handleMouseEnter: () => void; handleMouseLeave: () => void }>('hoverCard');
</script>

<template>
  <component
    :is="asChild ? 'span' : 'div'"
    data-slot="hover-card-trigger"
    @mouseenter="hoverCard?.handleMouseEnter"
    @mouseleave="hoverCard?.handleMouseLeave"
    v-bind="$attrs"
  >
    <slot />
  </component>
</template>






