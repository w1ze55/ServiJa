<script setup lang="ts">
import { computed } from 'vue';
import Badge from '@/components/ui/badge.vue';

interface Props {
  count: number;
}

const props = defineProps<Props>();

const displayCount = computed(() => {
  if (props.count > 99) {
    return '99+';
  }

  return String(props.count);
});
</script>

<template>
  <Badge
    v-if="count > 0"
    class="absolute -top-1.5 -right-1.5 min-w-5 h-5 rounded-full bg-red-500 px-1.5 text-[0.6875rem] text-white hover:bg-red-500"
  >
    {{ displayCount }}
  </Badge>
</template>
