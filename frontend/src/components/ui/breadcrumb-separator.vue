<script setup lang="ts">
import { computed } from 'vue';
import { ChevronRight } from 'lucide-vue-next';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('[&>svg]:size-3.5', props.class)
);
</script>

<template>
  <li
    data-slot="breadcrumb-separator"
    role="presentation"
    aria-hidden="true"
    :class="classes"
    v-bind="$attrs"
  >
    <slot>
      <ChevronRight />
    </slot>
  </li>
</template>






