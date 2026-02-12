<script setup lang="ts">
interface Props {
  defaultSize?: number;
  minSize?: number;
  maxSize?: number;
  [key: string]: any;
}

defineProps<Props>();
</script>

<template>
  <div
    data-slot="resizable-panel"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






