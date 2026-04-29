<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
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
import ImageUploadField from '../shared/ImageUploadField.vue';
import { toast } from '../../utils/toast';
import { uploadImages, deleteImage } from '../../api/uploadService';
import { useCategoryCatalogStore } from '@/stores/categoryCatalogStore';

export interface ServiceFormData {
  id?: string;
  categoryId?: number | null;
  subcategoryId?: number | null;
  title: string;
  category: string;
  subcategory?: string;
  description: string;
  price: number;
  priceType: 'fixo' | 'hora' | 'dia';
  workModel: 'presencial' | 'remoto' | 'hibrido';
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
const categoryCatalogStore = useCategoryCatalogStore();

const WORK_MODELS = [
  { value: 'presencial', label: 'Presencial' },
  { value: 'remoto', label: 'Remoto' },
  { value: 'hibrido', label: 'Híbrido' },
] as const;

const formData = ref<ServiceFormData>({
  id: props.service?.id,
  categoryId: props.service?.categoryId ?? null,
  subcategoryId: props.service?.subcategoryId ?? null,
  title: props.service?.title || '',
  category: props.service?.category || '',
  subcategory: props.service?.subcategory || '',
  description: props.service?.description || '',
  price: props.service?.price || 0,
  priceType: props.service?.priceType || 'fixo',
  workModel: props.service?.workModel || 'presencial',
  deliveryTime: props.service?.deliveryTime || '',
  tags: props.service?.tags || [],
  images: props.service?.images || [],
  isActive: props.service?.isActive ?? true,
});

const tagInput = ref('');
const errors = ref<Record<string, string>>({});
const isUploading = ref(false);
const isDragOver = ref(false);
const dragCounter = ref(0);

const categories = computed(() => categoryCatalogStore.activePublicCategories);
const selectedSubcategories = computed(() =>
  categoryCatalogStore.getSubcategoriesByCategoryId(formData.value.categoryId || null)
);
const selectedCategoryLabel = computed(
  () => categories.value.find((category) => category.id === formData.value.categoryId)?.name || ''
);
const selectedSubcategoryLabel = computed(
  () => selectedSubcategories.value.find((subcategory) => subcategory.id === formData.value.subcategoryId)?.name || ''
);

watch(() => props.service, (newService) => {
  if (newService) {
    formData.value = {
      id: newService.id,
      categoryId: newService.categoryId ?? null,
      subcategoryId: newService.subcategoryId ?? null,
      title: newService.title || '',
      category: newService.category || '',
      subcategory: newService.subcategory || '',
      description: newService.description || '',
      price: newService.price || 0,
      priceType: newService.priceType || 'fixo',
      workModel: newService.workModel || 'presencial',
      deliveryTime: newService.deliveryTime || '',
      tags: newService.tags || [],
      images: newService.images || [],
      isActive: newService.isActive ?? true,
    };
    formattedPrice.value = formatPriceDisplay(newService.price || 0);
  }
}, { immediate: true });

onMounted(() => {
  categoryCatalogStore.loadPublicCategories();
});

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

watch(() => formData.value.categoryId, (categoryId) => {
  const selectedCategory = categories.value.find((category) => category.id === categoryId);
  formData.value.category = selectedCategory?.name || '';

  const subcategoryStillValid = selectedSubcategories.value.some(
    (subcategory) => subcategory.id === formData.value.subcategoryId
  );

  if (!subcategoryStillValid) {
    formData.value.subcategoryId = null;
    formData.value.subcategory = '';
  }

  if (errors.value.category) {
    const newErrors = { ...errors.value };
    delete newErrors.category;
    errors.value = newErrors;
  }
});

watch(() => formData.value.subcategoryId, (subcategoryId) => {
  const selectedSubcategory = selectedSubcategories.value.find((subcategory) => subcategory.id === subcategoryId);
  formData.value.subcategory = selectedSubcategory?.name || '';

  if (errors.value.subcategory) {
    const newErrors = { ...errors.value };
    delete newErrors.subcategory;
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

  if (!formData.value.categoryId) {
    newErrors.category = 'Categoria é obrigatória';
  }

  if (selectedSubcategories.value.length > 0 && !formData.value.subcategoryId) {
    newErrors.subcategory = 'Subcategoria é obrigatória';
  }

  if (!formData.value.description.trim()) {
    newErrors.description = 'Descrição é obrigatória';
  } else if (formData.value.description.length < 50) {
    newErrors.description = 'Descrição deve ter no mínimo 50 caracteres';
  }

  if (!formData.value.price || formData.value.price <= 0) {
    newErrors.price = 'Preço deve ser maior que zero';
  }

  if (!formData.value.workModel) {
    newErrors.workModel = 'Modelo de trabalho é obrigatório';
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

  const selectedCategory = categories.value.find((category) => category.id === formData.value.categoryId);
  const selectedSubcategory = selectedSubcategories.value.find(
    (subcategory) => subcategory.id === formData.value.subcategoryId
  );

  props.onSubmit({
    ...formData.value,
    category: selectedCategory?.name || formData.value.category,
    subcategory: selectedSubcategory?.name || formData.value.subcategory,
  });
  handleClose();
};

const handleClose = () => {
  formData.value = {
    id: undefined,
    categoryId: null,
    subcategoryId: null,
    title: '',
    category: '',
    subcategory: '',
    description: '',
    price: 0,
    priceType: 'fixo',
    workModel: 'presencial',
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

const uploadSelectedImages = async (files: File[]) => {
  if (files.length === 0 || isUploading.value) {
    return;
  }

  if (formData.value.images.length + files.length > 5) {
    toast.error('Máximo de 5 imagens permitido');
    return;
  }

  const invalidFiles = files.filter((file) => !file.type.startsWith('image/'));
  if (invalidFiles.length > 0) {
    toast.error('Envie apenas arquivos de imagem');
    return;
  }

  const oversizedFiles = files.filter((file) => file.size > 10 * 1024 * 1024);
  if (oversizedFiles.length > 0) {
    toast.error('Cada imagem deve ter no máximo 10MB');
    return;
  }

  isUploading.value = true;
  try {
    const uploadedUrls = await uploadImages(files);
    handleInputChange('images', [...formData.value.images, ...uploadedUrls]);
    toast.success(`${files.length} imagem(ns) enviada(s) com sucesso`);
  } catch (error) {
    console.error('Erro ao fazer upload:', error);
    toast.error('Erro ao enviar imagem(ns). Tente novamente.');
  } finally {
    isUploading.value = false;
  }
};

const hasDraggedFiles = (e: DragEvent) => {
  return Array.from(e.dataTransfer?.types || []).includes('Files');
};

const handleDragEnter = (e: DragEvent) => {
  if (isUploading.value || !hasDraggedFiles(e)) {
    return;
  }

  dragCounter.value += 1;
  isDragOver.value = true;
};

const handleRemovedImage = async (imageUrl: string) => {
  if (imageUrl && !imageUrl.startsWith('blob:')) {
    try {
      await deleteImage(imageUrl);
    } catch (error) {
      console.error('Erro ao deletar imagem do servidor:', error);
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
          <div class="relative">
            <Input
              id="title"
              placeholder="Ex: Instalação Elétrica Residencial"
              :model-value="formData.title"
              @update:model-value="(val) => handleInputChange('title', val)"
              :class="['h-14 pr-28', errors.title ? 'border-red-500' : '']"
              maxlength="50"
            />
            <span class="pointer-events-none absolute bottom-1.5 right-3 text-[11px] text-gray-400">
              {{ formData.title.length }}/50
            </span>
          </div>
          <p v-if="errors.title" class="text-sm text-red-500">{{ errors.title }}</p>
        </div>

        <!-- Categoria -->
        <div class="space-y-2">
          <Label for="category">
            Categoria <span class="text-red-500">*</span>
          </Label>
          <Select
            :model-value="formData.categoryId ? String(formData.categoryId) : ''"
            @update:model-value="(value) => handleInputChange('categoryId', value ? Number(value) : null)"
          >
            <SelectTrigger :class="errors.category ? 'border-red-500' : ''">
              <SelectValue
                :label="selectedCategoryLabel || undefined"
                placeholder="Selecione uma categoria"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="cat in categories" :key="cat.id" :value="String(cat.id)">
                {{ cat.name }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.category" class="text-sm text-red-500">{{ errors.category }}</p>
        </div>

        <div class="space-y-2">
          <Label for="subcategory">
            Subcategoria
            <span v-if="selectedSubcategories.length > 0" class="text-red-500">*</span>
          </Label>
          <Select
            :model-value="formData.subcategoryId ? String(formData.subcategoryId) : ''"
            @update:model-value="(value) => handleInputChange('subcategoryId', value ? Number(value) : null)"
            :disabled="!formData.categoryId || selectedSubcategories.length === 0"
          >
            <SelectTrigger :class="errors.subcategory ? 'border-red-500' : ''">
              <SelectValue
                :label="selectedSubcategoryLabel || undefined"
                :placeholder="selectedSubcategories.length > 0 ? 'Selecione uma subcategoria' : 'Escolha uma categoria primeiro'"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="subcategory in selectedSubcategories" :key="subcategory.id" :value="String(subcategory.id)">
                {{ subcategory.name }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.subcategory" class="text-sm text-red-500">{{ errors.subcategory }}</p>
        </div>

        <!-- Descrição -->
        <div class="space-y-2">
          <Label for="description">
            Descrição <span class="text-red-500">*</span>
          </Label>
          <div class="relative">
            <Textarea
              id="description"
              placeholder="Descreva em detalhes o serviço que você oferece..."
              v-model="formData.description"
              :rows="5"
              maxlength="500"
              :class="['resize-none pb-8 pr-24', errors.description ? 'border-red-500' : '']"
            />
            <span class="pointer-events-none absolute bottom-2 right-3 text-[11px] text-gray-400">
              {{ formData.description.length }}/500
            </span>
          </div>
          <p v-if="errors.description" class="text-sm text-red-500">{{ errors.description }}</p>
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
          <Label for="workModel">
            Modelo de Trabalho <span class="text-red-500">*</span>
          </Label>
          <Select v-model="formData.workModel">
            <SelectTrigger :class="errors.workModel ? 'border-red-500' : ''">
              <SelectValue placeholder="Selecione o modelo" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="model in WORK_MODELS" :key="model.value" :value="model.value">
                {{ model.label }}
              </SelectItem>
            </SelectContent>
          </Select>
          <p v-if="errors.workModel" class="text-sm text-red-500">{{ errors.workModel }}</p>

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
        <ImageUploadField
          label="Imagens do Serviço (opcional)"
          helper-text="PNG, JPG até 10MB (máximo 5 imagens)"
          :max-files="5"
          :model-value="formData.images"
          @update:model-value="(urls) => handleInputChange('images', urls)"
          @image-removed="handleRemovedImage"
        />

        <!-- Buttons -->
        <div class="flex justify-end gap-3 pt-4 border-t">
          <Button type="button" variant="outline" @click="handleClose">
            Cancelar
          </Button>
          <Button type="submit">
            {{ service?.id ? 'Salvar Alterações' : 'Cadastrar Serviço' }}
          </Button>
        </div>
      </form>
    </DialogContent>
  </Dialog>
</template>
