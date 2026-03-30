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
import ReviewList, { type ReviewItem } from '../shared/ReviewList.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import ScrollArea from '../ui/scroll-area.vue';
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
  profilePhoto: string;
  serviceType: string;
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
  workModel?: string | null;
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

type WorkModel = 'presencial' | 'remoto' | 'hibrido';

const createEmptyQuoteAddress = (): QuoteServiceAddress => ({
  zipCode: '',
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
});

const createEmptyQuoteAddressErrors = (): QuoteAddressErrors => ({
  zipCode: '',
  street: '',
  number: '',
  neighborhood: '',
  city: '',
  state: '',
});

const normalizeWorkModel = (value?: string | null): WorkModel | null => {
  const normalized = value?.toUpperCase();
  if (normalized === 'PRESENCIAL') return 'presencial';
  if (normalized === 'HIBRIDO') return 'hibrido';
  if (normalized === 'REMOTO') return 'remoto';
  return null;
};

const getWorkModelLabel = (value?: WorkModel | null) => {
  if (value === 'presencial') return 'Presencial';
  if (value === 'hibrido') return 'Híbrido';
  if (value === 'remoto') return 'Remoto';
  return 'Não informado';
};

const props = defineProps<Props>();

const freelancer = ref<FreelancerData | null>(null);
const services = ref<ServiceItem[]>([]);
const portfolio = ref<PortfolioProject[]>([]);
const reviews = ref<ReviewItem[]>([]);
const isLoadingReviews = ref(false);
const isLoading = ref(true);
const error = ref('');
const quoteMessage = ref('');
const quoteCategory = ref('');
const servicesPerPage = 4;
const currentServicePage = ref(1);

const totalServicePages = computed(() => {
  return Math.ceil(services.value.length / servicesPerPage);
});

const paginatedServices = computed(() => {
  const start = (currentServicePage.value - 1) * servicesPerPage;
  const end = start + servicesPerPage;
  return services.value.slice(start, end);
});

const goToServicePage = (page: number) => {
  if (page >= 1 && page <= totalServicePages.value) {
    currentServicePage.value = page;
  }
};

const visibleServicePages = computed(() => {
  const total = totalServicePages.value;
  const current = currentServicePage.value;
  const pages: (number | string)[] = [];
  
  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i);
    }
  } else {
    pages.push(1);
    
    if (current > 3) {
      pages.push('...');
    }
    
    const start = Math.max(2, current - 1);
    const end = Math.min(total - 1, current + 1);
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    
    if (current < total - 2) {
      pages.push('...');
    }
    
    pages.push(total);
  }
  
  return pages;
});
const quoteBudget = ref('5');
const formattedQuoteBudget = ref('');
const quoteDeadline = ref('');
const quoteDeadlineDate = ref('');
const quoteDeadlineType = ref<'estimated' | 'decided'>('estimated');
const requestedServiceId = ref<number | null>(null);
const requestedServiceTitle = ref('');
const requestedServiceWorkModel = ref<WorkModel | null>(null);
const showQuoteDialog = ref(false);
const activeTab = ref('services');
const isSendingQuote = ref(false);
const quoteAttachments = ref<File[]>([]);
const quoteServiceAddress = ref<QuoteServiceAddress>(createEmptyQuoteAddress());
const quoteAddressErrors = ref<QuoteAddressErrors>(createEmptyQuoteAddressErrors());
const isLoadingQuoteCEP = ref(false);
const favoritesStore = useFavoritesStore();
const router = useRouter();
const fileInputRef = ref<HTMLInputElement | null>(null);
const selectedProject = ref<PortfolioProject | null>(null);
const selectedImageIndex = ref(0);

const route = useRoute();
const selectedServiceModal = ref<ServiceItem | null>(null);
const selectedServiceImageIndex = ref(0);
const effectiveWorkModels = computed<WorkModel[]>(() => {
  const serviceModels = Array.from(
    new Set(
      services.value
        .map((service) => normalizeWorkModel(service.workModel))
        .filter((value): value is WorkModel => value !== null)
    )
  );

  if (serviceModels.length > 0) {
    return serviceModels;
  }
  return [];
});

const workModelSummary = computed(() => {
  if (!effectiveWorkModels.value.length) {
    return 'Não informado';
  }

  return effectiveWorkModels.value.map((value) => getWorkModelLabel(value)).join(' / ');
});

const hasOnsiteServices = computed(() =>
  effectiveWorkModels.value.some((value) => value === 'presencial' || value === 'hibrido')
);

const serviceRequiresAddress = computed(
  () => requestedServiceWorkModel.value === 'presencial' || requestedServiceWorkModel.value === 'hibrido'
);


const loadFreelancerData = async () => {
  isLoading.value = true;
  error.value = '';

  try {
    // Buscar dados do freelancer
    const freelancerResponse = await api.get(`/freelancer/${props.providerId}/profile`);
    freelancer.value = freelancerResponse.data;

    // Buscar serviços do freelancer
    const servicesResponse = await api.get(`/services/public/freelancer/${props.providerId}`);
    services.value = (servicesResponse.data || []).map((service: ServiceItem) => ({
      ...service,
      workModel: normalizeWorkModel(service.workModel),
    }));

    // Buscar portfólio do freelancer
    try {
      const portfolioResponse = await api.get(`/portfolio/freelancer/${props.providerId}`);
      portfolio.value = portfolioResponse.data || [];
    } catch (err) {
      console.log('Portfólio não disponível');
      portfolio.value = [];
    }

    // Buscar avaliações do freelancer
    loadFreelancerReviews();

  } catch (err: any) {
    console.error('Erro ao carregar dados do freelancer:', err);
    error.value = 'Não foi possível carregar os dados do profissional.';
    toast.error('Erro ao carregar perfil');
  } finally {
    isLoading.value = false;
  }
};

const loadFreelancerReviews = async () => {
  isLoadingReviews.value = true;
  try {
    const response = await api.get(`/reviews/user/${props.providerId}`);
    const reviewsData = response.data || [];
    
    reviews.value = reviewsData.map((review: any) => ({
      id: review.id,
      rating: review.rating,
      comment: review.comment || '',
      clientName: review.reviewerName,
      clientPhoto: review.reviewerAvatar,
      serviceName: review.serviceName,
      createdAt: review.createdAt,
    }));
  } catch (err) {
    console.log('Avaliações não disponíveis');
    reviews.value = [];
  } finally {
    isLoadingReviews.value = false;
  }
};

// Watch for serviceId in query to redirect to new service page
watch(() => route.query.serviceId, (newId) => {
  if (newId) {
    router.replace({
      name: 'service-detail',
      params: { providerId: props.providerId, serviceId: String(newId) }
    });
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

  // Limitar a 9 dígitos (100.000.000 centavos = R$ 1.000.000,00)
  if (digits.length > 9) {
    digits = digits.slice(0, 9);
  }

  // Converter para reais: últimos 2 dígitos são decimais
  let numValue = parseInt(digits) / 100;

  // Limite de 1 milhão (R$ 1.000.000,00)
  if (numValue > 1000000) {
    numValue = 1000000;
    digits = '100000000';
  }

  // Formatar para exibição
  formattedQuoteBudget.value = formatPriceDisplay(numValue);
  quoteBudget.value = numValue.toString();
};

const blockNonNumericKeys = (event: KeyboardEvent) => {
  // Permitir teclas de controle: Backspace, Delete, Tab, Escape, Enter, setas
  const allowedKeys = [
    'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
    'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
    'Home', 'End'
  ];
  
  if (allowedKeys.includes(event.key)) {
    return;
  }
  
  // Permitir Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
  if (event.ctrlKey || event.metaKey) {
    return;
  }
  
  // Bloquear tudo que não é número
  if (!/^[0-9]$/.test(event.key)) {
    event.preventDefault();
  }
};

const blockBudgetExceedingMax = (event: KeyboardEvent) => {
  // Permitir teclas de controle
  const allowedKeys = [
    'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
    'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
    'Home', 'End'
  ];
  
  if (allowedKeys.includes(event.key)) {
    return;
  }
  
  // Permitir Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
  if (event.ctrlKey || event.metaKey) {
    return;
  }
  
  // Bloquear tudo que não é número
  if (!/^[0-9]$/.test(event.key)) {
    event.preventDefault();
    return;
  }
  
  // Verificar se já atingiu o valor máximo (R$ 1.000.000,00)
  const currentDigits = formattedQuoteBudget.value.replace(/\D/g, '');
  if (currentDigits.length >= 9 || Number(quoteBudget.value) >= 1000000) {
    event.preventDefault();
  }
};

const handleBudgetPaste = (event: ClipboardEvent) => {
  const pastedText = event.clipboardData?.getData('text') || '';
  const digits = pastedText.replace(/\D/g, '');
  
  if (!digits) {
    return;
  }

  let numValue = parseInt(digits) / 100;
  
  if (numValue > 1000000) {
    numValue = 1000000;
  }

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

const handleDeadlinePaste = (event: ClipboardEvent) => {
  const pastedText = event.clipboardData?.getData('text') || '';
  const digits = pastedText.replace(/\D/g, '');
  
  if (digits) {
    const d = parseInt(digits);
    if (d > 365) {
      quoteDeadline.value = '365';
    } else {
      quoteDeadline.value = digits;
    }
  }
};

const clearQuoteAddressErrors = () => {
  quoteAddressErrors.value = createEmptyQuoteAddressErrors();
};

const formatCEP = (value: string) => {
  const numbers = value.replace(/\D/g, '').slice(0, 8);
  return numbers.replace(/(\d{5})(\d{0,3})/, '$1-$2');
};

const fetchQuoteAddressByCEP = async (cep: string) => {
  const cleanCEP = cep.replace(/\D/g, '');
  if (cleanCEP.length !== 8) return;

  isLoadingQuoteCEP.value = true;

  try {
    const response = await fetch(`https://viacep.com.br/ws/${cleanCEP}/json/`);
    const data = await response.json();

    if (data.erro) {
      toast.error('CEP não encontrado');
      return;
    }

    quoteServiceAddress.value = {
      ...quoteServiceAddress.value,
      zipCode: formatCEP(cleanCEP),
      street: data.logradouro || '',
      neighborhood: data.bairro || '',
      city: data.localidade || '',
      state: data.uf || '',
      complement: quoteServiceAddress.value.complement || data.complemento || '',
    };
  } catch (error) {
    console.error('Erro ao buscar CEP:', error);
    toast.error('Não foi possível buscar o endereço pelo CEP informado');
  } finally {
    isLoadingQuoteCEP.value = false;
  }
};

const handleQuoteCEPInput = (value: string) => {
  const formatted = formatCEP(value);
  quoteServiceAddress.value.zipCode = formatted;
  quoteAddressErrors.value.zipCode = '';

  const cleanCEP = formatted.replace(/\D/g, '');
  if (cleanCEP.length === 8) {
    fetchQuoteAddressByCEP(cleanCEP);
  }
};

const validateQuoteAddress = () => {
  clearQuoteAddressErrors();

  if (!serviceRequiresAddress.value) {
    return true;
  }

  let isValid = true;
  const cleanCEP = quoteServiceAddress.value.zipCode.replace(/\D/g, '');

  if (!cleanCEP) {
    quoteAddressErrors.value.zipCode = 'CEP é obrigatório';
    isValid = false;
  } else if (cleanCEP.length !== 8) {
    quoteAddressErrors.value.zipCode = 'CEP deve ter 8 dígitos';
    isValid = false;
  }

  if (!quoteServiceAddress.value.street.trim()) {
    quoteAddressErrors.value.street = 'Rua é obrigatória';
    isValid = false;
  }

  if (!quoteServiceAddress.value.number.trim()) {
    quoteAddressErrors.value.number = 'Número é obrigatório';
    isValid = false;
  }

  if (!quoteServiceAddress.value.neighborhood.trim()) {
    quoteAddressErrors.value.neighborhood = 'Bairro é obrigatório';
    isValid = false;
  }

  if (!quoteServiceAddress.value.city.trim()) {
    quoteAddressErrors.value.city = 'Cidade é obrigatória';
    isValid = false;
  }

  if (!quoteServiceAddress.value.state.trim()) {
    quoteAddressErrors.value.state = 'Estado é obrigatório';
    isValid = false;
  } else if (quoteServiceAddress.value.state.trim().length !== 2) {
    quoteAddressErrors.value.state = 'Use a sigla do estado';
    isValid = false;
  }

  return isValid;
};

const buildQuoteAddress = () => {
  const address = quoteServiceAddress.value;
  const streetLine = [
    `${address.street.trim()}, ${address.number.trim()}`,
    address.complement.trim(),
  ].filter(Boolean).join(', ');

  return `${streetLine} - ${address.neighborhood.trim()} - ${address.city.trim()}/${address.state.trim().toUpperCase()} - CEP: ${address.zipCode.trim()}`;
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

  // Validar que a data decidida não é no passado
  if (quoteDeadlineType.value === 'decided' && quoteDeadlineDate.value) {
    const selectedDate = new Date(quoteDeadlineDate.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Remove hora para comparar apenas a data
    
    if (selectedDate < today) {
      toast.error('A data desejada não pode ser anterior à data atual');
      return;
    }
  }

  if (!validateQuoteAddress()) {
    const firstAddressError = Object.values(quoteAddressErrors.value).find(Boolean);
    toast.error(firstAddressError || 'Informe a localização do serviço');
    return;
  }

  const serviceAddress = serviceRequiresAddress.value ? buildQuoteAddress() : null;

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
      description: quoteMessage.value,
      deadline: deadline,
      workModel: requestedServiceWorkModel.value?.toUpperCase(),
      address: serviceAddress,
      serviceId: requestedServiceId.value,
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
  quoteBudget.value = '5';
  formattedQuoteBudget.value = '';
  quoteDeadline.value = '';
  quoteDeadlineDate.value = '';
  quoteDeadlineType.value = 'estimated';
  requestedServiceId.value = null;
  requestedServiceTitle.value = '';
  requestedServiceWorkModel.value = null;
  quoteAttachments.value = [];
  quoteServiceAddress.value = createEmptyQuoteAddress();
  clearQuoteAddressErrors();
  isLoadingQuoteCEP.value = false;
};

const handleServiceClick = (service: ServiceItem) => {
  router.push({
    name: 'service-detail',
    params: { providerId: props.providerId, serviceId: service.id }
  });
};

const handleCloseServiceModal = () => {
  selectedServiceModal.value = null;
  const query = { ...route.query };
  delete query.serviceId;
  router.replace({ query });
};

const handleRequestService = (service: ServiceItem) => {
  resetQuoteForm();
  requestedServiceId.value = service.id;
  quoteCategory.value = service.category;
  requestedServiceTitle.value = service.title;
  requestedServiceWorkModel.value = normalizeWorkModel(service.workModel);
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
        <Button @click="loadFreelancerData">
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

                <!-- Professional Links (Compact) -->
                <div v-if="freelancer.professionalLinks && freelancer.professionalLinks.length > 0" class="mb-8 flex flex-wrap gap-2 justify-start">
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

              <!-- Action Buttons removed as per request -->
            </div>
          </div>
        </Card>

        <div class="grid lg:grid-cols-3 gap-6">
          <!-- Main Content -->
          <div class="lg:col-span-2">
            <Tabs v-model="activeTab" class="space-y-6">
              <TabsList class="grid w-full grid-cols-4">
                <TabsTrigger value="services">Serviços ({{ services.length }})</TabsTrigger>
                <TabsTrigger value="about">Sobre</TabsTrigger>
                <TabsTrigger value="portfolio">Portfólio ({{ portfolio.length }})</TabsTrigger>
                <TabsTrigger value="reviews">Avaliações ({{ reviews.length }})</TabsTrigger>
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
                        <div v-if="service.workModel" class="mb-3">
                          <Badge variant="outline" class="bg-white text-[#435574]">
                            {{ getWorkModelLabel(normalizeWorkModel(service.workModel)) }}
                          </Badge>
                        </div>
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

                  <!-- Pagination -->
                  <div v-if="totalServicePages > 1" class="flex justify-center items-center gap-2 mt-8">
                    <Button
                      variant="outline"
                      size="icon"
                      :disabled="currentServicePage === 1"
                      @click="goToServicePage(currentServicePage - 1)"
                      class="h-9 w-9"
                    >
                      <ChevronLeft class="h-4 w-4" />
                    </Button>
                    
                    <template v-for="(page, index) in visibleServicePages" :key="index">
                      <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
                      <Button
                        v-else
                        :variant="currentServicePage === page ? 'default' : 'outline'"
                        @click="goToServicePage(page as number)"
                        class="h-9 w-9 p-0"
                      >
                        {{ page }}
                      </Button>
                    </template>
                    
                    <Button
                      variant="outline"
                      size="icon"
                      :disabled="currentServicePage === totalServicePages"
                      @click="goToServicePage(currentServicePage + 1)"
                      class="h-9 w-9"
                    >
                      <ChevronRight class="h-4 w-4" />
                    </Button>
                  </div>
                </div>
                <Card v-else class="p-12 text-center">
                  <Briefcase class="h-12 w-12 text-[#435574]/30 mx-auto mb-4" />
                  <p class="text-[#435574]">Nenhum serviço cadastrado ainda</p>
                </Card>
              </TabsContent>

              <!-- Reviews Tab -->
              <TabsContent value="reviews">
                <ReviewList 
                  :reviews="reviews" 
                  :is-loading="isLoadingReviews"
                  :page-size="5"
                />
              </TabsContent>

              <!-- About Tab -->
              <TabsContent value="about">
                <Card class="p-6">
                  <h3 class="font-semibold text-[#263238] mb-4">Sobre o profissional</h3>
                  <p v-if="freelancer.bio" class="text-[#435574] mb-6 whitespace-pre-wrap">{{ freelancer.bio }}</p>
                  <p v-else class="text-[#435574] mb-6">Nenhuma descrição disponível.</p>

                  <!-- Service Cities -->
                  <div v-if="hasOnsiteServices && freelancer.serviceCities && freelancer.serviceCities.length > 0" class="mb-6">
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
                    {{ workModelSummary }}
                  </Badge>
                </div>

                <div v-if="freelancer.serviceType" class="flex items-center justify-between border-t pt-3">
                  <span class="text-sm text-[#435574]">Tipo:</span>
                  <span class="text-sm font-medium text-[#263238]">{{ freelancer.serviceType }}</span>
                </div>

                <div v-if="hasOnsiteServices && freelancer.serviceCities?.length" class="border-t pt-3">
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
                  <Button v-if="selectedProject?.projectUrl" asChild class="flex-1">
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

    <!-- Quote Dialog -->
    <Dialog v-model:open="showQuoteDialog">
      <DialogContent class="max-w-md max-h-[90vh] overflow-hidden p-0 gap-0">
        <ScrollArea class="mt-10 max-h-[calc(90vh-2.5rem)]">
        <div class="space-y-4 px-6 pb-6 pr-8">
          <DialogHeader class="pr-6">
            <DialogTitle>Solicitar Orçamento</DialogTitle>
          </DialogHeader>

          <div v-if="false">
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

          <div class="rounded-lg border border-blue-100 bg-blue-50/60 p-3 text-sm text-[#435574]">
            O valor nao e informado nesta etapa. Descreva o escopo e o freelancer enviara o orcamento depois.
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
              <Input
                type="date"
                v-model="quoteDeadlineDate"
                :min="new Date().toISOString().split('T')[0]"
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

          <div v-if="serviceRequiresAddress" class="space-y-3 rounded-lg border border-blue-100 bg-blue-50/40 p-4">
            <div class="flex items-start gap-2">
              <MapPin class="h-4 w-4 text-[#286AD8] mt-0.5" />
              <div>
                <Label class="text-[#435574] mb-1 block text-sm">
                  Localização do serviço <span class="text-red-500">*</span>
                </Label>
                <p class="text-xs text-[#435574]">
                  Digite o CEP para buscar o endereço e complete os demais dados do local.
                </p>
              </div>
            </div>

            <div class="space-y-1">
              <Label for="quote-service-zipcode" class="text-sm text-[#435574]">CEP *</Label>
              <div class="relative">
                <Input
                  id="quote-service-zipcode"
                  :model-value="quoteServiceAddress.zipCode"
                  @update:model-value="(val) => handleQuoteCEPInput(val as string)"
                  placeholder="00000-000"
                  maxlength="9"
                  :disabled="isLoadingQuoteCEP"
                  :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.zipCode }"
                />
                <div v-if="isLoadingQuoteCEP" class="absolute right-3 top-1/2 -translate-y-1/2">
                  <Loader2 class="h-4 w-4 animate-spin text-[#286AD8]" />
                </div>
              </div>
              <p v-if="quoteAddressErrors.zipCode" class="text-[11px] text-red-500">
                {{ quoteAddressErrors.zipCode }}
              </p>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div class="sm:col-span-2 space-y-1">
                <Label for="quote-service-street" class="text-sm text-[#435574]">Rua *</Label>
                <Input
                  id="quote-service-street"
                  :model-value="quoteServiceAddress.street"
                  @update:model-value="(val) => { quoteServiceAddress.street = val as string; quoteAddressErrors.street = '' }"
                  placeholder="Nome da rua"
                  :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.street }"
                />
                <p v-if="quoteAddressErrors.street" class="text-[11px] text-red-500">
                  {{ quoteAddressErrors.street }}
                </p>
              </div>

              <div class="space-y-1">
                <Label for="quote-service-number" class="text-sm text-[#435574]">Número *</Label>
                <Input
                  id="quote-service-number"
                  :model-value="quoteServiceAddress.number"
                  @update:model-value="(val) => { quoteServiceAddress.number = val as string; quoteAddressErrors.number = '' }"
                  placeholder="123"
                  :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.number }"
                />
                <p v-if="quoteAddressErrors.number" class="text-[11px] text-red-500">
                  {{ quoteAddressErrors.number }}
                </p>
              </div>
            </div>

            <div class="space-y-1">
              <Label for="quote-service-complement" class="text-sm text-[#435574]">Complemento</Label>
              <Input
                id="quote-service-complement"
                :model-value="quoteServiceAddress.complement"
                @update:model-value="(val) => quoteServiceAddress.complement = val as string"
                placeholder="Apto, bloco, referência..."
              />
            </div>

            <div class="space-y-1">
              <Label for="quote-service-neighborhood" class="text-sm text-[#435574]">Bairro *</Label>
              <Input
                id="quote-service-neighborhood"
                :model-value="quoteServiceAddress.neighborhood"
                @update:model-value="(val) => { quoteServiceAddress.neighborhood = val as string; quoteAddressErrors.neighborhood = '' }"
                placeholder="Nome do bairro"
                :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.neighborhood }"
              />
              <p v-if="quoteAddressErrors.neighborhood" class="text-[11px] text-red-500">
                {{ quoteAddressErrors.neighborhood }}
              </p>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div class="sm:col-span-2 space-y-1">
                <Label for="quote-service-city" class="text-sm text-[#435574]">Cidade *</Label>
                <Input
                  id="quote-service-city"
                  :model-value="quoteServiceAddress.city"
                  @update:model-value="(val) => { quoteServiceAddress.city = val as string; quoteAddressErrors.city = '' }"
                  placeholder="Cidade"
                  :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.city }"
                />
                <p v-if="quoteAddressErrors.city" class="text-[11px] text-red-500">
                  {{ quoteAddressErrors.city }}
                </p>
              </div>

              <div class="space-y-1">
                <Label for="quote-service-state" class="text-sm text-[#435574]">Estado *</Label>
                <Input
                  id="quote-service-state"
                  :model-value="quoteServiceAddress.state"
                  @update:model-value="(val) => { quoteServiceAddress.state = (val as string).toUpperCase().slice(0, 2); quoteAddressErrors.state = '' }"
                  placeholder="SP"
                  maxlength="2"
                  :class="{ 'border-red-500 focus-visible:ring-red-500': quoteAddressErrors.state }"
                />
                <p v-if="quoteAddressErrors.state" class="text-[11px] text-red-500">
                  {{ quoteAddressErrors.state }}
                </p>
              </div>
            </div>
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
            class="w-full"
            :disabled="isSendingQuote"
          >
            <Loader2 v-if="isSendingQuote" class="h-4 w-4 mr-2 animate-spin" />
            <Send v-else class="h-4 w-4 mr-2" />
            {{ isSendingQuote ? 'Enviando...' : 'Enviar Solicitação' }}
          </Button>
        </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
</template>
