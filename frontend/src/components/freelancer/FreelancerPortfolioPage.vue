<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import {
  Plus,
  Edit,
  Trash2,
  Eye,
  Image as ImageIcon,
  Video,
  ExternalLink,
  Calendar,
  Tag,
  X,
  Upload,
  Search,
  Filter,
  Grid3x3,
  List,
  Heart,
  MessageSquare,
  Share2,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import { toast } from '../../utils/toast';
import api from '@/api/config/axios';
import { uploadImages } from '@/api/uploadService';

interface PortfolioImage {
  id: string;
  url: string;
  caption?: string;
}

interface PortfolioProject {
  id: string;
  title: string;
  description: string;
  category: string;
  tags: string[];
  images: PortfolioImage[];
  videoUrl?: string;
  projectUrl?: string;
  completedDate: string;
  client?: string;
  featured: boolean;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const CATEGORIES = [
  'Todos',
  'Instalação Elétrica',
  'Automação',
  'Manutenção',
  'Projeto Elétrico',
  'Painéis Solares',
  'Iluminação',
  'Outros',
];

const viewMode = ref<'grid' | 'list'>('grid');
const selectedCategory = ref('Todos');
const searchQuery = ref('');
const showAddDialog = ref(false);
const showViewDialog = ref(false);
const showDeleteDialog = ref(false);
const editingProject = ref<PortfolioProject | null>(null);
const viewingProject = ref<PortfolioProject | null>(null);
const projectToDelete = ref<PortfolioProject | null>(null);
const selectedImageIndex = ref(0);

// Form state
const projectForm = ref({
  title: '',
  description: '',
  category: '',
  tags: [] as string[],
  images: [] as File[],
  imageUrls: [] as string[],
  videoUrl: '',
  projectUrl: '',
  completedDate: '',
  client: '',
  featured: false,
});

const tagInput = ref('');
const isLoading = ref(false);
const isUploading = ref(false);

// Carregar projetos do backend
onMounted(async () => {
  await loadProjects();
});

const loadProjects = async () => {
  isLoading.value = true;
  try {
    const response = await api.get('/freelancer/portfolio');
    projects.value = response.data.map((project: any) => ({
      id: project.id.toString(),
      title: project.title,
      description: project.description,
      category: project.category,
      tags: project.tags || [],
      images: project.images?.map((img: any) => ({
        id: img.id.toString(),
        url: img.imageUrl,
        caption: img.caption,
      })) || [],
      videoUrl: project.videoUrl,
      projectUrl: project.projectUrl,
      completedDate: project.completedDate,
      client: project.client,
      featured: project.featured,
    }));
  } catch (error) {
    console.error('Erro ao carregar projetos:', error);
    toast.error('Erro ao carregar projetos');
  } finally {
    isLoading.value = false;
  }
};

// Mock data - será substituído por dados reais
const projects = ref<PortfolioProject[]>([]);

// Filter projects
const filteredProjects = computed(() => {
  let filtered = projects.value;
  if (selectedCategory.value !== 'Todos') {
    filtered = filtered.filter((p) => p.category === selectedCategory.value);
  }
  if (searchQuery.value) {
    filtered = filtered.filter(
      (p) =>
        p.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        p.description.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        p.tags.some((tag) => tag.toLowerCase().includes(searchQuery.value.toLowerCase()))
    );
  }
  return filtered;
});

const stats = computed(() => ({
  total: projects.value.length,
  featured: projects.value.filter((p) => p.featured).length,
}));

const handleAddTag = () => {
  if (tagInput.value.trim() && !projectForm.value.tags.includes(tagInput.value.trim())) {
    projectForm.value = {
      ...projectForm.value,
      tags: [...projectForm.value.tags, tagInput.value.trim()],
    };
    tagInput.value = '';
  }
};

const handleRemoveTag = (tag: string) => {
  projectForm.value = {
    ...projectForm.value,
    tags: projectForm.value.tags.filter((t) => t !== tag),
  };
};

const handleImageUploadClick = () => {
  const input = document.getElementById('image-upload') as HTMLInputElement;
  if (input) {
    input.click();
  }
};

const handleImageUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    // Validar tamanho dos arquivos (10MB max)
    const files = Array.from(target.files);
    const oversizedFiles = files.filter((file) => file.size > 10 * 1024 * 1024);
    if (oversizedFiles.length > 0) {
      toast.error('Cada imagem deve ter no máximo 10MB');
      target.value = ''; // Limpar input
      return;
    }

    isUploading.value = true;
    try {
      const uploadedUrls = await uploadImages(files);
      projectForm.value = {
        ...projectForm.value,
        images: [], // Não precisamos mais dos Files
        imageUrls: [...projectForm.value.imageUrls, ...uploadedUrls],
      };
      toast.success(`${files.length} imagem(ns) enviada(s) com sucesso`);
    } catch (error) {
      console.error('Erro ao fazer upload:', error);
      toast.error('Erro ao enviar imagem(ns). Tente novamente.');
    } finally {
      isUploading.value = false;
      target.value = ''; // Limpar input
    }
  }
};

const handleRemoveImage = (index: number) => {
  const newUrls = projectForm.value.imageUrls.filter((_, i) => i !== index);
  projectForm.value = { ...projectForm.value, imageUrls: newUrls };
};

const handleSaveProject = async () => {
  if (!projectForm.value.title || !projectForm.value.description || !projectForm.value.category) {
    toast.error('Preencha todos os campos obrigatórios');
    return;
  }

  if (projectForm.value.imageUrls.length === 0) {
    toast.error('Adicione pelo menos uma imagem ao projeto');
    return;
  }

  isLoading.value = true;

  try {
    const payload = {
      title: projectForm.value.title,
      description: projectForm.value.description,
      category: projectForm.value.category,
      tags: projectForm.value.tags,
      videoUrl: projectForm.value.videoUrl || null,
      projectUrl: projectForm.value.projectUrl || null,
      completedDate: projectForm.value.completedDate || null,
      client: projectForm.value.client || null,
      featured: projectForm.value.featured,
      images: projectForm.value.imageUrls.map((url, idx) => ({
        imageUrl: url,
        caption: '',
        displayOrder: idx,
      })),
    };

    if (editingProject.value) {
      // Update existing project
      console.log('Atualizando projeto:', editingProject.value.id, payload);
      await api.put(`/freelancer/portfolio/${editingProject.value.id}`, payload);
      toast.success('Projeto atualizado com sucesso!');
    } else {
      // Add new project
      console.log('Criando novo projeto:', payload);
      await api.post('/freelancer/portfolio', payload);
      toast.success('Projeto adicionado com sucesso!');
    }

    await loadProjects();
    resetForm();
    showAddDialog.value = false;
  } catch (error: any) {
    console.error('Erro ao salvar projeto:', error);
    console.error('Resposta do servidor:', error.response?.data);
    const errorMessage = error.response?.data?.message || 'Erro ao salvar projeto. Tente novamente.';
    toast.error(errorMessage);
  } finally {
    isLoading.value = false;
  }
};

const resetForm = () => {
  projectForm.value = {
    title: '',
    description: '',
    category: '',
    tags: [],
    images: [],
    imageUrls: [],
    videoUrl: '',
    projectUrl: '',
    completedDate: '',
    client: '',
    featured: false,
  };
  editingProject.value = null;
};

const handleEditProject = (project: PortfolioProject) => {
  editingProject.value = project;
  projectForm.value = {
    title: project.title,
    description: project.description,
    category: project.category,
    tags: project.tags,
    images: [],
    imageUrls: project.images.map((img) => img.url),
    videoUrl: project.videoUrl || '',
    projectUrl: project.projectUrl || '',
    completedDate: project.completedDate,
    client: project.client || '',
    featured: project.featured,
  };
  showAddDialog.value = true;
};

const handleDeleteProject = async () => {
  if (projectToDelete.value) {
    isLoading.value = true;
    try {
      await api.delete(`/freelancer/portfolio/${projectToDelete.value.id}`);
      toast.success('Projeto excluído com sucesso');
      await loadProjects();
      projectToDelete.value = null;
      showDeleteDialog.value = false;
    } catch (error) {
      console.error('Erro ao excluir projeto:', error);
      toast.error('Erro ao excluir projeto');
    } finally {
      isLoading.value = false;
    }
  }
};

const handleViewProject = (project: PortfolioProject) => {
  viewingProject.value = project;
  selectedImageIndex.value = 0;
  showViewDialog.value = true;
};

const handleTagKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    handleAddTag();
  }
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Meu Portfólio</h1>
        <p class="text-[#435574]">Gerencie seus projetos e trabalhos realizados</p>
      </div>
      <Button
        @click="
          resetForm();
          showAddDialog = true;
        "
        variant="default"
      >
        <Plus class="h-4 w-4 mr-2" />
        Novo Projeto
      </Button>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="flex items-center justify-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-[#286AD8]"></div>
    </div>

    <!-- Content (only show when not loading) -->
    <template v-else>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
            <Grid3x3 class="h-5 w-5 text-blue-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Total de Projetos</p>
            <p class="text-xl text-[#263238]">{{ stats.total }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-yellow-100 flex items-center justify-center">
            <Tag class="h-5 w-5 text-yellow-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Projetos em Destaque</p>
            <p class="text-xl text-[#263238]">{{ stats.featured }}</p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Filters and Search -->
    <Card class="p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
          <Input
            placeholder="Buscar projetos, tags..."
            :model-value="searchQuery"
            @update:model-value="(val) => searchQuery = val as string"
            class="pl-10"
          />
        </div>

        <div class="flex gap-2">
          <Select
            :model-value="selectedCategory"
            @update:model-value="(val: string) => selectedCategory = val"
          >
            <SelectTrigger class="w-[200px]">
              <SelectValue placeholder="Categoria" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem v-for="cat in CATEGORIES" :key="cat" :value="cat">
                {{ cat }}
              </SelectItem>
            </SelectContent>
          </Select>

          <div class="hidden sm:flex border rounded-lg">
            <Button
              @click="viewMode = 'grid'"
              variant="ghost"
              size="icon"
              :class="viewMode === 'grid' ? 'bg-gray-100' : ''"
            >
              <Grid3x3 class="h-5 w-5" />
            </Button>
            <Button
              @click="viewMode = 'list'"
              variant="ghost"
              size="icon"
              :class="viewMode === 'list' ? 'bg-gray-100' : ''"
            >
              <List class="h-5 w-5" />
            </Button>
          </div>
        </div>
      </div>
    </Card>

    <!-- Projects Grid/List -->
    <Card v-if="filteredProjects.length === 0" class="p-12 text-center">
      <ImageIcon class="h-12 w-12 text-gray-400 mx-auto mb-4" />
      <h3 class="text-[#263238] mb-2">Nenhum projeto encontrado</h3>
      <p class="text-[#435574] mb-4">
        {{ searchQuery || selectedCategory !== 'Todos'
          ? 'Tente ajustar os filtros'
          : 'Comece adicionando seu primeiro projeto' }}
      </p>
      <Button
        v-if="!searchQuery && selectedCategory === 'Todos'"
        @click="
          resetForm();
          showAddDialog = true;
        "
        variant="default"
      >
        <Plus class="h-4 w-4 mr-2" />
        Adicionar Projeto
      </Button>
    </Card>
    <div v-else-if="viewMode === 'grid'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <Card
        v-for="project in filteredProjects"
        :key="project.id"
        class="overflow-hidden group hover:shadow-lg transition-shadow"
      >
        <!-- Project Image -->
        <div class="relative aspect-video overflow-hidden bg-gray-100">
          <img
            :src="project.images[0]?.url"
            :alt="project.title"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          <Badge v-if="project.featured" class="absolute top-3 left-3 bg-yellow-500 hover:bg-yellow-600">
            <Tag class="h-3 w-3 mr-1" />
            Destaque
          </Badge>
          <div class="absolute top-3 right-3 flex gap-2">
            <Badge variant="secondary" class="bg-white/90">
              <ImageIcon class="h-3 w-3 mr-1" />
              {{ project.images.length }}
            </Badge>
          </div>
          <div class="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
            <Button @click="handleViewProject(project)" size="icon" variant="secondary" class="bg-white hover:bg-gray-100">
              <Eye class="h-4 w-4" />
            </Button>
            <Button @click="handleEditProject(project)" size="icon" variant="secondary" class="bg-white hover:bg-gray-100">
              <Edit class="h-4 w-4" />
            </Button>
            <Button
              @click="
                projectToDelete = project;
                showDeleteDialog = true;
              "
              size="icon"
              class="bg-white text-red-600 hover:bg-red-50"
            >
              <Trash2 class="h-4 w-4" />
            </Button>
          </div>
        </div>

        <!-- Project Info -->
        <div class="p-4">
          <div class="flex items-start justify-between mb-2">
            <h3 class="text-[#263238] line-clamp-2">{{ project.title }}</h3>
          </div>

          <p
            class="text-sm text-[#435574] mb-3"
            style="display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; word-break: break-word;"
          >{{ project.description }}</p>

          <div class="flex flex-wrap gap-1 mb-3">
            <Badge
              v-for="tag in project.tags.slice(0, 3)"
              :key="tag"
              variant="secondary"
              class="text-xs"
            >
              {{ tag }}
            </Badge>
            <Badge v-if="project.tags.length > 3" variant="secondary" class="text-xs">
              +{{ project.tags.length - 3 }}
            </Badge>
          </div>

          <div class="flex items-center justify-between text-sm text-[#435574] pt-3 border-t">
            <span class="flex items-center gap-1">
              <ImageIcon class="h-4 w-4" />
              {{ project.images.length }} {{ project.images.length === 1 ? 'imagem' : 'imagens' }}
            </span>
            <span class="flex items-center gap-1">
              <Calendar class="h-4 w-4" />
              {{ new Date(project.completedDate).toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' }) }}
            </span>
          </div>
        </div>
      </Card>
    </div>
    <div v-else class="space-y-4">
      <Card
        v-for="project in filteredProjects"
        :key="project.id"
        class="p-6 hover:shadow-lg transition-shadow"
      >
        <div class="flex gap-6">
          <!-- Thumbnail -->
          <div class="w-48 h-32 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0">
            <img :src="project.images[0]?.url" :alt="project.title" class="w-full h-full object-cover" />
          </div>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between mb-2">
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-1">
                  <h3 class="text-[#263238] truncate">{{ project.title }}</h3>
                  <Badge v-if="project.featured" class="bg-yellow-500 hover:bg-yellow-600">
                    <Tag class="h-3 w-3 mr-1" />
                    Destaque
                  </Badge>
                </div>
                <p
                  class="text-sm text-[#435574] mb-2"
                  style="display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; word-break: break-word;"
                >{{ project.description }}</p>
                <div class="flex flex-wrap gap-1">
                  <Badge v-for="tag in project.tags" :key="tag" variant="secondary" class="text-xs">
                    {{ tag }}
                  </Badge>
                </div>
              </div>

              <div class="flex gap-2">
                <Button @click="handleViewProject(project)" size="icon" variant="ghost">
                  <Eye class="h-4 w-4" />
                </Button>
                <Button @click="handleEditProject(project)" size="icon" variant="ghost">
                  <Edit class="h-4 w-4" />
                </Button>
                <Button
                  @click="
                    projectToDelete = project;
                    showDeleteDialog = true;
                  "
                  size="icon"
                  variant="ghost"
                  class="text-red-600 hover:text-red-700 hover:bg-red-50"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </div>

            <div class="flex items-center gap-6 text-sm text-[#435574] mt-4">
              <span class="flex items-center gap-1">
                <ImageIcon class="h-4 w-4" />
                {{ project.images.length }} {{ project.images.length === 1 ? 'imagem' : 'imagens' }}
              </span>
              <span class="flex items-center gap-1">
                <Calendar class="h-4 w-4" />
                {{ new Date(project.completedDate).toLocaleDateString('pt-BR') }}
              </span>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <!-- Add/Edit Project Dialog -->
    <Dialog :open="showAddDialog" @update:open="(val: boolean) => (showAddDialog = val)">
      <DialogContent class="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{{ editingProject ? 'Editar Projeto' : 'Adicionar Novo Projeto' }}</DialogTitle>
          <DialogDescription>
            Adicione fotos e informações sobre seus trabalhos realizados
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <!-- Title -->
          <div class="space-y-2">
            <Label for="title">Título do Projeto *</Label>
            <Input
              id="title"
              :model-value="projectForm.title"
              @update:model-value="(val) => projectForm.title = val as string"
              placeholder="Ex: Instalação Elétrica Residencial"
            />
          </div>

          <!-- Description -->
          <div class="space-y-2">
            <Label for="description">Descrição *</Label>
            <Textarea
              id="description"
              :model-value="projectForm.description"
              @update:model-value="(val: string) => projectForm.description = val"
              placeholder="Descreva os detalhes do projeto..."
              :rows="4"
            />
          </div>

          <!-- Category and Date -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="category">Categoria *</Label>
              <Select
                :model-value="projectForm.category"
                @update:model-value="(val: string) => projectForm.category = val"
              >
                <SelectTrigger>
                  <SelectValue placeholder="Selecione a categoria" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem
                    v-for="cat in CATEGORIES.filter((c) => c !== 'Todos')"
                    :key="cat"
                    :value="cat"
                  >
                    {{ cat }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div class="space-y-2">
              <Label for="completedDate">Data de Conclusão</Label>
              <Input
                id="completedDate"
                type="date"
                :model-value="projectForm.completedDate"
                @update:model-value="(val) => projectForm.completedDate = val as string"
              />
            </div>
          </div>

          <!-- Client -->
          <div class="space-y-2">
            <Label for="client">Cliente (opcional)</Label>
            <Input
              id="client"
              :model-value="projectForm.client"
              @update:model-value="(val) => projectForm.client = val as string"
              placeholder="Nome do cliente"
            />
          </div>

          <!-- Tags -->
          <div class="space-y-2">
            <Label>Tags</Label>
            <div class="flex gap-2">
              <Input
                :model-value="tagInput"
                @update:model-value="(val) => tagInput = val as string"
                @keypress="handleTagKeyPress"
                placeholder="Digite uma tag e pressione Enter"
              />
              <Button @click="handleAddTag" type="button" variant="outline">
                Adicionar
              </Button>
            </div>
            <div v-if="projectForm.tags.length > 0" class="flex flex-wrap gap-2 mt-2">
              <Badge v-for="tag in projectForm.tags" :key="tag" variant="secondary" class="pl-3 pr-1">
                {{ tag }}
                <button
                  @click="handleRemoveTag(tag)"
                  class="ml-2 hover:bg-gray-300 rounded-full p-0.5"
                >
                  <X class="h-3 w-3" />
                </button>
              </Badge>
            </div>
          </div>

          <!-- Images -->
          <div class="space-y-2">
            <Label>Imagens do Projeto *</Label>
            <div 
              class="border-2 border-dashed rounded-lg p-8 text-center transition-colors"
              :class="isUploading ? 'border-[#286AD8] bg-blue-50' : 'border-gray-300 hover:border-[#286AD8] cursor-pointer'"
            >
              <template v-if="!isUploading">
                <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p class="text-[#263238] mb-2">Clique para fazer upload ou arraste as imagens</p>
                <p class="text-sm text-[#435574] mb-4">PNG, JPG até 10MB cada</p>
                <input
                  type="file"
                  multiple
                  accept="image/*"
                  @change="handleImageUpload"
                  class="hidden"
                  id="image-upload"
                  :disabled="isUploading"
                />
                <Button @click="handleImageUploadClick" size="sm">
                  Selecionar Imagens
                </Button>
              </template>
              <template v-else>
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-[#286AD8] mx-auto mb-4"></div>
                <p class="text-[#286AD8] mb-2">Enviando imagens...</p>
                <p class="text-sm text-[#435574]">Aguarde o upload ser concluído</p>
              </template>
            </div>

            <div v-if="projectForm.imageUrls.length > 0" class="grid grid-cols-3 gap-4 mt-4">
              <div
                v-for="(url, index) in projectForm.imageUrls"
                :key="index"
                class="relative group aspect-video rounded-lg overflow-hidden"
              >
                <img :src="url" :alt="`Preview ${index + 1}`" class="w-full h-full object-cover" />
                <button
                  @click="handleRemoveImage(index)"
                  class="absolute top-2 right-2 bg-destructive text-white p-1.5 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>

          <!-- Optional Links -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="videoUrl">URL do Vídeo (opcional)</Label>
              <Input
                id="videoUrl"
                :model-value="projectForm.videoUrl"
                @update:model-value="(val: string) => projectForm.videoUrl = val"
                placeholder="https://youtube.com/..."
              />
            </div>

            <div class="space-y-2">
              <Label for="projectUrl">Link do Projeto (opcional)</Label>
              <Input
                id="projectUrl"
                :model-value="projectForm.projectUrl"
                @update:model-value="(val: string) => projectForm.projectUrl = val"
                placeholder="https://..."
              />
            </div>
          </div>

          <!-- Featured -->
          <div class="flex items-center gap-2">
            <input
              type="checkbox"
              id="featured"
              :checked="projectForm.featured"
              @change="(e) => projectForm.featured = (e.target as HTMLInputElement).checked"
              class="w-4 h-4 rounded border-gray-300"
            />
            <Label for="featured" class="cursor-pointer">
              Destacar este projeto no portfólio
            </Label>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showAddDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleSaveProject" variant="default">
            {{ editingProject ? 'Atualizar Projeto' : 'Adicionar Projeto' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- View Project Dialog -->
    <Dialog :open="showViewDialog" @update:open="(val: boolean) => showViewDialog = val">
      <DialogContent class="sm:max-w-4xl max-h-[90vh] overflow-y-auto">
        <template v-if="viewingProject">
          <DialogHeader>
            <div class="flex items-center gap-2 mb-2">
              <DialogTitle>{{ viewingProject.title }}</DialogTitle>
              <Badge v-if="viewingProject.featured" class="bg-yellow-500 hover:bg-yellow-600 flex-shrink-0">
                <Tag class="h-3 w-3 mr-1" />
                Destaque
              </Badge>
            </div>
            <div class="flex items-center gap-3 text-sm text-[#435574] flex-wrap">
              <Badge variant="secondary">{{ viewingProject.category }}</Badge>
              <span class="flex items-center gap-1">
                <Calendar class="h-4 w-4" />
                {{ new Date(viewingProject.completedDate).toLocaleDateString('pt-BR') }}
              </span>
              <span v-if="viewingProject.client">Cliente: {{ viewingProject.client }}</span>
            </div>
          </DialogHeader>

          <div class="space-y-6 overflow-hidden">
            <!-- Image Gallery -->
            <div class="space-y-4 overflow-hidden">
              <div class="rounded-lg bg-gray-100 flex justify-center items-center h-[350px] overflow-hidden">
                <img
                  :src="viewingProject.images[selectedImageIndex]?.url"
                  :alt="viewingProject.images[selectedImageIndex]?.caption || viewingProject.title"
                  class="max-w-full max-h-full object-contain"
                />
              </div>

              <div v-if="viewingProject.images.length > 1" class="flex gap-3 overflow-x-auto pb-2">
                <button
                  v-for="(img, idx) in viewingProject.images"
                  :key="img.id"
                  @click="selectedImageIndex = idx"
                  :class="`relative flex-shrink-0 w-24 h-16 rounded-lg overflow-hidden transition-all mt-2 ml-1 mr-1 ${
                    selectedImageIndex === idx ? 'ring-2 ring-[#286AD8] ring-offset-2' : 'hover:opacity-80'
                  }`"
                >
                  <img :src="img.url" :alt="img.caption || ''" class="w-full h-full object-cover" />
                </button>
              </div>

              <p
                v-if="viewingProject.images[selectedImageIndex]?.caption"
                class="text-sm text-[#435574] text-center"
              >
                {{ viewingProject.images[selectedImageIndex].caption }}
              </p>
            </div>

            <!-- Description -->
            <div>
              <h4 class="text-[#263238] mb-2">Descrição do Projeto</h4>
              <p class="text-[#435574] whitespace-pre-wrap break-words">{{ viewingProject.description }}</p>
            </div>

            <!-- Tags -->
            <div v-if="viewingProject.tags.length > 0">
              <h4 class="text-[#263238] mb-2">Tags</h4>
              <div class="flex flex-wrap gap-2">
                <Badge v-for="tag in viewingProject.tags" :key="tag" variant="secondary">
                  {{ tag }}
                </Badge>
              </div>
            </div>

            <!-- Links -->
            <div v-if="viewingProject.videoUrl || viewingProject.projectUrl">
              <h4 class="text-[#263238] mb-2">Links</h4>
              <div class="flex gap-3">
                <a
                  v-if="viewingProject.videoUrl"
                  :href="viewingProject.videoUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="flex items-center gap-2 text-[#286AD8] hover:underline"
                >
                  <Video class="h-4 w-4" />
                  Ver Vídeo
                </a>
                <a
                  v-if="viewingProject.projectUrl"
                  :href="viewingProject.projectUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="flex items-center gap-2 text-[#286AD8] hover:underline"
                >
                  <ExternalLink class="h-4 w-4" />
                  Ver Projeto
                </a>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button @click="showViewDialog = false" variant="outline">
              Fechar
            </Button>
            <Button
              @click="
                showViewDialog = false;
                handleEditProject(viewingProject);
              "
              variant="default"
            >
              <Edit class="h-4 w-4 mr-2" />
              Editar Projeto
            </Button>
          </DialogFooter>
        </template>
      </DialogContent>
    </Dialog>

    <!-- Delete Confirmation -->
    <AlertDialog :open="showDeleteDialog" @update:open="(val: boolean) => showDeleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Excluir Projeto</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja excluir o projeto "{{ projectToDelete?.title }}"?
            Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleDeleteProject"
            class="bg-destructive hover:bg-destructive/90"
          >
            Excluir
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
    </AlertDialog>
    </template>
  </div>
</template>

