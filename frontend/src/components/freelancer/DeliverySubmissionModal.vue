<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Send } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Label from '../ui/label.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AttachmentUploadField from '../shared/AttachmentUploadField.vue';
import { toast } from '../../utils/toast';

interface Props {
  open?: boolean;
  serviceName?: string;
  isLoading?: boolean;
  onSubmit: (payload: { description: string; files: File[] }) => boolean | void | Promise<boolean | void>;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = computed({
  get: () => props.open ?? false,
  set: (value) => emit('update:open', value),
});

const description = ref('');
const files = ref<File[]>([]);
const descriptionError = ref('');
const isSubmitting = ref(false);

const isBusy = computed(() => Boolean(props.isLoading) || isSubmitting.value);

const dialogDescription = computed(() =>
  props.serviceName
    ? `Descreva o que foi entregue em "${props.serviceName}" e anexe os arquivos finais.`
    : 'Descreva o que foi entregue e anexe os arquivos finais.'
);

watch(isOpen, (open) => {
  if (!open) {
    description.value = '';
    files.value = [];
    descriptionError.value = '';
  }
});

const handleOpenChange = (value: boolean) => {
  isOpen.value = value;
};

const handleDescriptionInput = (value: string | number) => {
  description.value = String(value ?? '');
  if (descriptionError.value) {
    descriptionError.value = '';
  }
};

const handleFilesUpdate = (value: File[]) => {
  files.value = value;
};

const handleRejectedFile = (message: string) => {
  toast.error(message);
};

const handleSubmit = async () => {
  if (isBusy.value) return;

  if (!description.value.trim()) {
    descriptionError.value = 'A descrição da entrega é obrigatória';
    return;
  }

  isSubmitting.value = true;
  try {
    const result = await props.onSubmit({
      description: description.value.trim(),
      files: files.value,
    });

    if (result !== false) {
      isOpen.value = false;
    }
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <Dialog :open="isOpen" @update:open="handleOpenChange">
    <DialogContent class="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle>Enviar Entrega</DialogTitle>
        <DialogDescription>
          {{ dialogDescription }}
        </DialogDescription>
      </DialogHeader>

      <div class="space-y-5">
        <div class="space-y-2">
          <Label for="delivery-description">
            Descrição da Entrega <span class="text-red-500">*</span>
          </Label>
          <Textarea
            id="delivery-description"
            :model-value="description"
            :rows="5"
            :disabled="isBusy"
            placeholder="Explique o que foi feito, instruções de uso, próximos passos, etc."
            :class="descriptionError ? 'border-red-500' : ''"
            @update:model-value="handleDescriptionInput"
          />
          <p v-if="descriptionError" class="text-sm text-red-500">{{ descriptionError }}</p>
        </div>

        <AttachmentUploadField
          :files="files"
          label="Anexos da entrega (opcional)"
          empty-message="Clique para fazer upload ou arraste os arquivos finais"
          helper-text="PDF, JPG, PNG, DOC, DOCX até 10MB cada"
          button-label="Selecionar Arquivos"
          accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
          :multiple="true"
          :max-size-mb="10"
          :allowed-mime-types="[
            'application/pdf',
            'image/jpeg',
            'image/png',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
          ]"
          :allowed-extensions="['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx']"
          :disabled="isBusy"
          @update:files="handleFilesUpdate"
          @file-rejected="handleRejectedFile"
        />
      </div>

      <DialogFooter>
        <Button @click="isOpen = false" variant="outline" :disabled="isBusy">
          Cancelar
        </Button>
        <Button @click="handleSubmit" variant="default" :disabled="isBusy">
          <Send class="h-4 w-4 mr-2" />
          {{ isBusy ? 'Enviando...' : 'Enviar Entrega' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
