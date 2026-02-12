<script setup lang="ts">
interface Props {
  [key: string]: any;
}

defineProps<Props>();
</script>

<template>
  <div
    data-slot="command-empty"
    class="py-6 text-center text-sm"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






