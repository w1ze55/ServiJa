<script setup lang="ts">
import { computed } from 'vue';
import { ArrowLeftRight, MapPin, Monitor } from 'lucide-vue-next';
import Badge from '../ui/badge.vue';
import { getWorkModelLabel, normalizeWorkModel } from '@/utils/workModel';

interface Props {
  workModel?: string | null;
}

const props = defineProps<Props>();

const workModelIcon = computed(() => {
  const normalized = normalizeWorkModel(props.workModel);

  if (normalized === 'PRESENCIAL') return MapPin;
  if (normalized === 'HIBRIDO') return ArrowLeftRight;
  return Monitor;
});
</script>

<template>
  <Badge
    class="inline-flex items-center gap-1 border border-gray-200 bg-gray-100 text-gray-700 hover:bg-gray-100"
  >
    <component :is="workModelIcon" class="h-3 w-3" />
    {{ getWorkModelLabel(workModel) }}
  </Badge>
</template>
