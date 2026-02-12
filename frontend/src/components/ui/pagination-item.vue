<script setup lang="ts">
interface Props {
  [key: string]: any;
}

defineProps<Props>();
</script>

<template>
  <li data-slot="pagination-item" v-bind="$attrs">
    <slot />
  </li>
</template>






