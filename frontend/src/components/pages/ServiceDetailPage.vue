<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  ArrowLeft,
  Star,
  MapPin,
  CheckCircle,
  Send,
  Heart,
  Share2,
  Briefcase,
  Loader2,
  AlertCircle,
  Paperclip,
  Trash2,
} from 'lucide-vue-next';
import { uploadImages } from '@/api/uploadService';
import Button from '@/components/ui/button.vue';
import Card from '@/components/ui/card.vue';
import Badge from '@/components/ui/badge.vue';
import Avatar from '@/components/ui/avatar.vue';
import AvatarImage from '@/components/ui/avatar-image.vue';
import AvatarFallback from '@/components/ui/avatar-fallback.vue';
import Dialog from '@/components/ui/dialog.vue';
import DialogContent from '@/components/ui/dialog-content.vue';
import DialogHeader from '@/components/ui/dialog-header.vue';
import DialogTitle from '@/components/ui/dialog-title.vue';
import ScrollArea from '@/components/ui/scroll-area.vue';
import Textarea from '@/components/ui/textarea.vue';
import Input from '@/components/ui/input.vue';
import Label from '@/components/ui/label.vue';
import api from '@/api/config/axios';
import { toast } from '@/utils/toast';
import { useFavoritesStore } from '@/stores/favorites';
import { getWorkModelLabel, normalizeWorkModel } from '@/utils/workModel';
import ImageGallery from '@/components/shared/ImageGallery.vue';
import ReviewList from '@/components/shared/ReviewList.vue';
import type { ReviewItem } from '@/components/shared/ReviewList.vue';
import WorkModelBadge from '@/components/shared/WorkModelBadge.vue';

// ─── Types ───────────────────────────────────────────────────────────────────

interface ServiceItem {
  id: number;
  freelancerId: number;
  freelancerName: string;
  title: string;
  category: string;
  description: string;
  price: number;
  completedServicesCount?: number | null;
  averageCompletedPrice?: number | null;
  averagePriceMin?: number | null;
  averagePriceMax?: number | null;
  priceType: string;
  deliveryTime: number;
  deliveryUnit: string;
  workModel?: string | null;
  tags: string[];
  images: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface FreelancerData {
  id: number;
  name: string;
  profession: string;
  profilePhoto: string;
  rating: number;
  totalReviews: number;
  completedJobs: number;
  location: string;
  available: boolean;
}

type Review = ReviewItem;

interface QuoteServiceAddress {
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
}

interface QuoteAddressErrors {
  zipCode: string;
  street: string;
  number: string;
  neighborhood: string;
  city: string;
  state: string;
}

type WorkModel = 'PRESENCIAL' | 'REMOTO' | 'HIBRIDO';

// ─── Props ────────────────────────────────────────────────────────────────────

const props = defineProps<{
  providerId: number;
  serviceId: number;
}>();

// ─── State ────────────────────────────────────────────────────────────────────

const router = useRouter();
const favoritesStore = useFavoritesStore();

const service = ref<ServiceItem | null>(null);
const freelancer = ref<FreelancerData | null>(null);
const reviews = ref<Review[]>([]);
const isLoading = ref(true);
const isLoadingReviews = ref(false);
const error = ref('');

// Quote dialog state
const showQuoteDialog = ref(false);
const isSendingQuote = ref(false);
const quoteMessage = ref('');
const quoteBudget = ref('');
const formattedQuoteBudget = ref('');
const quoteDeadline = ref('');
const quoteDeadlineDate = ref('');
const quoteDeadlineType = ref<'estimated' | 'decided'>('estimated');
const quoteAttachments = ref<File[]>([]);
const quoteServiceAddress = ref<QuoteServiceAddress>(createEmptyAddress());
const quoteAddressErrors = ref<QuoteAddressErrors>(createEmptyAddressErrors());
const isLoadingCEP = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);

// ─── Helpers ──────────────────────────────────────────────────────────────────

function createEmptyAddress(): QuoteServiceAddress {
  return { zipCode: '', street: '', number: '', complement: '', neighborhood: '', city: '', state: '' };
}

function createEmptyAddressErrors(): QuoteAddressErrors {
  return { zipCode: '', street: '', number: '', neighborhood: '', city: '', state: '' };
}

const formatPrice = (price: number, priceType: string) => {
  const f = formatCurrency(price);
  const suffix = getPriceTypeSuffix(priceType);
  return `${f}${suffix}`;
};

const formatCurrency = (price: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(price);

const getPriceTypeSuffix = (priceType: string) =>
  priceType === 'HOURLY' ? '/hora' : priceType === 'DAILY' ? '/dia' : '';

const roundAverageRangeValue = (value: number) => {
  const normalized = (value + 0.1) / 10;
  const lowerCandidate = (Math.floor(normalized) * 10) - 0.1;
  const upperCandidate = (Math.ceil(normalized) * 10) - 0.1;

  if (lowerCandidate <= 0) {
    return Number(upperCandidate.toFixed(2));
  }

  const nearestCandidate = Math.abs(value - lowerCandidate) <= Math.abs(upperCandidate - value)
    ? lowerCandidate
    : upperCandidate;

  return Number(nearestCandidate.toFixed(2));
};

const formatAveragePriceRange = (min: number, max: number) => {
  const roundedMin = roundAverageRangeValue(min);
  const roundedMax = Math.max(roundAverageRangeValue(max), roundedMin);
  return `${formatCurrency(roundedMin)} - ${formatCurrency(roundedMax)}`;
};

const getInitials = (name: string) =>
  name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString('pt-BR', { day: 'numeric', month: 'short', year: 'numeric' });

const getDetailedApproximation = (totalDays: number) => {
  const d = Math.min(totalDays, 365);
  if (d <= 0) return '';
  if (d === 365) return '1 ano';
  if (d >= 30) {
    const months = Math.floor(d / 30);
    const rem = d % 30;
    return `${months} ${months === 1 ? 'mês' : 'meses'}${rem > 0 ? ` e ${rem} dia(s)` : ''}`;
  }
  if (d >= 7) {
    const weeks = Math.floor(d / 7);
    const rem = d % 7;
    return `${weeks} ${weeks === 1 ? 'semana' : 'semanas'}${rem > 0 ? ` e ${rem} dia(s)` : ''}`;
  }
  return '';
};

const getDeadlineApproximation = (days: string) => {
  const d = parseInt(days);
  if (isNaN(d) || d <= 0) return '';
  const detailed = getDetailedApproximation(d);
  return detailed ? `~ ${detailed}` : '';
};

// ─── Computed ─────────────────────────────────────────────────────────────────

const isFavorite = computed(() => favoritesStore.isFavoriteService(props.serviceId));
const workModel = computed(() => service.value ? normalizeWorkModel(service.value.workModel) : null);
const serviceRequiresAddress = computed(() => workModel.value === 'PRESENCIAL' || workModel.value === 'HIBRIDO');
const serviceWorkModelLabel = computed(() => getWorkModelLabel(service.value?.workModel));
const serviceExperienceLabel = computed(() => {
  const completedServicesCount = service.value?.completedServicesCount ?? 0;

  if (completedServicesCount <= 0) {
    return 'Primeiras entregas';
  }

  return `${completedServicesCount} ${completedServicesCount === 1 ? 'entrega concluída' : 'entregas concluídas'}`;
});
const hasAveragePriceRange = computed(() =>
  Boolean(
    service.value
    && (service.value.completedServicesCount ?? 0) > 0
    && service.value.averagePriceMin != null
    && service.value.averagePriceMax != null
  )
);
const servicePriceLabel = computed(() => hasAveragePriceRange.value ? 'Valor médio do serviço' : 'Valor aproximado');
const servicePriceDisplay = computed(() => {
  if (!service.value) return '';
  if (!hasAveragePriceRange.value) return formatPrice(service.value.price, service.value.priceType);
  return formatAveragePriceRange(service.value.averagePriceMin!, service.value.averagePriceMax!);
});
const averageRating = computed(() => {
  if (!reviews.value.length) return null;
  return reviews.value.reduce((sum, r) => sum + r.rating, 0) / reviews.value.length;
});

// ─── Data loading ─────────────────────────────────────────────────────────────

const loadData = async () => {
  isLoading.value = true;
  error.value = '';
  try {
    // Buscar perfil do freelancer e dados do serviço em paralelo
    const [freelancerRes, serviceRes] = await Promise.all([
      api.get(`/freelancer/${props.providerId}/profile`),
      api.get(`/services/public/${props.serviceId}`)
    ]);

    freelancer.value = freelancerRes.data;
    
    const serviceData = serviceRes.data as ServiceItem;

    if (!serviceData) {
      error.value = 'Serviço não encontrado.';
      return;
    }

    service.value = { 
      ...serviceData, 
      workModel: normalizeWorkModel(serviceData.workModel) 
    };

    // Load reviews separately (non-blocking)
    loadReviews();
  } catch (err: any) {
    console.error('Erro ao carregar serviço:', err);
    error.value = 'Não foi possível carregar os dados do serviço.';
    toast.error('Erro ao carregar serviço');
  } finally {
    isLoading.value = false;
  }
};

const loadReviews = async () => {
  isLoadingReviews.value = true;
  try {
    const res = await api.get(`/reviews/service/${props.serviceId}`);
    reviews.value = (res.data || []).map((r: any) => ({
      id: r.id,
      rating: r.rating,
      comment: r.comment,
      clientName: r.reviewerName,
      clientPhoto: r.reviewerAvatar,
      serviceName: r.serviceName,
      createdAt: r.createdAt,
    }));
  } catch {
    reviews.value = [];
  } finally {
    isLoadingReviews.value = false;
  }
};

// ─── Navigation ───────────────────────────────────────────────────────────────

const goBack = () => {
  if (window.history.state?.back) {
    router.back();
    return;
  }

  router.push({ name: 'provider-profile', params: { providerId: props.providerId } });
};

const handleShare = () => {
  const url = window.location.href;
  if (navigator.share) {
    navigator.share({ title: service.value?.title || '', url });
  } else {
    navigator.clipboard.writeText(url);
    toast.success('Link copiado!');
  }
};

const handleToggleFavorite = async () => {
  await favoritesStore.toggleFavoriteService(props.serviceId);
};

// ─── Quote dialog ─────────────────────────────────────────────────────────────

const handleRequestService = () => {
  resetQuoteForm();
  showQuoteDialog.value = true;
};

const resetQuoteForm = () => {
  quoteMessage.value = '';
  quoteDeadline.value = '';
  quoteDeadlineDate.value = '';
  quoteDeadlineType.value = 'estimated';
  quoteAttachments.value = [];
  quoteServiceAddress.value = createEmptyAddress();
  quoteAddressErrors.value = createEmptyAddressErrors();
  isLoadingCEP.value = false;
};

const blockNonNumericKeys = (event: KeyboardEvent) => {
  const allowed = ['Backspace','Delete','Tab','Escape','Enter','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'];
  if (allowed.includes(event.key) || event.ctrlKey || event.metaKey) return;
  if (!/^[0-9]$/.test(event.key)) event.preventDefault();
};

const handleQuoteBudgetInput = (val: string) => {};
const blockBudgetExceedingMax = () => {};
const handleBudgetPaste = () => {};

const handleDeadlineInput = (val: string) => {
  let digits = val.replace(/\D/g, '');
  if (digits) {
    const d = parseInt(digits);
    if (d > 365) { quoteDeadline.value = '365'; return; }
  }
  quoteDeadline.value = digits;
};

const handleDeadlinePaste = (event: ClipboardEvent) => {
  const digits = (event.clipboardData?.getData('text') || '').replace(/\D/g, '');
  if (digits) quoteDeadline.value = Math.min(parseInt(digits), 365).toString();
};

const formatCEP = (value: string) => {
  const numbers = value.replace(/\D/g, '').slice(0, 8);
  return numbers.replace(/(\d{5})(\d{0,3})/, '$1-$2');
};

const fetchAddressByCEP = async (cep: string) => {
  const cleanCEP = cep.replace(/\D/g, '');
  if (cleanCEP.length !== 8) return;
  isLoadingCEP.value = true;
  try {
    const response = await fetch(`https://viacep.com.br/ws/${cleanCEP}/json/`);
    const data = await response.json();
    if (data.erro) { toast.error('CEP não encontrado'); return; }
    quoteServiceAddress.value = {
      ...quoteServiceAddress.value,
      zipCode: formatCEP(cleanCEP),
      street: data.logradouro || '',
      neighborhood: data.bairro || '',
      city: data.localidade || '',
      state: data.uf || '',
      complement: quoteServiceAddress.value.complement || data.complemento || '',
    };
  } catch {
    toast.error('Não foi possível buscar o endereço pelo CEP informado');
  } finally {
    isLoadingCEP.value = false;
  }
};

const handleCEPInput = (value: string) => {
  const formatted = formatCEP(value);
  quoteServiceAddress.value.zipCode = formatted;
  quoteAddressErrors.value.zipCode = '';
  const clean = formatted.replace(/\D/g, '');
  if (clean.length === 8) fetchAddressByCEP(clean);
};

const validateAddress = () => {
  quoteAddressErrors.value = createEmptyAddressErrors();
  if (!serviceRequiresAddress.value) return true;
  let valid = true;
  const clean = quoteServiceAddress.value.zipCode.replace(/\D/g, '');
  if (!clean) { quoteAddressErrors.value.zipCode = 'CEP é obrigatório'; valid = false; }
  else if (clean.length !== 8) { quoteAddressErrors.value.zipCode = 'CEP deve ter 8 dígitos'; valid = false; }
  if (!quoteServiceAddress.value.street.trim()) { quoteAddressErrors.value.street = 'Rua é obrigatória'; valid = false; }
  if (!quoteServiceAddress.value.number.trim()) { quoteAddressErrors.value.number = 'Número é obrigatório'; valid = false; }
  if (!quoteServiceAddress.value.neighborhood.trim()) { quoteAddressErrors.value.neighborhood = 'Bairro é obrigatório'; valid = false; }
  if (!quoteServiceAddress.value.city.trim()) { quoteAddressErrors.value.city = 'Cidade é obrigatória'; valid = false; }
  if (!quoteServiceAddress.value.state.trim()) { quoteAddressErrors.value.state = 'Estado é obrigatório'; valid = false; }
  else if (quoteServiceAddress.value.state.trim().length !== 2) { quoteAddressErrors.value.state = 'Use a sigla do estado'; valid = false; }
  return valid;
};

const buildQuoteAddress = () => {
  const a = quoteServiceAddress.value;
  const streetLine = [`${a.street.trim()}, ${a.number.trim()}`, a.complement.trim()].filter(Boolean).join(', ');
  return `${streetLine} - ${a.neighborhood.trim()} - ${a.city.trim()}/${a.state.trim().toUpperCase()} - CEP: ${a.zipCode.trim()}`;
};

const handleAddFiles = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;
  const maxSize = 10 * 1024 * 1024;
  Array.from(input.files).forEach((file) => {
    if (file.size > maxSize) toast.error(`${file.name} excede 10MB`);
    else if (quoteAttachments.value.length < 5) quoteAttachments.value.push(file);
    else toast.warning('Máximo de 5 arquivos');
  });
  if (fileInputRef.value) fileInputRef.value.value = '';
};

const handleRemoveAttachment = (index: number) => { quoteAttachments.value.splice(index, 1); };

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const handleSendQuote = async () => {
  if (!quoteMessage.value.trim()) { toast.error('Por favor, descreva o serviço que você precisa'); return; }
  if (!quoteDeadline.value && quoteDeadlineType.value === 'estimated') { toast.error('Informe a quantidade de dias'); return; }
  if (!quoteDeadlineDate.value && quoteDeadlineType.value === 'decided') { toast.error('Selecione uma data'); return; }
  if (quoteDeadlineType.value === 'decided' && quoteDeadlineDate.value) {
    const selected = new Date(quoteDeadlineDate.value);
    const today = new Date(); today.setHours(0, 0, 0, 0);
    if (selected < today) { toast.error('A data desejada não pode ser anterior à data atual'); return; }
  }
  if (!validateAddress()) {
    const firstErr = Object.values(quoteAddressErrors.value).find(Boolean);
    toast.error(firstErr || 'Informe a localização do serviço');
    return;
  }

  const deadline = quoteDeadlineType.value === 'decided' ? quoteDeadlineDate.value : `${quoteDeadline.value} dias`;
  isSendingQuote.value = true;
  try {
    let attachmentUrls: string[] = [];
    if (quoteAttachments.value.length > 0) {
      toast.info('Enviando anexos...');
      attachmentUrls = await uploadImages(quoteAttachments.value);
    }
    await api.post('/service-requests', {
      serviceName: service.value ? `${service.value.title} - Solicitação` : 'Solicitação',
      category: service.value?.category || '',
      description: quoteMessage.value,
      deadline,
      workModel: workModel.value?.toUpperCase(),
      address: serviceRequiresAddress.value ? buildQuoteAddress() : null,
      serviceId: props.serviceId,
      freelancerId: props.providerId,
      attachments: attachmentUrls,
    });
    toast.success('Solicitação enviada com sucesso!');
    showQuoteDialog.value = false;
    resetQuoteForm();
    setTimeout(() => router.push({ name: 'my-requests' }), 1500);
  } catch (err: any) {
    toast.error(err.response?.data?.message || 'Erro ao enviar solicitação. Tente novamente.');
  } finally {
    isSendingQuote.value = false;
  }
};

// ─── Lifecycle ────────────────────────────────────────────────────────────────

onMounted(() => {
  loadData();
  if (!favoritesStore.initialized) favoritesStore.fetchFavorites();
});
</script>

<template>
  <div class="min-h-screen bg-gray-50">

    <!-- ── Loading ────────────────────────────────────────────── -->
    <div v-if="isLoading" class="flex items-center justify-center min-h-screen">
      <div class="text-center">
        <Loader2 class="h-10 w-10 text-[#286AD8] animate-spin mx-auto mb-4" />
        <p class="text-[#435574]">Carregando serviço...</p>
      </div>
    </div>

    <!-- ── Error ──────────────────────────────────────────────── -->
    <div v-else-if="error" class="flex flex-col items-center justify-center min-h-screen gap-4">
      <AlertCircle class="h-12 w-12 text-red-400" />
      <h3 class="text-lg font-semibold text-[#263238]">Ops, algo deu errado</h3>
      <p class="text-[#435574]">{{ error }}</p>
      <Button @click="loadData">Tentar novamente</Button>
    </div>

    <!-- ── Content ────────────────────────────────────────────── -->
    <div v-else-if="service && freelancer" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

      <!-- Back Button & Actions -->
      <div class="flex items-center justify-between mb-6">
        <Button
          variant="ghost"
          @click="goBack"
          class="text-[#435574] hover:text-[#286AD8]"
        >
          <ArrowLeft class="h-4 w-4 mr-2" />
          Voltar 
        </Button>
        <div class="flex items-center gap-2">
          <Button
            variant="outline"
            size="icon"
            @click="handleToggleFavorite"
            :class="isFavorite ? 'text-red-500 border-red-200 bg-red-50 hover:bg-red-100 hover:text-red-600' : 'text-gray-400 hover:text-red-500'"
          >
            <Heart :class="['h-5 w-5', isFavorite ? 'fill-red-500 text-red-500' : '']" />
          </Button>
          <Button variant="outline" size="icon" @click="handleShare" class="text-gray-400 hover:text-[#286AD8]">
            <Share2 class="h-5 w-5" />
          </Button>
        </div>
      </div>

      <!-- Main layout -->
      <div class="grid lg:grid-cols-3 gap-8">

          <!-- ── Left column ──────────────────────────────────── -->
          <div class="lg:col-span-2 space-y-6">

            <!-- Image Gallery -->
            <ImageGallery :images="service.images || []" :alt="service.title" />

            <!-- Title & Badges -->
            <div>
              <div class="flex flex-wrap items-center gap-2 mb-3">
                <Badge class="bg-blue-50 text-[#286AD8] border-none font-semibold uppercase tracking-wide text-[10px]">
                  {{ service.category }}
                </Badge>
              </div>
              <h1 class="text-2xl sm:text-3xl font-bold text-[#263238] leading-tight">{{ service.title }}</h1>
            </div>

            <!-- Quick stats -->
            <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
              <div class="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm flex items-center gap-3">
                <div class="p-2.5 bg-blue-50 rounded-xl text-[#286AD8]">
                  <CheckCircle class="h-5 w-5" />
                </div>
                <div>
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Experiência neste serviço</p>
                  <p class="text-base font-bold leading-tight text-[#263238]">{{ serviceExperienceLabel }}</p>
                </div>
              </div>

              <div class="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm flex items-center gap-3">
                <div class="p-2.5 bg-green-50 rounded-xl text-green-600">
                  <Briefcase class="h-5 w-5" />
                </div>
                <div>
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Modalidade</p>
                  <p class="text-base font-bold text-[#263238]">{{ serviceWorkModelLabel }}</p>
                </div>
              </div>

              <div v-if="reviews.length > 0" class="bg-white rounded-2xl p-4 border border-gray-100 shadow-sm flex items-center gap-3">
                <div class="p-2.5 bg-yellow-50 rounded-xl text-yellow-500">
                  <Star class="h-5 w-5 fill-yellow-400" />
                </div>
                <div>
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider">Avaliação</p>
                  <p class="text-base font-bold text-[#263238]">{{ averageRating?.toFixed(1) }} <span class="text-xs font-normal text-gray-400">({{ reviews.length }})</span></p>
                </div>
              </div>
            </div>

            <!-- Description -->
            <Card class="p-6">
              <h2 class="text-lg font-bold text-[#263238] mb-4">Sobre o Serviço</h2>
              <p class="text-[#435574] leading-relaxed whitespace-pre-line">{{ service.description }}</p>
            </Card>

            <!-- What's included -->
            <Card class="p-6">
              <h2 class="text-lg font-bold text-[#263238] mb-5">O que está incluído</h2>
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div class="flex items-start gap-3 text-[#435574]">
                  <CheckCircle class="h-5 w-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Atendimento profissional e especializado</span>
                </div>
                <div class="flex items-start gap-3 text-[#435574]">
                  <CheckCircle class="h-5 w-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Materiais e ferramentas inclusas</span>
                </div>
                <div class="flex items-start gap-3 text-[#435574]">
                  <CheckCircle class="h-5 w-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Garantia de qualidade e suporte</span>
                </div>
                <div class="flex items-start gap-3 text-[#435574]">
                  <CheckCircle class="h-5 w-5 text-green-500 shrink-0 mt-0.5" />
                  <span>Agendamento flexível conforme sua necessidade</span>
                </div>
              </div>
            </Card>

            <!-- Tags -->
            <div v-if="service.tags && service.tags.length > 0" class="flex flex-wrap gap-2">
              <Badge
                v-for="tag in service.tags"
                :key="tag"
                variant="secondary"
                class="bg-gray-100 text-[#435574] hover:bg-gray-200 transition-colors"
              >
                {{ tag }}
              </Badge>
            </div>

            <!-- ── Reviews Section ──────────────────────────── -->
            <ReviewList :reviews="reviews" :is-loading="isLoadingReviews" />
          </div>

          <!-- ── Right sidebar (sticky) ───────────────────────── -->
          <div class="space-y-4">
            <div class="lg:sticky lg:top-20 space-y-4">

              <!-- Price CTA card -->
              <Card class="p-6 border-2 border-[#286AD8]/20">
                <div class="mb-1">
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-wider mb-1">{{ servicePriceLabel }}</p>
                  <p class="text-3xl font-black leading-tight text-[#263238]">{{ servicePriceDisplay }}</p>
                  <p v-if="hasAveragePriceRange" class="mt-2 text-xs text-[#435574]">
                    Faixa calculada com base nos serviços concluídos.
                  </p>
                </div>
                <Button
                  @click="handleRequestService"
                  class="w-full h-13 text-base font-bold shadow-lg shadow-primary/20 transition-all active:scale-95 rounded-xl mb-3"
                >
                  <Send class="h-5 w-5 mr-2" />
                  Solicitar este serviço
                </Button>
                <p class="text-xs text-center text-gray-400">Sem compromisso · Receba um orçamento</p>
              </Card>

              <!-- Provider card -->
              <Card class="p-5">
                <div
                  class="flex items-center gap-3 mb-4 cursor-pointer group"
                  @click="goBack"
                >
                  <Avatar class="h-12 w-12 border-2 border-[#286AD8]/20">
                    <AvatarImage :src="freelancer.profilePhoto" :alt="freelancer.name" />
                    <AvatarFallback class="bg-[#286AD8] text-white font-bold">
                      {{ getInitials(freelancer.name) }}
                    </AvatarFallback>
                  </Avatar>
                  <div class="flex-1 min-w-0">
                    <p class="font-bold text-[#263238] group-hover:text-[#286AD8] transition-colors truncate">{{ freelancer.name }}</p>
                    <p class="text-sm text-[#435574] truncate">{{ freelancer.profession }}</p>
                  </div>
                </div>

                <div class="space-y-2 text-sm text-[#435574] mb-4">
                  <div class="flex items-center gap-2">
                    <Star class="h-4 w-4 fill-yellow-400 text-yellow-400 shrink-0" />
                    <span class="font-semibold text-[#263238]">{{ freelancer.rating?.toFixed(1) || '0.0' }}</span>
                    <span>({{ freelancer.totalReviews || 0 }} avaliações)</span>
                  </div>
                  <div v-if="freelancer.location" class="flex items-center gap-2">
                    <MapPin class="h-4 w-4 shrink-0" />
                    <span>{{ freelancer.location }}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <Briefcase class="h-4 w-4 shrink-0" />
                    <span>{{ freelancer.completedJobs || 0 }} trabalhos concluídos</span>
                  </div>
                </div>

                <div class="mb-4">
                  <Badge v-if="freelancer.available" class="bg-green-100 text-green-700 hover:bg-green-100">
                    ✓ Disponível agora
                  </Badge>
                  <Badge v-else variant="secondary">Indisponível</Badge>
                </div>

                <Button variant="outline" class="w-full text-primary border-primary/30" @click="goBack">
                  Ver perfil completo
                </Button>
              </Card>

              <!-- Safety info -->
              <Card class="p-4 bg-blue-50/50 border-blue-100">
                <div class="space-y-2 text-sm text-[#435574]">
                  <div class="flex items-center gap-2 text-green-600 font-medium">
                    <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center text-xs">✓</div>
                    <span>Profissional verificado</span>
                  </div>
                  <div class="flex items-center gap-2 text-green-600 font-medium">
                    <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center text-xs">✓</div>
                    <span>Orçamento sem compromisso</span>
                  </div>
                </div>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Quote Dialog ──────────────────────────────────────────────────── -->
    <Dialog v-model:open="showQuoteDialog">
      <DialogContent class="max-w-md max-h-[90vh] overflow-hidden p-0 gap-0">
        <ScrollArea class="mt-10 max-h-[calc(90vh-2.5rem)]">
          <div class="space-y-4 px-6 pb-6 pr-8">
            <DialogHeader class="pr-6">
              <DialogTitle>Solicitar Orçamento</DialogTitle>
            </DialogHeader>

            <!-- Budget -->
            <div v-if="false">
              <Label class="text-[#435574] mb-2 block text-sm">
                Orçamento estimado <span class="text-red-500">*</span>
              </Label>
              <div class="relative">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 text-sm">R$</span>
                <Input
                  type="text"
                  inputmode="numeric"
                  placeholder="0,00"
                  :model-value="formattedQuoteBudget"
                  @update:model-value="(val: string) => handleQuoteBudgetInput(val)"
                  @keydown="blockBudgetExceedingMax"
                  @paste.prevent="handleBudgetPaste"
                  class="pl-10"
                  :class="{
                    'border-red-500 focus-visible:ring-red-500': quoteBudget && Number(quoteBudget) < 5,
                    'border-amber-500 focus-visible:ring-amber-500': Number(quoteBudget) >= 1000000
                  }"
                />
              </div>
              <p v-if="quoteBudget && Number(quoteBudget) < 5" class="text-[11px] text-red-500 font-medium mt-1">
                O orçamento mínimo é R$ 5,00
              </p>
              <p v-else-if="Number(quoteBudget) >= 1000000" class="text-[11px] text-amber-600 font-medium mt-1">
                Valor máximo atingido: R$ 1.000.000,00
              </p>
            </div>
            <!-- Deadline -->
            <div class="space-y-3">
              <Label class="text-[#435574] mb-2 block text-sm">
                Prazo desejado <span class="text-red-500">*</span>
              </Label>
              <div class="flex p-1 bg-gray-100 rounded-lg mb-2">
                <button
                  type="button"
                  @click="quoteDeadlineType = 'estimated'"
                  :class="['flex-1 py-1.5 text-xs font-bold rounded-md transition-all', quoteDeadlineType === 'estimated' ? 'bg-white text-[#286AD8] shadow-sm' : 'text-[#435574] hover:text-[#263238]']"
                >
                  Prazo estimado
                </button>
                <button
                  type="button"
                  @click="quoteDeadlineType = 'decided'"
                  :class="['flex-1 py-1.5 text-xs font-bold rounded-md transition-all', quoteDeadlineType === 'decided' ? 'bg-white text-[#286AD8] shadow-sm' : 'text-[#435574] hover:text-[#263238]']"
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
                    @keydown="blockNonNumericKeys"
                    @paste.prevent="handleDeadlinePaste"
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
                <Input type="date" v-model="quoteDeadlineDate" :min="new Date().toISOString().split('T')[0]" class="w-full" />
              </div>
            </div>

            <!-- Description -->
            <div>
              <Label class="text-[#435574] mb-2 block text-sm">
                Descrição do serviço <span class="text-red-500">*</span>
              </Label>
              <Textarea
                placeholder="Insira detalhes sobre o serviço que você precisa, características, objetivos, preferências e qualquer informação relevante para que o profissional possa entender melhor sua demanda."
                :rows="5"
                v-model="quoteMessage"
                class="resize-none"
              />
            </div>

            <!-- Address (if onsite) -->
            <div v-if="serviceRequiresAddress" class="space-y-3 rounded-lg border border-blue-100 bg-blue-50/40 p-4">
              <div class="flex items-start gap-2">
                <MapPin class="h-4 w-4 text-[#286AD8] mt-0.5" />
                <div>
                  <Label class="text-[#435574] mb-1 block text-sm">
                    Localização do serviço <span class="text-red-500">*</span>
                  </Label>
                  <p class="text-xs text-[#435574]">Digite o CEP para buscar o endereço.</p>
                </div>
              </div>
              <!-- CEP -->
              <div class="space-y-1">
                <Label class="text-sm text-[#435574]">CEP *</Label>
                <div class="relative">
                  <Input
                    :model-value="quoteServiceAddress.zipCode"
                    @update:model-value="(val) => handleCEPInput(val as string)"
                    placeholder="00000-000"
                    maxlength="9"
                    :disabled="isLoadingCEP"
                    :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.zipCode }"
                  />
                  <div v-if="isLoadingCEP" class="absolute right-3 top-1/2 -translate-y-1/2">
                    <Loader2 class="h-4 w-4 animate-spin text-[#286AD8]" />
                  </div>
                </div>
                <p v-if="quoteAddressErrors.zipCode" class="text-[11px] text-red-500">{{ quoteAddressErrors.zipCode }}</p>
              </div>
              <!-- Street + Number -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div class="sm:col-span-2 space-y-1">
                  <Label class="text-sm text-[#435574]">Rua *</Label>
                  <Input
                    :model-value="quoteServiceAddress.street"
                    @update:model-value="(val) => { quoteServiceAddress.street = val as string; quoteAddressErrors.street = '' }"
                    placeholder="Nome da rua"
                    :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.street }"
                  />
                  <p v-if="quoteAddressErrors.street" class="text-[11px] text-red-500">{{ quoteAddressErrors.street }}</p>
                </div>
                <div class="space-y-1">
                  <Label class="text-sm text-[#435574]">Número *</Label>
                  <Input
                    :model-value="quoteServiceAddress.number"
                    @update:model-value="(val) => { quoteServiceAddress.number = val as string; quoteAddressErrors.number = '' }"
                    placeholder="123"
                    :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.number }"
                  />
                  <p v-if="quoteAddressErrors.number" class="text-[11px] text-red-500">{{ quoteAddressErrors.number }}</p>
                </div>
              </div>
              <!-- Complement -->
              <div class="space-y-1">
                <Label class="text-sm text-[#435574]">Complemento</Label>
                <Input
                  :model-value="quoteServiceAddress.complement"
                  @update:model-value="(val) => quoteServiceAddress.complement = val as string"
                  placeholder="Apto, bloco, referência..."
                />
              </div>
              <!-- Neighborhood -->
              <div class="space-y-1">
                <Label class="text-sm text-[#435574]">Bairro *</Label>
                <Input
                  :model-value="quoteServiceAddress.neighborhood"
                  @update:model-value="(val) => { quoteServiceAddress.neighborhood = val as string; quoteAddressErrors.neighborhood = '' }"
                  placeholder="Nome do bairro"
                  :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.neighborhood }"
                />
                <p v-if="quoteAddressErrors.neighborhood" class="text-[11px] text-red-500">{{ quoteAddressErrors.neighborhood }}</p>
              </div>
              <!-- City + State -->
              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div class="sm:col-span-2 space-y-1">
                  <Label class="text-sm text-[#435574]">Cidade *</Label>
                  <Input
                    :model-value="quoteServiceAddress.city"
                    @update:model-value="(val) => { quoteServiceAddress.city = val as string; quoteAddressErrors.city = '' }"
                    placeholder="Cidade"
                    :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.city }"
                  />
                  <p v-if="quoteAddressErrors.city" class="text-[11px] text-red-500">{{ quoteAddressErrors.city }}</p>
                </div>
                <div class="space-y-1">
                  <Label class="text-sm text-[#435574]">Estado *</Label>
                  <Input
                    :model-value="quoteServiceAddress.state"
                    @update:model-value="(val) => { quoteServiceAddress.state = (val as string).toUpperCase().slice(0, 2); quoteAddressErrors.state = '' }"
                    placeholder="SP"
                    maxlength="2"
                    :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.state }"
                  />
                  <p v-if="quoteAddressErrors.state" class="text-[11px] text-red-500">{{ quoteAddressErrors.state }}</p>
                </div>
              </div>
            </div>

            <!-- Attachments -->
            <div>
              <Label class="text-[#435574] mb-2 block text-sm">
                Anexos <span class="text-xs text-gray-400">(opcional · até 5 arquivos · máx. 10MB cada)</span>
              </Label>
              <input ref="fileInputRef" type="file" multiple class="hidden" @change="handleAddFiles" />
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

            <!-- Submit -->
            <Button @click="handleSendQuote" class="w-full" :disabled="isSendingQuote">
              <Loader2 v-if="isSendingQuote" class="h-4 w-4 mr-2 animate-spin" />
              <Send v-else class="h-4 w-4 mr-2" />
              {{ isSendingQuote ? 'Enviando...' : 'Enviar Solicitação' }}
            </Button>
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
</template>
