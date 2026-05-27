<script setup lang="ts">
interface Props {
  open: boolean
  onClose?: () => void
}
defineProps<Props>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/50" @click="onClose && onClose()" />
      <div
        class="relative z-10 w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl"
        @click.stop
      >
        <slot />
      </div>
    </div>
  </Teleport>
</template>
