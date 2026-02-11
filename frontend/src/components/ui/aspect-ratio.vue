<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  ratio?: number;
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  ratio: 16 / 9,
});

const style = computed(() => ({
  paddingBottom: `${(1 / props.ratio) * 100}%`,
}));
</script>

<template>
  <div
    data-slot="aspect-ratio"
    :class="['relative w-full', props.class]"
    :style="style"
  >
    <div class="absolute inset-0">
      <slot />
    </div>
  </div>
</template>
