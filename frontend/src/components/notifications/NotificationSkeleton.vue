<script setup lang="ts">
import Skeleton from '@/components/ui/skeleton.vue';

interface Props {
  count?: number;
  compact?: boolean;
}

withDefaults(defineProps<Props>(), {
  count: 3,
  compact: false,
});
</script>

<template>
  <div class="space-y-3">
    <div
      v-for="index in count"
      :key="index"
      :class="[
        'flex items-start gap-3 rounded-xl border border-slate-200 bg-white',
        compact ? 'p-3' : 'p-4',
      ]"
    >
      <Skeleton :class="compact ? 'h-9 w-9 rounded-full' : 'h-11 w-11 rounded-full'" />
      <div class="flex-1 space-y-2">
        <Skeleton class="h-4 w-3/5" />
        <Skeleton class="h-3 w-full" />
        <Skeleton class="h-3 w-2/3" />
      </div>
    </div>
  </div>
</template>
