<script setup lang="ts">
// Sonner é uma biblioteca de toast notifications
// Para Vue, podemos usar uma alternativa ou criar um wrapper simples
// Por enquanto, vamos criar um componente básico que pode ser usado com sonner-vue se disponível

interface Props {
  [key: string]: any;
}

defineProps<Props>();
</script>

<template>
  <div v-bind="$attrs">
    <slot />
  </div>
</template>

<script lang="ts">
// Nota: Sonner é uma biblioteca específica do React
// Para Vue, considere usar uma alternativa como vue-toastification ou criar um wrapper
// Este componente é um placeholder básico
export default {
  name: 'Toaster',
};
</script>






