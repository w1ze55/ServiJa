<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Vue3Lottie } from 'vue3-lottie';
import animationData from './404-animation.json';

const router = useRouter();

const goHome = () => {
  router.push('/');
};

</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-50 via-white to-primary-50 flex items-center justify-center p-4">
    <div class="max-w-4xl w-full text-center">
      <!-- Animação Lottie -->
      <div class="mb-8 flex justify-center">
        <div class="lottie-container">
          <Vue3Lottie 
            :animationData="animationData"
            :height="600"
            :width="800"
            :loop="true"
            :autoPlay="true"
          />
        </div>
      </div>

      <!-- Mensagem -->
      <div class="mb-8 msg fade-in-up">
        <h2 class="text-3xl md:text-4xl font-bold text-gray-900 mb-4 fade-in-up" style="animation-delay: 0.2s">
          Página não encontrada
        </h2>
        <p class="text-lg text-gray-600 max-w-md mx-auto fade-in-up" style="animation-delay: 0.4s">
          Ops! A página que você está procurando não existe ou foi movida.
        </p>
        <button @click="goHome" class="btn-primary btn-voltar-home fade-in-up" style="animation-delay: 0.6s">Voltar para a página inicial</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Animação de fade-in e slide-up */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-in-up {
  animation: fadeInUp 0.8s ease-out forwards;
  opacity: 0;
}

.btn-voltar-home {
  margin-top: 2rem;
  background-color: #265cff;
  color: white;
  font-weight: 600;
  font-size: 1.125rem;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.btn-voltar-home:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

.lottie-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.lottie-container :deep(svg) {
  width: 100% !important;
  height: auto !important;
  max-height: 600px;
}

.lottie-container :deep(.lottie-animation-container) {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
}

@media (max-width: 768px) {
  .btn-voltar-home {
    font-size: 0.8rem;
    padding: 0.5rem 1rem;
  }
  .btn-voltar-home:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  }

  .min-h-screen {
    padding: 1rem;
  }

  .lottie-container {
    max-width: 90vw;
    padding: 0 1rem;
  }

  .lottie-container :deep(svg) {
    max-height: 350px;
    max-width: 100%;
  }

  h2 {
    font-size: 1.5rem !important;
    line-height: 2rem !important;
  }

  p {
    font-size: 0.875rem !important;
    line-height: 1.25rem !important;
    padding: 0 1rem;
  }
}

@media (max-width: 480px) {
  .min-h-screen {
    padding: 0.5rem;
  }

  .lottie-container {
    max-width: 85vw;
    padding: 0 0.5rem;
  }

  .lottie-container :deep(svg) {
    max-height: 250px;
    max-width: 100%;
  }

  h2 {
    font-size: 1.25rem !important;
    line-height: 1.75rem !important;
    margin-bottom: 0.75rem !important;
  }

  p {
    font-size: 0.8125rem !important;
    line-height: 1.125rem !important;
  }

  .mb-8 {
    margin-bottom: 1.5rem !important;
  }
}
</style>
