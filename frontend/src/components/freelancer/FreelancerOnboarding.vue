<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  User,
  Briefcase,
  Images,
  Calendar,
  CheckCircle,
  MapPin,
  Upload,
  Plus,
  X,
  ArrowRight,
  ArrowLeft,
  Save,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Textarea from '../ui/textarea.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Progress from '../ui/progress.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import { toast } from '../../utils/toast';
import { formatCPF, formatPhone } from '@/utils/masks';
import { useCategoryCatalogStore } from '@/stores/categoryCatalogStore';

interface OnboardingData {
  name: string;
  email: string;
  phone: string;
  cpf: string;
  profilePhoto?: string;
  profession: string;
  categoryId?: number | null;
  subcategoryId?: number | null;
  category: string;
  subcategory: string;
  bio: string;
  skills: string[];
  portfolioItems: Array<{
    id: string;
    title: string;
    description: string;
    image?: string;
  }>;
  workingDays: string[];
  workingHours: {
    start: string;
    end: string;
  };
  serviceAreas: string[];
  serviceType: 'presencial' | 'remoto' | 'hibrido';
  documentType: string;
  documentNumber: string;
  documentPhoto?: string;
}

interface Props {
  onComplete: () => void;
  onSaveDraft: (data: any) => void;
}

const props = defineProps<Props>();

const steps = [
  { id: 1, title: 'Dados Pessoais', icon: User },
  { id: 2, title: 'Serviços', icon: Briefcase },
  { id: 3, title: 'Portfólio', icon: Images },
  { id: 4, title: 'Disponibilidade', icon: Calendar },
  { id: 5, title: 'Verificação', icon: CheckCircle },
];

const categoryCatalogStore = useCategoryCatalogStore();
const legacyCategories = [
  { value: 'eletricista', label: 'Eletricista' },
  { value: 'encanador', label: 'Encanador' },
  { value: 'pintor', label: 'Pintor' },
  { value: 'designer', label: 'Designer' },
  { value: 'desenvolvedor', label: 'Desenvolvedor' },
];
const categories = computed(() =>
  categoryCatalogStore.activePublicCategories.length > 0
    ? categoryCatalogStore.activePublicCategories.map((category) => ({
        value: String(category.id),
        label: category.name,
      }))
    : legacyCategories
);
const selectedSubcategories = computed(() =>
  categoryCatalogStore.getSubcategoriesByCategoryId(data.value.categoryId || null)
);
const selectedCategoryLabel = computed(
  () => categories.value.find((category) => category.value === String(data.value.categoryId))?.label || ''
);
const selectedSubcategoryLabel = computed(
  () => selectedSubcategories.value.find((subcategory) => subcategory.id === data.value.subcategoryId)?.name || ''
);

const availableSkills = [
  'Instalação',
  'Manutenção',
  'Reparo',
  'Automação',
  'UI/UX',
  'Branding',
  'React',
  'Node.js',
  'TypeScript',
  'Fotografia',
  'Edição',
];

const currentStep = ref(1);
const data = ref<OnboardingData>({
  name: '',
  email: '',
  phone: '',
  cpf: '',
  profession: '',
  categoryId: null,
  subcategoryId: null,
  category: '',
  subcategory: '',
  bio: '',
  skills: [],
  portfolioItems: [],
  workingDays: [],
  workingHours: { start: '08:00', end: '18:00' },
  serviceAreas: [],
  serviceType: 'presencial',
  documentType: 'rg',
  documentNumber: '',
});

const newSkill = ref('');
const newPortfolioItem = ref({
  title: '',
  description: '',
});

const progress = computed(() => (currentStep.value / steps.length) * 100);

onMounted(() => {
  categoryCatalogStore.loadPublicCategories();
});

watch(
  () => data.value.categoryId,
  (categoryId) => {
    const selectedCategory = categoryCatalogStore.activePublicCategories.find(
      (category) => category.id === categoryId
    );
    data.value.category = selectedCategory?.name || '';

    const subcategoryStillValid = selectedSubcategories.value.some(
      (subcategory) => subcategory.id === data.value.subcategoryId
    );

    if (!subcategoryStillValid) {
      data.value.subcategoryId = null;
      data.value.subcategory = '';
    }
  }
);

watch(
  () => data.value.subcategoryId,
  (subcategoryId) => {
    const selectedSubcategory = selectedSubcategories.value.find(
      (subcategory) => subcategory.id === subcategoryId
    );
    data.value.subcategory = selectedSubcategory?.name || '';
  }
);


const handleAddSkill = (skill: string) => {
  if (skill && !data.value.skills.includes(skill)) {
    data.value = { ...data.value, skills: [...data.value.skills, skill] };
    newSkill.value = '';
  }
};

const handleRemoveSkill = (skill: string) => {
  data.value = { ...data.value, skills: data.value.skills.filter((s) => s !== skill) };
};

const handleAddPortfolioItem = () => {
  if (newPortfolioItem.value.title && newPortfolioItem.value.description) {
    data.value = {
      ...data.value,
      portfolioItems: [
        ...data.value.portfolioItems,
        {
          id: Date.now().toString(),
          ...newPortfolioItem.value,
        },
      ],
    };
    newPortfolioItem.value = { title: '', description: '' };
    toast.success('Item adicionado ao portfólio');
  } else {
    toast.error('Preencha o título e a descrição');
  }
};

const handleRemovePortfolioItem = (id: string) => {
  data.value = {
    ...data.value,
    portfolioItems: data.value.portfolioItems.filter((item) => item.id !== id),
  };
  toast.success('Item removido do portfólio');
};

const toggleWorkingDay = (day: string) => {
  const days = data.value.workingDays.includes(day)
    ? data.value.workingDays.filter((d) => d !== day)
    : [...data.value.workingDays, day];
  data.value = { ...data.value, workingDays: days };
};

const validateStep = (step: number): boolean => {
  switch (step) {
    case 1:
      if (!data.value.name || !data.value.email || !data.value.phone || !data.value.cpf) {
        toast.error('Preencha todos os campos obrigatórios');
        return false;
      }
      if (!data.value.email.includes('@')) {
        toast.error('E-mail inválido');
        return false;
      }
      return true;

    case 2:
      if (!data.value.profession || !data.value.categoryId || !data.value.bio) {
        toast.error('Preencha todos os campos obrigatórios');
        return false;
      }
      if (selectedSubcategories.value.length > 0 && !data.value.subcategoryId) {
        toast.error('Selecione uma subcategoria');
        return false;
      }
      if (data.value.skills.length === 0) {
        toast.error('Adicione pelo menos uma habilidade');
        return false;
      }
      return true;

    case 3:
      if (data.value.portfolioItems.length === 0) {
        toast.error('Adicione pelo menos um item ao portfólio');
        return false;
      }
      return true;

    case 4:
      if (data.value.workingDays.length === 0) {
        toast.error('Selecione pelo menos um dia de trabalho');
        return false;
      }
      if (data.value.serviceAreas.length === 0) {
        toast.error('Adicione pelo menos uma área de atendimento');
        return false;
      }
      return true;

    case 5:
      if (!data.value.documentType || !data.value.documentNumber) {
        toast.error('Preencha os dados de verificação');
        return false;
      }
      return true;

    default:
      return true;
  }
};

const handleNext = () => {
  if (validateStep(currentStep.value)) {
    if (currentStep.value < steps.length) {
      currentStep.value++;
      toast.success('Progresso salvo');
    }
  }
};

const handleBack = () => {
  if (currentStep.value > 1) {
    currentStep.value--;
  }
};

const handleSaveDraft = () => {
  props.onSaveDraft(data.value);
  toast.success('Rascunho salvo com sucesso!');
};

const handlePublish = () => {
  if (validateStep(5)) {
    toast.success('Perfil publicado com sucesso!');
    props.onComplete();
  }
};

const weekDays = [
  { value: 'seg', label: 'Seg' },
  { value: 'ter', label: 'Ter' },
  { value: 'qua', label: 'Qua' },
  { value: 'qui', label: 'Qui' },
  { value: 'sex', label: 'Sex' },
  { value: 'sab', label: 'Sáb' },
  { value: 'dom', label: 'Dom' },
];

const handleServiceAreaKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && (e.target as HTMLInputElement).value) {
    data.value = {
      ...data.value,
      serviceAreas: [...data.value.serviceAreas, (e.target as HTMLInputElement).value],
    };
    (e.target as HTMLInputElement).value = '';
  }
};
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-8">
    <div class="max-w-4xl mx-auto px-4">
      <!-- Header -->
      <div class="mb-8 text-center">
        <h1 class="text-[#263238] mb-2">Bem-vindo ao Servi Já!</h1>
        <p class="text-[#435574]">
          Complete seu cadastro para começar a receber propostas
        </p>
      </div>

      <!-- Progress Bar -->
      <div class="mb-8">
        <div class="flex items-center justify-between mb-2">
          <span class="text-sm text-[#435574]">
            Etapa {{ currentStep }} de {{ steps.length }}
          </span>
          <span class="text-sm text-[#435574]">{{ Math.round(progress) }}%</span>
        </div>
        <Progress :value="progress" class="h-2" />
      </div>

      <!-- Steps Indicator -->
      <div class="mb-8">
        <div class="flex items-center justify-between">
          <div
            v-for="(step, index) in steps"
            :key="step.id"
            class="flex flex-col items-center flex-1 relative"
          >
            <div
              :class="`w-12 h-12 rounded-full flex items-center justify-center mb-2 ${
                currentStep > step.id
                  ? 'bg-green-500 text-white'
                  : currentStep === step.id
                  ? 'bg-[#286AD8] text-white'
                  : 'bg-gray-200 text-gray-400'
              }`"
            >
              <CheckCircle v-if="currentStep > step.id" class="h-6 w-6" />
              <component v-else :is="step.icon" class="h-6 w-6" />
            </div>
            <p
              :class="`text-xs text-center ${currentStep === step.id ? 'text-[#263238]' : 'text-[#435574]'}`"
            >
              {{ step.title }}
            </p>
          </div>
        </div>
      </div>

      <!-- Step Content -->
      <Card class="p-6 mb-6">
        <!-- Step 1: Personal Data -->
        <div v-if="currentStep === 1" class="space-y-6">
          <div class="text-center mb-6">
            <h2 class="text-[#263238] mb-2">Dados Pessoais</h2>
            <p class="text-[#435574]">Conte-nos um pouco sobre você</p>
          </div>

          <!-- Profile Photo -->
          <div class="flex flex-col items-center gap-4">
            <Avatar class="h-24 w-24">
              <AvatarImage :src="data.profilePhoto" />
              <AvatarFallback class="bg-[#286AD8] text-white text-2xl">
                <User class="h-12 w-12" />
              </AvatarFallback>
            </Avatar>
            <Button variant="outline" size="sm">
              <Upload class="h-4 w-4 mr-2" />
              Enviar Foto
            </Button>
            <p class="text-xs text-[#435574]">JPG ou PNG, máximo 5MB</p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="name">Nome Completo *</Label>
              <Input
                id="name"
                :model-value="data.name"
                @update:model-value="(val) => data.name = val as string"
                placeholder="Seu nome completo"
              />
            </div>

            <div class="space-y-2">
              <Label for="email">E-mail *</Label>
              <Input
                id="email"
                type="email"
                :model-value="data.email"
                @update:model-value="(val) => data.email = val as string"
                placeholder="seu@email.com"
              />
            </div>

            <div class="space-y-2">
              <Label for="phone">Telefone *</Label>
              <Input
                id="phone"
                :model-value="data.phone"
                @update:model-value="(val) => data.phone = formatPhone(val as string)"
                placeholder="(11) 98765-4321"
                maxlength="15"
              />
            </div>

            <div class="space-y-2">
              <Label for="cpf">CPF *</Label>
              <Input
                id="cpf"
                :model-value="data.cpf"
                @update:model-value="(val) => data.cpf = formatCPF(val as string)"
                placeholder="000.000.000-00"
                maxlength="14"
              />
            </div>
          </div>
        </div>

        <!-- Step 2: Services -->
        <div v-if="currentStep === 2" class="space-y-6">
          <div class="text-center mb-6">
            <h2 class="text-[#263238] mb-2">Serviços Oferecidos</h2>
            <p class="text-[#435574]">Defina sua área de atuação e habilidades</p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="profession">Profissão *</Label>
              <Input
                id="profession"
                :model-value="data.profession"
                @update:model-value="(val) => data.profession = val as string"
                placeholder="Ex: Eletricista Profissional"
              />
            </div>

            <div class="space-y-2">
              <Label for="category">Categoria *</Label>
              <Select
                :model-value="data.categoryId ? String(data.categoryId) : ''"
                @update:model-value="(val: string) => data.categoryId = val ? Number(val) : null"
              >
                <SelectTrigger>
                  <SelectValue
                    :label="selectedCategoryLabel || undefined"
                    placeholder="Selecione uma categoria"
                  />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="cat in categories" :key="cat.value" :value="cat.value">
                    {{ cat.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div class="space-y-2">
            <Label for="subcategory">
              Subcategoria
              <span v-if="selectedSubcategories.length > 0" class="text-red-500">*</span>
            </Label>
            <Select
              :model-value="data.subcategoryId ? String(data.subcategoryId) : ''"
              @update:model-value="(val: string) => data.subcategoryId = val ? Number(val) : null"
              :disabled="!data.categoryId || selectedSubcategories.length === 0"
            >
              <SelectTrigger>
                <SelectValue
                  :label="selectedSubcategoryLabel || undefined"
                  :placeholder="selectedSubcategories.length > 0 ? 'Selecione uma subcategoria' : 'Escolha uma categoria primeiro'"
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="subcategory in selectedSubcategories"
                  :key="subcategory.id"
                  :value="String(subcategory.id)"
                >
                  {{ subcategory.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label for="bio">Biografia Profissional *</Label>
            <Textarea
              id="bio"
              :model-value="data.bio"
              @update:model-value="(val: string) => data.bio = val"
              placeholder="Conte sobre sua experiência, especialidades e diferenciais..."
              :rows="4"
            />
            <p class="text-xs text-[#435574]">{{ data.bio.length }}/500 caracteres</p>
          </div>

          <div class="space-y-2">
            <Label>Habilidades *</Label>
            <div class="flex gap-2 mb-3">
              <Select :model-value="newSkill" @update:model-value="(val: string) => newSkill = val">
                <SelectTrigger class="flex-1">
                  <SelectValue placeholder="Selecione uma habilidade" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="skill in availableSkills" :key="skill" :value="skill">
                    {{ skill }}
                  </SelectItem>
                </SelectContent>
              </Select>
              <Button @click="handleAddSkill(newSkill)" :disabled="!newSkill">
                <Plus class="h-4 w-4" />
              </Button>
            </div>
            <div class="flex flex-wrap gap-2">
              <Badge
                v-for="skill in data.skills"
                :key="skill"
                variant="secondary"
                class="pr-1"
              >
                {{ skill }}
                <button
                  @click="handleRemoveSkill(skill)"
                  class="ml-2 hover:text-red-600"
                >
                  <X class="h-3 w-3" />
                </button>
              </Badge>
            </div>
          </div>
        </div>

        <!-- Step 3: Portfolio -->
        <div v-if="currentStep === 3" class="space-y-6">
          <div class="text-center mb-6">
            <h2 class="text-[#263238] mb-2">Portfólio</h2>
            <p class="text-[#435574]">Mostre seus melhores trabalhos</p>
          </div>

          <!-- Add Portfolio Item -->
          <Card class="p-4 bg-gray-50">
            <h3 class="text-[#263238] mb-4">Adicionar Item</h3>
            <div class="space-y-3">
              <div class="space-y-2">
                <Label for="portfolio-title">Título do Projeto</Label>
                <Input
                  id="portfolio-title"
                  :model-value="newPortfolioItem.title"
                  @update:model-value="(val) => newPortfolioItem.title = val as string"
                  placeholder="Ex: Instalação Elétrica Residencial"
                />
              </div>
              <div class="space-y-2">
                <Label for="portfolio-description">Descrição</Label>
                <Textarea
                  id="portfolio-description"
                  :model-value="newPortfolioItem.description"
                  @update:model-value="(val: string) => newPortfolioItem.description = val"
                  placeholder="Descreva o projeto e os resultados..."
                  :rows="3"
                />
              </div>
              <Button @click="handleAddPortfolioItem" class="w-full">
                <Plus class="h-4 w-4 mr-2" />
                Adicionar ao Portfólio
              </Button>
            </div>
          </Card>

          <!-- Portfolio Items List -->
          <div class="space-y-3">
            <h3 class="text-[#263238]">
              Itens do Portfólio ({{ data.portfolioItems.length }})
            </h3>
            <p v-if="data.portfolioItems.length === 0" class="text-center text-[#435574] py-8">
              Nenhum item adicionado ainda
            </p>
            <Card
              v-for="item in data.portfolioItems"
              :key="item.id"
              class="p-4"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <h4 class="text-[#263238] mb-1">{{ item.title }}</h4>
                  <p class="text-sm text-[#435574]">{{ item.description }}</p>
                </div>
                <Button
                  @click="handleRemovePortfolioItem(item.id)"
                  variant="ghost"
                  size="icon"
                  class="text-red-500 hover:text-red-700 hover:bg-red-50"
                >
                  <X class="h-4 w-4" />
                </Button>
              </div>
            </Card>
          </div>
        </div>

        <!-- Step 4: Availability -->
        <div v-if="currentStep === 4" class="space-y-6">
          <div class="text-center mb-6">
            <h2 class="text-[#263238] mb-2">Disponibilidade</h2>
            <p class="text-[#435574]">Defina seus horários e áreas de atendimento</p>
          </div>

          <div class="space-y-2">
            <Label>Dias de Trabalho *</Label>
            <div class="flex gap-2">
              <Button
                v-for="day in weekDays"
                :key="day.value"
                @click="toggleWorkingDay(day.value)"
                :variant="data.workingDays.includes(day.value) ? 'default' : 'outline'"
                size="sm"
              >
                {{ day.label }}
              </Button>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="start-time">Horário de Início</Label>
              <Input
                id="start-time"
                type="time"
                :model-value="data.workingHours.start"
                @update:model-value="(val) => data.workingHours = { ...data.workingHours, start: val as string }"
              />
            </div>
            <div class="space-y-2">
              <Label for="end-time">Horário de Término</Label>
              <Input
                id="end-time"
                type="time"
                :model-value="data.workingHours.end"
                @update:model-value="(val) => data.workingHours = { ...data.workingHours, end: val as string }"
              />
            </div>
          </div>

          <div class="space-y-2">
            <Label>Tipo de Atendimento *</Label>
            <div class="grid grid-cols-3 gap-2">
              <Button
                @click="data.serviceType = 'presencial'"
                :variant="data.serviceType === 'presencial' ? 'default' : 'outline'"
              >
                <MapPin class="h-4 w-4 mr-2" />
                Presencial
              </Button>
              <Button
                @click="data.serviceType = 'remoto'"
                :variant="data.serviceType === 'remoto' ? 'default' : 'outline'"
              >
                Remoto
              </Button>
              <Button
                @click="data.serviceType = 'hibrido'"
                :variant="data.serviceType === 'hibrido' ? 'default' : 'outline'"
              >
                Híbrido
              </Button>
            </div>
          </div>

          <div class="space-y-2">
            <Label>Áreas de Atendimento *</Label>
            <div class="flex gap-2">
              <Input
                placeholder="Digite uma cidade ou região"
                @keypress="handleServiceAreaKeyPress"
              />
            </div>
            <div class="flex flex-wrap gap-2 mt-2">
              <Badge
                v-for="(area, index) in data.serviceAreas"
                :key="index"
                variant="secondary"
                class="pr-1"
              >
                {{ area }}
                <button
                  @click="data.serviceAreas = data.serviceAreas.filter((_, i) => i !== index)"
                  class="ml-2 hover:text-red-600"
                >
                  <X class="h-3 w-3" />
                </button>
              </Badge>
            </div>
          </div>
        </div>

        <!-- Step 5: Verification -->
        <div v-if="currentStep === 5" class="space-y-6">
          <div class="text-center mb-6">
            <h2 class="text-[#263238] mb-2">Verificação de Identidade</h2>
            <p class="text-[#435574]">Para sua segurança e dos clientes</p>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="document-type">Tipo de Documento *</Label>
              <Select
                :model-value="data.documentType"
                @update:model-value="(val: string) => data.documentType = val"
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="rg">RG</SelectItem>
                  <SelectItem value="cnh">CNH</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div class="space-y-2">
              <Label for="document-number">Número do Documento *</Label>
              <Input
                id="document-number"
                :model-value="data.documentNumber"
                @update:model-value="(val: string) => data.documentNumber = val"
                placeholder="Digite o número"
              />
            </div>
          </div>

          <div class="space-y-2">
            <Label>Foto do Documento</Label>
            <div class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center">
              <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p class="text-[#435574] mb-2">
                Clique para fazer upload ou arraste a foto
              </p>
              <p class="text-xs text-[#435574]">JPG ou PNG, máximo 5MB</p>
            </div>
          </div>

          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p class="text-sm text-[#263238]">
              <strong>Importante:</strong> Seus documentos são armazenados de forma segura e
              utilizados apenas para verificação de identidade, conforme a LGPD.
            </p>
          </div>
        </div>
      </Card>

      <!-- Actions -->
      <div class="flex items-center justify-between">
        <div class="flex gap-2">
          <Button v-if="currentStep > 1" @click="handleBack" variant="outline">
            <ArrowLeft class="h-4 w-4 mr-2" />
            Voltar
          </Button>
          <Button @click="handleSaveDraft" variant="outline">
            <Save class="h-4 w-4 mr-2" />
            Salvar Rascunho
          </Button>
        </div>

        <Button
          v-if="currentStep < steps.length"
          @click="handleNext"
        >
          Próximo
          <ArrowRight class="h-4 w-4 ml-2" />
        </Button>
        <Button
          v-else
          @click="handlePublish"
          variant="success"
        >
          <CheckCircle class="h-4 w-4 mr-2" />
          Publicar Perfil
        </Button>
      </div>
    </div>
  </div>
</template>
