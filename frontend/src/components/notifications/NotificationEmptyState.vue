<script setup lang="ts">
import { BellOff } from 'lucide-vue-next';

interface Props {
  title: string;
  description: string;
  compact?: boolean;
}

withDefaults(defineProps<Props>(), {
  compact: false,
});
</script>

<template>
  <div
    :class="[
      'flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50/80 text-center',
      compact ? 'p-6 gap-3' : 'p-10 gap-4',
    ]"
  >
    <div class="flex h-12 w-12 items-center justify-center rounded-full bg-white shadow-sm">
      <BellOff class="h-5 w-5 text-slate-400" />
    </div>
    <div class="space-y-1">
      <h3 class="text-sm font-semibold text-[#263238]">{{ title }}</h3>
      <p class="text-sm text-[#435574]">{{ description }}</p>
    </div>
  </div>
</template>
