<script setup lang="ts">
import { computed } from 'vue';
import { cn } from './utils';

interface Props {
  src?: string;
  alt?: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

const classes = computed(() =>
  cn('aspect-square size-full', props.class)
);
</script>

<template>
  <img
    v-if="src"
    data-slot="avatar-image"
    :src="src"
    :alt="alt"
    :class="classes"
    v-bind="$attrs"
  />
</template>

