<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  ArrowLeft,
  Download,
  FileText,
  Image as ImageIcon,
  Film,
  CheckCircle2,
  AlertCircle,
  Eye,
  History,
  Headset,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';
import DeliveryAdjustmentModal from './DeliveryAdjustmentModal.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import SupportRequestModal from '../support/SupportRequestModal.vue';
import { toast } from '../../utils/toast';

const win = window;

interface DeliveryFile {
  id: string;
  name: string;
  size: string;
  type: 'document' | 'image' | 'video' | 'other';
  url: string;
  thumbnail?: string;
}

interface Props {
  serviceId: string;
  serviceName: string;
  freelancerName: string;
  freelancerAvatar?: string;
  deliveryDescription: string;
  deliveryDate: string;
  files: DeliveryFile[];
  adjustmentAlreadyUsed?: boolean;
  isLoading?: boolean;
  onBack: () => void;
  onApprove: () => void;
  onRequestAdjustments: (reason: string) => boolean | void | Promise<boolean | void>;
  onGoToChat?: () => void;
}

const props = defineProps<Props>();

const showApprovalDialog = ref(false);
const showAdjustmentsModal = ref(false);
const showSupportModal = ref(false);
const activeTab = ref<'description' | 'history'>('description');
const previewFile = ref<DeliveryFile | null>(null);
const previewPdfUrl = ref<string | null>(null);
const previewLoading = ref(false);

const deliveryDescriptionDisplay = computed(() => {
  const description = props.deliveryDescription?.trim();
  if (description) return description;

  const firstName = props.freelancerName?.trim().split(' ')[0] || 'O freelancer';
  return `${firstName} não adicionou uma descrição de entrega.`;
});

const getFileIcon = (type: DeliveryFile['type']) => {
  switch (type) {
    case 'image':
      return ImageIcon;
    case 'video':
      return Film;
    case 'document':
      return FileText;
    default:
      return FileText;
  }
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const handleApprove = () => {
  showApprovalDialog.value = false;
  props.onApprove();
};

const canPreview = (file: DeliveryFile) => {
  return file.type === 'image' || file.type === 'video' || file.type === 'document';
};

const isPdf = (file: DeliveryFile) => {
  const target = (file.url || file.name || '').toLowerCase().split('?')[0];
  return target.endsWith('.pdf');
};

const closePreview = () => {
  if (previewPdfUrl.value) {
    window.URL.revokeObjectURL(previewPdfUrl.value);
    previewPdfUrl.value = null;
  }
  previewLoading.value = false;
  previewFile.value = null;
};

const openPreview = async (file: DeliveryFile) => {
  closePreview();
  previewFile.value = file;

  if (!isPdf(file)) return;

  // Busca o PDF como blob e exibe via object URL, garantindo a renderização inline
  // mesmo quando o storage serve o arquivo com Content-Disposition: attachment.
  previewLoading.value = true;
  try {
    const response = await fetch(file.url);
    const blob = await response.blob();
    const pdfBlob = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });

    // Evita aplicar o resultado se o usuário já fechou ou trocou de arquivo.
    if (previewFile.value?.id === file.id) {
      previewPdfUrl.value = window.URL.createObjectURL(pdfBlob);
    }
  } catch {
    // Mantém previewPdfUrl nulo → exibe mensagem de fallback.
  } finally {
    if (previewFile.value?.id === file.id) {
      previewLoading.value = false;
    }
  }
};

const downloadFile = async (url: string, fileName: string) => {
  try {
    const response = await fetch(url);
    const blob = await response.blob();
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(blobUrl);
  } catch {
    // Fallback: abrir em nova aba
    win.open(url, '_blank');
  }
};

const handleDownloadFile = async (url: string, fileName: string) => {
  toast.info(`Iniciando download de ${fileName}...`);
  await downloadFile(url, fileName);
};

const handleDownloadAll = async () => {
  if (!props.files || props.files.length === 0) {
    toast.warning('Não há arquivos para baixar.');
    return;
  }

  toast.success('Download de todos os arquivos iniciado');

  // Baixa os arquivos sequencialmente (via blob) para não esbarrar no bloqueio
  // de múltiplos popups que ocorre com window.open em loop.
  for (const file of props.files) {
    await downloadFile(file.url, file.name);
  }
};
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center gap-4">
      <Button
        @click="props.onBack"
        variant="ghost"
        size="icon"
        class="hover:bg-gray-100"
      >
        <ArrowLeft class="h-5 w-5" />
      </Button>
      <div class="flex-1">
        <h1 class="text-[#263238]">Entrega Final</h1>
        <p class="text-[#435574]">{{ props.serviceName }}</p>
      </div>
      <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
        Aguardando Aprovação
      </Badge>
    </div>

    <!-- Freelancer Info -->
    <Card class="p-6">
      <div class="flex items-center gap-3 mb-4">
        <Avatar class="h-12 w-12">
          <AvatarImage :src="props.freelancerAvatar" :alt="props.freelancerName" />
          <AvatarFallback class="bg-[#286AD8] text-white">
            {{ getInitials(props.freelancerName) }}
          </AvatarFallback>
        </Avatar>
        <div>
          <p class="text-[#263238]">{{ props.freelancerName }}</p>
          <p class="text-sm text-[#435574]">Entregue em {{ props.deliveryDate }}</p>
        </div>
      </div>

      <Separator class="my-4" />

      <!-- Tabs: Descrição da Entrega / Histórico -->
      <div class="flex gap-2 border-b border-gray-200 mb-4">
        <button
          @click="activeTab = 'description'"
          :class="`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'description'
              ? 'border-[#286AD8] text-[#286AD8]'
              : 'border-transparent text-[#435574] hover:text-[#263238]'
          }`"
        >
          <FileText class="h-4 w-4 mr-1 inline" />
          Descrição da Entrega
        </button>
        <button
          @click="activeTab = 'history'"
          :class="`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'history'
              ? 'border-[#286AD8] text-[#286AD8]'
              : 'border-transparent text-[#435574] hover:text-[#263238]'
          }`"
        >
          <History class="h-4 w-4 mr-1 inline" />
          Histórico
        </button>
      </div>

      <!-- Delivery Description -->
      <div v-if="activeTab === 'description'" class="space-y-2">
        <p class="text-[#435574] whitespace-pre-wrap">{{ deliveryDescriptionDisplay }}</p>
      </div>

      <!-- History -->
      <ServiceRequestTimeline
        v-else
        :request-id="props.serviceId"
        title="Histórico da Solicitação"
      />
    </Card>

    <!-- Files Section -->
    <Card class="p-6">
      <h3 class="text-[#263238] mb-4">Arquivos Entregues ({{ props.files.length }})</h3>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div
          v-for="file in props.files"
          :key="file.id"
          class="border border-gray-200 rounded-lg p-4 hover:border-[#286AD8] transition-colors"
        >
          <!-- File Info -->
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0 mt-1">
              <component :is="getFileIcon(file.type)" class="h-5 w-5 text-blue-600" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-[#263238] truncate mb-1">{{ file.name }}</p>
              <p class="text-xs text-[#435574]">{{ file.size }}</p>
            </div>
          </div>

          <!-- File Actions -->
          <div class="flex gap-2 mt-3">
            <Button
              v-if="canPreview(file)"
              @click="openPreview(file)"
              variant="outline"
              size="sm"
              class="flex-1"
            >
              <Eye class="h-3 w-3 mr-1" />
              Visualizar
            </Button>
            <Button
              @click="handleDownloadFile(file.url, file.name)"
              variant="outline"
              size="sm"
              class="flex-1"
            >
              <Download class="h-3 w-3 mr-1" />
              Download
            </Button>
          </div>
        </div>
      </div>

      <div class="mt-4 pt-4 border-t">
        <Button
          @click="handleDownloadAll"
          variant="outline"
          class="w-full text-primary border-primary"
        >
          <Download class="h-4 w-4 mr-2" />
          Fazer download de todos os Arquivos
        </Button>
      </div>
    </Card>

    <!-- Action Buttons -->
    <div class="flex gap-4 flex-wrap">
      <Button
        @click="showSupportModal = true"
        :disabled="props.isLoading"
        variant="outline"
        class="flex-1"
        size="lg"
      >
        <Headset class="h-4 w-4 mr-2" />
        Reportar Problema
      </Button>
      <Button
        @click="showAdjustmentsModal = true"
        :disabled="props.isLoading"
        variant="warning"
        class="flex-1"
        size="lg"
      >
        <AlertCircle class="h-4 w-4 mr-2" />
        Solicitar Ajustes
      </Button>
      <Button
        @click="showApprovalDialog = true"
        :disabled="props.isLoading"
        variant="success"
        class="flex-1"
        size="lg"
      >
        <CheckCircle2 class="h-4 w-4 mr-2" />
        Aprovar Entrega
      </Button>
    </div>

    <!-- Important Notice -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
      <p class="text-sm text-[#435574]">
        <strong class="text-[#263238]">Importante:</strong> Ao aprovar a entrega, o serviço será
        marcado como concluído e o pagamento será liberado para o freelancer. Certifique-se de que
        todos os arquivos estão corretos antes de aprovar.
      </p>
    </div>

    <!-- Approval Confirmation Dialog -->
    <AlertDialog v-model:open="showApprovalDialog">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Aprovar Entrega Final</AlertDialogTitle>
          <AlertDialogDescription>
            Você confirma que revisou todos os arquivos e está satisfeito com a entrega? Esta ação
            não poderá ser desfeita e o pagamento será liberado para o freelancer.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showApprovalDialog = false">Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleApprove" class="bg-success hover:bg-success/90">
            Sim, Aprovar Entrega
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <DeliveryAdjustmentModal
      v-model:open="showAdjustmentsModal"
      :service-name="props.serviceName"
      :adjustment-already-used="props.adjustmentAlreadyUsed"
      :is-loading="props.isLoading"
      :on-submit="props.onRequestAdjustments"
      :on-go-to-chat="props.onGoToChat"
    />

    <SupportRequestModal
      :open="showSupportModal"
      :default-category="'SERVICE_ISSUE'"
      :on-close="() => (showSupportModal = false)"
    />

    <!-- File Preview Modal -->
    <Teleport to="body">
      <div
        v-if="previewFile"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4"
        @click.self="closePreview"
      >
        <Card class="max-w-4xl w-full max-h-[90vh] overflow-auto">
          <div class="p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-[#263238]">{{ previewFile.name }}</h3>
              <Button @click="closePreview" variant="ghost" size="sm">
                Fechar
              </Button>
            </div>
            <div class="bg-gray-100 rounded-lg p-4 flex items-center justify-center min-h-[400px]">
              <img
                v-if="previewFile.type === 'image'"
                :src="previewFile.thumbnail || previewFile.url"
                :alt="previewFile.name"
                class="max-w-full max-h-[600px] object-contain"
              />
              <video v-else-if="previewFile.type === 'video'" controls class="max-w-full max-h-[600px]">
                <source :src="previewFile.url" />
              </video>
              <template v-else-if="isPdf(previewFile)">
                <iframe
                  v-if="previewPdfUrl"
                  :src="previewPdfUrl"
                  :title="previewFile.name"
                  class="w-full h-[70vh] rounded border-0 bg-white"
                />
                <p v-else-if="previewLoading" class="text-[#435574]">Carregando preview...</p>
                <p v-else class="text-[#435574]">
                  Não foi possível carregar o preview. Faça o download para visualizar.
                </p>
              </template>
              <p v-else class="text-[#435574]">
                Preview de documento não disponível. Faça o download para visualizar.
              </p>
            </div>
          </div>
        </Card>
      </div>
    </Teleport>
  </div>
</template>

