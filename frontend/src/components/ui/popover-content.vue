<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  align?: 'start' | 'center' | 'end';
  sideOffset?: number;
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  align: 'center',
  sideOffset: 4,
});

const popover = inject<{ isOpen: { value: boolean } }>('popover');

const classes = computed(() =>
  cn(
    'bg-popover text-popover-foreground data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 z-50 w-72 origin-(--radix-popover-content-transform-origin) rounded-md border p-4 shadow-md outline-hidden',
    props.class
  )
);
</script>

<template>
  <Teleport to="body">
    <div
      v-if="popover?.isOpen.value"
      data-slot="popover-content"
      :class="classes"
      :style="{
        position: 'fixed',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
      }"
      @click.stop
      v-bind="$attrs"
    >
      <slot />
    </div>
  </Teleport>
</template>






