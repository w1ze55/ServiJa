<script setup lang="ts">
import { ref } from 'vue';
import {
  Play,
  Briefcase,
  Sparkles,
  ExternalLink,
  Calendar,
  X as CloseIcon,
  Youtube,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import ImageWithFallback from '../figma/ImageWithFallback.vue';

interface PortfolioItem {
  id: string;
  type: 'image' | 'video';
  url: string;
  thumbnail?: string;
  title: string;
  description: string;
  relatedServices: string[];
  date: string;
  projectUrl?: string;
  client?: string;
  featured?: boolean;
}

interface Props {
  freelancerId: string;
}

const props = defineProps<Props>();

const selectedItem = ref<PortfolioItem | null>(null);
const imageErrors = ref<Record<string, boolean>>({});

// Mock data - substituir por dados reais da API
const portfolioItems: PortfolioItem[] = [
  {
    id: '1',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=800',
    title: 'Instalação Elétrica Completa - Apartamento 120m²',
    description: 'Projeto completo de instalação elétrica em apartamento novo, incluindo automação residencial e sistema de iluminação inteligente.',
    relatedServices: ['Instalação Elétrica Residencial', 'Projeto Elétrico'],
    date: '2024-10-15',
  },
  {
    id: '2',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=800',
    title: 'Quadro de Distribuição - Residência',
    description: 'Instalação de novo quadro de distribuição com disjuntores individualizados e proteção DR.',
    relatedServices: ['Troca de Disjuntores', 'Manutenção Preventiva'],
    date: '2024-09-22',
  },
  {
    id: '3',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1621905252507-b35492cc74b4?w=800',
    title: 'Instalação de Painéis Solares',
    description: 'Sistema fotovoltaico residencial com 8 painéis solares e inversor de 3kW.',
    relatedServices: ['Instalação Elétrica Residencial'],
    date: '2024-08-10',
  },
  {
    id: '4',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1581094271901-8022df4466f9?w=800',
    title: 'Iluminação Comercial - Loja',
    description: 'Projeto de iluminação para loja de roupas com spots direcionáveis e LED.',
    relatedServices: ['Projeto Elétrico Comercial'],
    date: '2024-07-18',
  },
  {
    id: '5',
    type: 'video',
    url: 'https://www.w3schools.com/html/mov_bbb.mp4',
    thumbnail: 'https://images.unsplash.com/photo-1621905251918-48416bd8575a?w=800',
    title: 'Demonstração - Sistema de Automação',
    description: 'Vídeo demonstrativo do sistema de automação residencial instalado, com controle de iluminação, persianas e climatização via app.',
    relatedServices: ['Instalação Elétrica Residencial'],
    date: '2024-06-05',
  },
  {
    id: '6',
    type: 'image',
    url: 'https://images.unsplash.com/photo-1581092160562-40aa08e78837?w=800',
    title: 'Instalação Industrial - Painel de Comando',
    description: 'Montagem de painel de comando para linha de produção industrial.',
    relatedServices: ['Projeto Elétrico Comercial'],
    date: '2024-05-12',
  },
];

const handleImageError = (itemId: string) => {
  imageErrors.value[itemId] = true;
};

const getItemCategory = (item?: PortfolioItem | null) =>
  item?.relatedServices[0] || (item?.type === 'video' ? 'Vídeo' : 'Projeto');

const formatCompletedDate = (value?: string) =>
  value
    ? new Date(value).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })
    : 'Data não informada';
</script>

<template>
  <div class="space-y-6">
    <div>
      <h3 class="text-[#263238] mb-2">Portfólio</h3>
      <p class="text-[#435574]">
        Galeria de trabalhos realizados ({{ portfolioItems.length }} itens)
      </p>
    </div>

    <div
      v-if="portfolioItems.length === 0"
      class="text-center py-12 bg-gray-50 rounded-lg border border-gray-200"
    >
      <p class="text-[#435574]">Nenhum item no portfólio ainda</p>
    </div>
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="item in portfolioItems"
        :key="item.id"
        class="group relative bg-white border border-gray-200 rounded-lg overflow-hidden cursor-pointer hover:border-[#286AD8] hover:shadow-lg transition-all"
        @click="selectedItem = item"
      >
        <div class="aspect-video relative bg-gray-100">
          <div
            v-if="imageErrors[item.id]"
            class="absolute inset-0 flex items-center justify-center"
          >
            <p class="text-sm text-[#435574]">Erro ao carregar imagem</p>
          </div>
          <template v-else>
            <ImageWithFallback
              :src="item.type === 'video' && item.thumbnail ? item.thumbnail : item.url"
              :alt="item.title"
              class="w-full h-full object-cover"
              @error="handleImageError(item.id)"
            />
            <div
              v-if="item.type === 'video'"
              class="absolute inset-0 flex items-center justify-center bg-black/30 group-hover:bg-black/40 transition-colors"
            >
              <div class="w-16 h-16 bg-white/90 rounded-full flex items-center justify-center">
                <Play class="h-8 w-8 text-[#286AD8] ml-1" />
              </div>
            </div>
          </template>
        </div>

        <div class="p-4">
          <h4 class="text-[#263238] mb-1 line-clamp-2 group-hover:text-[#286AD8] transition-colors">
            {{ item.title }}
          </h4>
          <p class="text-sm text-[#435574] line-clamp-2 mb-3">
            {{ item.description }}
          </p>
          <div v-if="item.relatedServices.length > 0" class="flex flex-wrap gap-1">
            <Badge
              v-for="(service, index) in item.relatedServices.slice(0, 2)"
              :key="index"
              variant="secondary"
              class="bg-blue-100 text-[#286AD8] text-xs"
            >
              {{ service }}
            </Badge>
            <Badge
              v-if="item.relatedServices.length > 2"
              variant="secondary"
              class="bg-gray-100 text-[#435574] text-xs"
            >
              +{{ item.relatedServices.length - 2 }}
            </Badge>
          </div>
        </div>
      </div>
    </div>

    <!-- Portfolio Item Details Modal -->
    <Dialog :open="selectedItem !== null" @update:open="(val) => !val && (selectedItem = null)">
      <DialogContent class="max-w-[80vw] lg:max-w-5xl xl:max-w-6xl 2xl:max-w-7xl max-h-[90vh] p-0 border-none bg-transparent" :hide-close="true">
        <div class="relative bg-white rounded-xl overflow-hidden shadow-2xl h-[80vh]">
          <button
            @click="selectedItem = null"
            class="absolute top-4 right-4 p-2 bg-black/20 hover:bg-black/40 backdrop-blur-md rounded-full text-white transition-all z-50"
          >
            <CloseIcon class="h-5 w-5" />
          </button>

          <div class="grid grid-cols-1 md:grid-cols-2 h-full overflow-y-auto md:overflow-hidden">
            <div class="relative bg-gray-900 flex items-center justify-center min-h-[300px] md:min-h-[500px]">
              <video
                v-if="selectedItem?.type === 'video'"
                :src="selectedItem.url"
                controls
                class="w-full h-full object-contain"
                :poster="selectedItem.thumbnail"
              >
                Seu navegador não suporta a reprodução de vídeos.
              </video>
              <ImageWithFallback
                v-else-if="selectedItem"
                :src="selectedItem.url"
                :alt="selectedItem.title"
                class="w-full h-full object-contain"
              />
              <div v-else class="flex flex-col items-center text-white/50">
                <Briefcase class="h-16 w-16 mb-4" />
                <span>Nenhuma imagem disponível</span>
              </div>
            </div>

            <div class="p-6 md:p-8 flex flex-col md:h-full md:overflow-y-auto">
              <div>
                <Badge variant="secondary" class="mb-3 bg-blue-50 text-[#286AD8] border-none">
                  {{ getItemCategory(selectedItem) }}
                </Badge>
                <h2 class="text-2xl font-bold text-[#263238] mb-4 break-words">{{ selectedItem?.title }}</h2>

                <p class="text-[#435574] leading-relaxed mb-6 whitespace-pre-line break-words w-full">
                  {{ selectedItem?.description }}
                </p>
              </div>

              <div class="mt-auto pt-6 border-t border-gray-100 flex flex-col gap-4">
                <div class="flex flex-wrap items-center gap-3">
                  <Badge v-if="selectedItem?.featured" class="bg-yellow-500 text-white border-none">
                    <Sparkles class="h-3 w-3 mr-1" />
                    Projeto em Destaque
                  </Badge>
                  <div class="flex items-center gap-2 text-sm text-[#435574]">
                    <div class="p-2 bg-gray-50 rounded-lg">
                      <Calendar class="h-4 w-4 text-[#286AD8]" />
                    </div>
                    <div>
                      <p class="text-[10px] uppercase font-semibold text-gray-400">Concluído em</p>
                      <p class="font-medium">
                        {{ formatCompletedDate(selectedItem?.date) }}
                      </p>
                    </div>
                  </div>

                  <div v-if="selectedItem?.client" class="flex items-center gap-2 text-sm text-[#435574]">
                    <div class="p-2 bg-gray-50 rounded-lg">
                      <Sparkles class="h-4 w-4 text-[#286AD8]" />
                    </div>
                    <div>
                      <p class="text-[10px] uppercase font-semibold text-gray-400">Cliente</p>
                      <p class="font-medium">{{ selectedItem.client }}</p>
                    </div>
                  </div>
                </div>

                <div v-if="selectedItem?.relatedServices && selectedItem.relatedServices.length > 0" class="flex flex-wrap gap-2">
                  <Badge
                    v-for="service in selectedItem.relatedServices"
                    :key="service"
                    variant="secondary"
                    class="bg-gray-100 hover:bg-gray-200 text-[#435574] transition-colors border-none"
                  >
                    {{ service }}
                  </Badge>
                </div>

                <div v-if="selectedItem?.projectUrl || selectedItem?.type === 'video'" class="flex flex-col sm:flex-row gap-3 pt-6 border-t border-gray-100">
                  <Button v-if="selectedItem?.projectUrl" asChild class="flex-1">
                    <a :href="selectedItem.projectUrl" target="_blank" class="flex items-center justify-center">
                      <ExternalLink class="h-4 w-4 mr-2" />
                      Acessar Projeto
                    </a>
                  </Button>
                  <Button v-if="selectedItem?.type === 'video'" asChild variant="outline" class="flex-1">
                    <a :href="selectedItem.url" target="_blank" class="flex items-center justify-center">
                      <Youtube class="h-4 w-4 mr-2" />
                      Ver Vídeo
                    </a>
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>
