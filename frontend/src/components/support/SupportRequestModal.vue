<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { FileText, ImageIcon, Loader2, Paperclip, Upload, X } from 'lucide-vue-next';
import Dialog from '@/components/ui/dialog.vue';
import DialogContent from '@/components/ui/dialog-content.vue';
import DialogDescription from '@/components/ui/dialog-description.vue';
import DialogFooter from '@/components/ui/dialog-footer.vue';
import DialogHeader from '@/components/ui/dialog-header.vue';
import DialogTitle from '@/components/ui/dialog-title.vue';
import Button from '@/components/ui/button.vue';
import Label from '@/components/ui/label.vue';
import Select from '@/components/ui/select.vue';
import SelectContent from '@/components/ui/select-content.vue';
import SelectItem from '@/components/ui/select-item.vue';
import SelectTrigger from '@/components/ui/select-trigger.vue';
import SelectValue from '@/components/ui/select-value.vue';
import Textarea from '@/components/ui/textarea.vue';
import { toast } from '@/utils/toast';
import { deleteFile, uploadFiles } from '@/api/uploadService';
import { supportService } from '@/api/services/supportService';
import {
  SUPPORT_ATTACHMENT_ACCEPT,
  SUPPORT_CATEGORY_OPTIONS,
  type SupportAttachment,
  type SupportCategoryValue,
  formatSupportFileSize,
  isAllowedSupportFile,
} from '@/constants/support';
import { useUserStore } from '@/stores/userStore';

interface Props {
  open: boolean;
  onClose: () => void;
  defaultCategory?: SupportCategoryValue | null;
  onSuccess?: () => void;
}

interface SupportFormState {
  supportCategory: SupportCategoryValue | '';
  description: string;
}

const props = defineProps<Props>();
const userStore = useUserStore();

const formData = ref<SupportFormState>({
  supportCategory: '',
  description: '',
});

const selectedFiles = ref<File[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const errors = ref<Record<string, string>>({});
const isSubmitting = ref(false);

const currentUser = computed(() => {
  if (userStore.user) {
    return userStore.user;
  }

  try {
    return JSON.parse(localStorage.getItem('user') || 'null') as
      | { id?: string; name?: string; email?: string }
      | null;
  } catch {
    return null;
  }
});

const resetForm = () => {
  formData.value = {
    supportCategory: props.defaultCategory || '',
    description: '',
  };
  selectedFiles.value = [];
  errors.value = {};
};

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      resetForm();
      return;
    }

    selectedFiles.value = [];
    errors.value = {};
  },
  { immediate: true }
);

watch(
  () => props.defaultCategory,
  (category) => {
    if (props.open) {
      formData.value.supportCategory = category || '';
    }
  }
);

const removeFile = (index: number) => {
  selectedFiles.value = selectedFiles.value.filter((_, currentIndex) => currentIndex !== index);
};

const openFilePicker = () => {
  if (isSubmitting.value) return;
  fileInput.value?.click();
};

const validateSelectedFiles = (files: File[]) => {
  if (selectedFiles.value.length + files.length > 5) {
    toast.error('Você pode enviar no máximo 5 anexos por solicitação.');
    return false;
  }

  const invalidFile = files.find((file) => !isAllowedSupportFile(file));
  if (invalidFile) {
    toast.error('Envie apenas imagens ou documentos compatíveis.');
    return false;
  }

  const oversizedFile = files.find((file) => file.size > 10 * 1024 * 1024);
  if (oversizedFile) {
    toast.error('Cada anexo deve ter no máximo 10MB.');
    return false;
  }

  return true;
};

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = target.files ? Array.from(target.files) : [];

  if (files.length > 0 && validateSelectedFiles(files)) {
    selectedFiles.value = [...selectedFiles.value, ...files];
  }

  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const validateForm = () => {
  const nextErrors: Record<string, string> = {};

  if (!formData.value.supportCategory) {
    nextErrors.supportCategory = 'Selecione uma categoria de suporte.';
  }

  if (!formData.value.description.trim()) {
    nextErrors.description = 'Descreva sua solicitação.';
  }

  errors.value = nextErrors;
  return Object.keys(nextErrors).length === 0;
};

const extractErrorMessage = (error: unknown) => {
  const apiMessage = (error as { response?: { data?: { message?: string; error?: string } } })?.response?.data;
  return apiMessage?.message || apiMessage?.error || 'Não foi possível enviar sua solicitação agora.';
};

const handleClose = () => {
  if (isSubmitting.value) return;
  closeModal();
};

const closeModal = () => {
  resetForm();
  props.onClose();
};

const handleSubmit = async () => {
  if (!validateForm()) {
    toast.error('Preencha os campos obrigatórios para continuar.');
    return;
  }

  if (!currentUser.value?.name || !currentUser.value?.email) {
    toast.error('Não foi possível identificar o usuário logado.');
    return;
  }

  isSubmitting.value = true;
  let uploadedUrls: string[] = [];

  try {
    let attachments: SupportAttachment[] = [];

    if (selectedFiles.value.length > 0) {
      uploadedUrls = await uploadFiles(selectedFiles.value);
      attachments = uploadedUrls.map((url, index) => ({
        id: `${Date.now()}-${index}`,
        name: selectedFiles.value[index]?.name || `Anexo ${index + 1}`,
        url,
        type: selectedFiles.value[index]?.type || undefined,
        size: selectedFiles.value[index]?.size || undefined,
      }));
    }

    await supportService.createSupportRequest({
      userId: currentUser.value.id ? Number(currentUser.value.id) : undefined,
      userName: currentUser.value.name,
      userEmail: currentUser.value.email,
      supportCategory: formData.value.supportCategory as SupportCategoryValue,
      description: formData.value.description.trim(),
      attachments,
    });

    toast.success('Solicitação de suporte enviada com sucesso.');
    props.onSuccess?.();
    closeModal();
  } catch (error) {
    if (uploadedUrls.length > 0) {
      await Promise.allSettled(uploadedUrls.map((url) => deleteFile(url)));
    }
    toast.error(extractErrorMessage(error));
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <Dialog :open="open" @update:open="(value) => !value && handleClose()">
    <DialogContent class="sm:max-w-2xl">
      <DialogHeader>
        <DialogTitle>Solicitação de suporte</DialogTitle>
        <DialogDescription>
          Envie sua dúvida ou pedido para a equipe administrativa. Se houver anexos, eles podem
          ajudar a contextualizar o chamado.
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-5 py-2">
        <div class="space-y-2">
          <Label for="support-category">
            Categoria de suporte <span class="text-red-500">*</span>
          </Label>
          <Select
            :model-value="formData.supportCategory"
            @update:model-value="(value) => formData.supportCategory = value as SupportCategoryValue"
          >
            <SelectTrigger :class="errors.supportCategory ? 'border-red-500' : ''">
              <SelectValue placeholder="Selecione a categoria do chamado" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem
                v-for="option in SUPPORT_CATEGORY_OPTIONS"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.supportCategory" class="text-sm text-red-500">
            {{ errors.supportCategory }}
          </p>
        </div>

        <div class="space-y-2">
          <Label for="support-description">
            Descrição <span class="text-red-500">*</span>
          </Label>
          <Textarea
            id="support-description"
            v-model="formData.description"
            :rows="6"
            maxlength="5000"
            placeholder="Explique o problema, a categoria desejada ou qualquer contexto que ajude nossa equipe."
            :class="errors.description ? 'border-red-500' : ''"
          />
          <div class="flex items-center justify-between text-xs text-gray-500">
            <p v-if="errors.description" class="text-sm text-red-500">{{ errors.description }}</p>
            <span class="ml-auto">{{ formData.description.length }}/5000</span>
          </div>
        </div>

        <div class="space-y-2">
          <Label>Anexos</Label>
          <div
            class="rounded-xl border border-dashed border-slate-300 bg-slate-50/70 p-5 text-center transition hover:border-[#286AD8] hover:bg-blue-50/40"
          >
            <input
              ref="fileInput"
              type="file"
              multiple
              :accept="SUPPORT_ATTACHMENT_ACCEPT"
              class="hidden"
              @change="handleFileChange"
            />

            <Upload class="mx-auto mb-3 h-8 w-8 text-slate-400" />
            <p class="text-sm font-medium text-slate-700">
              Adicione imagens ou documentos para complementar sua solicitação
            </p>
            <p class="mt-1 text-xs text-slate-500">
              Aceitamos imagens, PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX e TXT. Máximo de 5 arquivos
              com até 10MB cada.
            </p>
            <Button type="button" variant="outline" class="mt-4" @click="openFilePicker">
              <Paperclip class="mr-2 h-4 w-4" />
              Selecionar anexos
            </Button>
          </div>

          <div v-if="selectedFiles.length > 0" class="space-y-2">
            <div
              v-for="(file, index) in selectedFiles"
              :key="`${file.name}-${index}`"
              class="flex items-center gap-3 rounded-xl border border-slate-200 bg-white px-3 py-2"
            >
              <div class="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-slate-600">
                <ImageIcon v-if="file.type.startsWith('image/')" class="h-4 w-4" />
                <FileText v-else class="h-4 w-4" />
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-slate-800">{{ file.name }}</p>
                <p class="text-xs text-slate-500">{{ formatSupportFileSize(file.size) }}</p>
              </div>
              <button
                type="button"
                class="rounded-full p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                @click="removeFile(index)"
              >
                <X class="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <DialogFooter>
        <Button type="button" variant="outline" :disabled="isSubmitting" @click="handleClose">
          Cancelar
        </Button>
        <Button type="button" :disabled="isSubmitting" @click="handleSubmit">
          <Loader2 v-if="isSubmitting" class="mr-2 h-4 w-4 animate-spin" />
          {{ isSubmitting ? 'Enviando...' : 'Enviar solicitação' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
