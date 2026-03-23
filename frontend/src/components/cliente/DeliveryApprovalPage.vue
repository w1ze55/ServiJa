<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  ArrowLeft,
  Download,
  FileText,
  Image as ImageIcon,
  Film,
  CheckCircle2,
  AlertCircle,
  Eye,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Textarea from '../ui/textarea.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
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
  onRequestAdjustments: (reason: string) => void;
  onGoToChat?: () => void;
}

const props = defineProps<Props>();

const showApprovalDialog = ref(false);
const showAdjustmentsForm = ref(false);
const showAdjustmentConfirmDialog = ref(false);
const adjustmentsReason = ref('');
const previewFile = ref<DeliveryFile | null>(null);
const adjustmentUsed = ref(props.adjustmentAlreadyUsed ?? false);

watch(() => props.adjustmentAlreadyUsed, (newVal) => {
  if (newVal) {
    adjustmentUsed.value = true;
  }
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

const handleRequestAdjustments = () => {
  if (adjustmentsReason.value.trim()) {
    showAdjustmentConfirmDialog.value = false;
    props.onRequestAdjustments(adjustmentsReason.value);
    adjustmentsReason.value = '';
    showAdjustmentsForm.value = false;
    adjustmentUsed.value = true;
  }
};

const canPreview = (file: DeliveryFile) => {
  return file.type === 'image' || file.type === 'video' || file.type === 'document';
};

const handleDownloadAll = () => {
  if (!props.files || props.files.length === 0) {
    toast.warning('Não há arquivos para baixar.');
    return;
  }
  
  toast.success('Download de todos os arquivos iniciado');
  
  props.files.forEach(file => {
    win.open(file.url, '_blank');
  });
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

      <!-- Delivery Description -->
      <div class="space-y-2">
        <h3 class="text-[#263238]">Descrição da Entrega</h3>
        <p class="text-[#435574] whitespace-pre-wrap">{{ props.deliveryDescription }}</p>
      </div>
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
          <!-- File Preview -->
          <div v-if="file.thumbnail" class="mb-3 bg-gray-100 rounded-lg overflow-hidden aspect-video flex items-center justify-center">
            <img :src="file.thumbnail" :alt="file.name" class="w-full h-full object-cover" />
          </div>

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
              @click="previewFile = file"
              variant="outline"
              size="sm"
              class="flex-1"
            >
              <Eye class="h-3 w-3 mr-1" />
              Visualizar
            </Button>
            <Button
              @click="() => win.open(file.url, '_blank')"
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
          class="w-full border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
        >
          <Download class="h-4 w-4 mr-2" />
          Baixar Todos os Arquivos
        </Button>
      </div>
    </Card>

    <!-- Request Adjustments Form -->
    <Card v-if="showAdjustmentsForm" class="p-6 border-yellow-200 bg-yellow-50">
      <div class="flex items-start gap-3 mb-4">
        <AlertCircle class="h-5 w-5 text-yellow-600 mt-0.5" />
        <div class="flex-1">
          <h3 class="text-[#263238] mb-2">Solicitar Ajustes</h3>
          <p class="text-sm text-[#435574] mb-2">
            Descreva detalhadamente os ajustes necessários para que o freelancer possa realizar as
            correções.
          </p>
          <p class="text-xs text-yellow-700 font-medium mb-4 flex items-center gap-1">
            <AlertCircle class="h-3.5 w-3.5" />
            Atenção: você só pode solicitar ajustes <strong>uma única vez</strong>. Após o envio,
            quaisquer outras negociações deverão ser feitas pelo chat.
          </p>
          <Textarea
            placeholder="Descreva os ajustes necessários..."
            v-model="adjustmentsReason"
            :rows="4"
            class="mb-4"
          />
          <div class="flex gap-2">
            <Button
              @click="showAdjustmentConfirmDialog = true"
              :disabled="!adjustmentsReason.trim()"
              class="bg-yellow-600 hover:bg-yellow-700"
            >
              Enviar Solicitação
            </Button>
            <Button
              @click="
                showAdjustmentsForm = false;
                adjustmentsReason = '';
              "
              variant="outline"
            >
              Cancelar
            </Button>
          </div>
        </div>
      </div>
    </Card>

    <!-- Action Buttons -->
    <div v-if="!showAdjustmentsForm" class="flex gap-4">
      <!-- Adjustment already used: show chat CTA instead -->
      <template v-if="adjustmentUsed">
        <div class="flex-1 rounded-lg border border-yellow-300 bg-yellow-50 p-4 flex items-start gap-3">
          <AlertCircle class="h-5 w-5 text-yellow-600 mt-0.5 flex-shrink-0" />
          <div>
            <p class="text-sm font-medium text-[#263238] mb-1">Solicitação de ajuste já enviada</p>
            <p class="text-xs text-[#435574] mb-3">
              Você já utilizou sua única solicitação de ajuste. Para negociar novas correções ou
              esclarecimentos, entre em contato com o freelancer pelo chat.
            </p>
            <Button
              v-if="props.onGoToChat"
              @click="props.onGoToChat"
              size="sm"
              variant="outline"
              class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
            >
              Ir para o Chat
            </Button>
          </div>
        </div>
      </template>
      <template v-else>
        <Button
          @click="showAdjustmentsForm = true"
          :disabled="props.isLoading"
          variant="outline"
          class="flex-1 border-yellow-600 text-yellow-600 hover:bg-yellow-50"
          size="lg"
        >
          <AlertCircle class="h-4 w-4 mr-2" />
          Solicitar Ajustes
        </Button>
      </template>
      <Button
        @click="showApprovalDialog = true"
        :disabled="props.isLoading"
        class="flex-1 bg-green-600 hover:bg-green-700"
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
          <AlertDialogAction @click="handleApprove" class="bg-green-600 hover:bg-green-700">
            Sim, Aprovar Entrega
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Adjustment Confirmation Dialog -->
    <AlertDialog v-model:open="showAdjustmentConfirmDialog">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmar Solicitação de Ajuste</AlertDialogTitle>
          <AlertDialogDescription class="space-y-3">
            <span class="block">
              Você tem direito a apenas <strong>uma única solicitação de ajuste</strong>.
              Após o envio, não será possível solicitar novos ajustes por aqui.
            </span>
            <span class="block">
              Caso precise de mais alterações futuramente, você e o freelancer deverão
              resolver diretamente pelo <strong>chat</strong>.
            </span>
            <span class="block font-medium text-yellow-700">
              Deseja realmente enviar esta solicitação?
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showAdjustmentConfirmDialog = false">Voltar e Revisar</AlertDialogCancel>
          <AlertDialogAction @click="handleRequestAdjustments" class="bg-yellow-600 hover:bg-yellow-700">
            Sim, Enviar Solicitação
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- File Preview Modal -->
    <template v-if="previewFile">
      <div
        class="fixed inset-0 bg-black/80 z-50"
        @click="previewFile = null"
      />
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <Card class="max-w-4xl w-full max-h-[90vh] overflow-auto">
          <div class="p-6">
            <div class="flex items-center justify-between mb-4">
              <h3 class="text-[#263238]">{{ previewFile.name }}</h3>
              <Button @click="previewFile = null" variant="ghost" size="sm">
                Fechar
              </Button>
            </div>
            <div class="bg-gray-100 rounded-lg p-4 flex items-center justify-center min-h-[400px]">
              <img
                v-if="previewFile.type === 'image' && previewFile.thumbnail"
                :src="previewFile.thumbnail"
                :alt="previewFile.name"
                class="max-w-full max-h-[600px] object-contain"
              />
              <video v-else-if="previewFile.type === 'video'" controls class="max-w-full max-h-[600px]">
                <source :src="previewFile.url" />
              </video>
              <p v-else-if="previewFile.type === 'document'" class="text-[#435574]">
                Preview de documento não disponível. Faça o download para visualizar.
              </p>
            </div>
          </div>
        </Card>
      </div>
    </template>
  </div>
</template>




