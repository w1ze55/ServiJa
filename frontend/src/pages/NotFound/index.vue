<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Home, ArrowLeft, Search } from 'lucide-vue-next';

const router = useRouter();

const goHome = () => {
  router.push('/');
};

const goBack = () => {
  router.back();
};

const goToSearch = () => {
  router.push('/search');
};
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-50 via-white to-primary-50 flex items-center justify-center p-4">
    <div class="max-w-2xl w-full text-center">
      <!-- 404 Número -->
      <div class="mb-8">
        <h1 class="text-[150px] md:text-[200px] font-bold text-primary-600 leading-none tracking-tight animate-pulse">
          404
        </h1>
      </div>

      <!-- Mensagem -->
      <div class="mb-12">
        <h2 class="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
          Página não encontrada
        </h2>
        <p class="text-lg text-gray-600 max-w-md mx-auto">
          Ops! A página que você está procurando não existe ou foi movida.
        </p>
      </div>

      <!-- Ilustração -->
      <div class="mb-12">
        <div class="w-64 h-64 mx-auto bg-primary-100 rounded-full flex items-center justify-center">
          <Search :size="100" class="text-primary-300" stroke-width="1.5" />
        </div>
      </div>

      <!-- Botões de ação -->
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <button
          @click="goBack"
          class="inline-flex items-center justify-center gap-2 px-6 py-3 bg-white border-2 border-primary-600 text-primary-600 font-semibold rounded-lg hover:bg-primary-50 transition-colors duration-200"
        >
          <ArrowLeft :size="20" />
          Voltar
        </button>

        <button
          @click="goHome"
          class="inline-flex items-center justify-center gap-2 px-6 py-3 bg-primary-600 text-white font-semibold rounded-lg hover:bg-primary-700 transition-colors duration-200 shadow-lg shadow-primary-600/30"
        >
          <Home :size="20" />
          Ir para Início
        </button>

        <button
          @click="goToSearch"
          class="inline-flex items-center justify-center gap-2 px-6 py-3 bg-white border-2 border-gray-300 text-gray-700 font-semibold rounded-lg hover:bg-gray-50 transition-colors duration-200"
        >
          <Search :size="20" />
          Buscar Serviços
        </button>
      </div>

      <!-- Links úteis -->
      <div class="mt-12 pt-8 border-t border-gray-200">
        <p class="text-sm text-gray-500 mb-4">Links úteis:</p>
        <div class="flex flex-wrap gap-4 justify-center text-sm">
          <router-link to="/login" class="text-primary-600 hover:text-primary-700 hover:underline">
            Login
          </router-link>
          <router-link to="/explore" class="text-primary-600 hover:text-primary-700 hover:underline">
            Explorar
          </router-link>
          <router-link to="/map" class="text-primary-600 hover:text-primary-700 hover:underline">
            Mapa de Profissionais
          </router-link>
          <router-link to="/settings" class="text-primary-600 hover:text-primary-700 hover:underline">
            Configurações
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>
