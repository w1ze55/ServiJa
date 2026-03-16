<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Briefcase,
  DollarSign,
  MapPin,
  Link as LinkIcon,
  FileText,
  Wrench,
  Edit,
  Check,
  X,
  Plus,
  Trash2,
  AlertTriangle,
  Globe,
  Linkedin,
  Instagram,
  Github,
  ExternalLink,
} from 'lucide-vue-next'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Card from '../ui/card.vue'
import Badge from '../ui/badge.vue'
import Textarea from '../ui/textarea.vue'
import Select from '../ui/select.vue'
import SelectContent from '../ui/select-content.vue'
import SelectItem from '../ui/select-item.vue'
import SelectTrigger from '../ui/select-trigger.vue'
import SelectValue from '../ui/select-value.vue'
import AlertDialog from '../ui/alert-dialog.vue'
import AlertDialogAction from '../ui/alert-dialog-action.vue'
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue'
import AlertDialogContent from '../ui/alert-dialog-content.vue'
import AlertDialogDescription from '../ui/alert-dialog-description.vue'
import AlertDialogFooter from '../ui/alert-dialog-footer.vue'
import AlertDialogHeader from '../ui/alert-dialog-header.vue'
import AlertDialogTitle from '../ui/alert-dialog-title.vue'
import api from '@/api/config/axios'
import { toast } from '@/utils/toast'

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

interface ServiceCity {
  id: string
  city: string
  state: string
}

interface ProfessionalLink {
  id: string
  type: 'website' | 'linkedin' | 'instagram' | 'github' | 'portfolio' | 'other'
  url: string
  label?: string
}

interface ProfessionalProfile {
  workModel: 'presencial' | 'remoto' | 'hibrido'
  serviceType: string
  bio: string
  serviceCities: ServiceCity[]
  professionalLinks: ProfessionalLink[]
}

const WORK_MODELS = [
  { value: 'presencial', label: 'Presencial' },
  { value: 'remoto', label: 'Remoto' },
  { value: 'hibrido', label: 'Híbrido' },
]

const SERVICE_TYPES = [
  'Eletricista',
  'Encanador',
  'Pintor',
  'Designer',
  'Desenvolvedor',
  'Fotógrafo',
  'Marceneiro',
  'Pedreiro',
  'Jardineiro',
  'Técnico de Informática',
  'Diarista',
  'Cuidador',
  'Personal Trainer',
  'Professor Particular',
  'Mecânico',
  'Outro',
]

const LINK_TYPES = [
  { value: 'website', label: 'Website', icon: Globe },
  { value: 'linkedin', label: 'LinkedIn', icon: Linkedin },
  { value: 'instagram', label: 'Instagram', icon: Instagram },
  { value: 'github', label: 'GitHub', icon: Github },
  { value: 'portfolio', label: 'Portfólio', icon: ExternalLink },
  { value: 'other', label: 'Outro', icon: LinkIcon },
]

const BRAZILIAN_STATES = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA',
  'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN',
  'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
]

const isLoading = ref(false)
const isEditingWorkModel = ref(false)
const isEditingBio = ref(false)
const isEditingLinks = ref(false)
const isSaving = ref(false)

// Profile data
const profileData = ref<ProfessionalProfile>({
  workModel: 'presencial',
  serviceType: '',
  bio: '',
  serviceCities: [],
  professionalLinks: [],
})

const editedData = ref<ProfessionalProfile>({ ...profileData.value })

// City Dialog
const showCityDialog = ref(false)
const editingCity = ref<ServiceCity | null>(null)
const newCity = ref({
  city: '',
  state: '',
})
const cityErrors = ref({
  city: '',
  state: '',
})
const isSavingCity = ref(false)
const cityToDelete = ref<ServiceCity | null>(null)

// Link Dialog
const showLinkDialog = ref(false)
const editingLink = ref<ProfessionalLink | null>(null)
const newLink = ref({
  type: 'website' as ProfessionalLink['type'],
  url: '',
  label: '',
})
const linkErrors = ref({
  url: '',
})
const isSavingLink = ref(false)
const linkToDelete = ref<ProfessionalLink | null>(null)

onMounted(() => {
  loadProfileData()
})

const loadProfileData = async () => {
  isLoading.value = true
  try {
    const response = await api.get('/freelancer/profile')
    const data = response.data
    
    // Mapear serviceCities do backend (ids são numéricos)
    const mappedCities: ServiceCity[] = (data.serviceCities || []).map((c: any) => ({
      id: c.id?.toString() || Date.now().toString(),
      city: c.city,
      state: c.state,
    }))

    // Mapear professionalLinks do backend
    const mappedLinks: ProfessionalLink[] = (data.professionalLinks || []).map((l: any) => ({
      id: l.id?.toString() || Date.now().toString(),
      type: l.type || 'website',
      url: l.url,
      label: l.label || '',
    }))

    profileData.value = {
      workModel: data.workModel || 'presencial',
      serviceType: data.serviceType || data.category || '',
      bio: data.bio || '',
      serviceCities: mappedCities,
      professionalLinks: mappedLinks,
    }
    editedData.value = JSON.parse(JSON.stringify(profileData.value))
  } catch (error: any) {
    console.error('Erro ao carregar perfil profissional:', error)
    const userString = localStorage.getItem('user')
    if (userString) {
      const user = JSON.parse(userString)
      profileData.value = {
        workModel: user.workModel || 'presencial',
        serviceType: user.serviceType || user.category || '',
        bio: user.bio || '',
        serviceCities: [],
        professionalLinks: [],
      }
      editedData.value = JSON.parse(JSON.stringify(profileData.value))
    }
  } finally {
    isLoading.value = false
  }
}

// Work Model & Service Type
const handleSaveWorkModel = async () => {
  if (!editedData.value.serviceType) {
    toast.error('Tipo de serviço é obrigatório')
    return
  }

  isSaving.value = true
  try {
    await api.put('/freelancer/profile', {
      workModel: editedData.value.workModel,
      serviceType: editedData.value.serviceType,
      category: editedData.value.serviceType,
    })

    profileData.value.workModel = editedData.value.workModel
    profileData.value.serviceType = editedData.value.serviceType
    isEditingWorkModel.value = false
    toast.success('Configurações de trabalho atualizadas!')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao atualizar configurações'
    toast.error(errorMessage)
  } finally {
    isSaving.value = false
  }
}

const handleCancelWorkModel = () => {
  editedData.value.workModel = profileData.value.workModel
  editedData.value.serviceType = profileData.value.serviceType
  isEditingWorkModel.value = false
}

// Biography
const handleSaveBio = async () => {
  if (editedData.value.bio.length > 1000) {
    toast.error('A biografia deve ter no máximo 1000 caracteres')
    return
  }

  isSaving.value = true
  try {
    await api.put('/freelancer/profile', {
      bio: editedData.value.bio,
      description: editedData.value.bio,
    })

    profileData.value.bio = editedData.value.bio
    isEditingBio.value = false
    toast.success('Biografia atualizada!')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao atualizar biografia'
    toast.error(errorMessage)
  } finally {
    isSaving.value = false
  }
}

const handleCancelBio = () => {
  editedData.value.bio = profileData.value.bio
  isEditingBio.value = false
}

// Service Cities
const clearCityErrors = () => {
  cityErrors.value = { city: '', state: '' }
}

// ViaCEP Integration para cidades
const isLoadingCEP = ref(false)

const fetchCityByCEP = async (cep: string) => {
  const cleanCEP = cep.replace(/\D/g, '')
  
  // Validar se tem 8 dígitos
  if (cleanCEP.length !== 8) {
    return
  }

  isLoadingCEP.value = true
  try {
    const response = await fetch(`https://viacep.com.br/ws/${cleanCEP}/json/`)
    const data = await response.json()

    if (data.erro) {
      toast.error('CEP não encontrado')
      return
    }

    // Preencher cidade e estado automaticamente
    newCity.value.city = data.localidade || ''
    newCity.value.state = data.uf || ''

    toast.success('Cidade encontrada!')
  } catch (error) {
    console.error('Erro ao buscar CEP:', error)
    toast.error('Erro ao buscar CEP. Verifique sua conexão.')
  } finally {
    isLoadingCEP.value = false
  }
}

const citySearchCEP = ref('')

const formatCEP = (value: string) => {
  const numbers = value.replace(/\D/g, '')
  return numbers.replace(/(\d{5})(\d{0,3})/, '$1-$2')
}

const handleCEPInput = (value: string) => {
  const formatted = formatCEP(value)
  citySearchCEP.value = formatted
  
  // Auto-buscar quando CEP completo (8 dígitos)
  const cleanCEP = formatted.replace(/\D/g, '')
  if (cleanCEP.length === 8) {
    fetchCityByCEP(cleanCEP)
  }
}

const openAddCityDialog = () => {
  editingCity.value = null
  newCity.value = { city: '', state: '' }
  citySearchCEP.value = ''
  clearCityErrors()
  showCityDialog.value = true
}

const openEditCityDialog = (city: ServiceCity) => {
  editingCity.value = city
  newCity.value = { city: city.city, state: city.state }
  citySearchCEP.value = ''
  clearCityErrors()
  showCityDialog.value = true
}

const closeCityDialog = () => {
  showCityDialog.value = false
  editingCity.value = null
  citySearchCEP.value = ''
  clearCityErrors()
}

const validateCityForm = (): boolean => {
  clearCityErrors()
  let isValid = true

  if (!newCity.value.city.trim()) {
    cityErrors.value.city = 'Cidade é obrigatória'
    isValid = false
  }

  if (!newCity.value.state) {
    cityErrors.value.state = 'Estado é obrigatório'
    isValid = false
  }

  // Verificar se a cidade já existe
  const cityExists = profileData.value.serviceCities.some(
    (c) => c.city.toLowerCase() === newCity.value.city.toLowerCase() && 
           c.state === newCity.value.state &&
           c.id !== editingCity.value?.id
  )
  if (cityExists) {
    cityErrors.value.city = 'Esta cidade já foi adicionada'
    isValid = false
  }

  return isValid
}

const handleSaveCity = async () => {
  if (!validateCityForm()) return

  isSavingCity.value = true
  try {
    if (editingCity.value) {
      profileData.value.serviceCities = profileData.value.serviceCities.map((c) =>
        c.id === editingCity.value!.id
          ? { ...c, city: newCity.value.city, state: newCity.value.state }
          : c
      )
    } else {
      profileData.value.serviceCities.push({
        id: Date.now().toString(),
        city: newCity.value.city,
        state: newCity.value.state,
      })
    }

    // Enviar lista completa para o backend como entidades
    const citiesToSend = profileData.value.serviceCities.map(c => ({
      city: c.city,
      state: c.state,
    }))

    await api.put('/freelancer/profile', {
      serviceCities: citiesToSend,
    })

    toast.success(editingCity.value ? 'Cidade atualizada!' : 'Cidade adicionada!')

    // Recarregar para obter IDs do backend
    await loadProfileData()
    closeCityDialog()
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao salvar cidade'
    toast.error(errorMessage)
  } finally {
    isSavingCity.value = false
  }
}

const handleDeleteCity = async () => {
  if (!cityToDelete.value) return

  try {
    profileData.value.serviceCities = profileData.value.serviceCities.filter(
      (c) => c.id !== cityToDelete.value!.id
    )

    const citiesToSend = profileData.value.serviceCities.map(c => ({
      city: c.city,
      state: c.state,
    }))

    await api.put('/freelancer/profile', {
      serviceCities: citiesToSend,
    })

    toast.success('Cidade removida')
    await loadProfileData()
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao remover cidade'
    toast.error(errorMessage)
  } finally {
    cityToDelete.value = null
  }
}

// Professional Links
const clearLinkErrors = () => {
  linkErrors.value = { url: '' }
}

const openAddLinkDialog = () => {
  editingLink.value = null
  newLink.value = { type: 'website', url: '', label: '' }
  clearLinkErrors()
  showLinkDialog.value = true
}

const openEditLinkDialog = (link: ProfessionalLink) => {
  editingLink.value = link
  newLink.value = { type: link.type, url: link.url, label: link.label || '' }
  clearLinkErrors()
  showLinkDialog.value = true
}

const closeLinkDialog = () => {
  showLinkDialog.value = false
  editingLink.value = null
  clearLinkErrors()
}

const validateLinkForm = (): boolean => {
  clearLinkErrors()
  let isValid = true

  if (!newLink.value.url.trim()) {
    linkErrors.value.url = 'URL é obrigatória'
    isValid = false
  } else {
    // Validar URL
    try {
      new URL(newLink.value.url)
    } catch {
      linkErrors.value.url = 'URL inválida (inclua https://)'
      isValid = false
    }
  }

  return isValid
}

const handleSaveLink = async () => {
  if (!validateLinkForm()) return

  isSavingLink.value = true
  try {
    if (editingLink.value) {
      profileData.value.professionalLinks = profileData.value.professionalLinks.map((l) =>
        l.id === editingLink.value!.id
          ? { ...l, type: newLink.value.type, url: newLink.value.url, label: newLink.value.label }
          : l
      )
    } else {
      profileData.value.professionalLinks.push({
        id: Date.now().toString(),
        type: newLink.value.type,
        url: newLink.value.url,
        label: newLink.value.label,
      })
    }

    // Enviar lista completa para o backend como entidades
    const linksToSend = profileData.value.professionalLinks.map(l => ({
      type: l.type,
      url: l.url,
      label: l.label || '',
    }))

    await api.put('/freelancer/profile', {
      professionalLinks: linksToSend,
    })

    toast.success(editingLink.value ? 'Link atualizado!' : 'Link adicionado!')

    // Recarregar para obter IDs do backend
    await loadProfileData()
    closeLinkDialog()
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao salvar link'
    toast.error(errorMessage)
  } finally {
    isSavingLink.value = false
  }
}

const handleDeleteLink = async () => {
  if (!linkToDelete.value) return

  try {
    profileData.value.professionalLinks = profileData.value.professionalLinks.filter(
      (l) => l.id !== linkToDelete.value!.id
    )

    const linksToSend = profileData.value.professionalLinks.map(l => ({
      type: l.type,
      url: l.url,
      label: l.label || '',
    }))

    await api.put('/freelancer/profile', {
      professionalLinks: linksToSend,
    })

    toast.success('Link removido')
    await loadProfileData()
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao remover link'
    toast.error(errorMessage)
  } finally {
    linkToDelete.value = null
  }
}

const getLinkIcon = (type: ProfessionalLink['type']) => {
  const linkType = LINK_TYPES.find((lt) => lt.value === type)
  return linkType?.icon || LinkIcon
}

const getLinkLabel = (link: ProfessionalLink) => {
  if (link.label) return link.label
  const linkType = LINK_TYPES.find((lt) => lt.value === link.type)
  return linkType?.label || 'Link'
}

const formatCurrency = (value: string) => {
  const numbers = value.replace(/\D/g, '')
  const formatted = (parseInt(numbers) / 100).toFixed(2).replace('.', ',')
  return formatted
}

const getWorkModelLabel = (value: string) => {
  const model = WORK_MODELS.find((m) => m.value === value)
  return model?.label || value
}
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6 p-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-[#263238] mb-2">Dados Profissionais</h1>
      <p class="text-[#435574]">Configure suas informações profissionais para atrair mais clientes</p>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="flex items-center justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[#286AD8]"></div>
    </div>

    <template v-else>
      <!-- Work Model, Hourly Rate & Service Type -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Configurações de Trabalho</h3>
          <Button v-if="!isEditingWorkModel" @click="isEditingWorkModel = true" variant="outline" size="sm">
            <Edit class="h-4 w-4 mr-2" />
            Editar
          </Button>
        </div>

        <div v-if="isEditingWorkModel" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Modelo de Trabalho -->
            <div class="space-y-2">
              <Label for="workModel">Modelo de Trabalho *</Label>
              <Select
                :model-value="editedData.workModel"
                @update:model-value="(val: string) => editedData.workModel = val as ProfessionalProfile['workModel']"
              >
                <SelectTrigger>
                  <SelectValue placeholder="Selecione o modelo" :label="WORK_MODELS.find(m => m.value === editedData.workModel)?.label" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="model in WORK_MODELS" :key="model.value" :value="model.value">
                    {{ model.label }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <!-- Tipo de Serviço -->
          <div class="space-y-2">
            <Label for="serviceType">Tipo de Serviço *</Label>
            <Select
              :model-value="editedData.serviceType"
              @update:model-value="(val: string) => editedData.serviceType = val"
            >
              <SelectTrigger>
                <SelectValue placeholder="Selecione o tipo de serviço" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="type in SERVICE_TYPES" :key="type" :value="type">
                  {{ type }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="flex gap-2 pt-4">
            <Button 
              @click="handleSaveWorkModel" 
              class="bg-[#286AD8] hover:bg-[#435574]"
              :disabled="isSaving"
            >
              <Check class="h-4 w-4 mr-2" />
              {{ isSaving ? 'Salvando...' : 'Salvar Alterações' }}
            </Button>
            <Button @click="handleCancelWorkModel" variant="outline">
              <X class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p class="text-sm text-[#435574] mb-1">Modelo de Trabalho</p>
            <div class="flex items-center gap-2">
              <Briefcase class="h-4 w-4 text-[#286AD8]" />
              <Badge variant="secondary">{{ getWorkModelLabel(profileData.workModel) }}</Badge>
            </div>
          </div>
          <div>
            <p class="text-sm text-[#435574] mb-1">Tipo de Serviço</p>
            <div class="flex items-center gap-2">
              <Wrench class="h-4 w-4 text-[#286AD8]" />
              <span class="text-[#263238]">{{ profileData.serviceType || 'Não definido' }}</span>
            </div>
          </div>
        </div>
      </Card>

      <!-- Service Cities -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Cidades de Atendimento</h3>
          <Button @click="openAddCityDialog" variant="outline" size="sm">
            <Plus class="h-4 w-4 mr-2" />
            Adicionar Cidade
          </Button>
        </div>

        <div v-if="profileData.serviceCities.length === 0" class="text-center py-8 text-[#435574]">
          <MapPin class="h-12 w-12 mx-auto mb-3 opacity-50" />
          <p>Nenhuma cidade de atendimento cadastrada</p>
          <p class="text-sm">Adicione as cidades onde você presta serviços</p>
        </div>

        <div v-else class="flex flex-wrap gap-2">
          <div
            v-for="city in profileData.serviceCities"
            :key="city.id"
            class="group flex items-center gap-2 px-3 py-2 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
          >
            <MapPin class="h-4 w-4 text-[#286AD8]" />
            <span class="text-[#263238]">{{ city.city }}/{{ city.state }}</span>
            <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                @click="openEditCityDialog(city)"
                class="p-1 hover:bg-blue-100 rounded"
              >
                <Edit class="h-3 w-3 text-[#286AD8]" />
              </button>
              <button
                @click="cityToDelete = city"
                class="p-1 hover:bg-red-100 rounded"
              >
                <Trash2 class="h-3 w-3 text-red-500" />
              </button>
            </div>
          </div>
        </div>
      </Card>

      <!-- Biography -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Biografia</h3>
          <Button v-if="!isEditingBio" @click="isEditingBio = true" variant="outline" size="sm">
            <Edit class="h-4 w-4 mr-2" />
            Editar
          </Button>
        </div>

        <div v-if="isEditingBio" class="space-y-4">
          <div class="space-y-2">
            <Label for="bio">Conte sobre você e sua experiência profissional</Label>
            <Textarea
              id="bio"
              v-model="editedData.bio"
              placeholder="Descreva sua experiência, habilidades, certificações e o que te diferencia como profissional..."
              :rows="6"
            />
            <p class="text-xs text-[#435574]">{{ editedData.bio.length }}/1000 caracteres</p>
          </div>

          <div class="flex gap-2 pt-4">
            <Button 
              @click="handleSaveBio" 
              class="bg-[#286AD8] hover:bg-[#435574]"
              :disabled="isSaving"
            >
              <Check class="h-4 w-4 mr-2" />
              {{ isSaving ? 'Salvando...' : 'Salvar Alterações' }}
            </Button>
            <Button @click="handleCancelBio" variant="outline">
              <X class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>

        <div v-else>
          <div v-if="profileData.bio" class="flex items-start gap-3">
            <FileText class="h-5 w-5 text-[#286AD8] mt-1 flex-shrink-0" />
            <p class="text-[#263238] whitespace-pre-wrap">{{ profileData.bio }}</p>
          </div>
          <div v-else class="text-center py-8 text-[#435574]">
            <FileText class="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>Nenhuma biografia cadastrada</p>
            <p class="text-sm">Adicione uma descrição sobre você e seus serviços</p>
          </div>
        </div>
      </Card>

      <!-- Professional Links -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Links Profissionais</h3>
          <Button @click="openAddLinkDialog" variant="outline" size="sm">
            <Plus class="h-4 w-4 mr-2" />
            Adicionar Link
          </Button>
        </div>

        <div v-if="profileData.professionalLinks.length === 0" class="text-center py-8 text-[#435574]">
          <LinkIcon class="h-12 w-12 mx-auto mb-3 opacity-50" />
          <p>Nenhum link profissional cadastrado</p>
          <p class="text-sm">Adicione seus links de redes sociais, portfólio, etc.</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="link in profileData.professionalLinks"
            :key="link.id"
            class="group flex items-center justify-between p-3 border rounded-lg hover:border-[#286AD8] transition-colors"
          >
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
                <component :is="getLinkIcon(link.type)" class="h-5 w-5 text-[#286AD8]" />
              </div>
              <div>
                <p class="font-medium text-[#263238]">{{ getLinkLabel(link) }}</p>
                <a
                  :href="link.url"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="text-sm text-[#286AD8] hover:underline"
                >
                  {{ link.url }}
                </a>
              </div>
            </div>
            <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <Button
                @click="openEditLinkDialog(link)"
                variant="ghost"
                size="icon"
                class="text-[#286AD8] hover:bg-blue-50"
              >
                <Edit class="h-4 w-4" />
              </Button>
              <Button
                @click="linkToDelete = link"
                variant="ghost"
                size="icon"
                class="text-red-500 hover:bg-red-50"
              >
                <Trash2 class="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </Card>
    </template>

    <!-- Add/Edit City Dialog -->
    <AlertDialog :open="showCityDialog" @update:open="(val) => { if (!val) closeCityDialog() }">
      <AlertDialogContent class="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>
            {{ editingCity ? 'Editar Cidade' : 'Adicionar Cidade de Atendimento' }}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {{ editingCity ? 'Atualize as informações da cidade.' : 'Digite um CEP para buscar a cidade ou preencha manualmente.' }}
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        <div class="space-y-4 py-4">
          <!-- CEP para busca automática -->
          <div class="flex flex-col gap-1">
            <Label for="city-cep">Buscar por CEP</Label>
            <div class="relative">
              <Input
                id="city-cep"
                :model-value="citySearchCEP"
                @update:model-value="(val) => handleCEPInput(val as string)"
                placeholder="00000-000"
                maxlength="9"
                :disabled="isLoadingCEP"
              />
              <div v-if="isLoadingCEP" class="absolute right-3 top-1/2 -translate-y-1/2">
                <div class="animate-spin h-4 w-4 border-2 border-[#286AD8] border-t-transparent rounded-full"></div>
              </div>
            </div>
            <p class="text-xs text-[#435574]">Digite o CEP para preencher cidade e estado automaticamente</p>
          </div>

          <!-- Separador -->
          <div class="relative">
            <div class="absolute inset-0 flex items-center">
              <span class="w-full border-t" />
            </div>
            <div class="relative flex justify-center text-xs uppercase">
              <span class="bg-white px-2 text-[#435574]">Ou preencha manualmente</span>
            </div>
          </div>

          <!-- Cidade -->
          <div class="flex flex-col gap-1">
            <Label for="city-name">Cidade *</Label>
            <Input
              id="city-name"
              :model-value="newCity.city"
              @update:model-value="(val) => { newCity.city = val as string; cityErrors.city = '' }"
              placeholder="Nome da cidade"
              :disabled="isLoadingCEP"
              :class="{ 'border-red-500 focus:ring-red-500': cityErrors.city, 'bg-gray-50': isLoadingCEP }"
            />
            <p v-if="cityErrors.city" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ cityErrors.city }}
            </p>
          </div>

          <!-- Estado -->
          <div class="flex flex-col gap-1">
            <Label for="city-state">Estado *</Label>
            <Select
              :model-value="newCity.state"
              @update:model-value="(val: string) => { newCity.state = val; cityErrors.state = '' }"
              :disabled="isLoadingCEP"
            >
              <SelectTrigger :class="{ 'border-red-500 focus:ring-red-500': cityErrors.state, 'bg-gray-50': isLoadingCEP }">
                <SelectValue placeholder="Selecione o estado" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="state in BRAZILIAN_STATES" :key="state" :value="state">
                  {{ state }}
                </SelectItem>
              </SelectContent>
            </Select>
            <p v-if="cityErrors.state" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ cityErrors.state }}
            </p>
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel @click="closeCityDialog">Cancelar</AlertDialogCancel>
          <AlertDialogAction 
            @click="handleSaveCity" 
            class="bg-[#286AD8] hover:bg-[#435574]"
            :disabled="isSavingCity || isLoadingCEP"
          >
            {{ isSavingCity ? 'Salvando...' : (editingCity ? 'Salvar Alterações' : 'Adicionar') }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Delete City Dialog -->
    <AlertDialog :open="!!cityToDelete" @update:open="(val) => cityToDelete = val ? cityToDelete : null">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Remover Cidade</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja remover a cidade "{{ cityToDelete?.city }}/{{ cityToDelete?.state }}"?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="cityToDelete = null">Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleDeleteCity" class="bg-red-600 hover:bg-red-700">
            Sim, Remover
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Add/Edit Link Dialog -->
    <AlertDialog :open="showLinkDialog" @update:open="(val) => { if (!val) closeLinkDialog() }">
      <AlertDialogContent class="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle>
            {{ editingLink ? 'Editar Link' : 'Adicionar Link Profissional' }}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {{ editingLink ? 'Atualize as informações do link.' : 'Adicione um link para seu perfil profissional.' }}
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        <div class="space-y-4 py-4">
          <div class="flex flex-col gap-1">
            <Label for="link-type">Tipo de Link *</Label>
            <Select
              :model-value="newLink.type"
              @update:model-value="(val: string) => newLink.type = val as ProfessionalLink['type']"
            >
              <SelectTrigger>
                <SelectValue placeholder="Selecione o tipo" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="type in LINK_TYPES" :key="type.value" :value="type.value">
                  <div class="flex items-center gap-2">
                    <component :is="type.icon" class="h-4 w-4" />
                    {{ type.label }}
                  </div>
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="flex flex-col gap-1">
            <Label for="link-url">URL *</Label>
            <Input
              id="link-url"
              :model-value="newLink.url"
              @update:model-value="(val) => { newLink.url = val as string; linkErrors.url = '' }"
              placeholder="https://..."
              :class="{ 'border-red-500 focus:ring-red-500': linkErrors.url }"
            />
            <p v-if="linkErrors.url" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ linkErrors.url }}
            </p>
          </div>

          <div class="flex flex-col gap-1">
            <Label for="link-label">Rótulo (opcional)</Label>
            <Input
              id="link-label"
              :model-value="newLink.label"
              @update:model-value="(val) => newLink.label = val as string"
              placeholder="Ex: Meu site pessoal"
            />
            <p class="text-xs text-[#435574]">Se não informado, será usado o tipo do link</p>
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel @click="closeLinkDialog">Cancelar</AlertDialogCancel>
          <AlertDialogAction 
            @click="handleSaveLink" 
            class="bg-[#286AD8] hover:bg-[#435574]"
            :disabled="isSavingLink"
          >
            {{ isSavingLink ? 'Salvando...' : (editingLink ? 'Salvar Alterações' : 'Adicionar') }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Delete Link Dialog -->
    <AlertDialog :open="!!linkToDelete" @update:open="(val) => linkToDelete = val ? linkToDelete : null">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Remover Link</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja remover o link "{{ getLinkLabel(linkToDelete!) }}"?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="linkToDelete = null">Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleDeleteLink" class="bg-red-600 hover:bg-red-700">
            Sim, Remover
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
