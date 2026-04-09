<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import {
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
  Sparkles,
} from 'lucide-vue-next'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Card from '../ui/card.vue'
import Badge from '../ui/badge.vue'
import Checkbox from '../ui/checkbox.vue'
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
import { useCategoryCatalogStore } from '@/stores/categoryCatalogStore'
import { freelancerService } from '@/api/services/freelancerService'

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
  categoryId?: number | null
  subcategoryId?: number | null
  subcategoryIds: number[]
  category: string
  subcategory: string
  subcategoryLabels: string[]
  serviceType: string
  bio: string
  serviceCities: ServiceCity[]
  professionalLinks: ProfessionalLink[]
}


const LEGACY_SERVICE_TYPES = [
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
const categoryCatalogStore = useCategoryCatalogStore()
const categories = computed(() =>
  categoryCatalogStore.activePublicCategories.length > 0
    ? categoryCatalogStore.activePublicCategories
    : LEGACY_SERVICE_TYPES.map((name, index) => ({
        id: index + 1,
        slug: name.toLowerCase().replace(/\s+/g, '-'),
        name,
        sortOrder: index,
        active: true,
        subcategories: [],
      }))
)

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
const isEditingServiceSettings = ref(false)
const isEditingBio = ref(false)
const isEditingLinks = ref(false)
const isSaving = ref(false)
const isRefiningBio = ref(false)

// Profile data
const profileData = ref<ProfessionalProfile>({
  categoryId: null,
  subcategoryId: null,
  subcategoryIds: [],
  category: '',
  subcategory: '',
  subcategoryLabels: [],
  serviceType: '',
  bio: '',
  serviceCities: [],
  professionalLinks: [],
})

const editedData = ref<ProfessionalProfile>({ ...profileData.value })
const selectedSubcategories = computed(() =>
  categoryCatalogStore.getSubcategoriesByCategoryId(editedData.value.categoryId || null)
)
const selectedCategoryLabel = computed(
  () => categories.value.find((category) => category.id === editedData.value.categoryId)?.name || ''
)
const selectedSubcategoryLabels = computed(() =>
  selectedSubcategories.value
    .filter((subcategory) => editedData.value.subcategoryIds.includes(subcategory.id))
    .map((subcategory) => subcategory.name)
)
const profileSubcategoryLabels = computed(() =>
  profileData.value.subcategoryLabels.length > 0
    ? profileData.value.subcategoryLabels
    : (profileData.value.subcategory ? [profileData.value.subcategory] : [])
)

const mapSubcategoryIds = (data: any) => {
  if (Array.isArray(data?.subcategoryIds) && data.subcategoryIds.length > 0) {
    return data.subcategoryIds
      .map((id: number | string) => Number(id))
      .filter((id: number) => !Number.isNaN(id))
  }

  if (data?.subcategoryId != null) {
    const primaryId = Number(data.subcategoryId)
    return Number.isNaN(primaryId) ? [] : [primaryId]
  }

  return []
}

const mapSubcategoryLabels = (data: any) => {
  if (Array.isArray(data?.subcategoryLabels) && data.subcategoryLabels.length > 0) {
    return data.subcategoryLabels.filter(
      (label: string | null | undefined): label is string => Boolean(label)
    )
  }

  const primaryLabel = data?.subcategoryLabel || data?.subcategory || ''
  return primaryLabel ? [primaryLabel] : []
}

const buildProfessionalProfile = (data: any): ProfessionalProfile => {
  const subcategoryIds = mapSubcategoryIds(data)
  const subcategoryLabels = mapSubcategoryLabels(data)
  const mappedCities: ServiceCity[] = (data?.serviceCities || []).map((c: any) => ({
    id: c.id?.toString() || Date.now().toString(),
    city: c.city,
    state: c.state,
  }))
  const mappedLinks: ProfessionalLink[] = (data?.professionalLinks || []).map((l: any) => ({
    id: l.id?.toString() || Date.now().toString(),
    type: l.type || 'website',
    url: l.url,
    label: l.label || '',
  }))

  return {
    categoryId: data?.categoryId ?? null,
    subcategoryId: subcategoryIds[0] ?? null,
    subcategoryIds,
    category: data?.categoryLabel || data?.category || data?.serviceType || '',
    subcategory: subcategoryLabels[0] || '',
    subcategoryLabels,
    serviceType: data?.categoryLabel || data?.category || data?.serviceType || '',
    bio: data?.bio || '',
    serviceCities: mappedCities,
    professionalLinks: mappedLinks,
  }
}

const syncEditedSubcategories = (nextIds: number[]) => {
  const normalizedIds = Array.from(new Set(nextIds))
  const selected = selectedSubcategories.value.filter((subcategory) => normalizedIds.includes(subcategory.id))

  editedData.value.subcategoryIds = selected.map((subcategory) => subcategory.id)
  editedData.value.subcategoryLabels = selected.map((subcategory) => subcategory.name)
  editedData.value.subcategoryId = selected[0]?.id ?? null
  editedData.value.subcategory = selected[0]?.name || ''
}

const toggleSubcategory = (subcategoryId: number, checked: boolean) => {
  const nextIds = checked
    ? [...editedData.value.subcategoryIds, subcategoryId]
    : editedData.value.subcategoryIds.filter((id) => id !== subcategoryId)

  syncEditedSubcategories(nextIds)
}

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

onMounted(async () => {
  await categoryCatalogStore.loadPublicCategories()
  await loadProfileData()
})

const loadProfileData = async () => {
  isLoading.value = true
  try {
    const response = await api.get('/freelancer/profile')
    const data = response.data
    const subcategoryIds = mapSubcategoryIds(data)
    const subcategoryLabels = mapSubcategoryLabels(data)
    
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
      categoryId: data.categoryId ?? null,
      subcategoryId: subcategoryIds[0] ?? null,
      subcategoryIds,
      category: data.categoryLabel || data.category || data.serviceType || '',
      subcategory: subcategoryLabels[0] || '',
      subcategoryLabels,
      serviceType: data.categoryLabel || data.category || data.serviceType || '',
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
      const subcategoryIds = mapSubcategoryIds(user)
      const subcategoryLabels = mapSubcategoryLabels(user)
      profileData.value = {
        categoryId: user.categoryId ?? null,
        subcategoryId: subcategoryIds[0] ?? null,
        subcategoryIds,
        category: user.category || user.serviceType || '',
        subcategory: subcategoryLabels[0] || '',
        subcategoryLabels,
        serviceType: user.category || user.serviceType || '',
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

// Service settings
const handleSaveServiceSettings = async () => {
  if (!editedData.value.categoryId) {
    toast.error('Categoria e obrigatoria')
    return
    /*
    toast.error('Tipo de serviço é obrigatório')
    return
  }

    */
  }

  if (selectedSubcategories.value.length > 0 && editedData.value.subcategoryIds.length === 0) {
    toast.error('Subcategoria e obrigatoria')
    return
  }

  const selectedCategory = categories.value.find((category) => category.id === editedData.value.categoryId)
  syncEditedSubcategories(editedData.value.subcategoryIds)

  isSaving.value = true
  try {
    const response = await api.put('/freelancer/profile', {
      serviceType: selectedCategory?.name || editedData.value.serviceType,
      categoryId: editedData.value.categoryId,
      subcategoryId: editedData.value.subcategoryId,
      subcategoryIds: editedData.value.subcategoryIds,
      category: selectedCategory?.name || editedData.value.category,
      subcategory: selectedSubcategoryLabels.value[0] || editedData.value.subcategory,
    })

    profileData.value = buildProfessionalProfile(response.data)
    editedData.value = JSON.parse(JSON.stringify(profileData.value))
    isEditingServiceSettings.value = false
    toast.success('Configurações de trabalho atualizadas!')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao atualizar configurações'
    toast.error(errorMessage)
  } finally {
    isSaving.value = false
  }
}

const handleCancelServiceSettings = () => {
  editedData.value = JSON.parse(JSON.stringify(profileData.value))
  isEditingServiceSettings.value = false
}

watch(
  () => editedData.value.categoryId,
  (categoryId) => {
    const selectedCategory = categories.value.find((category) => category.id === categoryId)
    editedData.value.category = selectedCategory?.name || ''
    editedData.value.serviceType = selectedCategory?.name || ''
    syncEditedSubcategories(editedData.value.subcategoryIds)
  }
)

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

const handleRefineBio = async () => {
  isRefiningBio.value = true
  try {
    editedData.value.bio = await freelancerService.refineBio(editedData.value.bio)
  } catch {
    toast.error('Não foi possível refinar a biografia. Tente novamente.')
  } finally {
    isRefiningBio.value = false
  }
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
      <!-- Service Type -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Configurações de Trabalho</h3>
          <Button v-if="!isEditingServiceSettings" @click="isEditingServiceSettings = true" variant="outline" size="sm">
            <Edit class="h-4 w-4 mr-2" />
            Editar
          </Button>
        </div>

        <div v-if="isEditingServiceSettings" class="space-y-4">
          <!-- Tipo de Serviço -->
          <div class="space-y-2">
            <Label for="serviceType">Tipo de Serviço *</Label>
            <Select
              :model-value="editedData.categoryId ? String(editedData.categoryId) : ''"
              @update:model-value="(val: string) => editedData.categoryId = val ? Number(val) : null"
            >
              <SelectTrigger>
                <SelectValue
                  :label="selectedCategoryLabel || undefined"
                  placeholder="Selecione o tipo de serviço"
                />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="category in categories" :key="category.id" :value="String(category.id)">
                  {{ category.name }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label for="subcategory">
              Subcategorias
              <span v-if="selectedSubcategories.length > 0" class="text-red-500">*</span>
            </Label>
            <div
              v-if="selectedSubcategories.length > 0"
              class="space-y-3 rounded-lg border border-slate-200 p-3"
            >
              <div v-if="editedData.subcategoryLabels.length > 0" class="flex flex-wrap gap-2">
                <Badge
                  v-for="label in editedData.subcategoryLabels"
                  :key="label"
                  variant="secondary"
                  class="bg-blue-50 text-[#286AD8]"
                >
                  {{ label }}
                </Badge>
              </div>

              <div class="space-y-2">
                <label
                  v-for="subcategory in selectedSubcategories"
                  :key="subcategory.id"
                  class="flex cursor-pointer items-center gap-3 rounded-md border border-slate-200 px-3 py-2 transition-colors hover:border-[#286AD8]/40"
                >
                  <Checkbox
                    :checked="editedData.subcategoryIds.includes(subcategory.id)"
                    @update:checked="(checked) => toggleSubcategory(subcategory.id, !!checked)"
                  />
                  <span class="text-sm text-[#263238]">{{ subcategory.name }}</span>
                </label>
              </div>
            </div>
            <div
              v-else
              class="rounded-lg border border-dashed border-slate-200 px-3 py-3 text-sm text-[#435574]"
            >
              Esta categoria não possui subcategorias cadastradas.
            </div>
          </div>

          <div class="flex gap-2 pt-4">
            <Button 
              @click="handleSaveServiceSettings" 
              variant="default"
              :disabled="isSaving"
            >
              <Check class="h-4 w-4 mr-2" />
              {{ isSaving ? 'Salvando...' : 'Salvar Alterações' }}
            </Button>
            <Button @click="handleCancelServiceSettings" variant="outline">
              <X class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>

        <div v-else class="space-y-4">
          <div>
            <p class="text-sm text-[#435574] mb-1">Tipo de Serviço</p>
            <div class="flex items-center gap-2">
              <Wrench class="h-4 w-4 text-[#286AD8]" />
              <span class="text-[#263238]">{{ profileData.serviceType || 'Não definido' }}</span>
            </div>
          </div>

          <div v-if="profileSubcategoryLabels.length > 0">
            <p class="text-sm text-[#435574] mb-2">Subcategorias</p>
            <div class="flex flex-wrap gap-2">
              <Badge
                v-for="subcategory in profileSubcategoryLabels"
                :key="subcategory"
                variant="secondary"
                class="bg-blue-50 text-[#286AD8]"
              >
                {{ subcategory }}
              </Badge>
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
            <div class="flex items-center justify-between">
              <Label for="bio">Conte sobre você e sua experiência profissional</Label>
              <span
                :class="editedData.bio.trim().length < 100 ? 'cursor-not-allowed' : ''"
                :title="editedData.bio.trim().length < 100
                  ? `Digite pelo menos 100 caracteres para refinar (${editedData.bio.trim().length}/100)`
                  : ''"
              >
                <Button
                  @click="handleRefineBio"
                  :disabled="isRefiningBio || editedData.bio.trim().length < 100"
                  variant="outline"
                  size="sm"
                  type="button"
                  :class="editedData.bio.trim().length >= 100
                    ? 'text-[#286AD8] border-[#286AD8] hover:bg-blue-50 hover:text-[#286AD8]'
                    : 'text-gray-400 border-gray-300 opacity-50'"
                >
                  <span v-if="isRefiningBio" class="flex items-center gap-1.5">
                    <span class="animate-spin h-3.5 w-3.5 border-2 border-[#286AD8] border-t-transparent rounded-full" />
                    Refinando...
                  </span>
                  <span v-else class="flex items-center gap-1.5">
                    <Sparkles class="h-3.5 w-3.5" />
                    Refinar com IA
                  </span>
                </Button>
              </span>
            </div>
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
              variant="default"
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
          <AlertDialogAction @click="handleDeleteCity" class="bg-destructive hover:bg-destructive/90">
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
          <AlertDialogAction @click="handleDeleteLink" class="bg-destructive hover:bg-destructive/90">
            Sim, Remover
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
