<script setup lang="ts">
import { ref, watch } from 'vue';
import { X, Upload, Plus, Loader2 } from 'lucide-vue-next';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Textarea from '../ui/textarea.vue';
import Label from '../ui/label.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Badge from '../ui/badge.vue';
import { toast } from '../../utils/toast';
import { uploadImages, deleteImage } from '../../api/uploadService';
import { CATEGORIES } from '@/constants/categories';

export interface ServiceFormData {
  id?: string;
  title: string;
  category: string;
  description: string;
  price: number;
  priceType: 'fixo' | 'hora' | 'dia';
  deliveryTime: string;
  tags: string[];
  images: string[];
  isActive: boolean;
}

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (service: ServiceFormData) => void;
  service?: ServiceFormData | null;
}

const props = defineProps<Props>();

const formData = ref<ServiceFormData>({
  id: props.service?.id,
  title: props.service?.title || '',
  category: props.service?.category || '',
  description: props.service?.description || '',
  price: props.service?.price || 0,
  priceType: props.service?.priceType || 'fixo',
  deliveryTime: props.service?.deliveryTime || '',
  tags: props.service?.tags || [],
  images: props.service?.images || [],
  isActive: props.service?.isActive ?? true,
});

const tagInput = ref('');
const errors = ref<Record<string, string>>({});
const isUploading = ref(false);

const categories = CATEGORIES;

watch(() => props.service, (newService) => {
  if (newService) {
    formData.value = {
      id: newService.id,
      title: newService.title || '',
      category: newService.category || '',
      description: newService.description || '',
      price: newService.price || 0,
      priceType: newService.priceType || 'fixo',
      deliveryTime: newService.deliveryTime || '',
      tags: newService.tags || [],
      images: newService.images || [],
      isActive: newService.isActive ?? true,
    };
    formattedPrice.value = formatPriceDisplay(newService.price || 0);
  }
}, { immediate: true });

watch(() => props.open, (isOpen) => {
  if (!isOpen) {
    handleClose();
  }
});

// Limpar erros quando os campos forem modificados
watch(() => formData.value.category, () => {
  if (errors.value.category) {
    const newErrors = { ...errors.value };
    delete newErrors.category;
    errors.value = newErrors;
  }
});

watch(() => formData.value.description, () => {
  if (errors.value.description) {
    const newErrors = { ...errors.value };
    delete newErrors.description;
    errors.value = newErrors;
  }
});

const handleInputChange = (field: keyof ServiceFormData, value: any) => {
  formData.value = { ...formData.value, [field]: value };
  // Limpar erro do campo ao alterar
  if (errors.value[field]) {
    const newErrors = { ...errors.value };
    delete newErrors[field];
    errors.value = newErrors;
  }
};

const handleAddTag = () => {
  if (tagInput.value.trim() && !formData.value.tags.includes(tagInput.value.trim())) {
    handleInputChange('tags', [...formData.value.tags, tagInput.value.trim()]);
    tagInput.value = '';
  }
};

const handleRemoveTag = (tagToRemove: string) => {
  handleInputChange(
    'tags',
    formData.value.tags.filter((tag) => tag !== tagToRemove)
  );
};

const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault();
    handleAddTag();
  }
};

// Formatação de preço no padrão brasileiro (XX.XXX.XXX,XX)
const formatPriceDisplay = (value: number): string => {
  if (!value) return '';
  const parts = value.toFixed(2).split('.');
  const intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${intPart},${parts[1]}`;
};

const formattedPrice = ref(formatPriceDisplay(formData.value.price));

const handlePriceInput = (val: string) => {
  // Remove tudo que não é dígito
  let digits = val.replace(/\D/g, '');
  
  // Limitar a 10 dígitos (XX.XXX.XXX,XX = 99.999.999,99)
  if (digits.length > 10) {
    digits = digits.slice(0, 10);
  }
  
  if (!digits) {
    formattedPrice.value = '';
    handleInputChange('price', 0);
    return;
  }
  
  // Converter para centavos: últimos 2 dígitos são decimais
  const numValue = parseInt(digits) / 100;
  
  // Formatar para exibição
  const parts = numValue.toFixed(2).split('.');
  const intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  formattedPrice.value = `${intPart},${parts[1]}`;
  
  handleInputChange('price', numValue);
};

// Prazo de entrega em dias (apenas dígitos, max 3 chars = 365 dias)
const handleDeliveryTimeChange = (val: string) => {
  const digits = val.replace(/\D/g, '').slice(0, 3);
  const num = parseInt(digits) || 0;
  
  if (!digits) {
    handleInputChange('deliveryTime', '');
    return;
  }

  // Limite máximo: 365 dias (~12 meses)
  if (num > 365) {
    handleInputChange('deliveryTime', '365');
    return;
  }

  handleInputChange('deliveryTime', digits);
};

const validateForm = () => {
  const newErrors: Record<string, string> = {};

  if (!formData.value.title.trim()) {
    newErrors.title = 'Título é obrigatório';
  } else if (formData.value.title.length > 50) {
    newErrors.title = 'Título deve ter no máximo 50 caracteres';
  }

  if (!formData.value.category) {
    newErrors.category = 'Categoria é obrigatória';
  }

  if (!formData.value.description.trim()) {
    newErrors.description = 'Descrição é obrigatória';
  } else if (formData.value.description.length < 50) {
    newErrors.description = 'Descrição deve ter no mínimo 50 caracteres';
  }

  if (!formData.value.price || formData.value.price <= 0) {
    newErrors.price = 'Preço deve ser maior que zero';
  }

  if (!formData.value.deliveryTime || parseInt(formData.value.deliveryTime) <= 0) {
    newErrors.deliveryTime = 'Prazo de entrega é obrigatório';
  } else if (parseInt(formData.value.deliveryTime) > 365) {
    newErrors.deliveryTime = 'Prazo máximo é de 365 dias (12 meses)';
  }

  errors.value = newErrors;
  return Object.keys(newErrors).length === 0;
};

const handleSubmit = (e: Event) => {
  e.preventDefault();

  if (!validateForm()) {
    toast.error('Por favor, corrija os erros no formulário');
    return;
  }

  props.onSubmit(formData.value);
  handleClose();
};

const handleClose = () => {
  formData.value = {
    id: undefined,
    title: '',
    category: '',
    description: '',
    price: 0,
    priceType: 'fixo',
    deliveryTime: '',
    tags: [],
    images: [],
    isActive: true,
  };
  tagInput.value = '';
  formattedPrice.value = '';
  errors.value = {};
  props.onClose();
};

const handleImageUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  const files = target.files;
  if (files && files.length > 0) {
    // Verificar limite de 5 imagens
    if (formData.value.images.length + files.length > 5) {
      toast.error('Máximo de 5 imagens permitido');
      return;
    }

    // Validar tamanho dos arquivos (5MB max)
    const fileArray = Array.from(files);
    const oversizedFiles = fileArray.filter((file) => file.size > 5 * 1024 * 1024);
    if (oversizedFiles.length > 0) {
      toast.error('Cada imagem deve ter no máximo 5MB');
      return;
    }

    isUploading.value = true;
    try {
      const uploadedUrls = await uploadImages(fileArray);
      handleInputChange('images', [...formData.value.images, ...uploadedUrls]);
      toast.success(`${files.length} imagem(ns) enviada(s) com sucesso`);
    } catch (error) {
      console.error('Erro ao fazer upload:', error);
      toast.error('Erro ao enviar imagem(ns). Tente novamente.');
    } finally {
      isUploading.value = false;
      // Limpar o input para permitir novo upload do mesmo arquivo
      target.value = '';
    }
  }
};

const handleRemoveImage = async (index: number) => {
  const imageUrl = formData.value.images[index];
  
  // Remover da lista local imediatamente
  handleInputChange(
    'images',
    formData.value.images.filter((_, i) => i !== index)
  );

  // Tentar deletar do servidor (não bloqueia a UI)
  if (imageUrl && !imageUrl.startsWith('blob:')) {
    try {
      await deleteImage(imageUrl);
    } catch (error) {
      console.error('Erro ao deletar imagem do servidor:', error);
      // Não mostrar erro ao usuário pois a imagem já foi removida da lista
    }
  }
};
</script>

<template>
  <Dialog :open="open" @update:open="(val) => !val && handleClose()">
    <DialogContent class="max-w-3xl max-h-[90vh] overflow-y-auto">
      <DialogHeader>
        <DialogTitle class="text-[#263238]">
          {{ service?.id ? 'Editar Serviço' : 'Cadastrar Novo Serviço' }}
        </DialogTitle>
      </DialogHeader>

      <form @submit="handleSubmit" class="space-y-6 py-4">
        <!-- Título -->
        <div class="space-y-2">
          <Label for="title">
            Título do Serviço <span class="text-red-500">*</span>
          </Label>
          <Input
            id="title"
            placeholder="Ex: Instalação Elétrica Residencial"
            :model-value="formData.title"
            @update:model-value="(val) => handleInputChange('title', val)"
            :class="errors.title ? 'border-red-500' : ''"
            maxlength="50"
          />
          <div class="flex items-center justify-between">
            <p v-if="errors.title" class="text-sm text-red-500">{{ errors.title }}</p>
            <p class="text-sm text-gray-500 ml-auto">{{ formData.title.length }} / 50 caracteres</p>
          </div>
        </div>

        <!-- Categoria -->
        <div class="space-y-2">
          <Label for="category">
            Categoria <span class="text-red-500">*</span>
          </Label>
          <Select
            v-model="formData.category"
          >
            <SelectTrigger :class="errors.category ? 'border-red-500' : ''">
              <SelectValue placeholder="Selecione uma categoria" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.category" class="text-sm text-red-500">{{ errors.category }}</p>
        </div>

        <!-- Descrição -->
        <div class="space-y-2">
          <Label for="description">
            Descrição <span class="text-red-500">*</span>
          </Label>
          <Textarea
            id="description"
            placeholder="Descreva em detalhes o serviço que você oferece..."
            v-model="formData.description"
            :rows="5"
            :class="errors.description ? 'border-red-500' : ''"
          />
          <div class="flex items-center justify-between">
            <p v-if="errors.description" class="text-sm text-red-500">{{ errors.description }}</p>
            <p class="text-sm text-gray-500 ml-auto">
              {{ formData.description.length }} / 500 caracteres
            </p>
          </div>
        </div>

        <!-- Preço e Tipo -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="space-y-2">
            <Label for="price">
              Preço <span class="text-red-500">*</span>
            </Label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500">
                R$
              </span>
              <Input
                id="price"
                type="text"
                inputmode="numeric"
                maxlength="13"
                placeholder="0,00"
                :model-value="formattedPrice"
                @update:model-value="(val: string) => handlePriceInput(val)"
                :class="`pl-10 ${errors.price ? 'border-red-500' : ''}`"
              />
            </div>
            <p v-if="errors.price" class="text-sm text-red-500">{{ errors.price }}</p>
          </div>

          <div class="space-y-2">
            <Label for="priceType">Tipo de Cobrança</Label>
            <Select
              v-model="formData.priceType"
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="fixo">Preço Fixo</SelectItem>
                <SelectItem value="hora">Por Hora</SelectItem>
                <SelectItem value="dia">Por Dia</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <!-- Prazo de Entrega -->
        <div class="space-y-2">
          <Label for="deliveryTime">
            Prazo de Entrega (em dias) <span class="text-red-500">*</span>
          </Label>
          <Input
            id="deliveryTime"
            type="text"
            inputmode="numeric"
            maxlength="3"
            placeholder="Ex: 7"
            :model-value="formData.deliveryTime"
            @update:model-value="(val: string) => handleDeliveryTimeChange(val)"
            :class="errors.deliveryTime ? 'border-red-500' : ''"
          />
          <p v-if="errors.deliveryTime" class="text-sm text-red-500">{{ errors.deliveryTime }}</p>
          <p v-if="formData.deliveryTime && parseInt(formData.deliveryTime) > 0" class="text-sm text-gray-500">
            ≈ {{ parseInt(formData.deliveryTime) >= 30 
              ? `${Math.floor(parseInt(formData.deliveryTime) / 30)} ${Math.floor(parseInt(formData.deliveryTime) / 30) === 1 ? 'mês' : 'meses'}` 
              : parseInt(formData.deliveryTime) >= 7 
                ? `${Math.floor(parseInt(formData.deliveryTime) / 7)} ${Math.floor(parseInt(formData.deliveryTime) / 7) === 1 ? 'semana' : 'semanas'}` 
                : `${formData.deliveryTime} ${parseInt(formData.deliveryTime) === 1 ? 'dia' : 'dias'}` 
            }}
          </p>
        </div>

        <!-- Tags/Habilidades -->
        <div class="space-y-2">
          <Label for="tags">
            Tags / Habilidades <span class="text-gray-400">(opcional)</span>
          </Label>
          <div class="flex gap-2">
            <Input
              id="tags"
              placeholder="Ex: Instalação, Manutenção, Emergência..."
              :model-value="tagInput"
              @update:model-value="(val: string) => tagInput = val"
              @keypress="handleKeyPress"
            />
            <Button type="button" @click="handleAddTag" variant="outline" size="icon">
              <Plus class="h-4 w-4" />
            </Button>
          </div>
          <div v-if="formData.tags.length > 0" class="flex flex-wrap gap-2 mt-2">
            <Badge
              v-for="tag in formData.tags"
              :key="tag"
              variant="secondary"
              class="pl-3 pr-1 py-1 gap-1"
            >
              {{ tag }}
              <button
                type="button"
                @click="handleRemoveTag(tag)"
                class="ml-1 hover:bg-gray-300 rounded-full p-0.5"
              >
                <X class="h-3 w-3" />
              </button>
            </Badge>
          </div>
        </div>

        <!-- Upload de Imagens -->
        <div class="space-y-2">
          <Label for="images">
            Imagens do Serviço <span class="text-gray-400">(opcional)</span>
          </Label>
          <div 
            class="border-2 border-dashed rounded-lg p-6 transition-colors"
            :class="isUploading ? 'border-[#286AD8] bg-blue-50' : 'border-gray-300 hover:border-[#286AD8]'"
          >
            <label
              v-if="!isUploading"
              for="images"
              class="flex flex-col items-center justify-center cursor-pointer"
            >
              <Upload class="h-10 w-10 text-gray-400 mb-2" />
              <p class="text-sm text-gray-600 text-center">
                Clique para fazer upload ou arraste as imagens aqui
              </p>
              <p class="text-xs text-gray-400 mt-1">
                PNG, JPG até 5MB (máximo 5 imagens)
              </p>
              <input
                id="images"
                type="file"
                accept="image/*"
                multiple
                class="hidden"
                @change="handleImageUpload"
                :disabled="isUploading"
              />
            </label>
            <div v-else class="flex flex-col items-center justify-center">
              <Loader2 class="h-10 w-10 text-[#286AD8] mb-2 animate-spin" />
              <p class="text-sm text-[#286AD8] text-center">
                Enviando imagens...
              </p>
            </div>
          </div>

          <div v-if="formData.images.length > 0" class="grid grid-cols-3 md:grid-cols-5 gap-3 mt-3">
            <div
              v-for="(image, index) in formData.images"
              :key="index"
              class="relative group"
            >
              <img
                :src="image"
                :alt="`Preview ${index + 1}`"
                class="w-full h-24 object-cover rounded-lg"
              />
              <button
                type="button"
                @click="handleRemoveImage(index)"
                class="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <X class="h-3 w-3" />
              </button>
            </div>
          </div>
        </div>

        <!-- Buttons -->
        <div class="flex justify-end gap-3 pt-4 border-t">
          <Button type="button" variant="outline" @click="handleClose">
            Cancelar
          </Button>
          <Button type="submit" class="bg-[#286AD8] hover:bg-[#435574]">
            {{ service?.id ? 'Salvar Alterações' : 'Cadastrar Serviço' }}
          </Button>
        </div>
      </form>
    </DialogContent>
  </Dialog>
</template>

