<script setup lang="ts">
import { computed, inject, watch, onUnmounted, ref, nextTick, onMounted } from 'vue';
import { cn } from './utils';

interface Props {
  position?: 'popper' | 'item-aligned';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  position: 'popper',
});

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
    'bg-white text-gray-900 relative z-[9999] max-h-[300px] min-w-[8rem] overflow-x-hidden overflow-y-auto rounded-md border border-gray-200 shadow-lg',
    props.class
  )
);

const updatePosition = () => {
  if (!contentRef.value || !select?.triggerRef?.value) return;

  const trigger = select.triggerRef.value;
  const triggerRect = trigger.getBoundingClientRect();
  
  let top = triggerRect.bottom + 4;
  let left = triggerRect.left;

  // Verificar se cabe na tela
  const spaceBelow = window.innerHeight - triggerRect.bottom;
  if (spaceBelow < 200) {
    top = triggerRect.top - 200;
  }

  // Garantir que não saia da tela
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
  
  // Se clicou dentro do content ou no trigger, não fechar
  if (contentRef.value?.contains(target)) {
    return;
  }
  
  if (select?.triggerRef?.value?.contains(target)) {
    return;
  }
  
  // Fechar dropdown
  if (select) {
    select.closeDropdown();
  }
};

watch(isOpen, async (newVal) => {
  if (newVal) {
    await nextTick();
    updatePosition();
    
    // Adicionar listener para click outside após um pequeno delay
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
      data-select-content
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
