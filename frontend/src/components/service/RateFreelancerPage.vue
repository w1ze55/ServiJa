<script setup lang="ts">
import { ref } from 'vue';
import { ArrowLeft, Star, Send, AlertCircle, Sparkles, CheckCircle2 } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Textarea from '../ui/textarea.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';

interface Props {
  serviceId: string;
  serviceName: string;
  freelancerName: string;
  freelancerAvatar?: string;
  onBack: () => void;
  onSubmitReview: (review: { rating: number; comment: string; tags: string[] }) => void;
}

const props = defineProps<Props>();

const availableTags = [
  { id: 'quality', label: 'Qualidade Técnica', icon: '⚡' },
  { id: 'punctuality', label: 'Pontualidade', icon: '⏰' },
  { id: 'communication', label: 'Comunicação', icon: '💬' },
  { id: 'professionalism', label: 'Profissionalismo', icon: '👔' },
  { id: 'creativity', label: 'Criatividade', icon: '🎨' },
  { id: 'flexibility', label: 'Flexibilidade', icon: '🔄' },
  { id: 'attention', label: 'Atenção aos Detalhes', icon: '🔍' },
  { id: 'price', label: 'Bom Custo-Benefício', icon: '💰' },
];

const rating = ref(0);
const hoverRating = ref(0);
const comment = ref('');
const selectedTags = ref<string[]>([]);
const showPolicyAlert = ref(true);
const isSubmitting = ref(false);
const submitted = ref(false);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const toggleTag = (tagId: string) => {
  if (selectedTags.value.includes(tagId)) {
    selectedTags.value = selectedTags.value.filter((t) => t !== tagId);
  } else {
    selectedTags.value = [...selectedTags.value, tagId];
  }
};

const getRatingLabel = (stars: number) => {
  switch (stars) {
    case 1:
      return 'Muito insatisfeito';
    case 2:
      return 'Insatisfeito';
    case 3:
      return 'Regular';
    case 4:
      return 'Satisfeito';
    case 5:
      return 'Muito satisfeito';
    default:
      return 'Selecione uma nota';
  }
};

const handleSubmit = async () => {
  if (rating.value === 0) {
    alert('Por favor, selecione uma nota de 1 a 5 estrelas');
    return;
  }

  isSubmitting.value = true;

  // Simular processamento
  setTimeout(() => {
    props.onSubmitReview({
      rating: rating.value,
      comment: comment.value,
      tags: selectedTags.value,
    });
    isSubmitting.value = false;
    submitted.value = true;
  }, 2000);
};
</script>

<template>
  <div v-if="submitted" class="max-w-2xl mx-auto py-12">
    <Card class="p-8 text-center space-y-6">
      <!-- Success Animation -->
      <div class="flex justify-center">
        <div class="relative">
          <div class="absolute inset-0 bg-green-100 rounded-full animate-ping opacity-75" />
          <div class="relative bg-green-100 rounded-full p-6">
            <CheckCircle2 class="h-16 w-16 text-green-600" />
          </div>
        </div>
      </div>

      <!-- Success Message -->
      <div>
        <h1 class="text-[#263238] mb-2">Avaliação Enviada!</h1>
        <p class="text-[#435574]">Obrigado por avaliar o trabalho do freelancer</p>
      </div>

      <!-- AI Processing Notice -->
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start gap-3">
        <Sparkles class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
        <div class="text-left text-sm">
          <p class="text-[#263238] mb-1">Processamento por IA</p>
          <p class="text-[#435574]">
            Sua avaliação será processada por nossa inteligência artificial para gerar um resumo
            automático que ajudará outros clientes a encontrar os melhores profissionais.
          </p>
        </div>
      </div>

      <!-- Summary -->
      <div class="bg-gray-50 rounded-lg p-4 text-left">
        <p class="text-sm text-[#435574] mb-2">Sua avaliação:</p>
        <div class="flex items-center gap-2 mb-2">
          <Star
            v-for="star in [1, 2, 3, 4, 5]"
            :key="star"
            :class="`h-5 w-5 ${
              star <= rating.value ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'
            }`"
          />
          <span class="text-[#263238] ml-2">{{ getRatingLabel(rating.value) }}</span>
        </div>
        <div v-if="selectedTags.length > 0" class="flex flex-wrap gap-2 mb-2">
          <Badge
            v-for="tagId in selectedTags"
            :key="tagId"
            variant="secondary"
          >
            {{ availableTags.find((t) => t.id === tagId)?.icon }}
            {{ availableTags.find((t) => t.id === tagId)?.label }}
          </Badge>
        </div>
        <p v-if="comment" class="text-sm text-[#435574] mt-2 italic">"{{ comment }}"</p>
      </div>

      <!-- Actions -->
      <Button
        @click="props.onBack"
        class="w-full bg-[#286AD8] hover:bg-[#435574]"
        size="lg"
      >
        Voltar
      </Button>
    </Card>
  </div>

  <div v-else class="max-w-3xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center gap-4">
      <Button @click="props.onBack" variant="ghost" size="icon" class="hover:bg-gray-100">
        <ArrowLeft class="h-5 w-5" />
      </Button>
      <div class="flex-1">
        <h1 class="text-[#263238]">Avaliar Freelancer</h1>
        <p class="text-[#435574]">{{ props.serviceName }}</p>
      </div>
    </div>

    <!-- Policy Alert -->
    <Card v-if="showPolicyAlert" class="p-4 bg-blue-50 border-blue-200">
      <div class="flex items-start gap-3">
        <AlertCircle class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
        <div class="flex-1">
          <p class="text-sm text-[#263238] mb-2">
            <strong>Política de Conduta e Respeito</strong>
          </p>
          <p class="text-sm text-[#435574] mb-3">
            Avaliações devem ser honestas e construtivas. Comentários ofensivos, discriminatórios
            ou que violem nossa política de conduta não serão permitidos. Seu primeiro nome será
            exibido junto à avaliação.
          </p>
          <Button
            @click="showPolicyAlert = false"
            variant="ghost"
            size="sm"
            class="text-[#286AD8] hover:bg-blue-100"
          >
            Entendi
          </Button>
        </div>
      </div>
    </Card>

    <!-- Freelancer Card -->
    <Card class="p-6">
      <div class="flex items-center gap-4">
        <Avatar class="h-16 w-16">
          <AvatarImage :src="props.freelancerAvatar" :alt="props.freelancerName" />
          <AvatarFallback class="bg-[#286AD8] text-white text-xl">
            {{ getInitials(props.freelancerName) }}
          </AvatarFallback>
        </Avatar>
        <div>
          <h3 class="text-[#263238]">{{ props.freelancerName }}</h3>
          <p class="text-sm text-[#435574]">Como foi sua experiência?</p>
        </div>
      </div>
    </Card>

    <!-- Rating Section -->
    <Card class="p-6">
      <h3 class="text-[#263238] mb-4">Avaliação Geral</h3>

      <!-- Star Rating -->
      <div class="flex flex-col items-center gap-4 py-6">
        <div class="flex gap-2">
          <button
            v-for="star in [1, 2, 3, 4, 5]"
            :key="star"
            @click="rating = star"
            @mouseenter="hoverRating = star"
            @mouseleave="hoverRating = 0"
            class="transition-transform hover:scale-110"
          >
            <Star
              :class="`h-12 w-12 transition-colors ${
                star <= (hoverRating || rating)
                  ? 'fill-yellow-400 text-yellow-400'
                  : 'text-gray-300 hover:text-yellow-200'
              }`"
            />
          </button>
        </div>

        <p class="text-[#435574]">{{ getRatingLabel(hoverRating || rating) }}</p>
      </div>

      <Separator class="my-6" />

      <!-- Tags Section -->
      <div class="space-y-3">
        <h4 class="text-[#263238]">Pontos Fortes (opcional)</h4>
        <p class="text-sm text-[#435574] mb-4">
          Selecione as características que se destacaram no trabalho do freelancer
        </p>

        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
          <button
            v-for="tag in availableTags"
            :key="tag.id"
            @click="toggleTag(tag.id)"
            :class="`
                  p-3 rounded-lg border-2 transition-all text-left
                  ${
                    selectedTags.includes(tag.id)
                      ? 'border-[#286AD8] bg-blue-50 shadow-sm'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  }
                `"
          >
            <div class="flex items-center gap-2">
              <span class="text-xl">{{ tag.icon }}</span>
              <span class="text-sm text-[#263238]">{{ tag.label }}</span>
            </div>
          </button>
        </div>
      </div>

      <Separator class="my-6" />

      <!-- Comment Section -->
      <div class="space-y-3">
        <h4 class="text-[#263238]">Comentário (opcional)</h4>
        <p class="text-sm text-[#435574] mb-4">
          Compartilhe detalhes sobre sua experiência para ajudar outros clientes
        </p>

        <Textarea
          placeholder="Conte como foi trabalhar com este freelancer..."
          v-model="comment"
          :rows="5"
          :maxlength="500"
        />

        <div class="flex justify-between text-sm text-[#435574]">
          <span>Mínimo recomendado: 50 caracteres</span>
          <span>{{ comment.length }}/500</span>
        </div>
      </div>
    </Card>

    <!-- AI Processing Info -->
    <div class="bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-lg p-4">
      <div class="flex items-start gap-3">
        <Sparkles class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
        <div class="text-sm">
          <p class="text-[#263238] mb-1">
            <strong>Resumo Automático por IA</strong>
          </p>
          <p class="text-[#435574]">
            Sua avaliação será processada em até 5 segundos por nossa inteligência artificial
            para gerar um resumo que destaque os principais pontos do freelancer.
          </p>
        </div>
      </div>
    </div>

    <!-- Privacy Notice -->
    <div class="bg-gray-50 rounded-lg p-4">
      <p class="text-sm text-[#435574]">
        <strong class="text-[#263238]">Privacidade:</strong> Sua avaliação será publicada
        com anonimato parcial, exibindo apenas seu primeiro nome, conforme nossa política de privacidade.
        Você só pode avaliar uma vez por contrato concluído.
      </p>
    </div>

    <!-- Submit Button -->
    <Button
      @click="handleSubmit"
      :disabled="rating === 0 || isSubmitting"
      class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
      size="lg"
    >
      <span v-if="isSubmitting" class="flex items-center gap-2">
        <span
          class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"
        />
        Processando avaliação...
      </span>
      <span v-else class="flex items-center gap-2">
        <Send class="h-4 w-4" />
        Enviar Avaliação
      </span>
    </Button>
  </div>
</template>

