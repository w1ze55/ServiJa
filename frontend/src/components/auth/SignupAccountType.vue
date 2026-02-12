<script setup lang="ts">
import { ref } from 'vue';
import Button from '../ui/button.vue';
import { Check, Briefcase, Users, ArrowLeft } from 'lucide-vue-next';
import Progress from '../ui/progress.vue';

interface SignupAccountTypeProps {
  onNext: (accountType: 'contractor' | 'provider') => void;
  onBack: () => void;
  onBackToLanding?: () => void;
}

const props = defineProps<SignupAccountTypeProps>();

const selectedType = ref<'contractor' | 'provider' | null>(null);

const accountTypes: Array<{ 
  type: 'contractor' | 'provider'; 
  icon: any; 
  title: string; 
  description: string; 
  features: string[];
}> = [
  {
    type: 'contractor' as const,
    icon: Users,
    title: 'Contratante',
    description: 'Encontre profissionais qualificados para realizar seus projetos',
    features: [
      'Busque e compare profissionais',
      'Solicite orçamentos grátis',
      'Avalie serviços prestados',
      'Gerencie suas contratações'
    ]
  },
  {
    type: 'provider' as const,
    icon: Briefcase,
    title: 'Freelancer',
    description: 'Ofereça seus serviços e conquiste novos clientes',
    features: [
      'Crie seu perfil profissional',
      'Receba solicitações de orçamento',
      'Gerencie seus projetos',
      'Construa sua reputação'
    ]
  }
];
</script>

<template>
  <div class="signup-account-type-container">
    <div class="signup-account-type-card">
      <button
        @click="props.onBack"
        class="signup-account-type-back-button"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar
      </button>

      <div class="signup-account-type-header">
        <div class="signup-account-type-progress-header">
          <h2 class="signup-account-type-title">Criar conta</h2>
          <span class="signup-account-type-step">Passo 1 de 3</span>
        </div>
        <Progress :value="33" class="h-2" />
      </div>

      <div class="signup-account-type-intro">
        <h3 class="signup-account-type-intro-title">Escolha o tipo de conta</h3>
        <p class="signup-account-type-intro-text">Selecione como você deseja usar o Servi Já</p>
      </div>

      <div class="signup-account-type-grid">
        <button
          v-for="account in accountTypes"
          :key="account.type"
          @click="selectedType = account.type"
          class="signup-account-type-button"
          :class="{ 'selected': selectedType === account.type }"
        >
          <div 
            v-if="selectedType === account.type"
            class="signup-account-type-check"
          >
            <Check class="h-4 w-4" />
          </div>

          <div 
            class="signup-account-type-icon"
            :class="{ 'selected': selectedType === account.type }"
          >
            <component 
              :is="account.icon" 
              class="h-6 w-6"
              :class="{ 'selected': selectedType === account.type }"
            />
          </div>

          <h4 class="signup-account-type-button-title">{{ account.title }}</h4>
          <p class="signup-account-type-button-description">{{ account.description }}</p>

          <ul class="signup-account-type-features">
            <li 
              v-for="(feature, index) in account.features" 
              :key="index" 
              class="signup-account-type-feature"
            >
              <Check 
                class="h-4 w-4"
                :class="{ 'selected': selectedType === account.type }"
              />
              <span>{{ feature }}</span>
            </li>
          </ul>
        </button>
      </div>

      <Button
        @click="selectedType && props.onNext(selectedType)"
        :disabled="!selectedType"
        class="signup-account-type-submit-button"
        size="lg"
      >
        Continuar
      </Button>

      <div v-if="props.onBackToLanding" class="signup-account-type-back-wrapper">
        <button
          @click="props.onBackToLanding"
          class="signup-account-type-back-link"
        >
          <ArrowLeft class="h-4 w-4" />
          Voltar para a página inicial
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '@/styles/auth/signup-account-type.css';
</style>
