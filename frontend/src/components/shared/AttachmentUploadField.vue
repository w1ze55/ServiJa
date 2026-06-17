<script setup lang="ts">
import { computed, ref } from 'vue';
import { FileText, Upload, X } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Label from '../ui/label.vue';
import Textarea from '../ui/textarea.vue';

interface Props {
  files: File[];
  description?: string;
  descriptionEnabled?: boolean;
  descriptionLabel?: string;
  descriptionPlaceholder?: string;
  label?: string;
  helperText?: string;
  buttonLabel?: string;
  emptyMessage?: string;
  accept?: string;
  multiple?: boolean;
  maxSizeMb?: number;
  allowedMimeTypes?: string[];
  allowedExtensions?: string[];
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  description: '',
  descriptionEnabled: false,
  descriptionLabel: 'Descrição (opcional)',
  descriptionPlaceholder: 'Adicione uma descrição sobre os arquivos...',
  label: 'Arquivos',
  helperText: 'Máximo 10 MB por arquivo',
  buttonLabel: 'Selecionar Arquivos',
  emptyMessage: 'Clique para fazer upload ou arraste os arquivos',
  accept: '',
  multiple: true,
  maxSizeMb: 10,
  allowedMimeTypes: () => [],
  allowedExtensions: () => [],
  disabled: false,
});

const emit = defineEmits<{
  (e: 'update:files', value: File[]): void;
  (e: 'update:description', value: string): void;
  (e: 'file-rejected', value: string): void;
}>();

const DESCRIPTION_MAX_LENGTH = 500;

const fileInput = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);
const dragDepth = ref(0);

const descriptionLength = computed(() => props.description.length);

const normalizedAllowedExtensions = computed(() =>
  props.allowedExtensions.map((extension) => extension.replace(/^\./, '').toLowerCase())
);

const maxBytes = computed(() => props.maxSizeMb * 1024 * 1024);

const openFilePicker = () => {
  if (props.disabled) return;
  fileInput.value?.click();
};

const resetFileInput = () => {
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const validateFile = (file: File): string | null => {
  if (file.size > maxBytes.value) {
    return `Arquivo ${file.name} excede o tamanho máximo de ${props.maxSizeMb} MB`;
  }

  const hasMimeRestriction = props.allowedMimeTypes.length > 0;
  const hasExtensionRestriction = normalizedAllowedExtensions.value.length > 0;
  if (!hasMimeRestriction && !hasExtensionRestriction) {
    return null;
  }

  const extension = file.name.split('.').pop()?.toLowerCase() || '';
  const isMimeAllowed = !hasMimeRestriction || props.allowedMimeTypes.includes(file.type);
  const isExtensionAllowed = !hasExtensionRestriction || normalizedAllowedExtensions.value.includes(extension);

  if (!isMimeAllowed && !isExtensionAllowed) {
    return `Tipo de arquivo ${file.name} não permitido`;
  }

  return null;
};

const appendFiles = (files: File[]) => {
  if (files.length === 0) {
    resetFileInput();
    return;
  }

  const validFiles: File[] = [];
  files.forEach((file) => {
    const validationError = validateFile(file);
    if (validationError) {
      emit('file-rejected', validationError);
      return;
    }
    validFiles.push(file);
  });

  if (validFiles.length > 0) {
    emit('update:files', [...props.files, ...validFiles]);
  }

  resetFileInput();
};

const handleInputChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (!target.files) return;
  appendFiles(Array.from(target.files));
};

const handleDragEnter = (event: DragEvent) => {
  event.preventDefault();
  if (props.disabled) return;
  dragDepth.value += 1;
  isDragging.value = true;
};

const handleDragOver = (event: DragEvent) => {
  event.preventDefault();
  if (props.disabled) return;
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy';
  }
  isDragging.value = true;
};

const handleDragLeave = (event: DragEvent) => {
  event.preventDefault();
  if (props.disabled) return;
  dragDepth.value = Math.max(0, dragDepth.value - 1);
  if (dragDepth.value === 0) {
    isDragging.value = false;
  }
};

const handleDrop = (event: DragEvent) => {
  event.preventDefault();
  if (props.disabled) return;

  dragDepth.value = 0;
  isDragging.value = false;

  const droppedFiles = event.dataTransfer?.files;
  if (!droppedFiles || droppedFiles.length === 0) return;
  appendFiles(Array.from(droppedFiles));
};

const removeFile = (index: number) => {
  emit('update:files', props.files.filter((_, currentIndex) => currentIndex !== index));
  resetFileInput();
};

const updateDescription = (value: string | number) => {
  emit('update:description', String(value ?? '').slice(0, DESCRIPTION_MAX_LENGTH));
};

const formatFileSize = (size: number) => {
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(2)} MB`;
  }

  return `${(size / 1024).toFixed(1)} KB`;
};
</script>

<template>
  <div class="space-y-4">
    <div v-if="descriptionEnabled" class="space-y-2">
      <Label for="attachment-upload-description">{{ descriptionLabel }}</Label>
      <div class="relative">
        <Textarea
          id="attachment-upload-description"
          :model-value="description"
          :rows="2"
          :maxlength="DESCRIPTION_MAX_LENGTH"
          :disabled="disabled"
          :placeholder="descriptionPlaceholder"
          class="pb-6"
          @update:model-value="updateDescription"
        />
        <span class="pointer-events-none absolute bottom-2 right-3 text-xs text-[#435574]">
          {{ descriptionLength }}/{{ DESCRIPTION_MAX_LENGTH }}
        </span>
      </div>
    </div>

    <div class="space-y-2">
      <Label>{{ label }}</Label>
      <div
        :class="[
          'border-2 border-dashed rounded-lg p-8 text-center transition-colors cursor-pointer',
          disabled ? 'opacity-60 cursor-not-allowed bg-gray-50' : 'hover:border-[#286AD8]',
          isDragging ? 'border-[#286AD8] bg-blue-50/60' : 'border-gray-300'
        ]"
        @click="openFilePicker"
        @dragenter="handleDragEnter"
        @dragover="handleDragOver"
        @dragleave="handleDragLeave"
        @drop="handleDrop"
      >
        <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p class="text-[#263238] mb-2">
          {{ emptyMessage }}
        </p>
        <p class="mx-auto mb-4 max-w-md text-sm leading-5 text-[#435574]">
          {{ helperText }}
        </p>
        <input
          ref="fileInput"
          type="file"
          :multiple="multiple"
          :accept="accept"
          :disabled="disabled"
          class="hidden"
          @change="handleInputChange"
        />
        <Button @click.stop="openFilePicker" size="sm" variant="outline" :disabled="disabled">
          {{ buttonLabel }}
        </Button>
      </div>

      <div v-if="files.length > 0" class="mt-3 space-y-2">
        <p class="text-sm font-medium text-[#263238]">
          Arquivos selecionados ({{ files.length }}):
        </p>
        <div
          v-for="(file, index) in files"
          :key="`${file.name}-${file.size}-${index}`"
          class="flex items-center justify-between gap-3 p-3 bg-gray-50 rounded-lg"
        >
          <div class="flex items-center gap-2 min-w-0">
            <FileText class="h-4 w-4 text-[#286AD8] shrink-0" />
            <div class="min-w-0">
              <p class="text-sm text-[#263238] truncate">{{ file.name }}</p>
              <p class="text-xs text-[#435574]">{{ formatFileSize(file.size) }}</p>
            </div>
          </div>
          <Button
            @click="removeFile(index)"
            variant="ghost"
            size="icon"
            class="h-7 w-7 text-red-500 hover:text-red-700 hover:bg-red-50 shrink-0"
            :disabled="disabled"
          >
            <X class="h-3 w-3" />
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>
