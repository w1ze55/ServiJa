<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();

// Inject avatar image state from parent Avatar component
const hasImage = inject<Ref<boolean>>('hasAvatarImage', ref(false));

const classes = computed(() =>
  cn(
    'bg-muted flex size-full items-center justify-center rounded-full',
    props.class
  )
);
</script>

<template>
  <div
    v-if="!hasImage"
    data-slot="avatar-fallback"
    :class="classes"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>

