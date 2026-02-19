<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  ArrowLeft,
  Calendar,
  MapPin,
  DollarSign,
  Clock,
  FileText,
  Download,
  Send,
  Plus,
  Trash2,
  AlertCircle,
  CheckCircle,
  User,
  MessageSquare,
  Milestone,
  Upload,
  X,
  Eye,
  Paperclip,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
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
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import { toast } from '../../utils/toast';

interface RequestAttachment {
  id: string;
  name: string;
  size: string;
  url: string;
  type: string;
}

interface Milestone {
  id: string;
  title: string;
  description: string;
  deadline: string;
  value: string;
}

interface Message {
  id: string;
  sender: 'freelancer' | 'client';
  senderName: string;
  message: string;
  timestamp: string;
  attachments?: RequestAttachment[];
}

interface AuditLog {
  id: string;
  action: string;
  user: string;
  timestamp: string;
  details?: string;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId?: string;
}

const props = defineProps<Props>();

const activeTab = ref('details');
const showSendProposalDialog = ref(false);
const showConfirmDialog = ref(false);

// Mock request data
const request = ref({
  id: props.requestId || '1',
  serviceTitle: 'Instalação Elétrica Residencial',
  category: 'Instalação Elétrica',
  client: {
    name: 'João Silva',
    avatar: 'https://i.pravatar.cc/150?img=12',
    location: 'São Paulo, SP',
  },
  createdAt: '2026-11-05T10:30:00',
  description:
    'Preciso de um eletricista qualificado para fazer a instalação elétrica completa de uma residência de 120m². O trabalho inclui:\n\n- Instalação de quadro de distribuição\n- Instalação de pontos de luz em todos os cômodos\n- Instalação de tomadas (padrão brasileiro)\n- Aterramento\n- Disjuntores e proteção\n\nA casa tem 3 quartos, 2 banheiros, sala, cozinha e área de serviço. Gostaria de receber propostas com valores e prazos.',
  address: 'Rua das Flores, 123 - Jardim Paulista, São Paulo - SP',
  desiredDate: '2026-11-20',
  budget: 'R$ 3.000 - R$ 5.000',
  urgency: 'Média',
  attachments: [
    {
      id: 'att1',
      name: 'planta-casa.pdf',
      size: '2.4 MB',
      url: '#',
      type: 'application/pdf',
    },
    {
      id: 'att2',
      name: 'foto-quadro.jpg',
      size: '1.2 MB',
      url: '#',
      type: 'image/jpeg',
    },
  ],
  status: 'pending',
});

// Proposal form
const proposalForm = ref({
  value: '',
  executionDays: '',
  scope: '',
  notes: '',
  attachments: [] as File[],
  attachmentUrls: [] as string[],
});

// Milestones
const milestones = ref<Milestone[]>([]);
const milestoneForm = ref({
  title: '',
  description: '',
  deadline: '',
  value: '',
});
const showAddMilestone = ref(false);

// Messages
const messages = ref<Message[]>([
  {
    id: '1',
    sender: 'client',
    senderName: 'João Silva',
    message: 'Olá, gostaria de mais informações sobre a disponibilidade para início do trabalho.',
    timestamp: '2026-11-05T14:20:00',
  },
]);
const newMessage = ref('');
const messageAttachments = ref<File[]>([]);

// Audit log
const auditLog = ref<AuditLog[]>([
  {
    id: '1',
    action: 'Solicitação criada',
    user: 'João Silva (Cliente)',
    timestamp: '2026-11-05T10:30:00',
    details: 'Cliente criou a solicitação de orçamento',
  },
  {
    id: '2',
    action: 'Anexo adicionado',
    user: 'João Silva (Cliente)',
    timestamp: '2026-11-05T10:35:00',
    details: 'planta-casa.pdf',
  },
  {
    id: '3',
    action: 'Mensagem enviada',
    user: 'João Silva (Cliente)',
    timestamp: '2026-11-05T14:20:00',
    details: 'Cliente enviou uma mensagem',
  },
]);

const clientMessagesCount = computed(() =>
  messages.value.filter((m) => m.sender === 'client').length
);

// Validation
const validateProposal = () => {
  const errors: string[] = [];

  if (!proposalForm.value.value) errors.push('Valor da proposta é obrigatório');
  if (!proposalForm.value.executionDays) errors.push('Prazo de execução é obrigatório');
  if (!proposalForm.value.scope) errors.push('Escopo do trabalho é obrigatório');

  const value = parseFloat(proposalForm.value.value.replace(/[^\d,]/g, '').replace(',', '.'));
  if (isNaN(value) || value <= 0) {
    errors.push('Valor da proposta deve ser um número válido maior que zero');
  }

  const days = parseInt(proposalForm.value.executionDays);
  if (isNaN(days) || days <= 0) {
    errors.push('Prazo de execução deve ser um número válido maior que zero');
  }

  // Validate milestone values sum
  if (milestones.value.length > 0) {
    const milestonesTotal = milestones.value.reduce((sum, m) => {
      const mValue = parseFloat(m.value.replace(/[^\d,]/g, '').replace(',', '.'));
      return sum + (isNaN(mValue) ? 0 : mValue);
    }, 0);

    if (Math.abs(milestonesTotal - value) > 0.01) {
      errors.push('A soma dos valores dos marcos deve ser igual ao valor total da proposta');
    }
  }

  return errors;
};

const handleAddMilestone = () => {
  if (!milestoneForm.value.title || !milestoneForm.value.deadline || !milestoneForm.value.value) {
    toast.error('Preencha todos os campos obrigatórios do marco');
    return;
  }

  const newMilestone: Milestone = {
    id: Date.now().toString(),
    ...milestoneForm.value,
  };

  milestones.value = [...milestones.value, newMilestone];
  milestoneForm.value = { title: '', description: '', deadline: '', value: '' };
  showAddMilestone.value = false;
  toast.success('Marco adicionado com sucesso');
};

const handleRemoveMilestone = (id: string) => {
  milestones.value = milestones.value.filter((m) => m.id !== id);
  toast.success('Marco removido');
};

const handleAttachmentUpload = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (!target.files) return;

  const validFiles: File[] = [];
  const maxSize = 10 * 1024 * 1024; // 10MB
  const allowedTypes = [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  ];

  Array.from(target.files).forEach((file) => {
    if (file.size > maxSize) {
      toast.error(`Arquivo ${file.name} excede o tamanho máximo de 10MB`);
      return;
    }

    if (!allowedTypes.includes(file.type)) {
      toast.error(`Tipo de arquivo ${file.name} não permitido`);
      return;
    }

    validFiles.push(file);
  });

  if (validFiles.length > 0) {
    const newUrls = validFiles.map((file) => URL.createObjectURL(file));
    proposalForm.value = {
      ...proposalForm.value,
      attachments: [...proposalForm.value.attachments, ...validFiles],
      attachmentUrls: [...proposalForm.value.attachmentUrls, ...newUrls],
    };
  }
};

const handleRemoveAttachment = (index: number) => {
  proposalForm.value = {
    ...proposalForm.value,
    attachments: proposalForm.value.attachments.filter((_, i) => i !== index),
    attachmentUrls: proposalForm.value.attachmentUrls.filter((_, i) => i !== index),
  };
};

const handleSendMessage = () => {
  if (!newMessage.value.trim() && messageAttachments.value.length === 0) {
    toast.error('Digite uma mensagem ou adicione um anexo');
    return;
  }

  const message: Message = {
    id: Date.now().toString(),
    sender: 'freelancer',
    senderName: 'Você',
    message: newMessage.value,
    timestamp: new Date().toISOString(),
    attachments: messageAttachments.value.map((file, idx) => ({
      id: `msg-att-${Date.now()}-${idx}`,
      name: file.name,
      size: `${(file.size / 1024).toFixed(1)} KB`,
      url: URL.createObjectURL(file),
      type: file.type,
    })),
  };

  messages.value = [...messages.value, message];
  newMessage.value = '';
  messageAttachments.value = [];
  toast.success('Mensagem enviada');
};

const handlePrepareProposal = () => {
  const errors = validateProposal();

  if (errors.length > 0) {
    toast.error(errors[0]);
    return;
  }

  showSendProposalDialog.value = false;
  showConfirmDialog.value = true;
};

const handleConfirmSendProposal = () => {
  toast.success('Proposta enviada com sucesso!');
  showConfirmDialog.value = false;
  showSendProposalDialog.value = false;

  // Reset form
  proposalForm.value = {
    value: '',
    executionDays: '',
    scope: '',
    notes: '',
    attachments: [],
    attachmentUrls: [],
  };
  milestones.value = [];

  // Navigate back
  setTimeout(() => {
    props.onNavigate('freelancer-proposals');
  }, 1500);
};

const formatCurrency = (value: string) => {
  const numbers = value.replace(/\D/g, '');
  const formatted = (parseFloat(numbers) / 100).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return `R$ ${formatted}`;
};

const handleCurrencyInput = (value: string) => {
  const numbers = value.replace(/\D/g, '');
  if (numbers) {
    return formatCurrency(numbers);
  }
  return '';
};

const handleMessageAttachmentChange = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files) {
    messageAttachments.value = [...messageAttachments.value, ...Array.from(target.files)];
  }
};

const getUrgencyVariant = (urgency: string) => {
  if (urgency === 'Alta') return 'destructive';
  if (urgency === 'Média') return 'default';
  return 'secondary';
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-4">
        <Button
          @click="props.onNavigate('freelancer-proposals')"
          variant="ghost"
          size="icon"
        >
          <ArrowLeft class="h-5 w-5" />
        </Button>
        <div>
          <h1 class="text-[#263238] mb-1">{{ request.serviceTitle }}</h1>
          <div class="flex items-center gap-3 text-sm text-[#435574]">
            <Badge variant="secondary">{{ request.category }}</Badge>
            <span class="flex items-center gap-1">
              <Calendar class="h-4 w-4" />
              Criado em {{ new Date(request.createdAt).toLocaleDateString('pt-BR') }}
            </span>
          </div>
        </div>
      </div>
      <Button
        @click="showSendProposalDialog = true"
        class="bg-[#286AD8] hover:bg-[#435574]"
      >
        <Send class="h-4 w-4 mr-2" />
        Enviar Proposta
      </Button>
    </div>

    <!-- Client Info -->
    <Card class="p-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <img
            :src="request.client.avatar"
            :alt="request.client.name"
            class="w-16 h-16 rounded-full"
          />
          <div>
            <h3 class="text-[#263238] mb-1">{{ request.client.name }}</h3>
            <div class="flex items-center gap-4 text-sm text-[#435574]">
              <span class="flex items-center gap-1">
                <MapPin class="h-4 w-4" />
                {{ request.client.location }}
              </span>
            </div>
          </div>
        </div>
        <Button variant="outline" @click="activeTab = 'messages'">
          <MessageSquare class="h-4 w-4 mr-2" />
          Enviar Mensagem
        </Button>
      </div>
    </Card>

    <!-- Tabs -->
    <Tabs :model-value="activeTab" @update:model-value="(val) => activeTab = val as string">
      <TabsList class="grid w-full grid-cols-4">
        <TabsTrigger value="details">Detalhes</TabsTrigger>
        <TabsTrigger value="messages">
          Mensagens
          <Badge v-if="clientMessagesCount > 0" class="ml-2 bg-red-500">
            {{ clientMessagesCount }}
          </Badge>
        </TabsTrigger>
        <TabsTrigger value="attachments">
          Anexos ({{ request.attachments.length }})
        </TabsTrigger>
        <TabsTrigger value="history">Histórico</TabsTrigger>
      </TabsList>

      <!-- Details Tab -->
      <TabsContent value="details" class="space-y-6">
        <!-- Request Info -->
        <Card class="p-6">
          <h3 class="text-[#263238] mb-4">Informações da Solicitação</h3>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div>
              <Label class="text-sm text-[#435574]">Data Desejada</Label>
              <p class="text-[#263238] mt-1">
                {{ new Date(request.desiredDate).toLocaleDateString('pt-BR') }}
              </p>
            </div>
            <div>
              <Label class="text-sm text-[#435574]">Orçamento Estimado</Label>
              <p class="text-[#263238] mt-1">{{ request.budget }}</p>
            </div>
            <div>
              <Label class="text-sm text-[#435574]">Urgência</Label>
              <p class="text-[#263238] mt-1">
                <Badge :variant="getUrgencyVariant(request.urgency)">
                  {{ request.urgency }}
                </Badge>
              </p>
            </div>
          </div>

          <div class="mb-6">
            <Label class="text-sm text-[#435574]">Endereço do Serviço</Label>
            <p class="text-[#263238] mt-1 flex items-start gap-2">
              <MapPin class="h-4 w-4 mt-1 flex-shrink-0" />
              {{ request.address }}
            </p>
          </div>

          <div>
            <Label class="text-sm text-[#435574] mb-2 block">
              Descrição Detalhada
            </Label>
            <div class="bg-gray-50 rounded-lg p-4 whitespace-pre-wrap text-[#263238]">
              {{ request.description }}
            </div>
          </div>
        </Card>
      </TabsContent>

      <!-- Messages Tab -->
      <TabsContent value="messages" class="space-y-4">
        <Card class="p-6">
          <div class="space-y-4 mb-6 max-h-[500px] overflow-y-auto">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="`flex gap-3 ${msg.sender === 'freelancer' ? 'flex-row-reverse' : ''}`"
            >
              <div :class="`flex-1 ${msg.sender === 'freelancer' ? 'text-right' : ''}`">
                <div
                  :class="`inline-block max-w-[70%] ${
                    msg.sender === 'freelancer'
                      ? 'bg-[#286AD8] text-white'
                      : 'bg-gray-100 text-[#263238]'
                  } rounded-lg p-4`"
                >
                  <p class="mb-1">{{ msg.message }}</p>
                  <div v-if="msg.attachments && msg.attachments.length > 0" class="mt-3 space-y-2">
                    <div
                      v-for="att in msg.attachments"
                      :key="att.id"
                      :class="`flex items-center gap-2 p-2 rounded ${
                        msg.sender === 'freelancer' ? 'bg-[#435574]' : 'bg-white'
                      }`"
                    >
                      <Paperclip class="h-4 w-4" />
                      <span class="text-sm flex-1">{{ att.name }}</span>
                      <span class="text-xs opacity-70">{{ att.size }}</span>
                    </div>
                  </div>
                </div>
                <p class="text-xs text-[#435574] mt-1">
                  {{ msg.senderName }} •
                  {{ new Date(msg.timestamp).toLocaleString('pt-BR', {
                    day: '2-digit',
                    month: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                  }) }}
                </p>
              </div>
            </div>
          </div>

          <!-- Message Input -->
          <div class="space-y-3 border-t pt-4">
            <div v-if="messageAttachments.length > 0" class="flex flex-wrap gap-2">
              <Badge
                v-for="(file, idx) in messageAttachments"
                :key="idx"
                variant="secondary"
                class="pl-3 pr-1"
              >
                {{ file.name }}
                <button
                  @click="messageAttachments = messageAttachments.filter((_, i) => i !== idx)"
                  class="ml-2 hover:bg-gray-300 rounded-full p-0.5"
                >
                  <X class="h-3 w-3" />
                </button>
              </Badge>
            </div>

            <div class="flex gap-2">
              <Textarea
                :model-value="newMessage"
                @update:model-value="(val) => newMessage = val as string"
                placeholder="Digite sua mensagem..."
                :rows="3"
                class="flex-1"
              />
            </div>

            <div class="flex gap-2">
              <input
                type="file"
                multiple
                id="message-attachment"
                class="hidden"
                @change="handleMessageAttachmentChange"
              />
              <Button
                @click="document.getElementById('message-attachment')?.click()"
                variant="outline"
              >
                <Paperclip class="h-4 w-4 mr-2" />
                Anexar
              </Button>
              <Button
                @click="handleSendMessage"
                class="bg-[#286AD8] hover:bg-[#435574] flex-1"
              >
                <Send class="h-4 w-4 mr-2" />
                Enviar Mensagem
              </Button>
            </div>
          </div>
        </Card>
      </TabsContent>

      <!-- Attachments Tab -->
      <TabsContent value="attachments" class="space-y-4">
        <Card class="p-6">
          <h3 class="text-[#263238] mb-4">Anexos do Cliente</h3>
          <div class="space-y-3">
            <div
              v-for="att in request.attachments"
              :key="att.id"
              class="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                  <FileText class="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p class="text-[#263238]">{{ att.name }}</p>
                  <p class="text-sm text-[#435574]">{{ att.size }}</p>
                </div>
              </div>
              <div class="flex gap-2">
                <Button size="sm" variant="outline">
                  <Eye class="h-4 w-4 mr-2" />
                  Visualizar
                </Button>
                <Button size="sm" variant="outline">
                  <Download class="h-4 w-4 mr-2" />
                  Baixar
                </Button>
              </div>
            </div>
          </div>
        </Card>
      </TabsContent>

      <!-- History Tab -->
      <TabsContent value="history" class="space-y-4">
        <Card class="p-6">
          <h3 class="text-[#263238] mb-4">Histórico de Ações</h3>
          <div class="space-y-4">
            <div v-for="(log, index) in auditLog" :key="log.id" class="flex gap-4">
              <div class="flex flex-col items-center">
                <div class="w-3 h-3 bg-[#286AD8] rounded-full" />
                <div
                  v-if="index < auditLog.length - 1"
                  class="w-0.5 h-full bg-gray-300 my-1"
                />
              </div>
              <div class="flex-1 pb-6">
                <div class="flex items-start justify-between mb-1">
                  <p class="text-[#263238]">{{ log.action }}</p>
                  <span class="text-xs text-[#435574]">
                    {{ new Date(log.timestamp).toLocaleString('pt-BR', {
                      day: '2-digit',
                      month: '2-digit',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    }) }}
                  </span>
                </div>
                <p class="text-sm text-[#435574]">{{ log.user }}</p>
                <p v-if="log.details" class="text-sm text-[#435574] mt-1">
                  {{ log.details }}
                </p>
              </div>
            </div>
          </div>
        </Card>
      </TabsContent>
    </Tabs>

    <!-- Send Proposal Dialog -->
    <Dialog :open="showSendProposalDialog" @update:open="(val) => showSendProposalDialog = val">
      <DialogContent class="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Enviar Proposta</DialogTitle>
          <DialogDescription>
            Preencha os detalhes da sua proposta para este serviço
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-6">
          <!-- Value and Deadline -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="value">Valor Total da Proposta *</Label>
              <Input
                id="value"
                :model-value="proposalForm.value"
                @update:model-value="(val) => proposalForm.value = handleCurrencyInput(val as string)"
                placeholder="R$ 0,00"
              />
            </div>

            <div class="space-y-2">
              <Label for="executionDays">Prazo de Execução (dias) *</Label>
              <Input
                id="executionDays"
                type="number"
                :model-value="proposalForm.executionDays"
                @update:model-value="(val) => proposalForm.executionDays = val as string"
                placeholder="Ex: 15"
              />
            </div>
          </div>

          <!-- Scope -->
          <div class="space-y-2">
            <Label for="scope">Escopo do Trabalho *</Label>
            <Textarea
              id="scope"
              :model-value="proposalForm.scope"
              @update:model-value="(val) => proposalForm.scope = val as string"
              placeholder="Descreva detalhadamente o que será realizado..."
              :rows="6"
            />
          </div>

          <!-- Milestones -->
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <Label>Marcos de Entrega (opcional)</Label>
              <Button
                @click="showAddMilestone = !showAddMilestone"
                variant="outline"
                size="sm"
              >
                <Plus class="h-4 w-4 mr-2" />
                Adicionar Marco
              </Button>
            </div>

            <Card v-if="showAddMilestone" class="p-4 bg-gray-50">
              <div class="space-y-3">
                <Input
                  :model-value="milestoneForm.title"
                  @update:model-value="(val) => milestoneForm.title = val as string"
                  placeholder="Título do marco"
                />
                <Textarea
                  :model-value="milestoneForm.description"
                  @update:model-value="(val) => milestoneForm.description = val as string"
                  placeholder="Descrição do marco"
                  :rows="2"
                />
                <div class="grid grid-cols-2 gap-3">
                  <Input
                    type="date"
                    :model-value="milestoneForm.deadline"
                    @update:model-value="(val) => milestoneForm.deadline = val as string"
                  />
                  <Input
                    :model-value="milestoneForm.value"
                    @update:model-value="(val) => milestoneForm.value = handleCurrencyInput(val as string)"
                    placeholder="Valor do marco"
                  />
                </div>
                <div class="flex gap-2">
                  <Button @click="handleAddMilestone" size="sm" class="flex-1">
                    Adicionar
                  </Button>
                  <Button
                    @click="showAddMilestone = false"
                    variant="outline"
                    size="sm"
                  >
                    Cancelar
                  </Button>
                </div>
              </div>
            </Card>

            <div v-if="milestones.length > 0" class="space-y-2">
              <Card
                v-for="(milestone, index) in milestones"
                :key="milestone.id"
                class="p-4"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <Milestone class="h-4 w-4 text-[#286AD8]" />
                      <h4 class="text-[#263238]">
                        {{ index + 1 }}. {{ milestone.title }}
                      </h4>
                    </div>
                    <p v-if="milestone.description" class="text-sm text-[#435574] mb-2">
                      {{ milestone.description }}
                    </p>
                    <div class="flex items-center gap-4 text-sm text-[#435574]">
                      <span class="flex items-center gap-1">
                        <Calendar class="h-4 w-4" />
                        {{ new Date(milestone.deadline).toLocaleDateString('pt-BR') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <DollarSign class="h-4 w-4" />
                        {{ milestone.value }}
                      </span>
                    </div>
                  </div>
                  <Button
                    @click="handleRemoveMilestone(milestone.id)"
                    variant="ghost"
                    size="icon"
                    class="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </Card>
            </div>
          </div>

          <!-- Notes -->
          <div class="space-y-2">
            <Label for="notes">Observações (opcional)</Label>
            <Textarea
              id="notes"
              :model-value="proposalForm.notes"
              @update:model-value="(val) => proposalForm.notes = val as string"
              placeholder="Informações adicionais, condições especiais, etc."
              :rows="3"
            />
          </div>

          <!-- Attachments -->
          <div class="space-y-2">
            <Label>Anexos (opcional)</Label>
            <div class="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-[#286AD8] transition-colors cursor-pointer">
              <Upload class="h-8 w-8 text-gray-400 mx-auto mb-2" />
              <p class="text-sm text-[#435574] mb-2">
                Adicione documentos, certificados ou imagens
              </p>
              <p class="text-xs text-[#435574] mb-3">
                PDF, JPG, PNG, DOC até 10MB cada
              </p>
              <input
                type="file"
                multiple
                accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
                @change="handleAttachmentUpload"
                class="hidden"
                id="proposal-attachment"
              />
              <Button
                @click="document.getElementById('proposal-attachment')?.click()"
                size="sm"
                variant="outline"
              >
                Selecionar Arquivos
              </Button>
            </div>

            <div v-if="proposalForm.attachments.length > 0" class="space-y-2">
              <div
                v-for="(file, index) in proposalForm.attachments"
                :key="index"
                class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
              >
                <div class="flex items-center gap-2">
                  <FileText class="h-4 w-4 text-[#435574]" />
                  <span class="text-sm text-[#263238]">{{ file.name }}</span>
                  <span class="text-xs text-[#435574]">
                    ({{ (file.size / 1024).toFixed(1) }} KB)
                  </span>
                </div>
                <button
                  @click="handleRemoveAttachment(index)"
                  class="text-red-600 hover:text-red-700"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showSendProposalDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handlePrepareProposal" class="bg-[#286AD8] hover:bg-[#435574]">
            <Send class="h-4 w-4 mr-2" />
            Enviar Proposta
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Confirmation Dialog -->
    <AlertDialog :open="showConfirmDialog" @update:open="(val) => showConfirmDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmar Envio da Proposta</AlertDialogTitle>
          <AlertDialogDescription>
            Você está prestes a enviar uma proposta de
            <strong>{{ proposalForm.value }}</strong> com prazo de
            <strong>{{ proposalForm.executionDays }} dias</strong>.
            <template v-if="milestones.length > 0">
              <br />
              <br />
              A proposta inclui <strong>{{ milestones.length }}</strong>
              {{ milestones.length === 1 ? 'marco' : 'marcos' }} de entrega.
            </template>
            <br />
            <br />
            Deseja confirmar o envio?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Revisar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleConfirmSendProposal"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            Confirmar Envio
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

