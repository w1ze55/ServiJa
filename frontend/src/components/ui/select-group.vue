<script setup lang="ts">
interface Props {
  [key: string]: any;
}

defineProps<Props>();
</script>

<template>
  <div data-slot="select-group" v-bind="$attrs">
    <slot />
  </div>
</template>






