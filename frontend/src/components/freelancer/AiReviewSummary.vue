<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Sparkles, TrendingUp, AlertCircle } from 'lucide-vue-next';
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

    <Card v-else-if="summary && summary.available" class="border border-gray-200 mb-6">
      <CardHeader class="pb-3">
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div class="flex items-center gap-2">
            <Sparkles class="h-5 w-5 text-[#286AD8]" />
            <h3 class="text-[#263238] font-semibold">Análise por IA</h3>
          </div>
          <Badge variant="secondary" class="text-xs">Gerado automaticamente</Badge>
        </div>
      </CardHeader>

      <CardContent class="space-y-4">
        <!-- Pontos Fortes -->
        <div v-if="summary.strengths.length > 0">
          <div class="flex items-center gap-2 mb-2">
            <TrendingUp class="h-4 w-4 text-green-600" />
            <span class="text-sm font-medium text-[#263238]">Pontos Fortes</span>
          </div>
          <ul class="space-y-1">
            <li
              v-for="(point, idx) in summary.strengths"
              :key="idx"
              class="flex items-start gap-2 text-sm text-[#435574]"
            >
              <span class="text-green-600 mt-0.5 flex-shrink-0">•</span>
              {{ point }}
            </li>
          </ul>
        </div>

        <!-- Pontos de Atenção -->
        <div v-if="summary.attentionPoints && summary.attentionPoints.length > 0">
          <div class="flex items-center gap-2 mb-2">
            <AlertCircle class="h-4 w-4 text-yellow-500" />
            <span class="text-sm font-medium text-[#263238]">Pontos de Atenção</span>
          </div>
          <ul class="space-y-1">
            <li
              v-for="(point, idx) in summary.attentionPoints"
              :key="idx"
              class="flex items-start gap-2 text-sm text-[#435574]"
            >
              <span class="text-yellow-500 mt-0.5 flex-shrink-0">•</span>
              {{ point }}
            </li>
          </ul>
        </div>

        <!-- Síntese Geral -->
        <p v-if="summary.overall" class="text-sm text-[#435574] leading-relaxed border-t border-gray-100 pt-3">
          {{ summary.overall }}
        </p>
      </CardContent>

      <CardFooter v-if="summary.generatedAt" class="pt-0 pb-3">
        <p class="text-xs text-gray-400">
          Atualizado em {{ formatDate(summary.generatedAt) }}
        </p>
      </CardFooter>
    </Card>
  </div>
</template>
