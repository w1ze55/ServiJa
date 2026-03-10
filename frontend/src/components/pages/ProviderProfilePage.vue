<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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
  ExternalLink,
  Calendar,
  ChevronLeft,
  X as CloseIcon,
  Github,
  Linkedin,
  Instagram,
  Twitter,
  Facebook,
  Youtube,
  Globe,
  Link2,
  CheckCircle,
  MessageCircle,
  Paperclip,
  Trash2,
} from 'lucide-vue-next';
import { uploadImages } from '@/api/uploadService';
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
import { useFavoritesStore } from '@/stores/favorites';

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
  completedDate: string;
  client: string;
  tags: string[];
  featured: boolean;
  images: Array<{
    id: number;
    imageUrl: string;
    caption: string;
    displayOrder: number;
  }>;
  projectUrl?: string;
  videoUrl?: string;
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
const displayedServicesCount = ref(6);

const paginatedServices = computed(() => {
  return services.value.slice(0, displayedServicesCount.value);
});

const hasMoreServices = computed(() => {
  return services.value.length > displayedServicesCount.value;
});

const handleShowMoreServices = () => {
  displayedServicesCount.value += 6;
};
const quoteBudget = ref('');
const formattedQuoteBudget = ref('');
const quoteDeadline = ref('');
const quoteDeadlineDate = ref('');
const quoteDeadlineType = ref<'estimated' | 'decided'>('estimated');
const quotePriority = ref('MEDIUM');
const requestedServiceTitle = ref('');
const showQuoteDialog = ref(false);
const activeTab = ref('services');
const isSendingQuote = ref(false);
const quoteAttachments = ref<File[]>([]);
const favoritesStore = useFavoritesStore();
const router = useRouter();
const fileInputRef = ref<HTMLInputElement | null>(null);
const selectedProject = ref<PortfolioProject | null>(null);
const selectedImageIndex = ref(0);

const route = useRoute();
const selectedServiceModal = ref<ServiceItem | null>(null);
const selectedServiceImageIndex = ref(0);


const loadFreelancerData = async () => {
  isLoading.value = true;
  error.value = '';

  try {
    // Buscar dados do freelancer
    const freelancerResponse = await api.get(`/freelancer/${props.providerId}/profile`);
    freelancer.value = freelancerResponse.data;

    // Buscar serviços do freelancer
    const servicesResponse = await api.get(`/services/public/freelancer/${props.providerId}`);
    services.value = servicesResponse.data;

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

    // Check for serviceId in URL query to auto-open modal
    const serviceId = route.query.serviceId;
    if (serviceId && services.value.length > 0) {
      const service = services.value.find(s => s.id === Number(serviceId));
      if (service) {
        selectedServiceModal.value = service;
        selectedServiceImageIndex.value = 0;
      }
    }
  }
};

// Watch for serviceId in query to open modal (handles navigation within the same profile)
watch(() => route.query.serviceId, (newId) => {
  if (newId && services.value.length > 0) {
    const service = services.value.find(s => s.id === Number(newId));
    if (service) {
      selectedServiceModal.value = service;
      selectedServiceImageIndex.value = 0;
    }
  }
});

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatPrice = (price: number, priceType: string) => {
  const formatted = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(price);
  const suffix = priceType === 'HOURLY' ? '/hora' : priceType === 'DAILY' ? '/dia' : '';
  return `${formatted}${suffix}`;
};

const formatPriceDisplay = (value: number): string => {
  if (!value) return '';
  const formatted = new Intl.NumberFormat('pt-BR', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
  return formatted;
};

const handleQuoteBudgetInput = (val: string) => {
  // Remove tudo que não é dígito
  let digits = val.replace(/\D/g, '');

  if (!digits) {
    formattedQuoteBudget.value = '';
    quoteBudget.value = '';
    return;
  }

  // Converter para centavos: últimos 2 dígitos são decimais
  let numValue = parseInt(digits) / 100;

  // Limite de 1 milhão (R$ 1.000.000,00)
  if (numValue > 1000000) {
    numValue = 1000000;
  }

  // Formatar para exibição
  formattedQuoteBudget.value = formatPriceDisplay(numValue);
  quoteBudget.value = numValue.toString();
};

const getDetailedApproximation = (totalDays: number) => {
  // Garantir máximo de 365 dias
  const d = Math.min(totalDays, 365);
  if (d <= 0) return '';

  if (d === 365) return '1 ano';

  if (d >= 30) {
    const months = Math.floor(d / 30);
    const rem = d % 30;
    let str = `${months} ${months === 1 ? 'mês' : 'meses'}`;
    if (rem > 0) str += ` e ${rem} ${rem === 1 ? 'dia' : 'dias'}`;
    return str;
  }

  if (d >= 7) {
    const weeks = Math.floor(d / 7);
    const rem = d % 7;
    let str = `${weeks} ${weeks === 1 ? 'semana' : 'semanas'}`;
    if (rem > 0) str += ` e ${rem} ${rem === 1 ? 'dia' : 'dias'}`;
    return str;
  }

  return '';
};

const formatDelivery = (time: number, unit: string, showApproximation: boolean = true) => {
  // Garantir máximo de 365 dias se a unidade for dias
  const effectiveTime = unit === 'DAYS' && time > 365 ? 365 : time;

  const unitLabel = unit === 'HOURS' ? 'hora(s)' : unit === 'DAYS' ? 'dia(s)' : 'semana(s)';

  let approximation = '';
  if (showApproximation && unit === 'DAYS') {
    const detailed = getDetailedApproximation(effectiveTime);
    if (detailed) {
      approximation = ` =~ ${detailed}`;
    }
  }

  return `${effectiveTime} ${unitLabel}${approximation}`;
};

const getDeadlineApproximation = (days: string) => {
  const d = parseInt(days);
  if (isNaN(d) || d <= 0) return '';

  const detailed = getDetailedApproximation(d);
  return detailed ? `~ ${detailed}` : '';
};

const handleDeadlineInput = (val: string) => {
  // Apenas dígitos
  let digits = val.replace(/\D/g, '');

  if (digits) {
    const d = parseInt(digits);
    if (d > 365) {
      if (quoteDeadline.value === '365') {
        // Truque para forçar o Vue a atualizar o DOM mesmo quando o valor reativo não muda
        quoteDeadline.value = '';
        nextTick(() => {
          quoteDeadline.value = '365';
        });
      } else {
        quoteDeadline.value = '365';
      }
      return;
    }
  }

  quoteDeadline.value = digits;
};

const handleViewProject = (project: PortfolioProject) => {
  selectedProject.value = project;
  selectedImageIndex.value = 0;
};

const getLinkIcon = (type: string) => {
  const t = type.toLowerCase();
  if (t.includes('github')) return Github;
  if (t.includes('linkedin')) return Linkedin;
  if (t.includes('instagram')) return Instagram;
  if (t.includes('twitter') || t === 'x') return Twitter;
  if (t.includes('facebook')) return Facebook;
  if (t.includes('youtube')) return Youtube;
  if (t.includes('portfolio') || t.includes('website') || t.includes('site')) return Globe;
  return Link2;
};

const handleSendQuote = async () => {
  if (!quoteMessage.value.trim()) {
    toast.error('Por favor, descreva o serviço que você precisa');
    return;
  }

  if (!quoteCategory.value) {
    toast.error('Erro: Categoria do serviço não definida. Tente selecionar o serviço novamente.');
    return;
  }

  if (!quoteBudget.value || Number(quoteBudget.value) < 5) {
    toast.error('Por favor, informe um orçamento de no mínimo R$ 5,00');
    return;
  }

  const deadline = quoteDeadlineType.value === 'decided'
    ? quoteDeadlineDate.value
    : `${quoteDeadline.value} dias`;

  if (!quoteDeadline.value && quoteDeadlineType.value === 'estimated') {
    toast.error('Por favor, informe a quantidade de dias');
    return;
  }
  if (!quoteDeadlineDate.value && quoteDeadlineType.value === 'decided') {
    toast.error('Por favor, selecione uma data');
    return;
  }

  isSendingQuote.value = true;
  try {
    // Upload attachments to S3 first if any
    let attachmentUrls: string[] = [];
    if (quoteAttachments.value.length > 0) {
      toast.info('Enviando anexos...');
      attachmentUrls = await uploadImages(quoteAttachments.value);
    }

    const requestData = {
      serviceName: requestedServiceTitle.value ? `${requestedServiceTitle.value} - Solicitação` : `${quoteCategory.value} - Solicitação`,
      category: quoteCategory.value,
      description: requestedServiceTitle.value
        ? `[Solicitação referente ao serviço: ${requestedServiceTitle.value}]\n\n${quoteMessage.value}`
        : quoteMessage.value,
      budget: Number(quoteBudget.value),
      deadline: deadline,
      priority: quotePriority.value,
      address: '',
      freelancerId: Number(props.providerId),
      attachments: attachmentUrls,
    };

    console.log('Enviando solicitação:', requestData);
    await api.post('/service-requests', requestData);

    toast.success('Solicitação de orçamento enviada com sucesso!');
    showQuoteDialog.value = false;
    resetQuoteForm();

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

const handleAddFiles = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;
  const maxSize = 10 * 1024 * 1024; // 10MB per file
  Array.from(input.files).forEach((file) => {
    if (file.size > maxSize) {
      toast.error(`${file.name} excede 10MB e não pode ser anexado`);
    } else if (quoteAttachments.value.length < 5) {
      quoteAttachments.value.push(file);
    } else {
      toast.warning('Máximo de 5 arquivos por solicitação');
    }
  });
  if (fileInputRef.value) fileInputRef.value.value = '';
};

const handleRemoveAttachment = (index: number) => {
  quoteAttachments.value.splice(index, 1);
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const resetQuoteForm = () => {
  quoteMessage.value = '';
  quoteCategory.value = '';
  quoteBudget.value = '';
  formattedQuoteBudget.value = '';
  quoteDeadline.value = '';
  quoteDeadlineDate.value = '';
  quotePriority.value = 'MEDIUM';
  requestedServiceTitle.value = '';
  quoteAttachments.value = [];
};

const handleServiceClick = (service: ServiceItem) => {
  selectedServiceModal.value = service;
  selectedServiceImageIndex.value = 0;

  // Update URL to include serviceId
  router.replace({
    query: { ...route.query, serviceId: service.id }
  });
};

const handleCloseServiceModal = () => {
  selectedServiceModal.value = null;

  // Remove serviceId from URL
  const query = { ...route.query };
  delete query.serviceId;

  router.replace({ query });
};

const handleRequestService = (service: ServiceItem) => {
  quoteCategory.value = service.category;
  requestedServiceTitle.value = service.title;
  quoteDeadlineType.value = 'estimated';
  quoteDeadlineDate.value = '';
  quoteDeadline.value = ''; // Clear previous estimate
  quotePriority.value = 'MEDIUM';
  selectedServiceModal.value = null;
  showQuoteDialog.value = true;
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

const handleToggleFavorite = async () => {
  await favoritesStore.toggleFavoriteFreelancer(props.providerId);
};

const isFavorite = computed(() => {
  return favoritesStore.isFavoriteFreelancer(props.providerId);
});

const isServiceFavorite = (serviceId: number) => {
  return favoritesStore.isFavoriteService(serviceId);
};

const handleToggleServiceFavorite = async (serviceId: number) => {
  await favoritesStore.toggleFavoriteService(serviceId);
};

onMounted(() => {
  loadFreelancerData();
  if (!favoritesStore.initialized) {
    favoritesStore.fetchFavorites();
  }
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
                  <Button
                    variant="outline"
                    size="icon"
                    @click="handleToggleFavorite"
                    :class="isFavorite ? 'text-red-500 border-red-200 bg-red-50 hover:bg-red-100 hover:text-red-600' : 'text-gray-400 hover:text-red-500'"
                  >
                    <Heart :class="['h-5 w-5', isFavorite ? 'fill-red-500 text-red-500' : '']" />
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

              <!-- Action Buttons removed as per request -->
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
                <div class="mb-6">
                  <h3 class="text-[#263238] mb-1">Serviços cadastrados</h3>
                  <p class="text-[#435574]">
                    Disponíveis para contratação ({{ services.length }} itens)
                  </p>
                </div>

                <div v-if="services.length > 0">
                  <div class="grid md:grid-cols-2 gap-4 mb-8">
                    <Card
                      v-for="service in paginatedServices"
                      :key="service.id"
                      class="overflow-hidden hover:shadow-lg hover:border-[#286AD8]/30 transition-all cursor-pointer group"
                      @click="handleServiceClick(service)"
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
                        <Badge class="absolute top-3 left-3 bg-[#286AD8]/90 backdrop-blur-sm">
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

                  <!-- Show More Button -->
                  <div v-if="hasMoreServices" class="flex justify-center mt-8">
                    <Button
                      variant="outline"
                      @click="handleShowMoreServices"
                      class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50 px-8"
                    >
                      Ver mais serviços
                    </Button>
                  </div>
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
                        class="flex items-center gap-3 p-3 rounded-lg border border-gray-100 hover:border-[#286AD8]/30 hover:bg-blue-50/30 transition-all group"
                      >
                        <component :is="getLinkIcon(link.type)" class="h-5 w-5 text-[#435574] group-hover:text-[#286AD8]" />
                        <span class="text-[#263238] font-medium group-hover:text-[#286AD8]">{{ link.label || link.type }}</span>
                        <ExternalLink class="h-3.5 w-3.5 ml-auto text-gray-300 group-hover:text-[#286AD8]" />
                      </a>
                    </div>
                  </div>
                </Card>
              </TabsContent>

              <!-- Portfolio Tab -->
              <TabsContent value="portfolio">
                <div class="mb-6">
                  <h3 class="text-[#263238] mb-1">Portfólio</h3>
                  <p class="text-[#435574]">
                    Galeria de trabalhos realizados ({{ portfolio.length }} itens)
                  </p>
                </div>

                <!-- Professional Links (Compact) -->
                <div v-if="freelancer.professionalLinks && freelancer.professionalLinks.length > 0" class="mb-8 flex flex-wrap gap-2 justify-center">
                  <a
                    v-for="link in freelancer.professionalLinks"
                    :key="link.id"
                    :href="link.url"
                    target="_blank"
                    class="flex items-center gap-2 px-3 py-1.5 rounded-full border border-gray-200 bg-white text-xs text-[#435574] hover:border-[#286AD8] hover:text-[#286AD8] transition-all"
                  >
                    <component :is="getLinkIcon(link.type)" class="h-3.5 w-3.5" />
                    <span>{{ link.label || link.type }}</span>
                  </a>
                </div>

                <div v-if="portfolio.length > 0" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Card
                    v-for="project in portfolio"
                    :key="project.id"
                    @click="handleViewProject(project)"
                    class="group relative bg-white border border-gray-200 rounded-lg overflow-hidden cursor-pointer hover:border-[#286AD8] hover:shadow-lg transition-all"
                  >
                    <div class="aspect-video relative bg-gray-100 overflow-hidden">
                      <img
                        v-if="project.images && project.images.length > 0"
                        :src="project.images[0].imageUrl"
                        :alt="project.title"
                        class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                      <div v-else class="w-full h-full flex items-center justify-center">
                        <Briefcase class="h-10 w-10 text-[#286AD8]/30" />
                      </div>
                      <Badge
                        v-if="project.featured"
                        class="absolute top-2 left-2 bg-yellow-500 hover:bg-yellow-600 text-white border-none text-[10px] px-1.5"
                      >
                        <Sparkles class="h-3 w-3 mr-1" />
                        Destaque
                      </Badge>
                    </div>

                    <div class="p-4">
                      <p class="text-sm text-[#435574] line-clamp-2 mb-3">
                        {{ project.description }}
                      </p>

                      <!-- Project Links on Card -->
                      <div v-if="project.projectUrl || project.videoUrl" class="flex gap-2 mb-3">
                        <Badge v-if="project.projectUrl" variant="outline" class="text-[10px] text-[#286AD8] border-[#286AD8]/20 bg-blue-50/50">
                          <Link2 class="h-3 w-3 mr-1" /> Link
                        </Badge>
                        <Badge v-if="project.videoUrl" variant="outline" class="text-[10px] text-red-600 border-red-200 bg-red-50/50">
                          <Youtube class="h-3 w-3 mr-1" /> Vídeo
                        </Badge>
                      </div>

                      <div v-if="project.tags && project.tags.length > 0" class="flex flex-wrap gap-1">
                        <Badge
                          v-for="(tag, index) in project.tags.slice(0, 2)"
                          :key="index"
                          variant="secondary"
                          class="bg-blue-50 text-[#286AD8] text-[10px] px-1.5 py-0"
                        >
                          {{ tag }}
                        </Badge>
                        <Badge
                          v-if="project.tags.length > 2"
                          variant="secondary"
                          class="bg-gray-50 text-[#435574] text-[10px] px-1.5 py-0"
                        >
                          +{{ project.tags.length - 2 }}
                        </Badge>
                      </div>
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
            <!-- Work Model -->
            <Card class="p-6">
              <h3 class="font-semibold text-[#263238] mb-3">Modelo de trabalho</h3>
              <div class="space-y-4">
                <div class="flex items-center justify-between">
                  <span class="text-sm text-[#435574]">Modo:</span>
                  <Badge variant="secondary" class="text-sm">
                    {{ freelancer.workModel === 'remoto' ? 'Remoto' : freelancer.workModel === 'presencial' ? 'Presencial' : freelancer.workModel === 'hibrido' ? 'Híbrido' : 'Não informado' }}
                  </Badge>
                </div>

                <div v-if="freelancer.serviceType" class="flex items-center justify-between border-t pt-3">
                  <span class="text-sm text-[#435574]">Tipo:</span>
                  <span class="text-sm font-medium text-[#263238]">{{ freelancer.serviceType }}</span>
                </div>

                <div v-if="(freelancer.workModel === 'presencial' || freelancer.workModel === 'hibrido') && freelancer.serviceCities?.length" class="border-t pt-3">
                  <p class="text-sm text-[#435574] mb-2">Cidades atendidas:</p>
                  <div class="flex flex-wrap gap-1">
                    <Badge v-for="city in freelancer.serviceCities.slice(0, 3)" :key="city.id" variant="outline" class="text-[10px] bg-white text-gray-600">
                      {{ city.city }}
                    </Badge>
                    <span v-if="freelancer.serviceCities.length > 3" class="text-[10px] text-gray-400 self-center ml-1">
                      +{{ freelancer.serviceCities.length - 3 }}
                    </span>
                  </div>
                </div>
              </div>
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
                  <span class="text-[#263238] font-semibold">{{ freelancer.completedJobs || (freelancer as any).completedProjects || 0 }}</span>
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

    <!-- Project Details Modal -->
    <Dialog :open="selectedProject !== null" @update:open="(val) => !val && (selectedProject = null)">
      <DialogContent class="max-w-4xl max-h-[90vh] p-0 border-none bg-transparent" :hide-close="true">
        <div class="relative bg-white rounded-xl overflow-hidden shadow-2xl h-[80vh]">
          <!-- Custom Close Button -->
          <button
            @click="selectedProject = null"
            class="absolute top-4 right-4 p-2 bg-black/20 hover:bg-black/40 backdrop-blur-md rounded-full text-white transition-all z-50"
          >
            <CloseIcon class="h-5 w-5" />
          </button>
          <div class="grid grid-cols-1 md:grid-cols-2 h-full">
            <!-- Image Carousel -->
            <div class="relative bg-gray-900 flex items-center justify-center min-h-[300px] md:min-h-[500px]">
              <img
                v-if="selectedProject?.images && selectedProject.images.length > 0"
                :src="selectedProject.images[selectedImageIndex].imageUrl"
                :alt="selectedProject.title"
                class="w-full h-full object-contain"
              />
              <div v-else class="flex flex-col items-center text-white/50">
                <Briefcase class="h-16 w-16 mb-4" />
                <span>Nenhuma imagem disponível</span>
              </div>

              <!-- Carousel Controls -->
              <template v-if="selectedProject?.images && selectedProject.images.length > 1">
                <button
                  @click.stop="selectedImageIndex = (selectedImageIndex - 1 + selectedProject.images.length) % selectedProject.images.length"
                  class="absolute left-4 top-1/2 -translate-y-1/2 p-2 bg-black/40 hover:bg-black/60 text-white rounded-full transition-colors"
                >
                  <ChevronLeft class="h-6 w-6" />
                </button>
                <button
                  @click.stop="selectedImageIndex = (selectedImageIndex + 1) % selectedProject.images.length"
                  class="absolute right-4 top-1/2 -translate-y-1/2 p-2 bg-black/40 hover:bg-black/60 text-white rounded-full transition-colors"
                >
                  <ChevronRight class="h-6 w-6" />
                </button>

                <!-- Indicators -->
                <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-1.5">
                  <span
                    v-for="(_, index) in selectedProject.images"
                    :key="index"
                    class="w-2 h-2 rounded-full transition-all"
                    :class="selectedImageIndex === index ? 'bg-[#286AD8] w-4' : 'bg-white/40'"
                  ></span>
                </div>
              </template>
            </div>

            <!-- Project Details -->
            <div class="p-6 md:p-8 flex flex-col h-full overflow-y-auto">
              <div>
                <Badge variant="secondary" class="mb-3 bg-blue-50 text-[#286AD8] border-none">
                  {{ selectedProject?.category }}
                </Badge>
                <h2 class="text-2xl font-bold text-[#263238] mb-4 break-words">{{ selectedProject?.title }}</h2>

                <p class="text-[#435574] leading-relaxed mb-6 whitespace-pre-line break-words w-full">
                  {{ selectedProject?.description }}
                </p>
              </div>

              <div class="mt-auto pt-6 border-t border-gray-100 flex flex-col gap-4">
                <div class="flex flex-wrap items-center gap-3">
                  <Badge v-if="selectedProject?.featured" class="bg-yellow-500 text-white border-none">
                    <Sparkles class="h-3 w-3 mr-1" />
                    Projeto em Destaque
                  </Badge>
                  <div class="flex items-center gap-2 text-sm text-[#435574]">
                    <div class="p-2 bg-gray-50 rounded-lg">
                      <Calendar class="h-4 w-4 text-[#286AD8]" />
                    </div>
                    <div>
                      <p class="text-[10px] uppercase font-semibold text-gray-400">Concluído em</p>
                      <p class="font-medium">
                        {{ selectedProject?.completedDate ? new Date(selectedProject.completedDate).toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' }) : 'Data não informada' }}
                      </p>
                    </div>
                  </div>

                  <div v-if="selectedProject?.client" class="flex items-center gap-2 text-sm text-[#435574]">
                    <div class="p-2 bg-gray-50 rounded-lg">
                      <Sparkles class="h-4 w-4 text-[#286AD8]" />
                    </div>
                    <div>
                      <p class="text-[10px] uppercase font-semibold text-gray-400">Cliente</p>
                      <p class="font-medium">{{ selectedProject.client }}</p>
                    </div>
                  </div>
                </div>

                <div v-if="selectedProject?.tags && selectedProject.tags.length > 0" class="flex flex-wrap gap-2">
                  <Badge
                    v-for="tag in selectedProject.tags"
                    :key="tag"
                    variant="secondary"
                    class="bg-gray-100 hover:bg-gray-200 text-[#435574] transition-colors border-none"
                  >
                    {{ tag }}
                  </Badge>
                </div>

                <!-- NEW: Project Links in Modal -->
                <div v-if="selectedProject?.projectUrl || selectedProject?.videoUrl" class="flex flex-col sm:flex-row gap-3 pt-6 border-t border-gray-100">
                  <Button v-if="selectedProject?.projectUrl" asChild class="flex-1 bg-[#286AD8] hover:bg-[#435574]">
                    <a :href="selectedProject.projectUrl" target="_blank" class="flex items-center justify-center">
                      <ExternalLink class="h-4 w-4 mr-2" />
                      Acessar Projeto
                    </a>
                  </Button>
                  <Button v-if="selectedProject?.videoUrl" asChild variant="outline" class="flex-1">
                    <a :href="selectedProject.videoUrl" target="_blank" class="flex items-center justify-center">
                      <Youtube class="h-4 w-4 mr-2" />
                      Ver Vídeo
                    </a>
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>

    <!-- Service Details Modal (Deep Link / Direct Action) -->
    <Dialog :open="selectedServiceModal !== null" @update:open="(val) => !val && handleCloseServiceModal()">
      <DialogContent class="max-w-4xl max-h-[95vh] p-4 md:p-0 border-none bg-transparent" :hide-close="true">
        <div v-if="selectedServiceModal" class="relative bg-white rounded-xl overflow-y-auto shadow-2xl max-h-[90vh] md:max-h-[85vh]">
          <!-- Gallery / Cover Image -->
          <div class="relative h-64 md:h-80 bg-gray-900 overflow-hidden">
            <img
              v-if="selectedServiceModal.images && selectedServiceModal.images.length > 0"
              :src="selectedServiceModal.images[selectedServiceImageIndex]"
              :alt="selectedServiceModal.title"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex flex-col items-center justify-center text-white/30 bg-gray-800">
              <Briefcase class="h-16 w-16 mb-4" />
              <span>Nenhuma imagem disponível</span>
            </div>

            <!-- Carousel Controls -->
            <template v-if="selectedServiceModal.images && selectedServiceModal.images.length > 1">
              <button
                @click.stop="selectedServiceImageIndex = (selectedServiceImageIndex - 1 + selectedServiceModal.images.length) % selectedServiceModal.images.length"
                class="absolute left-4 top-1/2 -translate-y-1/2 p-2 bg-black/40 hover:bg-black/60 text-white rounded-full transition-colors flex items-center justify-center"
              >
                <ChevronLeft class="h-6 w-6" />
              </button>
              <button
                @click.stop="selectedServiceImageIndex = (selectedServiceImageIndex + 1) % selectedServiceModal.images.length"
                class="absolute right-4 top-1/2 -translate-y-1/2 p-2 bg-black/40 hover:bg-black/60 text-white rounded-full transition-colors flex items-center justify-center"
              >
                <ChevronRight class="h-6 w-6" />
              </button>

              <!-- Thumbnails / Indicators -->
              <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-2">
                <button
                  v-for="(img, index) in selectedServiceModal.images"
                  :key="index"
                  @click.stop="selectedServiceImageIndex = index"
                  :class="[
                    'w-12 h-12 rounded-lg overflow-hidden border-2 transition-all',
                    selectedServiceImageIndex === index ? 'border-[#286AD8] scale-110' : 'border-transparent opacity-70 hover:opacity-100'
                  ]"
                >
                  <img :src="img" class="w-full h-full object-cover" />
                </button>
              </div>
            </template>

            <button
              @click="handleCloseServiceModal"
              class="absolute top-4 right-4 p-2 bg-black/20 hover:bg-black/40 backdrop-blur-md rounded-full text-white transition-all z-50"
            >
              <CloseIcon class="h-5 w-5" />
            </button>
          </div>

          <div class="p-8">
            <!-- Freelancer Mini Profile -->
            <div class="flex items-center gap-4 mb-8 pb-8 border-b border-gray-100">
              <Avatar class="h-14 w-14 border-2 border-white shadow-sm">
                <AvatarImage :src="freelancer?.profilePhoto" :alt="freelancer?.name" />
                <AvatarFallback class="bg-[#286AD8] text-white">{{ freelancer?.name ? getInitials(freelancer.name) : '?' }}</AvatarFallback>
              </Avatar>
              <div class="flex-1">
                <h3 class="font-bold text-[#263238] text-lg">{{ freelancer?.name }}</h3>
                <div class="flex items-center gap-3 text-sm text-[#435574]">
                  <div class="flex items-center gap-1">
                    <Star class="h-4 w-4 fill-yellow-400 text-yellow-400" />
                    <span class="font-bold">{{ freelancer?.rating?.toFixed(1) || '0.0' }}</span>
                    <span class="text-gray-400">({{ freelancer?.totalReviews || 0 }})</span>
                  </div>
                  <span class="text-gray-300">•</span>
                  <div class="flex items-center gap-1">
                    <MapPin class="h-4 w-4 text-gray-400" />
                    <span>{{ freelancer?.location || 'Localização não informada' }}</span>
                  </div>
                </div>
              </div>
              <div v-if="selectedServiceModal" class="hidden sm:flex gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  class="rounded-full"
                  @click="handleToggleServiceFavorite(selectedServiceModal.id)"
                  :class="isServiceFavorite(selectedServiceModal.id) ? 'text-red-500 border-red-200 bg-red-50 hover:bg-red-100 hover:text-red-600' : 'text-gray-400 border-gray-200 hover:text-red-500'"
                >
                  <Heart :class="['h-5 w-5', isServiceFavorite(selectedServiceModal.id) ? 'fill-red-500 text-red-500' : '']" />
                </Button>
                <Button variant="outline" size="icon" class="rounded-full text-gray-400 border-gray-200" @click="handleShare">
                  <Share2 class="h-5 w-5" />
                </Button>
              </div>
            </div>

            <!-- Title & Category -->
            <div class="mb-8">
              <div class="flex items-center gap-2 mb-3">
                <Badge class="bg-blue-50 text-[#286AD8] border-none font-bold uppercase tracking-wider text-[10px] px-3">
                  {{ selectedServiceModal.category }}
                </Badge>
              </div>
              <h2 class="text-3xl font-bold text-[#263238] leading-tight">{{ selectedServiceModal.title }}</h2>
            </div>

            <!-- Card Info Grid -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-8">
              <div class="p-5 bg-gray-50 rounded-2xl flex items-start gap-4 border border-gray-100">
                <div class="p-3 bg-white rounded-xl text-[#286AD8] shadow-sm">
                  <DollarSign class="h-6 w-6" />
                </div>
                <div>
                  <span class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Investimento</span>
                  <p class="text-2xl font-black text-[#263238]">
                    {{ formatPrice(selectedServiceModal.price, selectedServiceModal.priceType) }}
                  </p>
                </div>
              </div>

              <div class="p-5 bg-gray-50 rounded-2xl flex items-start gap-4 border border-gray-100">
                <div class="p-3 bg-white rounded-xl text-green-600 shadow-sm">
                  <Clock class="h-6 w-6" />
                </div>
                <div>
                  <span class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Tempo estimado</span>
                  <p class="text-2xl font-black text-[#263238]">
                    {{ formatDelivery(selectedServiceModal.deliveryTime, selectedServiceModal.deliveryUnit, false) }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Description -->
            <div class="mb-8">
              <h3 class="text-lg font-bold text-[#263238] mb-4">Sobre o Serviço</h3>
              <p class="text-[#435574] leading-relaxed whitespace-pre-line text-base sm:text-lg break-words">
                {{ selectedServiceModal.description }}
              </p>
            </div>

            <!-- What's included (Features) -->
            <div class="mb-10">
              <h3 class="text-lg font-bold text-[#263238] mb-4">O que está incluído</h3>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-y-4 gap-x-8">
                <div class="flex items-center gap-3 text-[#435574] leading-tight">
                  <CheckCircle class="h-5 w-5 text-green-500 flex-shrink-0" />
                  <span>Atendimento profissional e especializado</span>
                </div>
                <div class="flex items-center gap-3 text-[#435574] leading-tight">
                  <CheckCircle class="h-5 w-5 text-green-500 flex-shrink-0" />
                  <span>Materiais e ferramentas inclusas</span>
                </div>
                <div class="flex items-center gap-3 text-[#435574] leading-tight">
                  <CheckCircle class="h-5 w-5 text-green-500 flex-shrink-0" />
                  <span>Garantia de qualidade e suporte</span>
                </div>
                <div class="flex items-center gap-3 text-[#435574] leading-tight">
                  <CheckCircle class="h-5 w-5 text-green-500 flex-shrink-0" />
                  <span>Agendamento flexível de acordo com sua necessidade</span>
                </div>
              </div>
            </div>

            <!-- Freelancer Stats Bar -->
            <div class="bg-gray-50 rounded-2xl p-6 mb-10">
              <div class="grid grid-cols-3 gap-8 text-center">
                <div>
                  <p class="text-2xl font-black text-[#263238]">{{ freelancer?.completedJobs || 0 }}</p>
                  <p class="text-xs font-bold text-[#435574] uppercase tracking-wider">Trabalhos Concluídos</p>
                </div>
                <div>
                  <p class="text-2xl font-black text-[#263238]">{{ freelancer?.rating?.toFixed(1) || '0.0' }}</p>
                  <p class="text-xs font-bold text-[#435574] uppercase tracking-wider">Avaliação Média</p>
                </div>
                <div>
                  <p class="text-2xl font-black text-[#263238]">95%</p>
                  <p class="text-xs font-bold text-[#435574] uppercase tracking-wider">Taxa de Sucesso</p>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div class="flex flex-col sm:flex-row gap-4 mb-4">
              <Button
                @click="handleRequestService(selectedServiceModal)"
                class="flex-1 bg-[#286AD8] hover:bg-[#435574] h-14 text-base font-bold shadow-lg shadow-[#286AD8]/20 transition-all active:scale-95 rounded-xl"
              >
                <Send class="h-5 w-5 mr-2" />
                Solicitar este serviço
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
    <!-- Quote Dialog -->
    <Dialog v-model:open="showQuoteDialog">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>Solicitar Orçamento</DialogTitle>
        </DialogHeader>
        <div class="space-y-4">
          <!-- Categoria e Prioridade removidos conforme solicitação -->

          <div>
            <Label class="text-[#435574] mb-2 block text-sm">
              Orçamento estimado <span class="text-red-500">*</span>
            </Label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">
                R$
              </span>
              <Input
                type="text"
                inputmode="numeric"
                placeholder="0,00"
                :model-value="formattedQuoteBudget"
                @update:model-value="(val: string) => handleQuoteBudgetInput(val)"
                class="pl-10"
                :class="{ 'border-red-500 focus-visible:ring-red-500': quoteBudget && Number(quoteBudget) < 5 }"
              />
            </div>
            <p v-if="quoteBudget && Number(quoteBudget) < 5" class="text-[11px] text-red-500 font-medium mt-1">
              O orçamento mínimo é R$ 5,00
            </p>
          </div>

          <div class="space-y-3">
            <Label class="text-[#435574] mb-2 block text-sm">
              Prazo desejado <span class="text-red-500">*</span>
            </Label>

            <div class="flex p-1 bg-gray-100 rounded-lg mb-2">
              <button
                type="button"
                @click="quoteDeadlineType = 'estimated'"
                :class="[
                  'flex-1 py-1.5 text-xs font-bold rounded-md transition-all',
                  quoteDeadlineType === 'estimated' ? 'bg-white text-[#286AD8] shadow-sm' : 'text-[#435574] hover:text-[#263238]'
                ]"
              >
                Prazo estimado
              </button>
              <button
                type="button"
                @click="quoteDeadlineType = 'decided'"
                :class="[
                  'flex-1 py-1.5 text-xs font-bold rounded-md transition-all',
                  quoteDeadlineType === 'decided' ? 'bg-white text-[#286AD8] shadow-sm' : 'text-[#435574] hover:text-[#263238]'
                ]"
              >
                Prazo decidido
              </button>
            </div>

            <div v-if="quoteDeadlineType === 'estimated'">
              <div class="relative">
                <Input
                  type="text"
                  inputmode="numeric"
                  placeholder="Quantidade de dias"
                  :model-value="quoteDeadline"
                  @update:model-value="handleDeadlineInput"
                  class="pr-24"
                />
                <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-2 pointer-events-none">
                  <span class="text-xs text-gray-400 font-medium">dia(s)</span>
                  <Badge v-if="getDeadlineApproximation(quoteDeadline)" variant="secondary" class="bg-blue-50 text-[#286AD8] border-[#286AD8]/20 px-2 h-6 text-[10px]">
                    {{ getDeadlineApproximation(quoteDeadline) }}
                  </Badge>
                </div>
              </div>
            </div>
            <div v-else>
              <Input
                type="date"
                v-model="quoteDeadlineDate"
                class="w-full"
              />
            </div>
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

          <!-- Attachments -->
          <div>
            <Label class="text-[#435574] mb-2 block text-sm">
              Anexos <span class="text-xs text-gray-400">(opcional · até 5 arquivos · máx. 10MB cada)</span>
            </Label>
            <input
              ref="fileInputRef"
              type="file"
              multiple
              class="hidden"
              @change="handleAddFiles"
            />
            <button
              type="button"
              @click="fileInputRef?.click()"
              class="w-full border-2 border-dashed border-gray-200 rounded-lg p-4 flex items-center justify-center gap-2 text-sm text-[#435574] hover:border-[#286AD8] hover:text-[#286AD8] transition-colors cursor-pointer"
            >
              <Paperclip class="h-4 w-4" />
              Clique para anexar arquivos
            </button>

            <ul v-if="quoteAttachments.length > 0" class="mt-2 space-y-1">
              <li
                v-for="(file, i) in quoteAttachments"
                :key="i"
                class="flex items-center justify-between bg-blue-50 border border-blue-100 rounded-md px-3 py-2 text-sm"
              >
                <span class="flex items-center gap-2 min-w-0">
                  <Paperclip class="h-3.5 w-3.5 text-[#286AD8] shrink-0" />
                  <span class="truncate text-[#263238]">{{ file.name }}</span>
                  <span class="text-gray-400 shrink-0">{{ formatFileSize(file.size) }}</span>
                </span>
                <button type="button" @click="handleRemoveAttachment(i)" class="ml-2 text-gray-400 hover:text-red-500 transition-colors shrink-0">
                  <Trash2 class="h-3.5 w-3.5" />
                </button>
              </li>
            </ul>
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
</template>

```
