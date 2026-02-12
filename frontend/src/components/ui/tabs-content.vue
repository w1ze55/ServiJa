<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  value: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const tabs = inject<{ value: { value: string } }>('tabs');

const isActive = computed(() => tabs?.value.value === props.value);

const classes = computed(() =>
  cn('flex-1 outline-none', props.class)
);
</script>

<template>
  <div
    v-if="isActive"
    data-slot="tabs-content"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>






