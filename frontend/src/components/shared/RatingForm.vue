<script setup lang="ts">
import { ref, computed, type Component } from 'vue';
import { ArrowLeft, Star, Send, AlertCircle, Sparkles, CheckCircle2 } from 'lucide-vue-next';
import { toast } from '@/utils/toast';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Textarea from '../ui/textarea.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';
import reviewService from '@/api/services/reviewService';

export interface RatingTag {
  id: string;
  label: string;
  icon: string | Component;
}

export interface RatingFormProps {
  title: string;
  subtitle?: string;
  personName: string;
  personAvatar?: string;
  personLabel?: string;
  tags: RatingTag[];
  tagsTitle?: string;
  tagsDescription?: string;
  commentPlaceholder?: string;
  commentDescription?: string;
  showAiNotice?: boolean;
  privacyNotice?: string;
  successTitle?: string;
  successMessage?: string;
  backButtonText?: string;
}

interface Props extends RatingFormProps {
  onBack: () => void;
  onSubmit: (review: { rating: number; comment: string; tags: string[] }) => void | Promise<void>;
}

const props = withDefaults(defineProps<Props>(), {
  personLabel: 'Como foi sua experiência?',
  tagsTitle: 'Pontos Fortes (opcional)',
  tagsDescription: 'Selecione as características que se destacaram',
  commentPlaceholder: 'Compartilhe sua experiência...',
  commentDescription: 'Compartilhe detalhes sobre sua experiência',
  showAiNotice: true,
  privacyNotice: 'Sua avaliação será publicada com anonimato parcial, exibindo apenas seu primeiro nome. Você só pode avaliar uma vez por contrato concluído.',
  successTitle: 'Avaliação Enviada!',
  successMessage: 'Obrigado por sua avaliação',
  backButtonText: 'Voltar',
});

const rating = ref(0);
const hoverRating = ref(0);
const comment = ref('');
const selectedTags = ref<string[]>([]);
const showPolicyAlert = ref(true);
const isSubmitting = ref(false);
const isSuggestingComment = ref(false);
const submitted = ref(false);

const displayRating = computed(() => hoverRating.value || rating.value);

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
    case 1: return 'Muito insatisfeito';
    case 2: return 'Insatisfeito';
    case 3: return 'Regular';
    case 4: return 'Satisfeito';
    case 5: return 'Muito satisfeito';
    default: return 'Selecione uma nota';
  }
};

const getSelectedTagLabel = (tagId: string) => {
  return props.tags.find((t) => t.id === tagId);
};

const handleSubmit = async () => {
  if (rating.value === 0) {
    alert('Por favor, selecione uma nota de 1 a 5 estrelas');
    return;
  }

  isSubmitting.value = true;

  try {
    await props.onSubmit({
      rating: rating.value,
      comment: comment.value,
      tags: selectedTags.value,
    });
    submitted.value = true;
  } catch {
    // Erro tratado pelo componente pai
  } finally {
    isSubmitting.value = false;
  }
};

const handleSuggestComment = async () => {
  isSuggestingComment.value = true;
  try {
    const response = await reviewService.suggestComment({
      rating: rating.value,
      tags: selectedTags.value,
      serviceName: props.subtitle,
      freelancerName: props.personName,
      existingComment: comment.value.trim() || null,
    });
    comment.value = response.suggestion;
  } catch {
    toast.error('Não foi possível gerar a sugestão. Tente novamente.');
  } finally {
    isSuggestingComment.value = false;
  }
};

const reset = () => {
  rating.value = 0;
  hoverRating.value = 0;
  comment.value = '';
  selectedTags.value = [];
  submitted.value = false;
  isSubmitting.value = false;
  isSuggestingComment.value = false;
};

defineExpose({ reset });
</script>

<template>
  <!-- Success State -->
  <div v-if="submitted" class="max-w-2xl mx-auto py-12">
    <Card class="p-8 text-center space-y-6">
      <div class="flex justify-center">
        <div class="relative">
          <div class="absolute inset-0 bg-green-100 rounded-full animate-ping opacity-75" />
          <div class="relative bg-green-100 rounded-full p-6">
            <CheckCircle2 class="h-16 w-16 text-green-600" />
          </div>
        </div>
      </div>

      <div>
        <h1 class="text-[#263238] mb-2">{{ successTitle }}</h1>
        <p class="text-[#435574]">{{ successMessage }}</p>
      </div>

      <div v-if="showAiNotice" class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start gap-3">
        <Sparkles class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
        <div class="text-left text-sm">
          <p class="text-[#263238] mb-1">Processamento por IA</p>
          <p class="text-[#435574]">
            Sua avaliação será processada por nossa inteligência artificial para gerar um resumo
            automático que ajudará outros usuários.
          </p>
        </div>
      </div>

      <div class="bg-gray-50 rounded-lg p-4 text-left">
        <p class="text-sm text-[#435574] mb-2">Sua avaliação:</p>
        <div class="flex items-center gap-2 mb-2">
          <Star
            v-for="star in [1, 2, 3, 4, 5]"
            :key="star"
            :class="`h-5 w-5 ${
              star <= rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'
            }`"
          />
          <span class="text-[#263238] ml-2">{{ getRatingLabel(rating) }}</span>
        </div>
        <div v-if="selectedTags.length > 0" class="flex flex-wrap gap-2 mb-2">
          <Badge
            v-for="tagId in selectedTags"
            :key="tagId"
            variant="secondary"
          >
            <component
              :is="getSelectedTagLabel(tagId)?.icon"
              v-if="getSelectedTagLabel(tagId) && typeof getSelectedTagLabel(tagId)?.icon !== 'string'"
              class="h-4 w-4"
            />
            <span v-else>{{ getSelectedTagLabel(tagId)?.icon }}</span>
            {{ getSelectedTagLabel(tagId)?.label }}
          </Badge>
        </div>
        <p v-if="comment" class="text-sm text-[#435574] mt-2 italic">"{{ comment }}"</p>
      </div>

      <Button @click="onBack" variant="default" class="w-full" size="lg">
        {{ backButtonText }}
      </Button>
    </Card>
  </div>

  <!-- Form State -->
  <div v-else class="max-w-3xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center gap-4">
      <Button @click="onBack" variant="ghost" size="icon" class="hover:bg-gray-100">
        <ArrowLeft class="h-5 w-5" />
      </Button>
      <div class="flex-1">
        <h1 class="text-[#263238]">{{ title }}</h1>
        <p v-if="subtitle" class="text-[#435574]">{{ subtitle }}</p>
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
            ou que violem nossa política de conduta não serão permitidos.
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

    <!-- Person Card -->
    <Card class="p-6">
      <div class="flex items-center gap-4">
        <Avatar class="h-16 w-16">
          <AvatarImage :src="personAvatar" :alt="personName" />
          <AvatarFallback class="bg-[#286AD8] text-white text-xl">
            {{ getInitials(personName) }}
          </AvatarFallback>
        </Avatar>
        <div>
          <h3 class="text-[#263238]">{{ personName }}</h3>
          <p class="text-sm text-[#435574]">{{ personLabel }}</p>
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
            type="button"
          >
            <Star
              :class="`h-12 w-12 transition-colors ${
                star <= displayRating
                  ? 'fill-yellow-400 text-yellow-400'
                  : 'text-gray-300 hover:text-yellow-200'
              }`"
            />
          </button>
        </div>
        <p class="text-[#435574]">{{ getRatingLabel(displayRating) }}</p>
      </div>

      <Separator class="my-6" />

      <!-- Tags Section -->
      <div class="space-y-3">
        <h4 class="text-[#263238]">{{ tagsTitle }}</h4>
        <p class="text-sm text-[#435574] mb-4">{{ tagsDescription }}</p>

        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
          <button
            v-for="tag in tags"
            :key="tag.id"
            @click="toggleTag(tag.id)"
            type="button"
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
              <component
                :is="tag.icon"
                v-if="typeof tag.icon !== 'string'"
                class="h-5 w-5 text-[#286AD8]"
              />
              <span v-else class="text-xl">{{ tag.icon }}</span>
              <span class="text-sm text-[#263238]">{{ tag.label }}</span>
            </div>
          </button>
        </div>
      </div>

      <Separator class="my-6" />

      <!-- Comment Section -->
      <div class="space-y-3">
        <h4 class="text-[#263238]">Comentário (opcional)</h4>
        <p class="text-sm text-[#435574] mb-4">{{ commentDescription }}</p>

        <div v-if="rating >= 1" class="flex justify-end mb-2">
          <Button
            @click="handleSuggestComment"
            :disabled="isSuggestingComment"
            variant="outline"
            size="sm"
            type="button"
            class="text-[#286AD8] border-[#286AD8] hover:bg-blue-50"
          >
            <span v-if="isSuggestingComment" class="flex items-center gap-1.5">
              <span class="animate-spin h-3.5 w-3.5 border-2 border-[#286AD8] border-t-transparent rounded-full" />
              Gerando...
            </span>
            <span v-else class="flex items-center gap-1.5">
              <Sparkles class="h-3.5 w-3.5" />
              {{ comment.trim() ? 'Refinar com IA' : 'Sugerir com IA' }}
            </span>
          </Button>
        </div>

        <Textarea
          :placeholder="commentPlaceholder"
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
    <div v-if="showAiNotice" class="bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-lg p-4">
      <div class="flex items-start gap-3">
        <Sparkles class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
        <div class="text-sm">
          <p class="text-[#263238] mb-1">
            <strong>Resumo Automático por IA</strong>
          </p>
          <p class="text-[#435574]">
            Sua avaliação será processada em até 5 segundos por nossa inteligência artificial
            para gerar um resumo que destaque os principais pontos.
          </p>
        </div>
      </div>
    </div>

    <!-- Privacy Notice -->
    <div class="bg-gray-50 rounded-lg p-4">
      <p class="text-sm text-[#435574]">
        <strong class="text-[#263238]">Privacidade:</strong> {{ privacyNotice }}
      </p>
    </div>

    <!-- Submit Button -->
    <Button
      @click="handleSubmit"
      :disabled="rating === 0 || isSubmitting"
      variant="default"
      class="w-full"
      size="lg"
    >
      <span v-if="isSubmitting" class="flex items-center gap-2">
        <span class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
        Processando avaliação...
      </span>
      <span v-else class="flex items-center gap-2">
        <Send class="h-4 w-4" />
        Enviar Avaliação
      </span>
    </Button>
  </div>
</template>
