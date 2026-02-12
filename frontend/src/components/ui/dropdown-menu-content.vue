<script setup lang="ts">
import { computed, inject, watch, onUnmounted, ref, nextTick } from 'vue';
import { cn } from './utils';

interface Props {
  align?: 'start' | 'end' | 'center';
  class?: string;
  [key: string]: any;
}

const props = withDefaults(defineProps<Props>(), {
  align: 'end',
});

const dropdownMenu = inject<{ 
  isOpen: { value: boolean }; 
  close: () => void;
  triggerRef?: { value: HTMLElement | null };
}>('dropdownMenu');
const isOpen = computed(() => dropdownMenu?.isOpen.value || false);
const contentRef = ref<HTMLElement | null>(null);
const position = ref({ top: '0px', left: '0px', transform: 'none' });

const classes = computed(() =>
  cn(
    'bg-popover text-popover-foreground z-50 min-w-[8rem] overflow-hidden rounded-md border p-1 shadow-md',
    props.class
  )
);

const updatePosition = () => {
  if (!contentRef.value || !dropdownMenu?.triggerRef?.value) return;

  const trigger = dropdownMenu.triggerRef.value;
  const content = contentRef.value;
  const triggerRect = trigger.getBoundingClientRect();
  const contentRect = content.getBoundingClientRect();
  
  let top = triggerRect.bottom + 8; // 8px de espaçamento
  let left = triggerRect.left;

  // Ajuste baseado no alinhamento
  if (props.align === 'end') {
    left = triggerRect.right - contentRect.width;
  } else if (props.align === 'center') {
    left = triggerRect.left + (triggerRect.width - contentRect.width) / 2;
  }

  // Verifica se há espaço suficiente na parte inferior
  const spaceBelow = window.innerHeight - triggerRect.bottom;
  const spaceAbove = triggerRect.top;
  
  if (spaceBelow < contentRect.height && spaceAbove > contentRect.height) {
    // Mostra acima do trigger
    top = triggerRect.top - contentRect.height - 8;
  } else if (spaceBelow < contentRect.height) {
    // Ajusta para não sair da tela
    top = window.innerHeight - contentRect.height - 8;
  }

  // Garante que não saia da tela horizontalmente
  if (left + contentRect.width > window.innerWidth) {
    left = window.innerWidth - contentRect.width - 8;
  }
  if (left < 8) {
    left = 8;
  }

  position.value = {
    top: `${top}px`,
    left: `${left}px`,
    transform: 'none',
  };
};

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  if (contentRef.value && !contentRef.value.contains(target)) {
    const trigger = dropdownMenu?.triggerRef?.value;
    if (trigger && !trigger.contains(target)) {
      dropdownMenu?.close();
    }
  }
};

let updateOnResize: (() => void) | null = null;

watch(isOpen, async (newVal) => {
  if (newVal) {
    await nextTick();
    updatePosition();
    // Atualiza posição ao redimensionar ou scrollar
    updateOnResize = () => updatePosition();
    window.addEventListener('resize', updateOnResize);
    window.addEventListener('scroll', updateOnResize, true);
    
    setTimeout(() => {
      document.addEventListener('click', handleClickOutside);
    }, 0);
  } else {
    // Limpa os listeners quando fechar
    if (updateOnResize) {
      window.removeEventListener('resize', updateOnResize);
      window.removeEventListener('scroll', updateOnResize, true);
      updateOnResize = null;
    }
    document.removeEventListener('click', handleClickOutside);
  }
});

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
  <Teleport to="body">
    <div
      v-if="isOpen"
      ref="contentRef"
      data-dropdown-menu
      :class="classes"
      :style="{
        position: 'fixed',
        ...position,
      }"
      v-bind="$attrs"
    >
      <slot />
    </div>
  </Teleport>
</template>

