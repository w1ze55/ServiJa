<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Loader2 } from 'lucide-vue-next';
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
import AttachmentUploadField from '@/components/shared/AttachmentUploadField.vue';
import { toast } from '@/utils/toast';
import { deleteFile, uploadFiles } from '@/api/uploadService';
import { supportService } from '@/api/services/supportService';
import {
  SUPPORT_ATTACHMENT_ACCEPT,
  SUPPORT_CATEGORY_OPTIONS,
  type SupportAttachment,
  type SupportCategoryValue,
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

const MAX_SUPPORT_ATTACHMENTS = 5;
const SUPPORT_ALLOWED_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'text/plain',
];
const SUPPORT_ALLOWED_EXTENSIONS = [
  'jpg',
  'jpeg',
  'png',
  'gif',
  'webp',
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'txt',
  'ppt',
  'pptx',
];

const formData = ref<SupportFormState>({
  supportCategory: '',
  description: '',
});

const selectedFiles = ref<File[]>([]);
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

const selectedCategoryLabel = computed(() =>
  SUPPORT_CATEGORY_OPTIONS.find((option) => option.value === formData.value.supportCategory)?.label || ''
);

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

const handleSelectedFilesUpdate = (files: File[]) => {
  if (files.length > MAX_SUPPORT_ATTACHMENTS) {
    toast.error('Você pode enviar no máximo 5 anexos por solicitação.');
    return;
  }

  selectedFiles.value = files;
};

const handleRejectedFile = (message: string) => {
  toast.error(message);
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
    <DialogContent class="flex max-h-[calc(100vh-2rem)] w-[calc(100vw-2rem)] max-w-2xl flex-col gap-0 overflow-hidden p-0">
      <DialogHeader class="shrink-0 px-6 pb-4 pt-6">
        <DialogTitle>Solicitação de suporte</DialogTitle>
        <DialogDescription>
          Envie sua dúvida ou pedido para a equipe administrativa. Se houver anexos, eles podem
          ajudar a contextualizar o chamado.
        </DialogDescription>
      </DialogHeader>

      <div class="flex-1 overflow-y-auto px-6 py-2">
        <div class="space-y-5 pb-6">
        <div class="space-y-2">
          <Label for="support-category">
            Categoria de suporte <span class="text-red-500">*</span>
          </Label>
          <Select
            :model-value="formData.supportCategory"
            @update:model-value="(value) => formData.supportCategory = value as SupportCategoryValue"
          >
            <SelectTrigger :class="errors.supportCategory ? 'border-red-500' : ''">
              <SelectValue
                :label="selectedCategoryLabel"
                placeholder="Selecione a categoria do chamado"
              />
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
          <div class="relative">
            <Textarea
              id="support-description"
              v-model="formData.description"
              :rows="6"
              maxlength="5000"
              placeholder="Explique o problema, a categoria desejada ou qualquer contexto que ajude nossa equipe."
              :class="[
                'pr-20 pb-8',
                errors.description ? 'border-red-500' : '',
              ]"
            />
            <span class="pointer-events-none absolute bottom-3 right-3 text-xs text-gray-500">
              {{ formData.description.length }}/5000
            </span>
          </div>
          <p v-if="errors.description" class="text-sm text-red-500">{{ errors.description }}</p>
        </div>

        <AttachmentUploadField
          :files="selectedFiles"
          label="Anexos"
          empty-message="Clique para fazer upload ou arraste imagens e documentos"
          helper-text="Imagens, PDF, DOC, XLS, PPT e TXT. Até 5 arquivos de 10MB."
          button-label="Selecionar anexos"
          :accept="SUPPORT_ATTACHMENT_ACCEPT"
          :multiple="true"
          :max-size-mb="10"
          :allowed-mime-types="SUPPORT_ALLOWED_MIME_TYPES"
          :allowed-extensions="SUPPORT_ALLOWED_EXTENSIONS"
          :disabled="isSubmitting"
          @update:files="handleSelectedFilesUpdate"
          @file-rejected="handleRejectedFile"
        />
        </div>
      </div>

      <DialogFooter class="shrink-0 border-t bg-background px-6 py-4">
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
