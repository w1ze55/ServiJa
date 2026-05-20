<script setup lang="ts">
import { computed, inject, nextTick, onUnmounted, ref, watch } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: unknown;
}

const props = defineProps<Props>();

const select = inject<{
  isOpen: { value: boolean };
  triggerRef?: { value: HTMLElement | null };
  closeDropdown: () => void;
}>('select');

const isOpen = computed(() => select?.isOpen.value || false);
const contentRef = ref<HTMLElement | null>(null);
const position = ref({ top: '0px', left: '0px', minWidth: '0px' });

const classes = computed(() =>
  cn(
    'relative z-[9999] max-h-[300px] min-w-[8rem] overflow-x-hidden overflow-y-auto rounded-lg border border-slate-200 bg-white text-slate-900 shadow-lg',
    props.class
  )
);

const updatePosition = () => {
  if (!contentRef.value || !select?.triggerRef?.value) return;

  const trigger = select.triggerRef.value;
  const triggerRect = trigger.getBoundingClientRect();

  let top = triggerRect.bottom + 4;
  let left = triggerRect.left;

  const spaceBelow = window.innerHeight - triggerRect.bottom;
  if (spaceBelow < 200) {
    top = triggerRect.top - 200;
  }

  if (left + triggerRect.width > window.innerWidth) {
    left = window.innerWidth - triggerRect.width - 8;
  }

  position.value = {
    top: `${top}px`,
    left: `${left}px`,
    minWidth: `${triggerRect.width}px`,
  };
};

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement;

  if (contentRef.value?.contains(target)) {
    return;
  }

  if (select?.triggerRef?.value?.contains(target)) {
    return;
  }

  select?.closeDropdown();
};

watch(isOpen, async (newValue) => {
  if (newValue) {
    await nextTick();
    updatePosition();

    setTimeout(() => {
      document.addEventListener('click', handleClickOutside, true);
      window.addEventListener('resize', updatePosition);
      window.addEventListener('scroll', updatePosition, true);
    }, 10);
  } else {
    document.removeEventListener('click', handleClickOutside, true);
    window.removeEventListener('resize', updatePosition);
    window.removeEventListener('scroll', updatePosition, true);
  }
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true);
  window.removeEventListener('resize', updatePosition);
  window.removeEventListener('scroll', updatePosition, true);
});
</script>

<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      ref="contentRef"
      data-slot="select-content"
      :class="classes"
      :style="{
        position: 'fixed',
        top: position.top,
        left: position.left,
        minWidth: position.minWidth,
      }"
      v-bind="$attrs"
    >
      <div class="p-1">
        <slot />
      </div>
    </div>
  </Teleport>
</template>
