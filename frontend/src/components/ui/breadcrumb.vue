<script setup lang="ts">
import { computed } from 'vue';
import { ChevronRight, MoreHorizontal } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
</script>

<template>
  <nav
    aria-label="breadcrumb"
    data-slot="breadcrumb"
    :class="props.class"
    v-bind="$attrs"
  >
    <slot />
  </nav>
</template>


