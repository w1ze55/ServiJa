<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { ArrowLeft, Upload, X, FileText, AlertCircle, Check } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Textarea from '../ui/textarea.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Progress from '../ui/progress.vue';
import { toast } from '../../utils/toast';

interface FileUpload {
  id: string;
  file: File;
  progress: number;
  status: 'uploading' | 'success' | 'error';
}

interface Props {
  freelancerId: string;
  freelancerName: string;
  preSelectedService?: string;
  onBack: () => void;
  onSuccess: (quoteId: string) => void;
}

const props = defineProps<Props>();

const description = ref('');
const selectedService = ref(props.preSelectedService || '');
const budget = ref('');
const deadline = ref('');
const files = ref<FileUpload[]>([]);
const isSubmitting = ref(false);
const lastSaved = ref<Date | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
let autosaveTimeout: ReturnType<typeof setTimeout> | null = null;

// Mock services - substituir por dados reais da API
const services = [
  { id: '1', name: 'Instalação Elétrica Residencial' },
  { id: '2', name: 'Manutenção Preventiva' },
  { id: '3', name: 'Instalação de Ar Condicionado' },
  { id: '4', name: 'Troca de Disjuntores' },
  { id: '5', name: 'Instalação de Ventiladores de Teto' },
  { id: '6', name: 'Projeto Elétrico Comercial' },
];

// Autosave
watch([description, selectedService, budget, deadline], () => {
  if (description.value || selectedService.value || budget.value || deadline.value) {
    if (autosaveTimeout) {
      clearTimeout(autosaveTimeout);
    }

    autosaveTimeout = setTimeout(() => {
      // Salvar rascunho - mock
      localStorage.setItem(
        `quote_draft_${props.freelancerId}`,
        JSON.stringify({
          description: description.value,
          selectedService: selectedService.value,
          budget: budget.value,
          deadline: deadline.value,
          savedAt: new Date().toISOString(),
        })
      );
      lastSaved.value = new Date();
    }, 2000);
  }
});

// Carregar rascunho salvo
onMounted(() => {
  const draft = localStorage.getItem(`quote_draft_${props.freelancerId}`);
  if (draft) {
    try {
      const parsed = JSON.parse(draft);
      description.value = parsed.description || '';
      selectedService.value = parsed.selectedService || props.preSelectedService || '';
      budget.value = parsed.budget || '';
      deadline.value = parsed.deadline || '';
      lastSaved.value = new Date(parsed.savedAt);
      toast.info('Rascunho recuperado');
    } catch (error) {
      console.error('Erro ao carregar rascunho', error);
    }
  }
});

onUnmounted(() => {
  if (autosaveTimeout) {
    clearTimeout(autosaveTimeout);
  }
});

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const selectedFiles = Array.from(target.files || []);

  // Validar tamanho dos arquivos (25MB max)
  const invalidFiles = selectedFiles.filter((file) => file.size > 25 * 1024 * 1024);

  if (invalidFiles.length > 0) {
    toast.error(`${invalidFiles.length} arquivo(s) excede(m) o limite de 25 MB`);
    return;
  }

  // Simular upload
  selectedFiles.forEach((file) => {
    const fileUpload: FileUpload = {
      id: Math.random().toString(36).substring(7),
      file,
      progress: 0,
      status: 'uploading',
    };

    files.value = [...files.value, fileUpload];

    // Simular progresso do upload
    const interval = setInterval(() => {
      files.value = files.value.map((f) => {
        if (f.id === fileUpload.id) {
          const newProgress = Math.min(f.progress + 10, 100);
          return {
            ...f,
            progress: newProgress,
            status: newProgress === 100 ? 'success' : 'uploading',
          };
        }
        return f;
      });
    }, 200);

    setTimeout(() => {
      clearInterval(interval);
    }, 2200);
  });

  // Resetar input
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const removeFile = (id: string) => {
  files.value = files.value.filter((f) => f.id !== id);
};

const handleSubmit = async (e: Event) => {
  e.preventDefault();

  if (!description.value.trim()) {
    toast.error('Por favor, descreva o serviço que você precisa');
    return;
  }

  if (!deadline.value) {
    toast.error('Por favor, informe o prazo desejado');
    return;
  }

  isSubmitting.value = true;

  // Simular envio
  setTimeout(() => {
    const quoteId = Math.random().toString(36).substring(7);

    // Limpar rascunho
    localStorage.removeItem(`quote_draft_${props.freelancerId}`);

    toast.success('Solicitação enviada com sucesso!');
    isSubmitting.value = false;
    props.onSuccess(quoteId);
  }, 1500);
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const getFileIcon = (filename: string) => {
  const ext = filename.split('.').pop()?.toLowerCase();
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext || '')) return '🖼️';
  if (['pdf'].includes(ext || '')) return '📄';
  if (['doc', 'docx'].includes(ext || '')) return '📝';
  if (['xls', 'xlsx'].includes(ext || '')) return '📊';
  return '📎';
};
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <Button
        variant="ghost"
        @click="props.onBack"
        class="text-[#435574] hover:text-[#263238] mb-4"
      >
        <ArrowLeft class="h-4 w-4 mr-2" />
        Voltar
      </Button>

      <h1 class="text-[#263238] mb-2">Solicitar Orçamento</h1>
      <p class="text-[#435574]">Envie uma solicitação para {{ props.freelancerName }}</p>
    </div>

    <!-- Autosave indicator -->
    <div
      v-if="lastSaved"
      class="flex items-center gap-2 text-sm text-[#435574] bg-green-50 border border-green-200 rounded-lg p-3"
    >
      <Check class="h-4 w-4 text-green-600" />
      Rascunho salvo automaticamente às
      {{ lastSaved.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }) }}
    </div>

    <!-- Form -->
    <form @submit="handleSubmit" class="bg-white rounded-lg border border-gray-200 p-6 space-y-6">
      <!-- Service Selection (Optional) -->
      <div class="space-y-2">
        <Label for="service" class="text-[#263238]">Serviço (opcional)</Label>
        <Select :model-value="selectedService" @update:model-value="(val: string) => selectedService = val">
          <SelectTrigger id="service">
            <SelectValue placeholder="Selecione um serviço específico" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="none">Nenhum serviço específico</SelectItem>
            <SelectItem v-for="service in services" :key="service.id" :value="service.id">
              {{ service.name }}
            </SelectItem>
          </SelectContent>
        </Select>
        <p class="text-sm text-[#435574]">
          Você pode selecionar um serviço específico ou deixar em branco para uma solicitação geral
        </p>
      </div>

      <!-- Description -->
      <div class="space-y-2">
        <Label for="description" class="text-[#263238]">
          Descrição do serviço <span class="text-red-500">*</span>
        </Label>
        <Textarea
          id="description"
          placeholder="Descreva detalhadamente o que você precisa..."
          :model-value="description"
          @update:model-value="(val) => description = val as string"
          rows="6"
          class="resize-none"
          required
        />
        <p class="text-sm text-[#435574]">{{ description.length }} caracteres</p>
      </div>

      <!-- Budget -->
      <div class="space-y-2">
        <Label for="budget" class="text-[#263238]">Orçamento estimado (opcional)</Label>
        <div class="relative">
          <span class="absolute left-3 top-1/2 -translate-y-1/2 text-[#435574]">R$</span>
          <Input
            id="budget"
            type="number"
            placeholder="0,00"
            :model-value="budget"
            @update:model-value="(val) => budget = val as string"
            class="pl-10"
            min="0"
            step="0.01"
          />
        </div>
        <p class="text-sm text-[#435574]">Informe um valor aproximado para facilitar a negociação</p>
      </div>

      <!-- Deadline -->
      <div class="space-y-2">
        <Label for="deadline" class="text-[#263238]">
          Prazo desejado <span class="text-red-500">*</span>
        </Label>
        <Input
          id="deadline"
          type="date"
          :model-value="deadline"
          @update:model-value="(val) => deadline = val as string"
          :min="new Date().toISOString().split('T')[0]"
          required
        />
        <p class="text-sm text-[#435574]">Quando você precisa que o serviço seja concluído?</p>
      </div>

      <!-- File Upload -->
      <div class="space-y-2">
        <Label class="text-[#263238]">Anexos (opcional)</Label>
        <div class="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-[#286AD8] transition-colors">
          <input
            ref="fileInputRef"
            type="file"
            multiple
            @change="handleFileSelect"
            class="hidden"
            accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
          />
          <Button
            type="button"
            variant="outline"
            @click="fileInputRef?.click()"
            class="mb-2"
          >
            <Upload class="h-4 w-4 mr-2" />
            Escolher arquivos
          </Button>
          <p class="text-sm text-[#435574]">Imagens, PDFs ou documentos até 25 MB cada</p>
        </div>

        <!-- Uploaded Files -->
        <div v-if="files.length > 0" class="space-y-2 mt-4">
          <div
            v-for="file in files"
            :key="file.id"
            class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200"
          >
            <span class="text-2xl">{{ getFileIcon(file.file.name) }}</span>
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between mb-1">
                <p class="text-sm text-[#263238] truncate">{{ file.file.name }}</p>
                <span class="text-xs text-[#435574]">{{ formatFileSize(file.file.size) }}</span>
              </div>
              <Progress v-if="file.status === 'uploading'" :model-value="file.progress" class="h-1" />
              <div v-if="file.status === 'success'" class="flex items-center gap-1 text-xs text-green-600">
                <Check class="h-3 w-3" />
                Upload concluído
              </div>
              <div v-if="file.status === 'error'" class="flex items-center gap-1 text-xs text-red-600">
                <AlertCircle class="h-3 w-3" />
                Erro no upload
              </div>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              @click="removeFile(file.id)"
              class="flex-shrink-0"
            >
              <X class="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      <!-- Info Box -->
      <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex gap-3">
        <AlertCircle class="h-5 w-5 text-[#286AD8] flex-shrink-0 mt-0.5" />
        <div class="text-sm text-[#435574]">
          <p class="mb-1">
            <strong class="text-[#263238]">Importante:</strong>
          </p>
          <ul class="list-disc list-inside space-y-1">
            <li>O freelancer receberá uma notificação por e-mail e no painel</li>
            <li>Você poderá acompanhar o status da solicitação em "Minhas Solicitações"</li>
            <li>O freelancer terá até 48h para responder sua solicitação</li>
          </ul>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-3 pt-4 border-t border-gray-200">
        <Button
          type="button"
          variant="outline"
          @click="props.onBack"
          class="flex-1"
          :disabled="isSubmitting"
        >
          Cancelar
        </Button>
        <Button
          type="submit"
          class="flex-1 bg-[#286AD8] hover:bg-[#435574]"
          :disabled="isSubmitting"
        >
          <template v-if="isSubmitting">
            <div class="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
            Enviando...
          </template>
          <template v-else>
            <FileText class="h-4 w-4 mr-2" />
            Enviar Solicitação
          </template>
        </Button>
      </div>
    </form>
  </div>
</template>




