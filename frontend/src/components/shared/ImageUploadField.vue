<script setup lang="ts">
import { computed, ref } from 'vue';
import { Loader2, Upload, X } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Label from '../ui/label.vue';
import { toast } from '@/utils/toast';
import { uploadImages } from '@/api/uploadService';

interface Props {
  modelValue: string[];
  label?: string;
  required?: boolean;
  disabled?: boolean;
  maxFiles?: number;
  maxSizeMb?: number;
  buttonLabel?: string;
  emptyMessage?: string;
  helperText?: string;
}

const props = withDefaults(defineProps<Props>(), {
  label: 'Imagens',
  required: false,
  disabled: false,
  maxSizeMb: 10,
  buttonLabel: 'Selecionar Imagens',
  emptyMessage: 'Clique para fazer upload ou arraste as imagens',
  helperText: 'PNG, JPG até 10MB cada',
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void;
  (e: 'image-removed', value: string): void;
}>();

const fileInput = ref<HTMLInputElement | null>(null);
const isUploading = ref(false);
const isDragging = ref(false);
const dragDepth = ref(0);

const isDisabled = computed(() => props.disabled || isUploading.value);
const maxBytes = computed(() => props.maxSizeMb * 1024 * 1024);

const openFilePicker = () => {
  if (isDisabled.value) {
    return;
  }

  fileInput.value?.click();
};

const resetFileInput = () => {
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const hasDraggedFiles = (event: DragEvent) =>
  Array.from(event.dataTransfer?.types || []).includes('Files');

const validateFiles = (files: File[]) => {
  if (files.length === 0) {
    return false;
  }

  if (props.maxFiles !== undefined && props.modelValue.length + files.length > props.maxFiles) {
    toast.error(`Máximo de ${props.maxFiles} imagens permitido`);
    return false;
  }

  const invalidFiles = files.filter((file) => !file.type.startsWith('image/'));
  if (invalidFiles.length > 0) {
    toast.error('Envie apenas arquivos de imagem');
    return false;
  }

  const oversizedFiles = files.filter((file) => file.size > maxBytes.value);
  if (oversizedFiles.length > 0) {
    toast.error(`Cada imagem deve ter no máximo ${props.maxSizeMb}MB`);
    return false;
  }

  return true;
};

const uploadSelectedImages = async (files: File[]) => {
  if (isUploading.value || !validateFiles(files)) {
    return;
  }

  isUploading.value = true;

  try {
    const uploadedUrls = await uploadImages(files);
    emit('update:modelValue', [...props.modelValue, ...uploadedUrls]);
    toast.success(`${files.length} imagem(ns) enviada(s) com sucesso`);
  } catch (error) {
    console.error('Erro ao fazer upload:', error);
    toast.error('Erro ao enviar imagem(ns). Tente novamente.');
  } finally {
    isUploading.value = false;
  }
};

const handleInputChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = target.files ? Array.from(target.files) : [];

  try {
    await uploadSelectedImages(files);
  } finally {
    resetFileInput();
  }
};

const handleDragEnter = (event: DragEvent) => {
  event.preventDefault();
  if (isDisabled.value || !hasDraggedFiles(event)) {
    return;
  }

  dragDepth.value += 1;
  isDragging.value = true;
};

const handleDragOver = (event: DragEvent) => {
  event.preventDefault();
  if (isDisabled.value || !hasDraggedFiles(event)) {
    return;
  }

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy';
  }

  isDragging.value = true;
};

const handleDragLeave = (event: DragEvent) => {
  event.preventDefault();
  if (isDisabled.value) {
    return;
  }

  dragDepth.value = Math.max(dragDepth.value - 1, 0);
  if (dragDepth.value === 0) {
    isDragging.value = false;
  }
};

const handleDrop = async (event: DragEvent) => {
  event.preventDefault();

  dragDepth.value = 0;
  isDragging.value = false;

  if (isDisabled.value || !hasDraggedFiles(event)) {
    return;
  }

  const files = Array.from(event.dataTransfer?.files || []);
  await uploadSelectedImages(files);
  resetFileInput();
};

const removeImage = (index: number) => {
  if (isDisabled.value) {
    return;
  }

  const imageUrl = props.modelValue[index];
  emit(
    'update:modelValue',
    props.modelValue.filter((_, currentIndex) => currentIndex !== index)
  );

  if (imageUrl) {
    emit('image-removed', imageUrl);
  }

  resetFileInput();
};
</script>

<template>
  <div class="space-y-4">
    <div class="space-y-2">
      <Label>
        {{ label }}
        <span v-if="required" class="text-red-500">*</span>
      </Label>

      <div
        :class="[
          'border-2 border-dashed rounded-lg p-8 text-center transition-colors',
          isDisabled ? 'opacity-60 cursor-not-allowed bg-gray-50' : 'cursor-pointer hover:border-[#286AD8]',
          isUploading || isDragging ? 'border-[#286AD8] bg-blue-50' : 'border-gray-300'
        ]"
        @click="openFilePicker"
        @dragenter="handleDragEnter"
        @dragover="handleDragOver"
        @dragleave="handleDragLeave"
        @drop="handleDrop"
      >
        <template v-if="!isUploading">
          <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p class="text-[#263238] mb-2">{{ emptyMessage }}</p>
          <p class="text-sm text-[#435574] mb-4">{{ helperText }}</p>
          <input
            ref="fileInput"
            type="file"
            multiple
            accept="image/*"
            class="hidden"
            :disabled="isDisabled"
            @change="handleInputChange"
          />
          <Button type="button" @click.stop="openFilePicker" size="sm" variant="outline" :disabled="isDisabled">
            {{ buttonLabel }}
          </Button>
        </template>

        <template v-else>
          <Loader2 class="h-12 w-12 text-[#286AD8] mx-auto mb-4 animate-spin" />
          <p class="text-[#286AD8] mb-2">Enviando imagens...</p>
          <p class="text-sm text-[#435574]">Aguarde o upload ser concluído</p>
        </template>
      </div>

      <div v-if="modelValue.length > 0" class="grid grid-cols-3 md:grid-cols-5 gap-3 mt-3">
        <div
          v-for="(url, index) in modelValue"
          :key="`${url}-${index}`"
          class="relative group aspect-video rounded-lg overflow-hidden"
        >
          <img :src="url" :alt="`Preview ${index + 1}`" class="w-full h-full object-cover" />
          <button
            type="button"
            @click.stop="removeImage(index)"
            class="absolute top-2 right-2 bg-destructive text-white p-1.5 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
            :disabled="isDisabled"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
