<script setup lang="ts">
import { computed, provide, ref } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

// State to track if avatar has a valid image
const hasImage = ref(false);

// Provide state and setter to child components
provide('hasAvatarImage', hasImage);
provide('setHasAvatarImage', (value: boolean) => {
  hasImage.value = value;
});

const classes = computed(() =>
  cn(
    'relative flex size-10 shrink-0 overflow-hidden rounded-full',
    props.class
  )
);
</script>

<template>
  <div
    data-slot="avatar"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

