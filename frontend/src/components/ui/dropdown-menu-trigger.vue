<script setup lang="ts">
import { inject, ref, onMounted, nextTick } from 'vue';

interface Props {
  asChild?: boolean;
  [key: string]: any;
}

const props = defineProps<Props>();
const dropdownMenu = inject<{ toggle: () => void; triggerRef?: { value: HTMLElement | null } }>('dropdownMenu');
const triggerElementRef = ref<HTMLElement | null>(null);

const handleClick = () => {
  dropdownMenu?.toggle();
  // Atualiza a referência do trigger após abrir
  nextTick(() => {
    if (triggerElementRef.value && dropdownMenu?.triggerRef) {
      // Pega o primeiro elemento filho (o botão real) ou o próprio elemento
      const actualTrigger = triggerElementRef.value.querySelector('button') || triggerElementRef.value.firstElementChild as HTMLElement || triggerElementRef.value;
      dropdownMenu.triggerRef.value = actualTrigger;
    }
  });
};

onMounted(() => {
  nextTick(() => {
    if (triggerElementRef.value && dropdownMenu?.triggerRef) {
      // Pega o primeiro elemento filho (o botão real) ou o próprio elemento
      const actualTrigger = triggerElementRef.value.querySelector('button') || triggerElementRef.value.firstElementChild as HTMLElement || triggerElementRef.value;
      dropdownMenu.triggerRef.value = actualTrigger;
    }
  });
});
</script>

<template>
  <div ref="triggerElementRef" @click="handleClick" v-bind="$attrs">
    <slot />
  </div>
</template>

