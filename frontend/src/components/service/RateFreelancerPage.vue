<script setup lang="ts">
import RatingForm from '@/components/shared/RatingForm.vue';

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
  { id: 'quality', label: 'Qualidade Técnica', icon: '⚡' },
  { id: 'punctuality', label: 'Pontualidade', icon: '⏰' },
  { id: 'communication', label: 'Comunicação', icon: '💬' },
  { id: 'professionalism', label: 'Profissionalismo', icon: '👔' },
  { id: 'creativity', label: 'Criatividade', icon: '🎨' },
  { id: 'flexibility', label: 'Flexibilidade', icon: '🔄' },
  { id: 'attention', label: 'Atenção aos Detalhes', icon: '🔍' },
  { id: 'price', label: 'Bom Custo-Benefício', icon: '💰' },
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
