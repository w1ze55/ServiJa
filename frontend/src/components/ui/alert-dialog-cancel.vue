<script setup lang="ts">
import Button from './button.vue';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
</script>

<template>
  <Button
    data-slot="alert-dialog-cancel"
    variant="outline"
    :class="props.class"
    v-bind="$attrs"
  >
    <slot />
  </Button>
</template>

