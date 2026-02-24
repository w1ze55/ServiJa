<script setup lang="ts">
import { computed, inject, watch, ref as vueRef } from 'vue';
import { cn } from './utils';

interface Props {
  src?: string;
  alt?: string;
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

// Inject setter from parent Avatar component
const setHasImage = inject<(value: boolean) => void>('setHasAvatarImage', () => {});

// Track if image is loaded
const imageLoaded = vueRef(false);
const imageError = vueRef(false);

// Watch for src changes and update parent
watch(() => props.src, (newSrc) => {
  if (newSrc && newSrc.trim() !== '') {
    imageLoaded.value = false;
    imageError.value = false;
  } else {
    setHasImage(false);
  }
}, { immediate: true });

const handleImageLoad = () => {
  imageLoaded.value = true;
  imageError.value = false;
  setHasImage(true);
};

const handleImageError = () => {
  imageLoaded.value = false;
  imageError.value = true;
  setHasImage(false);
};

const classes = computed(() =>
  cn('aspect-square size-full object-cover', props.class)
);
</script>

<template>
  <img
    v-if="src && !imageError"
    data-slot="avatar-image"
    :src="src"
    :alt="alt"
    :class="classes"
    @load="handleImageLoad"
    @error="handleImageError"
    v-bind="$attrs"
  />
</template>

