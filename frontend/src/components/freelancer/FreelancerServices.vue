<script setup lang="ts">
import { ref } from 'vue';
import { FileText, Clock, DollarSign } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';

interface Service {
  id: string;
  name: string;
  description: string;
  price: string;
  duration: string;
  category: string;
  details: string;
}

interface Props {
  freelancerId: string;
  onRequestQuote: () => void;
}

const props = defineProps<Props>();

const selectedService = ref<Service | null>(null);

// Mock data - substituir por dados reais da API
const services: Service[] = [
  {
    id: '1',
    name: 'Instalação Elétrica Residencial',
    description: 'Instalação completa de sistema elétrico em residências',
    price: 'R$ 150 - R$ 300',
    duration: '4-8 horas',
    category: 'Instalação',
    details: 'Serviço completo de instalação elétrica residencial incluindo: planejamento, cabeamento, instalação de tomadas e interruptores, quadro de distribuição, aterramento e testes de segurança. Materiais não inclusos.',
  },
  {
    id: '2',
    name: 'Manutenção Preventiva',
    description: 'Revisão e manutenção de instalações elétricas',
    price: 'R$ 100 - R$ 200',
    duration: '2-4 horas',
    category: 'Manutenção',
    details: 'Inspeção completa do sistema elétrico, verificação de conexões, teste de disjuntores, medição de tensão, identificação de problemas potenciais e elaboração de relatório técnico.',
  },
  {
    id: '3',
    name: 'Instalação de Ar Condicionado',
    description: 'Instalação e configuração de aparelhos de ar condicionado',
    price: 'R$ 200 - R$ 400',
    duration: '3-6 horas',
    category: 'Instalação',
    details: 'Instalação completa de ar condicionado split ou janela, incluindo fixação das unidades, passagem de tubulação, conexões elétricas, teste de funcionamento e configuração. Materiais de instalação inclusos.',
  },
  {
    id: '4',
    name: 'Troca de Disjuntores',
    description: 'Substituição de disjuntores e proteção elétrica',
    price: 'R$ 80 - R$ 150',
    duration: '1-2 horas',
    category: 'Reparo',
    details: 'Troca de disjuntores defeituosos ou inadequados, instalação de DPS (Dispositivo de Proteção contra Surtos) e DR (Dispositivo Residual), garantindo a segurança do sistema elétrico.',
  },
  {
    id: '5',
    name: 'Instalação de Ventiladores de Teto',
    description: 'Instalação completa de ventiladores de teto',
    price: 'R$ 60 - R$ 120',
    duration: '1-2 horas',
    category: 'Instalação',
    details: 'Instalação de ventiladores de teto incluindo fixação segura, conexão elétrica adequada, instalação de controle (se aplicável) e teste de funcionamento em todas as velocidades.',
  },
  {
    id: '6',
    name: 'Projeto Elétrico Comercial',
    description: 'Elaboração de projeto elétrico para estabelecimentos comerciais',
    price: 'R$ 500 - R$ 1.500',
    duration: '1-3 dias',
    category: 'Projeto',
    details: 'Desenvolvimento completo de projeto elétrico comercial conforme normas ABNT, incluindo dimensionamento de carga, layout de pontos, especificação de materiais, memorial descritivo e ART (Anotação de Responsabilidade Técnica).',
  },
];

const handleServiceClick = (service: Service) => {
  selectedService.value = service;
};

const handleRequestQuote = (e?: MouseEvent) => {
  if (e) {
    e.stopPropagation();
  }
  selectedService.value = null;
  props.onRequestQuote();
};
</script>

<template>
  <div class="space-y-6">
    <div>
      <h3 class="text-[#263238] mb-2">Serviços Oferecidos</h3>
      <p class="text-[#435574]">
        Selecione um serviço para ver mais detalhes ou solicitar um orçamento personalizado
      </p>
    </div>

    <!-- Services Table - Desktop -->
    <div class="hidden md:block overflow-x-auto">
      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200">
            <th class="text-left py-3 px-4 text-[#263238]">Serviço</th>
            <th class="text-left py-3 px-4 text-[#263238]">Categoria</th>
            <th class="text-left py-3 px-4 text-[#263238]">Duração</th>
            <th class="text-left py-3 px-4 text-[#263238]">Preço</th>
            <th class="text-right py-3 px-4 text-[#263238]">Ação</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="service in services"
            :key="service.id"
            class="border-b border-gray-100 hover:bg-gray-50 transition-colors cursor-pointer"
            @click="handleServiceClick(service)"
          >
            <td class="py-4 px-4">
              <div>
                <p class="text-[#263238]">{{ service.name }}</p>
                <p class="text-sm text-[#435574] mt-1">{{ service.description }}</p>
              </div>
            </td>
            <td class="py-4 px-4">
              <Badge variant="secondary" class="bg-gray-100 text-[#435574]">
                {{ service.category }}
              </Badge>
            </td>
            <td class="py-4 px-4">
              <div class="flex items-center gap-2 text-[#435574]">
                <Clock class="h-4 w-4" />
                <span class="text-sm">{{ service.duration }}</span>
              </div>
            </td>
            <td class="py-4 px-4">
              <div class="flex items-center gap-2 text-[#286AD8]">
                <DollarSign class="h-4 w-4" />
                <span>{{ service.price }}</span>
              </div>
            </td>
            <td class="py-4 px-4 text-right">
              <Button
                @click="handleRequestQuote"
                size="sm"
              >
                <FileText class="h-4 w-4 mr-2" />
                Solicitar
              </Button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Services Cards - Mobile -->
    <div class="md:hidden space-y-4">
      <div
        v-for="service in services"
        :key="service.id"
        class="bg-white border border-gray-200 rounded-lg p-4 hover:border-[#286AD8] transition-colors cursor-pointer"
        @click="handleServiceClick(service)"
      >
        <div class="mb-3">
          <h4 class="text-[#263238] mb-1">{{ service.name }}</h4>
          <p class="text-sm text-[#435574]">{{ service.description }}</p>
        </div>

        <div class="flex flex-wrap gap-2 mb-3">
          <Badge variant="secondary" class="bg-gray-100 text-[#435574]">
            {{ service.category }}
          </Badge>
          <div class="flex items-center gap-1 text-sm text-[#435574]">
            <Clock class="h-3 w-3" />
            {{ service.duration }}
          </div>
        </div>

        <div class="flex items-center justify-between">
          <div class="flex items-center gap-1 text-[#286AD8]">
            <DollarSign class="h-4 w-4" />
            <span>{{ service.price }}</span>
          </div>
          <Button
            @click="handleRequestQuote"
            size="sm"
          >
            Solicitar
          </Button>
        </div>
      </div>
    </div>

    <!-- Service Details Modal -->
    <Dialog :open="selectedService !== null" @update:open="(val) => !val && (selectedService = null)">
      <DialogContent class="max-w-2xl">
        <DialogHeader>
          <DialogTitle class="text-[#263238]">{{ selectedService?.name }}</DialogTitle>
          <DialogDescription class="text-[#435574]">
            {{ selectedService?.description }}
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="flex flex-wrap gap-3">
            <Badge variant="secondary" class="bg-gray-100 text-[#435574]">
              {{ selectedService?.category }}
            </Badge>
            <div class="flex items-center gap-2 text-[#435574]">
              <Clock class="h-4 w-4" />
              <span class="text-sm">{{ selectedService?.duration }}</span>
            </div>
            <div class="flex items-center gap-2 text-[#286AD8]">
              <DollarSign class="h-4 w-4" />
              <span>{{ selectedService?.price }}</span>
            </div>
          </div>

          <div class="border-t border-gray-200 pt-4">
            <h4 class="text-[#263238] mb-2">Detalhes do Serviço</h4>
            <p class="text-[#435574]">{{ selectedService?.details }}</p>
          </div>

          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p class="text-sm text-[#435574]">
              <strong class="text-[#263238]">Nota:</strong> Os valores podem variar conforme a complexidade do projeto. Solicite um orçamento personalizado para obter o preço exato.
            </p>
          </div>

          <div class="flex gap-3 pt-4">
            <Button
              @click="handleRequestQuote"
              class="flex-1"
            >
              <FileText class="h-4 w-4 mr-2" />
              Solicitar Orçamento
            </Button>
            <Button
              @click="selectedService = null"
              variant="outline"
              class="flex-1"
            >
              Fechar
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>

