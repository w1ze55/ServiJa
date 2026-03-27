<script setup lang="ts">
import { ref } from 'vue';
import { Play, Flag } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import ImageWithFallback from '../figma/ImageWithFallback.vue';
import { toast } from '../../utils/toast';

interface PortfolioItem {
  id: string;
  type: 'image' | 'video';
  url: string;
  thumbnail?: string;
  title: string;
  description: string;
  relatedServices: string[];
  date: string;
}

interface Props {
  freelancerId: string;
}

const props = defineProps<Props>();

const selectedItem = ref<PortfolioItem | null>(null);
const reportItem = ref<PortfolioItem | null>(null);
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

const handleReport = (item: PortfolioItem) => {
  reportItem.value = item;
};

const confirmReport = () => {
  if (reportItem.value) {
    toast.success('Denúncia enviada com sucesso. Agradecemos sua colaboração!');
    reportItem.value = null;
    selectedItem.value = null;
  }
};
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
      <DialogContent class="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle class="text-[#263238] pr-8">{{ selectedItem?.title }}</DialogTitle>
          <DialogDescription class="text-[#435574]">
            Publicado em {{ selectedItem && new Date(selectedItem.date).toLocaleDateString('pt-BR') }}
          </DialogDescription>
        </DialogHeader>

        <div class="hidden sm:block space-y-4">
          <!-- Media -->
          <div class="rounded-lg overflow-hidden bg-gray-100">
            <video
              v-if="selectedItem?.type === 'video'"
              :src="selectedItem.url"
              controls
              class="w-full"
              :poster="selectedItem.thumbnail"
            >
              Seu navegador não suporta a reprodução de vídeos.
            </video>
            <ImageWithFallback
              v-else
              :src="selectedItem?.url || ''"
              :alt="selectedItem?.title || ''"
              class="w-full h-auto"
            />
          </div>

          <!-- Description -->
          <div>
            <h4 class="text-[#263238] mb-2">Descrição</h4>
            <p class="text-[#435574]">{{ selectedItem?.description }}</p>
          </div>

          <!-- Related Services -->
          <div v-if="selectedItem && selectedItem.relatedServices.length > 0">
            <h4 class="text-[#263238] mb-2">Serviços Relacionados</h4>
            <div class="flex flex-wrap gap-2">
              <Badge
                v-for="(service, index) in selectedItem.relatedServices"
                :key="index"
                variant="secondary"
                class="bg-blue-100 text-[#286AD8]"
              >
                {{ service }}
              </Badge>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 pt-4 border-t border-gray-200">
            <Button
              @click="selectedItem && handleReport(selectedItem)"
              variant="outline"
              class="text-red-600 border-red-600 hover:bg-red-50"
            >
              <Flag class="h-4 w-4 mr-2" />
              Denunciar conteúdo
            </Button>
            <Button @click="selectedItem = null" variant="outline" class="ml-auto">
              Fechar
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- Report Confirmation Dialog -->
    <AlertDialog :open="reportItem !== null" @update:open="(val) => !val && (reportItem = null)">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Denunciar conteúdo inadequado</AlertDialogTitle>
          <AlertDialogDescription>
            Você está prestes a denunciar o item "{{ reportItem?.title }}".
            Nossa equipe irá revisar este conteúdo e tomar as medidas necessárias.
            <br /><br />
            Deseja continuar?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="confirmReport" class="bg-red-600 hover:bg-red-700">
            Confirmar denúncia
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

