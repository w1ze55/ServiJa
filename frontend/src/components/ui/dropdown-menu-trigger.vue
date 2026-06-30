<script setup lang="ts">
import { inject, nextTick, onMounted, ref } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const dropdownMenu = inject<{ toggle: () => void; triggerRef?: { value: HTMLElement | null } }>('dropdownMenu');
const triggerElementRef = ref<HTMLElement | null>(null);

const syncTriggerRef = () => {
  if (!triggerElementRef.value || !dropdownMenu?.triggerRef) return;

  const actualTrigger =
    (triggerElementRef.value.querySelector('button') as HTMLElement | null) ||
    (triggerElementRef.value.firstElementChild as HTMLElement | null) ||
    triggerElementRef.value;

  dropdownMenu.triggerRef.value = actualTrigger;
};

const handleClick = () => {
  syncTriggerRef();
  dropdownMenu?.toggle();

  // Mantém a referência no botão real mesmo quando o slot re-renderiza.
  nextTick(syncTriggerRef);
};

onMounted(() => {
  nextTick(syncTriggerRef);
});
</script>

<template>
  <div
    ref="triggerElementRef"
    :class="props.asChild ? 'inline-flex' : undefined"
    @click.capture="handleClick"
    v-bind="$attrs"
  >
    <slot />
  </div>
</template>
