<script setup lang="ts">
import RatingForm from '@/components/shared/RatingForm.vue';
import {
  Wrench,
  Clock3,
  MessageSquare,
  Briefcase,
  Lightbulb,
  RefreshCw,
  Search,
  DollarSign,
} from 'lucide-vue-next';

interface Props {
  serviceId: string;
  serviceName: string;
  freelancerName: string;
  freelancerAvatar?: string;
  onBack: () => void;
  onSubmitReview: (review: { rating: number; comment: string; tags: string[] }) => void | Promise<void>;
}

const props = defineProps<Props>();

const freelancerTags = [
  { id: 'quality', label: 'Qualidade Técnica', icon: Wrench },
  { id: 'punctuality', label: 'Pontualidade', icon: Clock3 },
  { id: 'communication', label: 'Comunicação', icon: MessageSquare },
  { id: 'professionalism', label: 'Profissionalismo', icon: Briefcase },
  { id: 'creativity', label: 'Criatividade', icon: Lightbulb },
  { id: 'flexibility', label: 'Flexibilidade', icon: RefreshCw },
  { id: 'attention', label: 'Atenção aos Detalhes', icon: Search },
  { id: 'price', label: 'Bom Custo-Benefício', icon: DollarSign },
];
</script>

<template>
  <RatingForm
    title="Avaliar Freelancer"
    :subtitle="props.serviceName"
    :person-name="props.freelancerName"
    :person-avatar="props.freelancerAvatar"
    person-label="Como foi sua experiência?"
    :tags="freelancerTags"
    tags-title="Pontos Fortes (opcional)"
    tags-description="Selecione as características que se destacaram no trabalho do freelancer"
    comment-placeholder="Conte como foi trabalhar com este freelancer..."
    comment-description="Compartilhe detalhes sobre sua experiência para ajudar outros clientes"
    success-title="Avaliação Enviada!"
    success-message="Obrigado por avaliar o trabalho do freelancer"
    back-button-text="Voltar"
    :on-back="props.onBack"
    :on-submit="props.onSubmitReview"
  />
</template>
