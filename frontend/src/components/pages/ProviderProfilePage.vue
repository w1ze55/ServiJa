<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import {
  ArrowLeft,
  Star,
  MapPin,
  Clock,
  MessageSquare,
  Briefcase,
  Award,
  Sparkles,
  Send,
  DollarSign,
  Loader2,
  AlertCircle,
  Share2,
  Heart,
  ChevronRight,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import api from '@/api/config/axios';
import { toast } from '@/utils/toast';

interface FreelancerData {
  id: number;
  name: string;
  email: string;
  profession: string;
  category: string;
  bio: string;
  hourlyRate: number;
  profilePhoto: string;
  serviceType: string;
  workModel: string;
  location: string;
  rating: number;
  totalReviews: number;
  completedJobs: number;
  available: boolean;
  serviceCities: Array<{ id: number; city: string; state: string }>;
  professionalLinks: Array<{ id: number; type: string; url: string; label: string }>;
  socialLinks: {
    website?: string;
    linkedin?: string;
    instagram?: string;
    portfolio?: string;
  };
}

interface ServiceItem {
  id: number;
  freelancerId: number;
  freelancerName: string;
  title: string;
  category: string;
  description: string;
  price: number;
  priceType: string;
  deliveryTime: number;
  deliveryUnit: string;
  tags: string[];
  images: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface PortfolioProject {
  id: number;
  title: string;
  description: string;
  category: string;
  completedAt: string;
  tags: string[];
  images: Array<{
    id: number;
    url: string;
    caption: string;
    displayOrder: number;
  }>;
}

interface Props {
  providerId: number;
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const freelancer = ref<FreelancerData | null>(null);
const services = ref<ServiceItem[]>([]);
const portfolio = ref<PortfolioProject[]>([]);
const isLoading = ref(true);
const error = ref('');
const quoteMessage = ref('');
const quoteCategory = ref('');
const quoteBudget = ref('');
const quoteDeadline = ref('');
const showQuoteDialog = ref(false);
const activeTab = ref('services');
const isSendingQuote = ref(false);

const categories = [
  'Eletricista',
  'Encanador',
  'Pintor',
  'Pedreiro',
  'Marceneiro',
  'Jardineiro',
  'Mecânico',
  'Designer Gráfico',
  'Desenvolvedor Web',
  'Outro',
];

const loadFreelancerData = async () => {
  isLoading.value = true;
  error.value = '';
  
  try {
    // Buscar dados do freelancer
    const freelancerResponse = await api.get(`/freelancer/${props.providerId}/profile`);
    freelancer.value = freelancerResponse.data;
    
    // Buscar serviços do freelancer
    const servicesResponse = await api.get('/services/public/all');
    services.value = servicesResponse.data.filter((s: ServiceItem) => s.freelancerId === props.providerId);
    
    // Buscar portfólio do freelancer
    try {
      const portfolioResponse = await api.get(`/portfolio/freelancer/${props.providerId}`);
      portfolio.value = portfolioResponse.data || [];
    } catch (err) {
      console.log('Portfólio não disponível');
      portfolio.value = [];
    }
    
  } catch (err: any) {
    console.error('Erro ao carregar dados do freelancer:', err);
    error.value = 'Não foi possível carregar os dados do profissional.';
    toast.error('Erro ao carregar perfil');
  } finally {
    isLoading.value = false;
  }
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatPrice = (price: number, priceType: string) => {
  const formatted = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(price);
  const suffix = priceType === 'HOURLY' ? '/hora' : priceType === 'DAILY' ? '/dia' : '';
  return `${formatted}${suffix}`;
};

const formatDelivery = (time: number, unit: string) => {
  const unitLabel = unit === 'HOURS' ? 'hora(s)' : unit === 'DAYS' ? 'dia(s)' : 'semana(s)';
  return `${time} ${unitLabel}`;
};

const handleSendQuote = async () => {
  if (!quoteMessage.value.trim()) {
    toast.error('Por favor, descreva o serviço que você precisa');
    return;
  }

  if (!quoteCategory.value) {
    toast.error('Por favor, selecione uma categoria');
    return;
  }

  if (!quoteBudget.value || Number(quoteBudget.value) <= 0) {
    toast.error('Por favor, informe um orçamento válido');
    return;
  }

  if (!quoteDeadline.value) {
    toast.error('Por favor, informe um prazo');
    return;
  }

  isSendingQuote.value = true;
  try {
    const requestData = {
      serviceName: `${quoteCategory.value} - Solicitação`,
      category: quoteCategory.value,
      description: quoteMessage.value,
      budget: Number(quoteBudget.value),
      deadline: quoteDeadline.value,
      priority: 'MEDIUM',
      address: '',
      urgency: 'normal',
      freelancerId: Number(props.providerId),
    };

    console.log('Enviando solicitação:', requestData);
    await api.post('/service-requests', requestData);
    
    toast.success('Solicitação de orçamento enviada com sucesso!');
    showQuoteDialog.value = false;
    quoteMessage.value = '';
    quoteCategory.value = '';
    quoteBudget.value = '';
    quoteDeadline.value = '';

    setTimeout(() => {
      props.onNavigate('my-requests');
    }, 1500);
  } catch (error: any) {
    console.error('Erro ao enviar solicitação:', error);
    const errorMessage = error.response?.data?.message || 'Erro ao enviar solicitação. Tente novamente.';
    toast.error(errorMessage);
  } finally {
    isSendingQuote.value = false;
  }
};

const handleServiceClick = (serviceId: number) => {
  // Aqui você pode navegar para uma página de detalhes do serviço se desejar
  console.log('Service clicked:', serviceId);
};

const handleShare = () => {
  const url = window.location.href;
  if (navigator.share) {
    navigator.share({
      title: `${freelancer.value?.name} - ${freelancer.value?.profession}`,
      text: `Confira o perfil de ${freelancer.value?.name}`,
      url: url,
    });
  } else {
    navigator.clipboard.writeText(url);
    toast.success('Link copiado para a área de transferência!');
  }
};

onMounted(() => {
  loadFreelancerData();
});
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <!-- Back Button -->
      <Button
        variant="ghost"
        @click="onNavigate('explore')"
        class="mb-4 text-[#435574] hover:text-[#286AD8]"
      >
        <ArrowLeft class="h-4 w-4 mr-2" />
        Voltar para Explorar
      </Button>

      <!-- Loading State -->
      <div v-if="isLoading" class="flex items-center justify-center py-20">
        <div class="text-center">
          <Loader2 class="h-10 w-10 text-[#286AD8] animate-spin mx-auto mb-4" />
          <p class="text-[#435574]">Carregando perfil...</p>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="text-center py-16">
        <AlertCircle class="h-12 w-12 text-red-400 mx-auto mb-4" />
        <h3 class="text-lg font-semibold text-[#263238] mb-2">Ops, algo deu errado</h3>
        <p class="text-[#435574] mb-4">{{ error }}</p>
        <Button @click="loadFreelancerData" class="bg-[#286AD8]">
          Tentar novamente
        </Button>
      </div>

      <!-- Profile Content -->
      <div v-else-if="freelancer">
        <!-- Profile Header -->
        <Card class="p-6 mb-6">
          <div class="flex flex-col md:flex-row gap-6">
            <!-- Avatar -->
            <Avatar class="h-32 w-32 flex-shrink-0">
              <AvatarImage :src="freelancer.profilePhoto" :alt="freelancer.name" />
              <AvatarFallback class="bg-[#286AD8] text-white text-3xl">
                {{ getInitials(freelancer.name) }}
              </AvatarFallback>
            </Avatar>

            <div class="flex-1">
              <div class="flex items-start justify-between gap-4 mb-3">
                <div>
                  <h1 class="text-2xl font-bold text-[#263238] mb-2">{{ freelancer.name }}</h1>
                  <p class="text-[#286AD8] font-semibold mb-2">{{ freelancer.profession }}</p>
                  <div class="flex items-center gap-2 text-[#435574]">
                    <MapPin class="h-4 w-4" />
                    <span>{{ freelancer.location }}</span>
                  </div>
                </div>

                <div class="flex gap-2">
                  <Button variant="outline" size="icon" @click="handleShare">
                    <Share2 class="h-4 w-4" />
                  </Button>
                  <Button variant="outline" size="icon">
                    <Heart class="h-4 w-4" />
                  </Button>
                </div>
              </div>

              <!-- Stats -->
              <div class="flex flex-wrap gap-4 mb-4 text-[#435574]">
                <div class="flex items-center gap-2">
                  <Star class="h-5 w-5 fill-yellow-400 text-yellow-400" />
                  <span class="font-semibold">{{ freelancer.rating?.toFixed(1) || '0.0' }}</span>
                  <span>({{ freelancer.totalReviews || 0 }} avaliações)</span>
                </div>
                <div class="flex items-center gap-2">
                  <Briefcase class="h-4 w-4" />
                  <span>{{ freelancer.completedJobs || 0 }} trabalhos concluídos</span>
                </div>
                <div class="flex items-center gap-2">
                  <Clock class="h-4 w-4" />
                  <span>Responde rápido</span>
                </div>
              </div>

              <!-- Status Badge -->
              <div class="mb-4">
                <Badge v-if="freelancer.available" class="bg-green-100 text-green-700 hover:bg-green-100">
                  ✓ Disponível
                </Badge>
                <Badge v-else variant="secondary">
                  Indisponível
                </Badge>
              </div>

              <!-- Action Buttons -->
              <div class="flex gap-3">
                <Dialog v-model:open="showQuoteDialog">
                  <Button @click="showQuoteDialog = true" class="bg-[#286AD8] hover:bg-[#435574]">
                    <Send class="h-4 w-4 mr-2" />
                    Solicitar Orçamento
                  </Button>
                  <DialogContent class="max-w-md">
                    <DialogHeader>
                      <DialogTitle>Solicitar Orçamento</DialogTitle>
                    </DialogHeader>
                    <div class="space-y-4">
                      <div>
                        <Label class="text-[#435574] mb-2 block text-sm">
                          Categoria <span class="text-red-500">*</span>
                        </Label>
                        <Select v-model="quoteCategory">
                          <SelectTrigger>
                            <SelectValue placeholder="Selecione a categoria" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem v-for="cat in categories" :key="cat" :value="cat">
                              {{ cat }}
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </div>

                      <div>
                        <Label class="text-[#435574] mb-2 block text-sm">
                          Orçamento estimado (R$) <span class="text-red-500">*</span>
                        </Label>
                        <Input
                          type="number"
                          placeholder="Ex: 500"
                          v-model="quoteBudget"
                          min="0"
                          step="0.01"
                        />
                      </div>

                      <div>
                        <Label class="text-[#435574] mb-2 block text-sm">
                          Prazo desejado <span class="text-red-500">*</span>
                        </Label>
                        <Input
                          type="text"
                          placeholder="Ex: 7 dias, 2 semanas, urgente"
                          v-model="quoteDeadline"
                        />
                      </div>

                      <div>
                        <Label class="text-[#435574] mb-2 block text-sm">
                          Descrição do serviço <span class="text-red-500">*</span>
                        </Label>
                        <Textarea
                          placeholder="Ex: Preciso instalar 3 ventiladores de teto na sala e quartos..."
                          :rows="5"
                          v-model="quoteMessage"
                          class="resize-none"
                        />
                      </div>

                      <Button 
                        @click="handleSendQuote" 
                        class="w-full bg-[#286AD8]"
                        :disabled="isSendingQuote"
                      >
                        <Loader2 v-if="isSendingQuote" class="h-4 w-4 mr-2 animate-spin" />
                        <Send v-else class="h-4 w-4 mr-2" />
                        {{ isSendingQuote ? 'Enviando...' : 'Enviar Solicitação' }}
                      </Button>
                    </div>
                  </DialogContent>
                </Dialog>

                <Button variant="outline">
                  <MessageSquare class="h-4 w-4 mr-2" />
                  Mensagem
                </Button>
              </div>
            </div>
          </div>
        </Card>

        <div class="grid lg:grid-cols-3 gap-6">
          <!-- Main Content -->
          <div class="lg:col-span-2">
            <Tabs v-model="activeTab" class="space-y-6">
              <TabsList class="grid w-full grid-cols-3">
                <TabsTrigger value="services">Serviços ({{ services.length }})</TabsTrigger>
                <TabsTrigger value="about">Sobre</TabsTrigger>
                <TabsTrigger value="portfolio">Portfólio ({{ portfolio.length }})</TabsTrigger>
              </TabsList>

              <!-- Services Tab -->
              <TabsContent value="services">
                <div v-if="services.length > 0" class="grid md:grid-cols-2 gap-4">
                  <Card
                    v-for="service in services"
                    :key="service.id"
                    class="overflow-hidden hover:shadow-lg hover:border-[#286AD8]/30 transition-all cursor-pointer group"
                    @click="handleServiceClick(service.id)"
                  >
                    <!-- Service Image -->
                    <div class="relative h-40 bg-gradient-to-br from-[#286AD8]/10 to-[#435574]/10">
                      <img
                        v-if="service.images && service.images.length > 0"
                        :src="service.images[0]"
                        :alt="service.title"
                        class="w-full h-full object-cover group-hover:scale-105 transition-transform"
                      />
                      <div v-else class="w-full h-full flex items-center justify-center">
                        <Briefcase class="h-12 w-12 text-[#286AD8]/30" />
                      </div>
                      <Badge class="absolute top-3 left-3 bg-white/90 backdrop-blur-sm">
                        {{ service.category }}
                      </Badge>
                    </div>

                    <!-- Service Content -->
                    <div class="p-4">
                      <h3 class="font-semibold text-[#263238] mb-2 line-clamp-1 group-hover:text-[#286AD8]">
                        {{ service.title }}
                      </h3>
                      <p class="text-sm text-[#435574] mb-3 line-clamp-2">{{ service.description }}</p>

                      <!-- Tags -->
                      <div v-if="service.tags && service.tags.length > 0" class="flex flex-wrap gap-1 mb-3">
                        <Badge v-for="tag in service.tags.slice(0, 2)" :key="tag" variant="secondary" class="text-xs">
                          {{ tag }}
                        </Badge>
                        <Badge v-if="service.tags.length > 2" variant="secondary" class="text-xs">
                          +{{ service.tags.length - 2 }}
                        </Badge>
                      </div>

                      <!-- Footer -->
                      <div class="flex items-center justify-between pt-3 border-t border-gray-100">
                        <div class="flex items-center gap-1 text-[#286AD8]">
                          <DollarSign class="h-4 w-4" />
                          <span class="font-bold text-sm">{{ formatPrice(service.price, service.priceType) }}</span>
                        </div>
                        <div class="flex items-center gap-1 text-[#435574]">
                          <Clock class="h-3.5 w-3.5" />
                          <span class="text-xs">{{ formatDelivery(service.deliveryTime, service.deliveryUnit) }}</span>
                        </div>
                      </div>
                    </div>
                  </Card>
                </div>
                <Card v-else class="p-12 text-center">
                  <Briefcase class="h-12 w-12 text-[#435574]/30 mx-auto mb-4" />
                  <p class="text-[#435574]">Nenhum serviço cadastrado ainda</p>
                </Card>
              </TabsContent>

              <!-- About Tab -->
              <TabsContent value="about">
                <Card class="p-6">
                  <h3 class="font-semibold text-[#263238] mb-4">Sobre o profissional</h3>
                  <p v-if="freelancer.bio" class="text-[#435574] mb-6 whitespace-pre-wrap">{{ freelancer.bio }}</p>
                  <p v-else class="text-[#435574] mb-6">Nenhuma descrição disponível.</p>

                  <!-- Service Cities -->
                  <div v-if="freelancer.serviceCities && freelancer.serviceCities.length > 0" class="mb-6">
                    <h3 class="font-semibold text-[#263238] mb-3">Cidades atendidas</h3>
                    <div class="flex flex-wrap gap-2">
                      <Badge v-for="city in freelancer.serviceCities" :key="city.id" variant="secondary">
                        {{ city.city }}, {{ city.state }}
                      </Badge>
                    </div>
                  </div>

                  <!-- Professional Links -->
                  <div v-if="freelancer.professionalLinks && freelancer.professionalLinks.length > 0">
                    <h3 class="font-semibold text-[#263238] mb-3">Links profissionais</h3>
                    <div class="space-y-2">
                      <a
                        v-for="link in freelancer.professionalLinks"
                        :key="link.id"
                        :href="link.url"
                        target="_blank"
                        class="flex items-center gap-2 text-[#286AD8] hover:underline"
                      >
                        <ChevronRight class="h-4 w-4" />
                        {{ link.label || link.type }}
                      </a>
                    </div>
                  </div>
                </Card>
              </TabsContent>

              <!-- Portfolio Tab -->
              <TabsContent value="portfolio">
                <div v-if="portfolio.length > 0" class="grid md:grid-cols-2 gap-4">
                  <Card
                    v-for="project in portfolio"
                    :key="project.id"
                    class="overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
                  >
                    <img
                      v-if="project.images && project.images.length > 0"
                      :src="project.images[0].url"
                      :alt="project.title"
                      class="w-full h-48 object-cover"
                    />
                    <div class="p-4">
                      <Badge variant="secondary" class="mb-2">{{ project.category }}</Badge>
                      <h4 class="font-semibold text-[#263238] mb-1">{{ project.title }}</h4>
                      <p class="text-sm text-[#435574]">{{ project.description }}</p>
                    </div>
                  </Card>
                </div>
                <Card v-else class="p-12 text-center">
                  <Award class="h-12 w-12 text-[#435574]/30 mx-auto mb-4" />
                  <p class="text-[#435574]">Nenhum item no portfólio ainda</p>
                </Card>
              </TabsContent>
            </Tabs>
          </div>

          <!-- Sidebar -->
          <div class="space-y-6">
            <!-- Pricing -->
            <Card class="p-6">
              <h3 class="font-semibold text-[#263238] mb-3">Valor</h3>
              <p class="text-2xl font-bold text-[#286AD8] mb-2">
                {{ formatPrice(freelancer.hourlyRate || 0, 'HOURLY') }}
              </p>
              <p class="text-sm text-[#435574]">*Valores podem variar de acordo com a complexidade</p>
            </Card>

            <!-- Work Model -->
            <Card class="p-6">
              <h3 class="font-semibold text-[#263238] mb-3">Modelo de trabalho</h3>
              <Badge variant="secondary" class="text-sm">
                {{ freelancer.workModel === 'remote' ? 'Remoto' : freelancer.workModel === 'onsite' ? 'Presencial' : 'Híbrido' }}
              </Badge>
            </Card>

            <!-- Stats -->
            <Card class="p-6">
              <h3 class="font-semibold text-[#263238] mb-4">Estatísticas</h3>
              <div class="space-y-3 text-sm">
                <div class="flex items-center justify-between">
                  <span class="text-[#435574]">Avaliação</span>
                  <span class="text-[#263238] font-semibold">{{ freelancer.rating?.toFixed(1) || '0.0' }}/5.0</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-[#435574]">Trabalhos concluídos</span>
                  <span class="text-[#263238] font-semibold">{{ freelancer.completedJobs || 0 }}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-[#435574]">Serviços ativos</span>
                  <span class="text-[#263238] font-semibold">{{ services.length }}</span>
                </div>
              </div>
            </Card>

            <!-- Verification -->
            <Card class="p-6">
              <h3 class="font-semibold text-[#263238] mb-4">Verificações</h3>
              <div class="space-y-2 text-sm">
                <div class="flex items-center gap-2 text-green-600">
                  <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center text-xs">
                    ✓
                  </div>
                  <span>E-mail verificado</span>
                </div>
                <div class="flex items-center gap-2 text-green-600">
                  <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center text-xs">
                    ✓
                  </div>
                  <span>Telefone verificado</span>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
