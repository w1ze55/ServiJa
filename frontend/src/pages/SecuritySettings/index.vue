<script setup lang="ts">
import TwoFactorSection from '@/components/security/TwoFactorSection.vue'
</script>

<template>
  <div class="mx-auto max-w-3xl space-y-6 p-6">
    <header>
      <h1 class="text-2xl font-bold">Segurança</h1>
      <p class="text-sm text-gray-600">
        Gerencie como você acessa sua conta.
      </p>
    </header>

    <TwoFactorSection />
  </div>
</template>
