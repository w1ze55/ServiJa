<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Sparkles } from 'lucide-vue-next';
import Card from '../ui/card.vue';
import CardHeader from '../ui/card-header.vue';
import CardContent from '../ui/card-content.vue';
import CardFooter from '../ui/card-footer.vue';
import Badge from '../ui/badge.vue';
import reviewService, { type AiSummaryResponse } from '@/api/services/reviewService';

interface Props {
  freelancerId: string;
}

const props = defineProps<Props>();

const isLoading = ref(false);
const summary = ref<AiSummaryResponse | null>(null);

const formatDate = (dateStr: string) => {
  try {
    return new Date(dateStr).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
};

onMounted(async () => {
  isLoading.value = true;
  try {
    const data = await reviewService.getAiSummary(props.freelancerId);
    if (data.available) {
      summary.value = data;
    }
  } catch {
    // falha silenciosa: não renderiza o card
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <div>
    <div v-if="isLoading" class="flex justify-center py-6">
      <div class="animate-spin h-6 w-6 border-4 border-[#286AD8] border-t-transparent rounded-full" />
    </div>

    <Card v-else-if="summary && summary.available && summary.overall" class="border border-gray-200 mb-6">
      <CardHeader class="pb-2">
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div class="flex items-center gap-2">
            <Sparkles class="h-5 w-5 text-[#286AD8]" />
            <h3 class="text-[#263238] font-semibold">Análise por IA</h3>
          </div>
        </div>
      </CardHeader>

      <CardContent class="pt-0">
        <!-- Síntese Geral -->
        <p class="text-sm text-[#435574] leading-relaxed">
          {{ summary.overall }}
        </p>

        <!-- Pontos Fortes (opcionais, exibidos como tags) -->
        <div v-if="summary.strengths && summary.strengths.length > 0" class="flex flex-wrap gap-2 mt-3">
          <span
            v-for="(point, idx) in summary.strengths"
            :key="idx"
            class="inline-flex items-center gap-1 text-xs bg-green-50 text-green-700 border border-green-100 rounded-full px-2.5 py-1"
          >
            <span class="text-green-500">✓</span>
            {{ point }}
          </span>
        </div>
      </CardContent>

      <CardFooter v-if="summary.generatedAt" class="pt-0 pb-3">
        <p class="text-xs text-gray-400">
          Atualizado em {{ formatDate(summary.generatedAt) }}
        </p>
      </CardFooter>
    </Card>
  </div>
</template>
