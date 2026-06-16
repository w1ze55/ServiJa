<script setup lang="ts">
import { ref } from 'vue';
import {
  ArrowLeft,
  CheckCircle,
  XCircle,
  MessageCircle,
  FileText,
  DollarSign,
  Calendar,
  Clock,
  User,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Textarea from '../ui/textarea.vue';
import Input from '../ui/input.vue';
import DatePicker from '../ui/date-picker.vue';
import Label from '../ui/label.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import { toast } from '../../utils/toast';

interface TimelineEvent {
  id: string;
  type: 'created' | 'proposal' | 'counter' | 'accepted' | 'rejected' | 'message';
  title: string;
  description: string;
  date: string;
  author: 'client' | 'freelancer';
  data?: any;
}

interface Proposal {
  value: number;
  deadline: string;
  deliveryTime: string;
  scope: string;
  notes?: string;
}

interface Props {
  requestId: string;
  freelancerId: string;
  onBack: () => void;
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const showAcceptDialog = ref(false);
const showRejectDialog = ref(false);
const showCounterDialog = ref(false);
const counterValue = ref('');
const counterDeadline = ref('');
const counterNotes = ref('');

// Mock data - substituir por dados reais da API
const request = {
  id: props.requestId,
  freelancerId: props.freelancerId,
  freelancerName: 'João Silva',
  freelancerAvatar: '',
  freelancerRating: 4.9,
  service: 'Instalação Elétrica Residencial',
  description:
    'Preciso de instalação elétrica completa em apartamento de 80m². Incluindo quadro de distribuição, tomadas, interruptores e iluminação.',
  budget: 2500,
  deadline: '2024-12-01',
  status: 'responded' as const,
  createdAt: '2024-11-10T10:30:00',
};

const currentProposal: Proposal = {
  value: 3200,
  deadline: '2024-11-25',
  deliveryTime: '15 dias',
  scope:
    'Instalação elétrica completa incluindo:\n- Quadro de distribuição com disjuntores individualizados\n- 20 pontos de tomada\n- 15 pontos de iluminação\n- Instalação de interruptores\n- Aterramento\n- Testes e certificação\n\nMateriais inclusos no orçamento.',
  notes: 'Trabalho com materiais de primeira qualidade. Garantia de 1 ano para mão de obra.',
};

const timeline: TimelineEvent[] = [
  {
    id: '1',
    type: 'created',
    title: 'Solicitação enviada',
    description: 'Você enviou uma solicitação de orçamento',
    date: '2024-11-10T10:30:00',
    author: 'client',
  },
  {
    id: '2',
    type: 'proposal',
    title: 'Proposta recebida',
    description: 'João Silva enviou uma proposta de R$ 3.200,00',
    date: '2024-11-10T14:20:00',
    author: 'freelancer',
    data: currentProposal,
  },
  {
    id: '3',
    type: 'message',
    title: 'Nova mensagem',
    description: 'João: "Olá! Vi sua solicitação e posso realizar o serviço..."',
    date: '2024-11-10T14:22:00',
    author: 'freelancer',
  },
];

const handleAccept = () => {
  toast.success('Proposta aceita com sucesso!');
  showAcceptDialog.value = false;
  // Atualizar status e navegar
};

const handleReject = () => {
  toast.success('Proposta recusada');
  showRejectDialog.value = false;
  props.onBack();
};

const handleCounter = () => {
  if (!counterValue.value || !counterDeadline.value) {
    toast.error('Preencha o valor e o prazo');
    return;
  }

  toast.success('Contraproposta enviada!');
  showCounterDialog.value = false;
  counterValue.value = '';
  counterDeadline.value = '';
  counterNotes.value = '';
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatCurrency = (value: number) => {
  return value.toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });
};

const getEventIcon = (type: TimelineEvent['type']) => {
  switch (type) {
    case 'created':
      return FileText;
    case 'proposal':
    case 'counter':
      return DollarSign;
    case 'accepted':
      return CheckCircle;
    case 'rejected':
      return XCircle;
    case 'message':
      return MessageCircle;
    default:
      return Clock;
  }
};
</script>

<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <Button
        variant="ghost"
        @click="props.onBack"
        class="text-[#435574] hover:text-[#263238] mb-4"
      >
        <ArrowLeft class="h-4 w-4 mr-2" />
        Voltar para solicitações
      </Button>

      <div class="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
        <div>
          <h1 class="text-[#263238] mb-2">Detalhes da Solicitação</h1>
          <p class="text-[#435574]">#{{ props.requestId }}</p>
        </div>
        <Badge class="bg-blue-100 text-blue-700 border border-blue-200 w-fit">
          Aguardando sua resposta
        </Badge>
      </div>
    </div>

    <div class="grid lg:grid-cols-3 gap-6">
      <!-- Main Content -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Request Info -->
        <div class="bg-white rounded-lg border border-gray-200 p-6">
          <h3 class="text-[#263238] mb-4">Informações da Solicitação</h3>

          <div class="space-y-4">
            <div>
              <Label class="text-[#435574]">Serviço</Label>
              <p class="text-[#263238]">{{ request.service }}</p>
            </div>

            <div>
              <Label class="text-[#435574]">Descrição</Label>
              <p class="text-[#263238]">{{ request.description }}</p>
            </div>

            <div class="grid md:grid-cols-2 gap-4">
              <div>
                <Label class="text-[#435574]">Orçamento estimado</Label>
                <p class="text-[#263238]">{{ formatCurrency(request.budget) }}</p>
              </div>
              <div>
                <Label class="text-[#435574]">Prazo desejado</Label>
                <p class="text-[#263238]">{{ new Date(request.deadline).toLocaleDateString('pt-BR') }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Current Proposal -->
        <div class="bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-lg p-6">
          <div class="flex items-start gap-3 mb-4">
            <div class="w-10 h-10 bg-[#286AD8] rounded-full flex items-center justify-center">
              <DollarSign class="h-5 w-5 text-white" />
            </div>
            <div>
              <h3 class="text-[#263238] mb-1">Proposta do Freelancer</h3>
              <p class="text-sm text-[#435574]">
                Recebida em
                {{ formatDate(timeline.find((t) => t.type === 'proposal')?.date || '') }}
              </p>
            </div>
          </div>

          <div class="bg-white rounded-lg p-4 space-y-4 mb-4">
            <div class="grid md:grid-cols-2 gap-4">
              <div>
                <Label class="text-[#435574]">Valor</Label>
                <p class="text-2xl text-[#286AD8]">{{ formatCurrency(currentProposal.value) }}</p>
              </div>
              <div>
                <Label class="text-[#435574]">Prazo de entrega</Label>
                <p class="text-[#263238]">
                  {{ new Date(currentProposal.deadline).toLocaleDateString('pt-BR') }}
                </p>
              </div>
            </div>

            <div>
              <Label class="text-[#435574] mb-2 block">Escopo do serviço</Label>
              <div class="text-[#263238] whitespace-pre-line bg-gray-50 p-3 rounded border border-gray-200">
                {{ currentProposal.scope }}
              </div>
            </div>

            <div v-if="currentProposal.notes">
              <Label class="text-[#435574] mb-2 block">Observações</Label>
              <p class="text-[#263238] bg-gray-50 p-3 rounded border border-gray-200">
                {{ currentProposal.notes }}
              </p>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex flex-col sm:flex-row gap-3">
            <Button @click="showAcceptDialog = true" variant="success" class="flex-1">
              <CheckCircle class="h-4 w-4 mr-2" />
              Aceitar Proposta
            </Button>
            <Button
              @click="showCounterDialog = true"
              variant="outline"
              class="flex-1 text-primary border-primary"
            >
              <DollarSign class="h-4 w-4 mr-2" />
              Enviar Contraproposta
            </Button>
            <Button
              @click="showRejectDialog = true"
              variant="outline"
              class="flex-1 border-red-600 text-red-600 hover:bg-red-50"
            >
              <XCircle class="h-4 w-4 mr-2" />
              Recusar
            </Button>
          </div>
        </div>

        <!-- Timeline -->
        <div class="bg-white rounded-lg border border-gray-200 p-6">
          <h3 class="text-[#263238] mb-4">Histórico</h3>

          <div class="space-y-4">
            <div v-for="(event, index) in timeline" :key="event.id" class="flex gap-4">
              <div class="flex flex-col items-center">
                <div
                  :class="`w-10 h-10 rounded-full flex items-center justify-center ${
                    event.author === 'freelancer'
                      ? 'bg-blue-100 text-[#286AD8]'
                      : 'bg-gray-100 text-[#435574]'
                  }`"
                >
                  <component :is="getEventIcon(event.type)" class="h-5 w-5" />
                </div>
                <div v-if="index !== timeline.length - 1" class="w-0.5 flex-1 bg-gray-200 mt-2" />
              </div>

              <div class="flex-1 pb-6">
                <div class="flex items-start justify-between mb-1">
                  <h4 class="text-[#263238]">{{ event.title }}</h4>
                  <span class="text-sm text-[#435574]">{{ formatDate(event.date) }}</span>
                </div>
                <p class="text-[#435574]">{{ event.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Sidebar -->
      <div class="space-y-6">
        <!-- Freelancer Info -->
        <div class="bg-white rounded-lg border border-gray-200 p-6">
          <h3 class="text-[#263238] mb-4">Freelancer</h3>

          <div class="flex items-center gap-3 mb-4">
            <Avatar class="h-12 w-12">
              <AvatarImage :src="request.freelancerAvatar" :alt="request.freelancerName" />
              <AvatarFallback class="bg-[#286AD8] text-white">
                {{ getInitials(request.freelancerName) }}
              </AvatarFallback>
            </Avatar>
            <div>
              <p class="text-[#263238]">{{ request.freelancerName }}</p>
              <p class="text-sm text-[#435574]">Eletricista</p>
            </div>
          </div>

          <Button
            @click="props.onNavigate('chat', { freelancerId: request.freelancerId })"
            class="w-full"
          >
            <MessageCircle class="h-4 w-4 mr-2" />
            Abrir chat
          </Button>
        </div>

        <!-- Quick Stats -->
        <div class="bg-white rounded-lg border border-gray-200 p-6">
          <h3 class="text-[#263238] mb-4">Comparação</h3>

          <div class="space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-sm text-[#435574]">Seu orçamento</span>
              <span class="text-[#263238]">{{ formatCurrency(request.budget) }}</span>
            </div>
            <div class="flex justify-between items-center">
              <span class="text-sm text-[#435574]">Proposta</span>
              <span class="text-[#286AD8]">{{ formatCurrency(currentProposal.value) }}</span>
            </div>
            <div class="pt-3 border-t border-gray-200">
              <div class="flex justify-between items-center">
                <span class="text-sm text-[#435574]">Diferença</span>
                <span
                  :class="
                    currentProposal.value > request.budget ? 'text-red-600' : 'text-green-600'
                  "
                >
                  {{ currentProposal.value > request.budget ? '+' : '-' }}
                  {{ formatCurrency(Math.abs(currentProposal.value - request.budget)) }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Accept Dialog -->
    <AlertDialog :open="showAcceptDialog" @update:open="(val) => showAcceptDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Aceitar Proposta</AlertDialogTitle>
          <AlertDialogDescription>
            Você está aceitando a proposta de {{ request.freelancerName }} no valor de
            <strong>{{ formatCurrency(currentProposal.value) }}</strong>.
            <br />
            <br />
            Após aceitar, você será direcionado para o pagamento.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="
              showAcceptDialog = false;
              props.onNavigate('checkout', {
                requestId: props.requestId,
                freelancerId: props.freelancerId,
                freelancerName: request.freelancerName,
                freelancerAvatar: request.freelancerAvatar,
                freelancerRating: request.freelancerRating,
                service: request.service,
                description: request.description,
                price: currentProposal.value,
                deliveryTime: currentProposal.deliveryTime,
              });
            "
            class="bg-success hover:bg-success/90"
          >
            Confirmar e Ir para Pagamento
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Reject Dialog -->
    <AlertDialog :open="showRejectDialog" @update:open="(val) => showRejectDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Recusar Proposta</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja recusar esta proposta?
            <br />
            <br />
            Esta ação não pode ser desfeita e a solicitação será encerrada.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleReject" class="bg-destructive hover:bg-destructive/90">
            Confirmar recusa
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Counter Proposal Dialog -->
    <Dialog :open="showCounterDialog" @update:open="(val) => showCounterDialog = val">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>Enviar Contraproposta</DialogTitle>
          <DialogDescription>Negocie o valor e o prazo com o freelancer</DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div>
            <Label for="counter-value">Valor proposto *</Label>
            <div class="relative mt-2">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-[#435574]">R$</span>
              <Input
                id="counter-value"
                type="number"
                placeholder="0,00"
                :model-value="counterValue"
                @update:model-value="(val) => counterValue = val as string"
                class="pl-10"
                min="0"
                step="0.01"
              />
            </div>
          </div>

          <div>
            <Label for="counter-deadline">Prazo desejado *</Label>
            <DatePicker
              id="counter-deadline"
              :model-value="counterDeadline"
              @update:model-value="(val) => counterDeadline = val as string"
              :min="new Date().toISOString().split('T')[0]"
              placeholder="Selecione a data"
              class="mt-2"
            />
          </div>

          <div>
            <Label for="counter-notes">Observações (opcional)</Label>
            <Textarea
              id="counter-notes"
              placeholder="Adicione observações sobre sua contraproposta..."
              :model-value="counterNotes"
              @update:model-value="(val) => counterNotes = val as string"
              rows="3"
              class="mt-2"
            />
          </div>

          <div class="flex gap-3 pt-4">
            <Button @click="showCounterDialog = false" variant="outline" class="flex-1">
              Cancelar
            </Button>
            <Button @click="handleCounter" class="flex-1">
              Enviar
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </div>
</template>




