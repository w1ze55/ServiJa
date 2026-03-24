<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Plus,
  Edit,
  Trash2,
  Search,
  MoreVertical,
  Eye,
  EyeOff,
  DollarSign,
  Clock,
} from 'lucide-vue-next';
import Button from '@/components/ui/button.vue';
import Input from '@/components/ui/input.vue';
import Badge from '@/components/ui/badge.vue';
import Card from '@/components/ui/card.vue';
import Select from '@/components/ui/select.vue';
import SelectContent from '@/components/ui/select-content.vue';
import SelectItem from '@/components/ui/select-item.vue';
import SelectTrigger from '@/components/ui/select-trigger.vue';
import SelectValue from '@/components/ui/select-value.vue';
import DropdownMenu from '@/components/ui/dropdown-menu.vue';
import DropdownMenuContent from '@/components/ui/dropdown-menu-content.vue';
import DropdownMenuItem from '@/components/ui/dropdown-menu-item.vue';
import DropdownMenuSeparator from '@/components/ui/dropdown-menu-separator.vue';
import DropdownMenuTrigger from '@/components/ui/dropdown-menu-trigger.vue';
import AlertDialog from '@/components/ui/alert-dialog.vue';
import AlertDialogAction from '@/components/ui/alert-dialog-action.vue';
import AlertDialogCancel from '@/components/ui/alert-dialog-cancel.vue';
import AlertDialogContent from '@/components/ui/alert-dialog-content.vue';
import AlertDialogDescription from '@/components/ui/alert-dialog-description.vue';
import AlertDialogFooter from '@/components/ui/alert-dialog-footer.vue';
import AlertDialogHeader from '@/components/ui/alert-dialog-header.vue';
import AlertDialogTitle from '@/components/ui/alert-dialog-title.vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import ServiceFormDialog, { type ServiceFormData } from '@/components/freelancer/ServiceFormDialog.vue';
import ServicePreviewModal from '@/components/freelancer/ServicePreviewModal.vue';
import { toast } from '@/utils/toast';
import api from '@/api/config/axios';
import { useNavigation } from '@/utils/navigation';

interface Service {
  id: string;
  title: string;
  category: string;
  description: string;
  price: number;
  priceType: string;
  workModel: 'presencial' | 'remoto' | 'hibrido';
  deliveryTime: number;
  deliveryUnit: string;
  tags: string[];
  images: string[];
  isActive: boolean;
  createdAt: string;
}

interface ServiceStats {
  total: number;
  active: number;
  inactive: number;
}

const router = useRouter();
const { navigate, logout } = useNavigation();

// Obter usuário do localStorage
const userString = localStorage.getItem('user');
const user = userString ? JSON.parse(userString) : null;

const searchQuery = ref('');
const categoryFilter = ref('Todos');
const sortBy = ref('Mais recentes');
const serviceToDelete = ref<Service | null>(null);
const serviceToToggle = ref<Service | null>(null);
const currentPage = ref(1);
const showServiceForm = ref(false);
const editingService = ref<ServiceFormData | null>(null);
const showPreviewModal = ref(false);
const previewService = ref<Service | null>(null);
const itemsPerPage = 8;
const isLoading = ref(false);

// Dados
const services = ref<Service[]>([]);
const stats = ref<ServiceStats>({ total: 0, active: 0, inactive: 0 });

const categories = [
  'Eletricista',
  'Encanador',
  'Pintor',
  'Pedreiro',
  'Marceneiro',
  'Serralheiro',
  'Jardineiro',
  'Mecânico',
  'Designer Gráfico',
  'Desenvolvedor Web',
  'Fotógrafo',
  'Videomaker',
  'Redator',
  'Social Media',
  'Professor Particular',
  'Cozinheiro',
  'Diarista',
  'Cuidador',
  'Personal Trainer',
  'Advogado',
  'Contador',
  'Arquiteto',
  'Outro',
];

const normalizeWorkModel = (value?: string | null): 'presencial' | 'remoto' | 'hibrido' => {
  const normalized = value?.toUpperCase();
  if (normalized === 'HIBRIDO') return 'hibrido';
  if (normalized === 'PRESENCIAL') return 'presencial';
  return 'remoto';
};

const mapWorkModelToApi = (value: ServiceFormData['workModel']) => {
  if (value === 'hibrido') return 'HIBRIDO';
  if (value === 'presencial') return 'PRESENCIAL';
  return 'REMOTO';
};

const getWorkModelLabel = (value: Service['workModel']) => {
  if (value === 'hibrido') return 'Híbrido';
  if (value === 'presencial') return 'Presencial';
  return 'Remoto';
};

// Carregar serviços da API
const loadServices = async () => {
  isLoading.value = true;
  try {
    const response = await api.get('/services/my');
    // Mapear valores do backend para português
    const priceTypeMap: Record<string, string> = {
      'FIXED': 'fixo',
      'HOURLY': 'por hora',
      'DAILY': 'por dia',
    };
    const deliveryUnitMap: Record<string, string> = {
      'DAYS': 'dias',
      'WEEKS': 'semanas',
      'MONTHS': 'meses',
    };
    
    services.value = response.data.map((s: any) => ({
      id: String(s.id),
      title: s.title,
      category: s.category,
      description: s.description,
      price: Number(s.price),
      priceType: priceTypeMap[s.priceType] || 'fixo',
      workModel: normalizeWorkModel(s.workModel),
      deliveryTime: s.deliveryTime,
      deliveryUnit: deliveryUnitMap[s.deliveryUnit] || 'dias',
      tags: s.tags || [],
      images: s.images || [],
      isActive: s.isActive,
      createdAt: s.createdAt,
    }));
  } catch (error) {
    console.error('Erro ao carregar serviços:', error);
    toast.error('Erro ao carregar serviços');
  } finally {
    isLoading.value = false;
  }
};

// Carregar estatísticas
const loadStats = async () => {
  try {
    const response = await api.get('/services/stats');
    stats.value = response.data;
  } catch (error) {
    console.error('Erro ao carregar estatísticas:', error);
  }
};

// Filtrar e ordenar serviços
const filteredServices = computed(() => {
  let filtered = services.value.filter((service) => {
    const matchesSearch =
      service.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      service.description.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchesCategory = categoryFilter.value === 'Todos' || service.category === categoryFilter.value;
    return matchesSearch && matchesCategory;
  });

  // Ordenar
  if (sortBy.value === 'Mais recentes') {
    filtered.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  } else if (sortBy.value === 'Menor preço') {
    filtered.sort((a, b) => a.price - b.price);
  } else if (sortBy.value === 'Maior preço') {
    filtered.sort((a, b) => b.price - a.price);
  } else if (sortBy.value === 'Nome (A-Z)') {
    filtered.sort((a, b) => a.title.localeCompare(b.title));
  }

  return filtered;
});

// Paginação
const totalPages = computed(() => Math.ceil(filteredServices.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const endIndex = computed(() => startIndex.value + itemsPerPage);
const currentServices = computed(() => filteredServices.value.slice(startIndex.value, endIndex.value));

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDeliveryTime = (time: number, unit: string) => {
  if (unit === 'meses') {
    return time === 1 ? '1 mês' : `${time} meses`;
  }
  const unitLabel = unit === 'semanas' ? (time === 1 ? 'semana' : 'semanas') : (time === 1 ? 'dia' : 'dias');
  return `${time} ${unitLabel}`;
};

// Handlers
const handleDeleteService = async () => {
  if (serviceToDelete.value) {
    try {
      await api.delete(`/services/${serviceToDelete.value.id}`);
      services.value = services.value.filter((s) => s.id !== serviceToDelete.value!.id);
      toast.success('Serviço excluído com sucesso');
      loadStats();
    } catch (error) {
      console.error('Erro ao excluir serviço:', error);
      toast.error('Erro ao excluir serviço');
    } finally {
      serviceToDelete.value = null;
    }
  }
};

const handleToggleService = async () => {
  if (serviceToToggle.value) {
    try {
      const response = await api.patch(`/services/${serviceToToggle.value.id}/toggle`);
      const newStatus = response.data.isActive;
      services.value = services.value.map((s) =>
        s.id === serviceToToggle.value!.id ? { ...s, isActive: newStatus } : s
      );
      toast.success(newStatus ? 'Serviço ativado com sucesso' : 'Serviço desativado com sucesso');
      loadStats();
    } catch (error) {
      console.error('Erro ao alterar status:', error);
      toast.error('Erro ao alterar status do serviço');
    } finally {
      serviceToToggle.value = null;
    }
  }
};

const handleEditService = (serviceId: string) => {
  const service = services.value.find((s) => s.id === serviceId);
  if (service) {
    // Mapear valores armazenados (em português) para o formato do formulário
    const priceTypeMap: Record<string, 'fixo' | 'hora' | 'dia'> = {
      'fixo': 'fixo',
      'por hora': 'hora',
      'por dia': 'dia',
    };
    
    // Converter prazo armazenado para dias totais
    let totalDays = service.deliveryTime;
    if (service.deliveryUnit === 'semanas') {
      totalDays = service.deliveryTime * 7;
    } else if (service.deliveryUnit === 'meses') {
      totalDays = service.deliveryTime * 30;
    }
    
    editingService.value = {
      id: service.id,
      title: service.title,
      category: service.category,
      description: service.description,
      price: service.price,
      priceType: priceTypeMap[service.priceType] || 'fixo',
      workModel: normalizeWorkModel(service.workModel),
      deliveryTime: String(totalDays),
      tags: service.tags || [],
      images: service.images || [],
      isActive: service.isActive,
    };
    showServiceForm.value = true;
  }
};

const handleAddService = () => {
  editingService.value = null;
  showServiceForm.value = true;
};

const handleServiceFormClose = () => {
  showServiceForm.value = false;
  editingService.value = null;
};

const handleServiceFormSubmit = async (formData: ServiceFormData) => {
  try {
    // Log para debug
    console.log('Dados do formulário recebidos:', formData);
    
    // Mapear valores do formulário para o formato do backend
    const priceTypeMap: Record<string, string> = {
      'fixo': 'FIXED',
      'hora': 'HOURLY',
      'dia': 'DAILY',
    };
    
    // Auto-conversão de dias para a melhor unidade
    const days = parseInt(formData.deliveryTime);
    let deliveryTime: number;
    let deliveryUnit: string;
    
    if (days >= 30) {
      deliveryTime = Math.floor(days / 30);
      deliveryUnit = 'MONTHS';
    } else if (days >= 7) {
      deliveryTime = Math.floor(days / 7);
      deliveryUnit = 'WEEKS';
    } else {
      deliveryTime = days;
      deliveryUnit = 'DAYS';
    }
    
    const payload = {
      title: formData.title,
      category: formData.category,
      description: formData.description,
      price: formData.price,
      priceType: priceTypeMap[formData.priceType] || 'FIXED',
      workModel: mapWorkModelToApi(formData.workModel),
      deliveryTime,
      deliveryUnit,
      tags: formData.tags,
      images: formData.images,
      isActive: formData.isActive,
    };

    console.log('Payload enviado para API:', payload);

    if (formData.id) {
      // Atualizar
      const response = await api.put(`/services/${formData.id}`, payload);
      
      // Mapear valores do backend para português
      const priceTypeMapResponse: Record<string, string> = {
        'FIXED': 'fixo',
        'HOURLY': 'por hora',
        'DAILY': 'por dia',
      };
      const deliveryUnitMapResponse: Record<string, string> = {
        'DAYS': 'dias',
        'WEEKS': 'semanas',
        'MONTHS': 'meses',
      };
      
      services.value = services.value.map((s) =>
        s.id === formData.id
          ? {
              ...s,
              title: response.data.title,
              category: response.data.category,
              description: response.data.description,
              price: Number(response.data.price),
              priceType: priceTypeMapResponse[response.data.priceType] || 'fixo',
              workModel: normalizeWorkModel(response.data.workModel),
              deliveryTime: response.data.deliveryTime,
              deliveryUnit: deliveryUnitMapResponse[response.data.deliveryUnit] || 'dias',
              tags: response.data.tags || [],
              images: response.data.images || [],
              isActive: response.data.isActive,
            }
          : s
      );
      toast.success('Serviço atualizado com sucesso');
    } else {
      // Criar
      const response = await api.post('/services', payload);
      
      // Mapear valores do backend para português
      const priceTypeMapResponse: Record<string, string> = {
        'FIXED': 'fixo',
        'HOURLY': 'por hora',
        'DAILY': 'por dia',
      };
      const deliveryUnitMapResponse: Record<string, string> = {
        'DAYS': 'dias',
        'WEEKS': 'semanas',
        'MONTHS': 'meses',
      };
      
      services.value = [
        {
          id: String(response.data.id),
          title: response.data.title,
          category: response.data.category,
          description: response.data.description,
          price: Number(response.data.price),
          priceType: priceTypeMapResponse[response.data.priceType] || 'fixo',
          workModel: normalizeWorkModel(response.data.workModel),
          deliveryTime: response.data.deliveryTime,
          deliveryUnit: deliveryUnitMapResponse[response.data.deliveryUnit] || 'dias',
          tags: response.data.tags || [],
          images: response.data.images || [],
          isActive: response.data.isActive,
          createdAt: response.data.createdAt,
        },
        ...services.value,
      ];
      toast.success('Novo serviço criado com sucesso');
    }
    loadStats();
  } catch (error) {
    console.error('Erro ao salvar serviço:', error);
    toast.error('Erro ao salvar serviço');
  }
  showServiceForm.value = false;
  editingService.value = null;
};

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};

const handlePreviewService = (serviceId: string) => {
  const service = services.value.find((s) => s.id === serviceId);
  if (service) {
    previewService.value = service;
    showPreviewModal.value = true;
  }
};

const handleClosePreview = () => {
  showPreviewModal.value = false;
  previewService.value = null;
};

// Navegação
const handleNavigate = (page: string) => {
  navigate(page, 'provider');
};

// Logout
const handleLogout = () => {
  logout();
};

// Lifecycle
onMounted(() => {
  loadServices();
  loadStats();
});
</script>

<template>
  <MainLayout 
    v-if="user"
    :user="user" 
    current-page="freelancer-services" 
    :on-navigate="handleNavigate" 
    :on-logout="handleLogout"
  >
    <div class="max-w-7xl mx-auto space-y-6">
      <!-- Header -->
      <div class="flex items-start justify-between">
        <div>
          <h1 class="text-2xl font-bold text-[#263238] mb-2">Meus Serviços</h1>
          <p class="text-[#435574]">Gerencie os serviços que você oferece</p>
        </div>
        <Button @click="handleAddService" class="bg-[#286AD8] hover:bg-[#435574]">
          <Plus class="h-4 w-4 mr-2" />
          Novo Serviço
        </Button>
      </div>

      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card class="p-4 bg-white">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
              <DollarSign class="h-5 w-5 text-[#286AD8]" />
            </div>
            <div>
              <p class="text-sm text-[#435574]">Total de Serviços</p>
              <p class="text-xl font-bold text-[#263238]">{{ stats.total }}</p>
            </div>
          </div>
        </Card>

        <Card class="p-4 bg-white">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
              <Eye class="h-5 w-5 text-green-600" />
            </div>
            <div>
              <p class="text-sm text-[#435574]">Ativos</p>
              <p class="text-xl font-bold text-[#263238]">{{ stats.active }}</p>
            </div>
          </div>
        </Card>

        <Card class="p-4 bg-white">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
              <EyeOff class="h-5 w-5 text-gray-600" />
            </div>
            <div>
              <p class="text-sm text-[#435574]">Inativos</p>
              <p class="text-xl font-bold text-[#263238]">{{ stats.inactive }}</p>
            </div>
          </div>
        </Card>
      </div>

      <!-- Filters -->
      <Card class="p-4 bg-white">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <!-- Search -->
          <div class="relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
            <Input
              placeholder="Buscar serviços..."
              :model-value="searchQuery"
              @update:model-value="handleSearchChange"
              class="pl-10"
            />
          </div>

          <!-- Category Filter -->
          <Select :model-value="categoryFilter" @update:model-value="(val: string) => categoryFilter = val">
            <SelectTrigger>
              <SelectValue placeholder="Todas as categorias" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Todos">Todas as categorias</SelectItem>
              <SelectItem v-for="cat in categories" :key="cat" :value="cat">
                {{ cat }}
              </SelectItem>
            </SelectContent>
          </Select>

          <!-- Sort By -->
          <Select :model-value="sortBy" @update:model-value="(val: string) => sortBy = val">
            <SelectTrigger>
              <SelectValue placeholder="Ordenar por" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Mais recentes">Mais recentes</SelectItem>
              <SelectItem value="Nome (A-Z)">Nome (A-Z)</SelectItem>
              <SelectItem value="Menor preço">Menor preço</SelectItem>
              <SelectItem value="Maior preço">Maior preço</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      <!-- Loading -->
      <div v-if="isLoading" class="flex justify-center py-12">
        <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
      </div>

      <!-- Empty State -->
      <Card v-else-if="currentServices.length === 0" class="p-12 text-center bg-white">
        <div class="flex flex-col items-center gap-4">
          <DollarSign class="h-12 w-12 text-gray-400" />
          <div>
            <h3 class="text-lg font-semibold text-[#263238] mb-2">Nenhum serviço encontrado</h3>
            <p class="text-[#435574] mb-6">
              {{ searchQuery || categoryFilter !== 'Todos'
                ? 'Tente ajustar os filtros'
                : 'Comece criando seu primeiro serviço' }}
            </p>
            <Button
              v-if="!searchQuery && categoryFilter === 'Todos'"
              @click="handleAddService"
              class="bg-[#286AD8] hover:bg-[#435574]"
            >
              <Plus class="h-4 w-4 mr-2" />
              Criar Primeiro Serviço
            </Button>
          </div>
        </div>
      </Card>

      <template v-else>
        <!-- Desktop Table -->
        <Card class="hidden md:block overflow-hidden bg-white">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-[#435574] uppercase tracking-wider">
                    Serviço
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-[#435574] uppercase tracking-wider">
                    Categoria
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-[#435574] uppercase tracking-wider">
                    Preço
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-[#435574] uppercase tracking-wider">
                    Prazo
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-[#435574] uppercase tracking-wider">
                    Status
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-[#435574] uppercase tracking-wider">
                    Ações
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200">
                <tr v-for="service in currentServices" :key="service.id" class="hover:bg-gray-50">
                  <td class="px-6 py-4">
                    <div>
                      <p 
                        class="text-sm font-medium text-[#263238] hover:text-[#286AD8] cursor-pointer"
                        @click="handlePreviewService(service.id)"
                      >
                        {{ service.title }}
                      </p>
                      <p class="text-xs text-[#435574] max-w-xs truncate">
                        {{ service.description }}
                      </p>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex flex-wrap gap-2">
                      <Badge variant="secondary">{{ service.category }}</Badge>
                      <Badge variant="outline" class="bg-white text-[#435574]">
                        {{ getWorkModelLabel(service.workModel) }}
                      </Badge>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <p class="text-sm font-medium text-[#263238]">
                      {{ formatCurrency(service.price) }}
                    </p>
                    <p class="text-xs text-[#435574]">
                      {{ service.priceType === 'por hora' ? '/hora' : service.priceType === 'por dia' ? '/dia' : 'fixo' }}
                    </p>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center gap-1 text-sm text-[#435574]">
                      <Clock class="h-4 w-4" />
                      {{ formatDeliveryTime(service.deliveryTime, service.deliveryUnit) }}
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <Badge
                      :class="
                        service.isActive
                          ? 'bg-green-100 text-green-700 hover:bg-green-100'
                          : 'bg-gray-100 text-gray-700 hover:bg-gray-100'
                      "
                    >
                      {{ service.isActive ? 'Ativo' : 'Inativo' }}
                    </Badge>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <DropdownMenu>
                      <DropdownMenuTrigger as-child>
                        <Button variant="ghost" size="icon">
                          <MoreVertical class="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem @click="handlePreviewService(service.id)">
                          <Eye class="h-4 w-4 mr-2" />
                          Visualizar Preview
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem @click="handleEditService(service.id)">
                          <Edit class="h-4 w-4 mr-2" />
                          Editar
                        </DropdownMenuItem>
                        <DropdownMenuItem @click="serviceToToggle = service">
                          <EyeOff v-if="service.isActive" class="h-4 w-4 mr-2" />
                          <Eye v-else class="h-4 w-4 mr-2" />
                          {{ service.isActive ? 'Desativar' : 'Ativar' }}
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          @click="serviceToDelete = service"
                          class="text-red-600"
                        >
                          <Trash2 class="h-4 w-4 mr-2" />
                          Excluir
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </Card>

        <!-- Mobile Cards -->
        <div class="md:hidden space-y-3">
          <Card 
            v-for="service in currentServices" 
            :key="service.id" 
            class="p-4 bg-white cursor-pointer hover:shadow-md transition-shadow"
            @click="handlePreviewService(service.id)"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex-1">
                <h3 class="font-semibold text-[#263238] mb-1">{{ service.title }}</h3>
                <div class="flex flex-wrap gap-2">
                  <Badge variant="secondary" class="text-xs">
                    {{ service.category }}
                  </Badge>
                  <Badge variant="outline" class="text-xs bg-white text-[#435574]">
                    {{ getWorkModelLabel(service.workModel) }}
                  </Badge>
                </div>
              </div>
              <DropdownMenu>
                <DropdownMenuTrigger as-child>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    @click.stop
                  >
                    <MoreVertical class="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem @click="handlePreviewService(service.id)">
                    <Eye class="h-4 w-4 mr-2" />
                    Visualizar Preview
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem @click="handleEditService(service.id)">
                    <Edit class="h-4 w-4 mr-2" />
                    Editar
                  </DropdownMenuItem>
                  <DropdownMenuItem @click="serviceToToggle = service">
                    <EyeOff v-if="service.isActive" class="h-4 w-4 mr-2" />
                    <Eye v-else class="h-4 w-4 mr-2" />
                    {{ service.isActive ? 'Desativar' : 'Ativar' }}
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    @click="serviceToDelete = service"
                    class="text-red-600"
                  >
                    <Trash2 class="h-4 w-4 mr-2" />
                    Excluir
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>

            <p class="text-sm text-[#435574] mb-3 line-clamp-2">{{ service.description }}</p>

            <div class="flex items-center justify-between pt-3 border-t">
              <div class="flex items-center gap-4">
                <div>
                  <p class="text-xs text-[#435574]">Preço</p>
                  <p class="text-sm font-medium text-[#263238]">{{ formatCurrency(service.price) }}</p>
                </div>
                <div>
                  <p class="text-xs text-[#435574]">Prazo</p>
                  <p class="text-sm font-medium text-[#263238]">{{ formatDeliveryTime(service.deliveryTime, service.deliveryUnit) }}</p>
                </div>
              </div>
              <Badge
                :class="
                  service.isActive
                    ? 'bg-green-100 text-green-700 hover:bg-green-100'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-100'
                "
              >
                {{ service.isActive ? 'Ativo' : 'Inativo' }}
              </Badge>
            </div>
          </Card>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2">
          <Button
            @click="currentPage = Math.max(1, currentPage - 1)"
            :disabled="currentPage === 1"
            variant="outline"
            size="sm"
          >
            Anterior
          </Button>
          <div class="flex items-center gap-1">
            <Button
              v-for="pageNum in Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                if (totalPages <= 5) return i + 1;
                if (currentPage <= 3) return i + 1;
                if (currentPage >= totalPages - 2) return totalPages - 4 + i;
                return currentPage - 2 + i;
              })"
              :key="pageNum"
              @click="currentPage = pageNum"
              :variant="currentPage === pageNum ? 'default' : 'outline'"
              size="sm"
              :class="currentPage === pageNum ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
            >
              {{ pageNum }}
            </Button>
          </div>
          <Button
            @click="currentPage = Math.min(totalPages, currentPage + 1)"
            :disabled="currentPage === totalPages"
            variant="outline"
            size="sm"
          >
            Próxima
          </Button>
        </div>
      </template>

      <!-- Delete Confirmation Dialog -->
      <AlertDialog :open="!!serviceToDelete" @update:open="(val) => !val && (serviceToDelete = null)">
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir Serviço</AlertDialogTitle>
            <AlertDialogDescription>
              Tem certeza que deseja excluir o serviço "
              <strong>{{ serviceToDelete?.title }}</strong>"? Esta ação não pode ser desfeita.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel @click="serviceToDelete = null">Cancelar</AlertDialogCancel>
            <AlertDialogAction
              @click="handleDeleteService"
              class="bg-red-600 hover:bg-red-700"
            >
              Sim, Excluir
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <!-- Toggle Status Confirmation Dialog -->
      <AlertDialog :open="!!serviceToToggle" @update:open="(val) => !val && (serviceToToggle = null)">
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {{ serviceToToggle?.isActive ? 'Desativar' : 'Ativar' }} Serviço
            </AlertDialogTitle>
            <AlertDialogDescription>
              {{ serviceToToggle?.isActive
                ? 'Ao desativar este serviço, ele não ficará mais visível para os clientes.'
                : 'Ao ativar este serviço, ele ficará visível para os clientes na plataforma.' }}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel @click="serviceToToggle = null">Cancelar</AlertDialogCancel>
            <AlertDialogAction
              @click="handleToggleService"
              class="bg-[#286AD8] hover:bg-[#435574]"
            >
              Confirmar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <!-- Service Form Dialog -->
      <ServiceFormDialog
        :open="showServiceForm"
        :on-close="handleServiceFormClose"
        :on-submit="handleServiceFormSubmit"
        :service="editingService"
      />

      <!-- Service Preview Modal -->
      <ServicePreviewModal
        :open="showPreviewModal"
        :service="previewService"
        :on-close="handleClosePreview"
      />
    </div>
  </MainLayout>
</template>
