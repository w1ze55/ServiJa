<script setup lang="ts">
import {
  computed,
  inject,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue';
import { cn } from './utils';

interface Props {
  class?: string;
  [key: string]: any;
}

const props = defineProps<Props>();
const tabs = inject<{ value: { value: string } }>('tabs');
const listRef = ref<HTMLElement | null>(null);
const indicatorStyle = ref<Record<string, string>>({
  opacity: '0',
  transform: 'translate(0px, 0px)',
  width: '0px',
  height: '0px',
});

let resizeObserver: ResizeObserver | null = null;

const updateIndicator = async () => {
  await nextTick();

  const listElement = listRef.value;
  if (!listElement) {
    return;
  }

  const activeTrigger = listElement.querySelector<HTMLElement>(
    '[data-slot="tabs-trigger"][data-state="active"]'
  );

  if (!activeTrigger) {
    indicatorStyle.value = {
      ...indicatorStyle.value,
      opacity: '0',
    };
    return;
  }

  const centeredTop = (listElement.clientHeight - activeTrigger.offsetHeight) / 2;

  indicatorStyle.value = {
    opacity: '1',
    width: `${activeTrigger.offsetWidth}px`,
    height: `${activeTrigger.offsetHeight}px`,
    transform: `translate(${activeTrigger.offsetLeft}px, ${centeredTop}px)`,
  };
};

const classes = computed(() =>
  cn(
    'bg-muted text-muted-foreground relative inline-flex min-h-9 w-fit items-center justify-center rounded-xl p-[3px] flex overflow-hidden',
    props.class
  )
);

watch(
  () => tabs?.value.value,
  () => {
    updateIndicator();
  },
  { immediate: true }
);

onMounted(() => {
  updateIndicator();

  if (listRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      updateIndicator();
    });
    resizeObserver.observe(listRef.value);
  }

  window.addEventListener('resize', updateIndicator);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  window.removeEventListener('resize', updateIndicator);
});
</script>

<template>
  <div
    ref="listRef"
    data-slot="tabs-list"
    :class="classes"
    v-bind="$attrs"
  >
    <div
      class="pointer-events-none absolute left-0 top-0 z-0 rounded-xl border border-slate-200 bg-white shadow-sm transition-[transform,width,height,opacity] duration-150 ease-out"
      :style="indicatorStyle"
    />
    <slot />
  </div>
</template>



