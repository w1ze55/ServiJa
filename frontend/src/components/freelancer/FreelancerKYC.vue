<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Upload,
  FileText,
  CheckCircle,
  XCircle,
  Clock,
  AlertCircle,
  Download,
  Eye,
  Trash2,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Progress from '../ui/progress.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import { toast } from '../../utils/toast';

interface Document {
  id: string;
  type: 'rg' | 'cpf' | 'comprovante_residencia' | 'selfie';
  name: string;
  status: 'pending' | 'analyzing' | 'approved' | 'rejected';
  uploadedAt: string;
  reviewedAt?: string;
  rejectionReason?: string;
  fileSize: string;
  fileUrl?: string;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const documentTypes = [
  { value: 'rg', label: 'RG (Frente e Verso)', required: true },
  { value: 'cpf', label: 'CPF', required: true },
  { value: 'comprovante_residencia', label: 'Comprovante de Residência', required: true },
  { value: 'selfie', label: 'Selfie com Documento', required: true },
];

const documents = ref<Document[]>([
  {
    id: '1',
    type: 'rg',
    name: 'RG_Frente_Verso.pdf',
    status: 'approved',
    uploadedAt: '2026-11-01 10:30',
    reviewedAt: '2026-11-02 14:20',
    fileSize: '2.3 MB',
  },
  {
    id: '2',
    type: 'cpf',
    name: 'CPF.pdf',
    status: 'approved',
    uploadedAt: '2026-11-01 10:35',
    reviewedAt: '2026-11-02 14:22',
    fileSize: '1.1 MB',
  },
  {
    id: '3',
    type: 'comprovante_residencia',
    name: 'Comprovante_Endereco.pdf',
    status: 'rejected',
    uploadedAt: '2026-11-01 10:40',
    reviewedAt: '2026-11-02 14:25',
    rejectionReason: 'Documento com mais de 90 dias. Por favor, envie um comprovante recente.',
    fileSize: '1.8 MB',
  },
  {
    id: '4',
    type: 'selfie',
    name: 'Selfie_Documento.jpg',
    status: 'analyzing',
    uploadedAt: '2026-11-05 09:15',
    fileSize: '3.2 MB',
  },
]);

const documentToDelete = ref<Document | null>(null);

const getVerificationStatus = computed(() => {
  const total = documentTypes.filter((d) => d.required).length;
  const approved = documents.value.filter((d) => d.status === 'approved').length;
  const analyzing = documents.value.filter((d) => d.status === 'analyzing').length;
  const rejected = documents.value.filter((d) => d.status === 'rejected').length;

  if (approved === total) {
    return { status: 'approved', label: 'Verificado', color: 'green', progress: 100 };
  } else if (analyzing > 0) {
    return { status: 'analyzing', label: 'Em Análise', color: 'yellow', progress: (approved / total) * 100 };
  } else if (rejected > 0) {
    return { status: 'rejected', label: 'Ação Necessária', color: 'red', progress: (approved / total) * 100 };
  } else {
    return { status: 'pending', label: 'Pendente', color: 'gray', progress: (approved / total) * 100 };
  }
});

// Status badge removed - using templates directly

const getDocumentLabel = (type: string) => {
  return documentTypes.find((d) => d.value === type)?.label || type;
};

const handleFileUpload = (type: string) => {
  // Simular upload
  toast.success('Documento enviado com sucesso! Aguardando análise.');
  const newDoc: Document = {
    id: Date.now().toString(),
    type: type as any,
    name: `Documento_${type}.pdf`,
    status: 'analyzing',
    uploadedAt: new Date().toLocaleString('pt-BR'),
    fileSize: '2.5 MB',
  };
  documents.value = [...documents.value.filter((d) => d.type !== type), newDoc];
};

const handleReupload = (doc: Document) => {
  toast.info('Selecione o novo arquivo para reenvio');
  handleFileUpload(doc.type);
};

const handleDeleteDocument = () => {
  if (documentToDelete.value) {
    documents.value = documents.value.filter((d) => d.id !== documentToDelete.value!.id);
    documentToDelete.value = null;
    toast.success('Documento excluído com sucesso');
  }
};

const handleDownload = (doc: Document) => {
  toast.info(`Download de ${doc.name} iniciado`);
};

const getDocumentForType = (type: string) => {
  return documents.value.find((d) => d.type === type);
};
</script>

<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Verificação de Identidade (KYC)</h1>
      <p class="text-[#435574]">
        Envie seus documentos para verificação e aumente a confiança dos clientes
      </p>
    </div>

    <!-- Verification Status Card -->
    <Card class="p-6">
      <div class="flex items-start justify-between mb-4">
        <div>
          <h3 class="text-[#263238] mb-2">Status da Verificação</h3>
          <Badge
            :class="`${
              getVerificationStatus.status === 'approved'
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : getVerificationStatus.status === 'analyzing'
                ? 'bg-yellow-100 text-yellow-700 hover:bg-yellow-100'
                : getVerificationStatus.status === 'rejected'
                ? 'bg-red-100 text-red-700 hover:bg-red-100'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-100'
            }`"
          >
            {{ getVerificationStatus.label }}
          </Badge>
        </div>
        <CheckCircle
          v-if="getVerificationStatus.status === 'approved'"
          class="h-12 w-12 text-green-600"
        />
        <Clock
          v-else-if="getVerificationStatus.status === 'analyzing'"
          class="h-12 w-12 text-yellow-600"
        />
        <AlertCircle
          v-else-if="getVerificationStatus.status === 'rejected'"
          class="h-12 w-12 text-red-600"
        />
      </div>

      <div class="space-y-2">
        <div class="flex items-center justify-between text-sm">
          <span class="text-[#435574]">Progresso da Verificação</span>
          <span class="text-[#263238]">{{ Math.round(getVerificationStatus.progress) }}%</span>
        </div>
        <Progress :value="getVerificationStatus.progress" class="h-2" />
      </div>

      <div
        v-if="getVerificationStatus.status === 'approved'"
        class="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg"
      >
        <p class="text-sm text-green-800">
          ✓ Sua identidade foi verificada com sucesso! Seu perfil agora tem o selo de
          confiança.
        </p>
      </div>

      <div
        v-if="getVerificationStatus.status === 'analyzing'"
        class="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg"
      >
        <p class="text-sm text-yellow-800">
          ⏱ Seus documentos estão em análise. Você será notificado quando a verificação
          for concluída (geralmente em até 24 horas).
        </p>
      </div>

      <div
        v-if="getVerificationStatus.status === 'rejected'"
        class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg"
      >
        <p class="text-sm text-red-800">
          ⚠ Alguns documentos foram rejeitados. Verifique os motivos abaixo e reenvie os
          documentos corrigidos.
        </p>
      </div>
    </Card>

    <!-- Documents List -->
    <div class="space-y-4">
      <h3 class="text-[#263238]">Documentos Necessários</h3>

      <Card v-for="docType in documentTypes" :key="docType.value" class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-start gap-3 flex-1">
            <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <FileText class="h-6 w-6 text-[#286AD8]" />
            </div>
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <h4 class="text-[#263238]">{{ docType.label }}</h4>
                <Badge v-if="docType.required" variant="secondary" class="text-xs">
                  Obrigatório
                </Badge>
              </div>
              <p class="text-sm text-[#435574]">
                Formatos aceitos: PDF, JPG, PNG • Tamanho máximo: 5 MB
              </p>
            </div>
          </div>
          <template v-if="getDocumentForType(docType.value)">
            <template v-if="getDocumentForType(docType.value)!.status === 'approved'">
              <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                <CheckCircle class="h-3 w-3 mr-1" />
                Aprovado
              </Badge>
            </template>
            <template v-else-if="getDocumentForType(docType.value)!.status === 'analyzing'">
              <Badge class="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">
                <Clock class="h-3 w-3 mr-1" />
                Em Análise
              </Badge>
            </template>
            <template v-else-if="getDocumentForType(docType.value)!.status === 'rejected'">
              <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
                <XCircle class="h-3 w-3 mr-1" />
                Rejeitado
              </Badge>
            </template>
            <template v-else-if="getDocumentForType(docType.value)!.status === 'pending'">
              <Badge variant="secondary">
                <AlertCircle class="h-3 w-3 mr-1" />
                Pendente
              </Badge>
            </template>
          </template>
        </div>

        <template v-if="getDocumentForType(docType.value)">
          <div class="space-y-4">
            <!-- Uploaded Document Info -->
            <div class="p-4 bg-gray-50 rounded-lg">
              <div class="flex items-center justify-between mb-2">
                <div class="flex items-center gap-2">
                  <FileText class="h-5 w-5 text-[#435574]" />
                  <div>
                    <p class="text-sm text-[#263238]">{{ getDocumentForType(docType.value)!.name }}</p>
                    <p class="text-xs text-[#435574]">
                      Enviado em {{ getDocumentForType(docType.value)!.uploadedAt }} •
                      {{ getDocumentForType(docType.value)!.fileSize }}
                    </p>
                  </div>
                </div>
                <div class="flex gap-2">
                  <Button
                    @click="handleDownload(getDocumentForType(docType.value)!)"
                    variant="ghost"
                    size="icon"
                  >
                    <Download class="h-4 w-4" />
                  </Button>
                  <Button
                    @click="documentToDelete = getDocumentForType(docType.value)!"
                    variant="ghost"
                    size="icon"
                    class="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </div>

              <p v-if="getDocumentForType(docType.value)!.reviewedAt" class="text-xs text-[#435574]">
                Revisado em {{ getDocumentForType(docType.value)!.reviewedAt }}
              </p>
            </div>

            <!-- Rejection Reason -->
            <div
              v-if="getDocumentForType(docType.value)!.status === 'rejected' && getDocumentForType(docType.value)!.rejectionReason"
              class="p-4 bg-red-50 border border-red-200 rounded-lg"
            >
              <div class="flex items-start gap-2">
                <XCircle class="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <p class="text-sm text-[#263238] mb-1">Motivo da Recusa</p>
                  <p class="text-sm text-red-700">{{ getDocumentForType(docType.value)!.rejectionReason }}</p>
                </div>
              </div>
              <Button
                @click="handleReupload(getDocumentForType(docType.value)!)"
                size="sm"
                class="mt-3 bg-red-600 hover:bg-red-700"
              >
                <Upload class="h-4 w-4 mr-2" />
                Reenviar Documento
              </Button>
            </div>

            <!-- Reupload Option for Approved -->
            <Button
              v-if="getDocumentForType(docType.value)!.status === 'approved'"
              @click="handleReupload(getDocumentForType(docType.value)!)"
              variant="outline"
              size="sm"
            >
              <Upload class="h-4 w-4 mr-2" />
              Atualizar Documento
            </Button>
          </div>
        </template>
        <div
          v-else
          class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-[#286AD8] transition-colors cursor-pointer"
        >
          <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p class="text-[#263238] mb-2">
            Clique para fazer upload ou arraste o arquivo
          </p>
          <p class="text-sm text-[#435574] mb-4">
            PDF, JPG ou PNG • Máximo 5 MB
          </p>
          <Button
            @click="handleFileUpload(docType.value)"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            Selecionar Arquivo
          </Button>
        </div>
      </Card>
    </div>

    <!-- Security Notice -->
    <Card class="p-6 bg-blue-50 border-blue-200">
      <div class="flex items-start gap-3">
        <AlertCircle class="h-5 w-5 text-[#286AD8] flex-shrink-0 mt-0.5" />
        <div>
          <h4 class="text-[#263238] mb-2">Segurança e Privacidade</h4>
          <p class="text-sm text-[#435574] mb-2">
            Seus documentos são armazenados com criptografia de ponta a ponta e utilizados
            exclusivamente para verificação de identidade, em conformidade com a LGPD.
          </p>
          <ul class="text-sm text-[#435574] space-y-1 list-disc list-inside">
            <li>Acesso restrito apenas à equipe de verificação</li>
            <li>Logs de acesso registrados para auditoria</li>
            <li>Documentos eliminados após prazo legal de retenção</li>
            <li>Dados nunca compartilhados com terceiros</li>
          </ul>
        </div>
      </div>
    </Card>

    <!-- Delete Confirmation Dialog -->
    <AlertDialog :open="!!documentToDelete" @update:open="(val) => !val && (documentToDelete = null)">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Excluir Documento</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja excluir o documento "{{ documentToDelete?.name }}"?
            Esta ação não pode ser desfeita e você precisará enviar um novo documento.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleDeleteDocument"
            class="bg-red-600 hover:bg-red-700"
          >
            Sim, Excluir
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

