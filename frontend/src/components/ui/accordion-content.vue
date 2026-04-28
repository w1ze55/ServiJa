<script setup lang="ts">
import { computed, inject } from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const accordion = inject<{ isOpen: (value: string) => boolean }>('accordion');
const itemValue = inject<string>('accordionItemValue');
const TRANSITION_DURATION_MS = 200;

const isOpen = computed(() => accordion?.isOpen(itemValue || '') || false);

const contentClasses = computed(() =>
  cn('overflow-hidden text-sm', props.class)
);

const innerClasses = computed(() =>
  cn('pt-0 pb-4', props.class)
);

const setTransition = (element: Element) => {
  const el = element as HTMLElement;
  el.style.overflow = 'hidden';
  el.style.transition = `height ${TRANSITION_DURATION_MS}ms ease, opacity ${TRANSITION_DURATION_MS}ms ease`;
};

const beforeEnter = (element: Element) => {
  const el = element as HTMLElement;
  setTransition(el);
  el.style.height = '0';
  el.style.opacity = '0';
};

const enter = (element: Element) => {
  const el = element as HTMLElement;
  requestAnimationFrame(() => {
    el.style.height = `${el.scrollHeight}px`;
    el.style.opacity = '1';
  });
};

const afterEnter = (element: Element) => {
  const el = element as HTMLElement;
  el.style.height = 'auto';
  el.style.overflow = '';
};

const beforeLeave = (element: Element) => {
  const el = element as HTMLElement;
  setTransition(el);
  el.style.height = `${el.scrollHeight}px`;
  el.style.opacity = '1';
};

const leave = (element: Element) => {
  const el = element as HTMLElement;
  requestAnimationFrame(() => {
    el.style.height = '0';
    el.style.opacity = '0';
  });
};

const afterLeave = (element: Element) => {
  const el = element as HTMLElement;
  el.style.height = '';
  el.style.opacity = '';
  el.style.overflow = '';
  el.style.transition = '';
};
</script>

<template>
  <Transition
    @before-enter="beforeEnter"
    @enter="enter"
    @after-enter="afterEnter"
    @before-leave="beforeLeave"
    @leave="leave"
    @after-leave="afterLeave"
  >
    <div
      v-if="isOpen"
      data-slot="accordion-content"
      :class="contentClasses"
      :data-state="isOpen ? 'open' : 'closed'"
      v-bind="$attrs"
    >
      <div :class="innerClasses">
        <slot />
      </div>
    </div>
  </Transition>
</template>
