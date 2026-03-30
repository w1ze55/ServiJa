<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  User,
  Mail,
  Phone,
  Lock,
  MapPin,
  Trash2,
  Plus,
  Edit,
  Check,
  X,
  AlertTriangle,
} from 'lucide-vue-next'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Card from '../ui/card.vue'
import Separator from '../ui/separator.vue'
import Avatar from '../ui/avatar.vue'
import AvatarFallback from '../ui/avatar-fallback.vue'
import AvatarImage from '../ui/avatar-image.vue'
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
import { formatCPF, formatPhone } from '@/utils/masks'
import { useUserStore } from '@/stores/userStore'
import { uploadAvatar } from '@/api/uploadService'

const userStore = useUserStore()

interface Address {
  id: string
  label: string
  street: string
  number: string
  complement?: string
  neighborhood: string
  city: string
  state: string
  zipCode: string
  isDefault: boolean
}

const isEditingPersonal = ref(false)
const isEditingPassword = ref(false)
const showDeleteDialog = ref(false)
const showDeleteConfirmation = ref(false)
const deletePassword = ref('')
const isLoading = ref(false)
const isSavingPersonal = ref(false)
const isSavingPassword = ref(false)
const isDeletingAccount = ref(false)
const isUploadingAvatar = ref(false)

// Personal Data
const personalData = ref({
  name: '',
  email: '',
  phone: '',
  cpf: '',
  avatarUrl: '',
})

const editedPersonalData = ref({ ...personalData.value })

// Password Data
const passwordData = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// Password form validation errors
const passwordErrors = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const clearPasswordErrors = () => {
  passwordErrors.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  }
}

// Addresses
const addresses = ref<Address[]>([])
const addressToDelete = ref<Address | null>(null)
const showAddressDialog = ref(false)
const editingAddress = ref<Address | null>(null)
const isSavingAddress = ref(false)
const newAddress = ref({
  label: '',
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  zipCode: '',
  isDefault: false,
})

// Address form validation errors
const addressErrors = ref({
  label: '',
  street: '',
  number: '',
  neighborhood: '',
  city: '',
  state: '',
  zipCode: '',
})

const clearAddressErrors = () => {
  addressErrors.value = {
    label: '',
    street: '',
    number: '',
    neighborhood: '',
    city: '',
    state: '',
    zipCode: '',
  }
}

// File input ref
const fileInput = ref<HTMLInputElement | null>(null)

onMounted(() => {
  loadUserData()
})

const loadUserData = async () => {
  isLoading.value = true
  try {
    // Carregar dados do usuário e endereços em paralelo
    const [userResponse, addressesResponse] = await Promise.all([
      api.get('/users/me/profile'),
      api.get('/addresses').catch(() => ({ data: [] })) // Fallback para array vazio
    ])
    
    const userData = userResponse.data
    
    personalData.value = {
      name: userData.name || '',
      email: userData.email || '',
      phone: formatPhone(userData.phone || ''),
      cpf: formatCPF(userData.cpf || ''),
      avatarUrl: userData.avatarUrl || '',
    }
    editedPersonalData.value = { ...personalData.value }
    
    // Carregar endereços da API
    addresses.value = (addressesResponse.data || []).map((addr: any) => ({
      id: String(addr.id),
      label: addr.label,
      street: addr.street,
      number: addr.number,
      complement: addr.complement || '',
      neighborhood: addr.neighborhood,
      city: addr.city,
      state: addr.state,
      zipCode: addr.zipCode,
      isDefault: addr.isDefault,
    }))
  } catch (error: any) {
    console.error('Erro ao carregar dados:', error)
    // Usar dados de fallback se não conseguir carregar
    personalData.value = {
      name: '-',
      email: '',
      phone: '',
      cpf: '',
      avatarUrl: '',
    }
    editedPersonalData.value = { ...personalData.value }
    addresses.value = []
  } finally {
    isLoading.value = false
  }
}


const handleSavePersonalData = async () => {
  // Validações
  if (!editedPersonalData.value.name.trim()) {
    toast.error('Nome é obrigatório')
    return
  }
  if (editedPersonalData.value.phone.replace(/\D/g, '').length < 10) {
    toast.error('Telefone inválido')
    return
  }

  isSavingPersonal.value = true
  try {
    await api.put('/users/me', {
      name: editedPersonalData.value.name,
      phone: editedPersonalData.value.phone.replace(/\D/g, ''),
    })
    
    personalData.value = { ...editedPersonalData.value }
    
    // Atualizar store global para refletir as mudanças no header imediatamente
    userStore.updateUser({ name: personalData.value.name })
    
    isEditingPersonal.value = false
    toast.success('Dados atualizados com sucesso!')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao atualizar dados'
    toast.error(errorMessage)
  } finally {
    isSavingPersonal.value = false
  }
}

const handleCancelPersonalEdit = () => {
  editedPersonalData.value = { ...personalData.value }
  isEditingPersonal.value = false
}

const validatePasswordForm = (): boolean => {
  clearPasswordErrors()
  let isValid = true

  if (!passwordData.value.currentPassword) {
    passwordErrors.value.currentPassword = 'Senha atual é obrigatória'
    isValid = false
  }

  if (!passwordData.value.newPassword) {
    passwordErrors.value.newPassword = 'Nova senha é obrigatória'
    isValid = false
  } else if (passwordData.value.newPassword.length < 8) {
    passwordErrors.value.newPassword = 'Mínimo de 8 caracteres'
    isValid = false
  } else {
    // Validar força da senha (mesmas regras do backend)
    const pwd = passwordData.value.newPassword
    const hasUpperCase = /[A-Z]/.test(pwd)
    const hasLowerCase = /[a-z]/.test(pwd)
    const hasDigit = /[0-9]/.test(pwd)
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(pwd)

    if (!hasUpperCase || !hasLowerCase || !hasDigit || !hasSpecial) {
      passwordErrors.value.newPassword = 'Deve conter maiúscula, minúscula, número e caractere especial'
      isValid = false
    }
  }

  if (!passwordData.value.confirmPassword) {
    passwordErrors.value.confirmPassword = 'Confirmação é obrigatória'
    isValid = false
  } else if (passwordData.value.newPassword !== passwordData.value.confirmPassword) {
    passwordErrors.value.confirmPassword = 'As senhas não coincidem'
    isValid = false
  }

  return isValid
}

const handleSavePassword = async () => {
  // Validações com mensagens inline
  if (!validatePasswordForm()) {
    toast.error('Corrija os erros no formulário')
    return
  }

  isSavingPassword.value = true
  try {
    await api.put('/users/me/password', {
      currentPassword: passwordData.value.currentPassword,
      newPassword: passwordData.value.newPassword,
      confirmPassword: passwordData.value.confirmPassword,
    })
    
    passwordData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    clearPasswordErrors()
    isEditingPassword.value = false
    toast.success('Senha alterada com sucesso!')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao alterar senha'
    toast.error(errorMessage)
    
    // Se o erro for de senha atual incorreta, mostrar no campo
    if (errorMessage.toLowerCase().includes('atual') || errorMessage.toLowerCase().includes('incorreta')) {
      passwordErrors.value.currentPassword = errorMessage
    }
  } finally {
    isSavingPassword.value = false
  }
}

const handleCancelPasswordEdit = () => {
  passwordData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
  clearPasswordErrors()
  isEditingPassword.value = false
}

const handleAvatarClick = () => {
  fileInput.value?.click()
}

const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    toast.error('Por favor, selecione uma imagem')
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    toast.error('A imagem deve ter no máximo 10MB')
    return
  }

  isUploadingAvatar.value = true
  try {
    const url = await uploadAvatar(file)
    
    console.log('Avatar uploaded, URL:', url)
    
    personalData.value.avatarUrl = url
    editedPersonalData.value.avatarUrl = url
    
    // Atualizar store global para refletir as mudanças no header imediatamente
    userStore.updateUser({ avatar: url })
    
    toast.success('Foto atualizada com sucesso!')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao atualizar foto'
    toast.error(errorMessage)
  } finally {
    isUploadingAvatar.value = false
  }
}

const handleSetDefaultAddress = async (addressId: string) => {
  try {
    await api.put(`/addresses/${addressId}/default`)
    // Atualizar todos os endereços: apenas o selecionado é padrão
    addresses.value = addresses.value.map((addr) => ({
      ...addr,
      isDefault: addr.id === addressId,
    }))
    toast.success('Endereço padrão atualizado')
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao definir endereço padrão'
    toast.error(errorMessage)
  }
}

// Função auxiliar para recarregar endereços da API
const reloadAddresses = async () => {
  try {
    const response = await api.get('/addresses')
    addresses.value = (response.data || []).map((addr: any) => ({
      id: String(addr.id),
      label: addr.label,
      street: addr.street,
      number: addr.number,
      complement: addr.complement || '',
      neighborhood: addr.neighborhood,
      city: addr.city,
      state: addr.state,
      zipCode: addr.zipCode,
      isDefault: addr.isDefault,
    }))
  } catch (error) {
    console.error('Erro ao recarregar endereços:', error)
  }
}

const handleDeleteAddress = async () => {
  if (addressToDelete.value) {
    try {
      await api.delete(`/addresses/${addressToDelete.value.id}`)
      addresses.value = addresses.value.filter((addr) => addr.id !== addressToDelete.value!.id)
      addressToDelete.value = null
      toast.success('Endereço removido')
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Erro ao remover endereço'
      toast.error(errorMessage)
      addressToDelete.value = null
    }
  }
}

const openAddAddressDialog = () => {
  editingAddress.value = null
  newAddress.value = {
    label: '',
    street: '',
    number: '',
    complement: '',
    neighborhood: '',
    city: '',
    state: '',
    zipCode: '',
    isDefault: false,
  }
  clearAddressErrors()
  showAddressDialog.value = true
}

const openEditAddressDialog = (address: Address) => {
  editingAddress.value = address
  newAddress.value = {
    label: address.label,
    street: address.street,
    number: address.number,
    complement: address.complement || '',
    neighborhood: address.neighborhood,
    city: address.city,
    state: address.state,
    zipCode: address.zipCode,
    isDefault: address.isDefault,
  }
  clearAddressErrors()
  showAddressDialog.value = true
}

const closeAddressDialog = () => {
  showAddressDialog.value = false
  editingAddress.value = null
  clearAddressErrors()
}

const formatCEP = (value: string) => {
  const numbers = value.replace(/\D/g, '')
  return numbers.replace(/(\d{5})(\d{0,3})/, '$1-$2')
}

// ViaCEP Integration
const isLoadingCEP = ref(false)

const fetchAddressByCEP = async (cep: string) => {
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

    // Preencher campos automaticamente
    newAddress.value.street = data.logradouro || ''
    newAddress.value.neighborhood = data.bairro || ''
    newAddress.value.city = data.localidade || ''
    newAddress.value.state = data.uf || ''
    
    // Se houver complemento padrão do CEP, adicionar
    if (data.complemento && !newAddress.value.complement) {
      newAddress.value.complement = data.complemento
    }

    toast.success('Endereço encontrado!')
  } catch (error) {
    console.error('Erro ao buscar CEP:', error)
    toast.error('Erro ao buscar CEP. Verifique sua conexão.')
  } finally {
    isLoadingCEP.value = false
  }
}

const handleCEPInput = (value: string) => {
  const formatted = formatCEP(value)
  newAddress.value.zipCode = formatted
  
  // Auto-buscar quando CEP completo (8 dígitos)
  const cleanCEP = formatted.replace(/\D/g, '')
  if (cleanCEP.length === 8) {
    fetchAddressByCEP(cleanCEP)
  }
}

const validateAddressForm = (): boolean => {
  clearAddressErrors()
  let isValid = true

  if (!newAddress.value.label.trim()) {
    addressErrors.value.label = 'Identificação é obrigatória'
    isValid = false
  }

  const cleanCEP = newAddress.value.zipCode.replace(/\D/g, '')
  if (!cleanCEP) {
    addressErrors.value.zipCode = 'CEP é obrigatório'
    isValid = false
  } else if (cleanCEP.length !== 8) {
    addressErrors.value.zipCode = 'CEP deve ter 8 dígitos'
    isValid = false
  }

  if (!newAddress.value.street.trim()) {
    addressErrors.value.street = 'Rua é obrigatória'
    isValid = false
  }

  if (!newAddress.value.number.trim()) {
    addressErrors.value.number = 'Número é obrigatório'
    isValid = false
  }

  if (!newAddress.value.neighborhood.trim()) {
    addressErrors.value.neighborhood = 'Bairro é obrigatório'
    isValid = false
  }

  if (!newAddress.value.city.trim()) {
    addressErrors.value.city = 'Cidade é obrigatória'
    isValid = false
  }

  if (!newAddress.value.state.trim()) {
    addressErrors.value.state = 'Estado é obrigatório'
    isValid = false
  } else if (newAddress.value.state.length !== 2) {
    addressErrors.value.state = 'Use a sigla (ex: SP)'
    isValid = false
  }

  return isValid
}

const handleSaveAddress = async () => {
  // Validações com mensagens inline
  if (!validateAddressForm()) {
    return
  }

  isSavingAddress.value = true
  try {
    const addressData = {
      label: newAddress.value.label,
      street: newAddress.value.street,
      number: newAddress.value.number,
      complement: newAddress.value.complement || null,
      neighborhood: newAddress.value.neighborhood,
      city: newAddress.value.city,
      state: newAddress.value.state.toUpperCase(),
      zipCode: newAddress.value.zipCode,
      isDefault: newAddress.value.isDefault,
    }

    if (editingAddress.value) {
      // Atualizar endereço existente
      await api.put(`/addresses/${editingAddress.value.id}`, addressData)
      toast.success('Endereço atualizado com sucesso!')
    } else {
      // Criar novo endereço
      await api.post('/addresses', addressData)
      toast.success('Endereço adicionado com sucesso!')
    }
    
    // Recarregar endereços da API para garantir consistência do isDefault
    await reloadAddresses()

    closeAddressDialog()
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Erro ao salvar endereço'
    toast.error(errorMessage)
  } finally {
    isSavingAddress.value = false
  }
}

const handleDeleteAccount = async () => {
  if (!deletePassword.value) {
    toast.error('Digite sua senha para confirmar')
    return
  }

  isDeletingAccount.value = true
  try {
    await api.delete('/users/me', {
      data: { password: deletePassword.value }
    })
    
    showDeleteConfirmation.value = false
    toast.success('Conta excluída com sucesso')
    
    // Limpar token e redirecionar
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setTimeout(() => {
      window.location.href = '/'
    }, 2000)
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Senha incorreta'
    toast.error(errorMessage)
  } finally {
    isDeletingAccount.value = false
  }
}

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6 p-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-[#263238] mb-2">Meu Perfil</h1>
      <p class="text-[#435574]">Gerencie suas informações pessoais e preferências</p>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="flex items-center justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-[#286AD8]"></div>
    </div>

    <template v-else>
      <!-- Avatar Section -->
      <Card class="p-6">
        <div class="flex items-center gap-6">
          <Avatar class="h-24 w-24 cursor-pointer" @click="handleAvatarClick">
            <AvatarImage :src="personalData.avatarUrl" :alt="personalData.name" />
            <AvatarFallback class="bg-[#286AD8] text-white text-2xl">
              {{ getInitials(personalData.name) }}
            </AvatarFallback>
          </Avatar>
          <div class="flex-1">
            <h2 class="text-xl font-semibold text-[#263238] mb-1">{{ personalData.name }}</h2>
            <p class="text-[#435574] mb-3">{{ personalData.email }}</p>
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleFileUpload"
            />
            <Button 
              variant="outline" 
              size="sm" 
              class="avatar-upload-btn"
              @click="handleAvatarClick"
              :disabled="isUploadingAvatar"
            >
              <Edit class="h-4 w-4 mr-2" />
              {{ isUploadingAvatar ? 'Enviando...' : 'Alterar Foto' }}
            </Button>
          </div>
        </div>
      </Card>

      <!-- Personal Information -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Informações Pessoais</h3>
          <Button v-if="!isEditingPersonal" @click="isEditingPersonal = true" variant="outline" size="sm">
            <Edit class="h-4 w-4 mr-2" />
            Editar
          </Button>
        </div>

        <div v-if="isEditingPersonal" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="name">Nome Completo *</Label>
              <Input
                id="name"
                :model-value="editedPersonalData.name"
                @update:model-value="(val) => editedPersonalData.name = val as string"
                placeholder="Digite seu nome completo"
              />
            </div>

            <div class="space-y-2">
              <Label for="email">E-mail</Label>
              <Input
                id="email"
                type="email"
                :model-value="editedPersonalData.email"
                disabled
                class="bg-gray-50"
              />
              <p class="text-xs text-[#435574]">E-mail não pode ser alterado</p>
            </div>

            <div class="space-y-2">
              <Label for="phone">Telefone *</Label>
              <Input
                id="phone"
                :model-value="editedPersonalData.phone"
                @update:model-value="(val) => editedPersonalData.phone = formatPhone(val as string)"
                placeholder="(11) 98765-4321"
                maxlength="15"
              />
            </div>

            <div class="space-y-2">
              <Label for="cpf">CPF</Label>
              <Input id="cpf" :model-value="editedPersonalData.cpf" disabled maxlength="14" class="bg-gray-50" />
              <p class="text-xs text-[#435574]">CPF não pode ser alterado</p>
            </div>
          </div>

          <div class="flex gap-2 pt-4">
            <Button 
              @click="handleSavePersonalData" 
              variant="default"
              :disabled="isSavingPersonal"
            >
              <Check class="h-4 w-4 mr-2" />
              {{ isSavingPersonal ? 'Salvando...' : 'Salvar Alterações' }}
            </Button>
            <Button @click="handleCancelPersonalEdit" variant="outline">
              <X class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p class="text-sm text-[#435574] mb-1">Nome Completo</p>
            <p class="text-[#263238]">{{ personalData.name }}</p>
          </div>
          <div>
            <p class="text-sm text-[#435574] mb-1">E-mail</p>
            <p class="text-[#263238]">{{ personalData.email }}</p>
          </div>
          <div>
            <p class="text-sm text-[#435574] mb-1">Telefone</p>
            <p class="text-[#263238]">{{ personalData.phone || '-' }}</p>
          </div>
          <div>
            <p class="text-sm text-[#435574] mb-1">CPF</p>
            <p class="text-[#263238]">{{ personalData.cpf || '-' }}</p>
          </div>
        </div>
      </Card>

      <!-- Password Section -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Senha</h3>
          <Button v-if="!isEditingPassword" @click="isEditingPassword = true" variant="outline" size="sm">
            <Lock class="h-4 w-4 mr-2" />
            Alterar Senha
          </Button>
        </div>

        <div v-if="isEditingPassword" class="space-y-4">
          <!-- Senha Atual -->
          <div class="flex flex-col gap-1">
            <Label for="current-password">Senha Atual *</Label>
            <Input
              id="current-password"
              type="password"
              :model-value="passwordData.currentPassword"
              @update:model-value="(val) => { passwordData.currentPassword = val as string; passwordErrors.currentPassword = '' }"
              placeholder="Digite sua senha atual"
              :class="{ 'border-red-500 focus:ring-red-500': passwordErrors.currentPassword }"
            />
            <p v-if="passwordErrors.currentPassword" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ passwordErrors.currentPassword }}
            </p>
          </div>

          <!-- Nova Senha -->
          <div class="flex flex-col gap-1">
            <Label for="new-password">Nova Senha *</Label>
            <Input
              id="new-password"
              type="password"
              :model-value="passwordData.newPassword"
              @update:model-value="(val) => { passwordData.newPassword = val as string; passwordErrors.newPassword = '' }"
              placeholder="Digite a nova senha"
              :class="{ 'border-red-500 focus:ring-red-500': passwordErrors.newPassword }"
            />
            <p v-if="passwordErrors.newPassword" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ passwordErrors.newPassword }}
            </p>
            <p v-else class="text-xs text-[#435574]">Mínimo 8 caracteres: maiúscula, minúscula, número e especial (!@#$%)</p>
          </div>

          <!-- Confirmar Senha -->
          <div class="flex flex-col gap-1">
            <Label for="confirm-password">Confirmar Nova Senha *</Label>
            <Input
              id="confirm-password"
              type="password"
              :model-value="passwordData.confirmPassword"
              @update:model-value="(val) => { passwordData.confirmPassword = val as string; passwordErrors.confirmPassword = '' }"
              placeholder="Digite a nova senha novamente"
              :class="{ 'border-red-500 focus:ring-red-500': passwordErrors.confirmPassword }"
            />
            <p v-if="passwordErrors.confirmPassword" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ passwordErrors.confirmPassword }}
            </p>
          </div>

          <div class="flex gap-2 pt-4">
            <Button 
              @click="handleSavePassword" 
              variant="default"
              :disabled="isSavingPassword"
            >
              <Check class="h-4 w-4 mr-2" />
              {{ isSavingPassword ? 'Salvando...' : 'Salvar Senha' }}
            </Button>
            <Button
              @click="handleCancelPasswordEdit"
              variant="outline"
            >
              <X class="h-4 w-4 mr-2" />
              Cancelar
            </Button>
          </div>
        </div>

        <p v-else class="text-[#435574]">••••••••</p>
      </Card>

      <!-- Addresses Section -->
      <Card class="p-6">
        <div class="address-header flex items-center justify-between mb-4">
          <h3 class="text-lg font-semibold text-[#263238]">Endereços</h3>
          <Button @click="openAddAddressDialog" variant="outline" size="sm" class="add-address-btn">
            <Plus class="h-4 w-4 mr-2" />
            Adicionar Endereço
          </Button>
        </div>

        <div v-if="addresses.length === 0" class="text-center py-8 text-[#435574]">
          <MapPin class="h-12 w-12 mx-auto mb-3 opacity-50" />
          <p>Nenhum endereço cadastrado</p>
          <p class="text-sm">Adicione um endereço para facilitar suas contratações</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="address in addresses"
            :key="address.id"
            class="p-4 border rounded-lg hover:border-[#286AD8] transition-colors"
          >
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-2">
                  <h4 class="font-medium text-[#263238]">{{ address.label }}</h4>
                  <span v-if="address.isDefault" class="text-xs bg-primary text-white px-2 py-0.5 rounded">
                    Padrão
                  </span>
                </div>
                <p class="text-sm text-[#435574]">
                  {{ address.street }}, {{ address.number }}
                  <span v-if="address.complement"> - {{ address.complement }}</span>
                </p>
                <p class="text-sm text-[#435574]">
                  {{ address.neighborhood }} - {{ address.city }}/{{ address.state }}
                </p>
                <p class="text-sm text-[#435574]">CEP: {{ address.zipCode }}</p>
              </div>
              <div class="flex gap-2">
                  <Button
                    v-if="!address.isDefault"
                    @click="handleSetDefaultAddress(address.id)"
                    variant="ghost"
                    size="sm"
                  >
                    Definir como padrão
                  </Button>
                <Button
                  @click="openEditAddressDialog(address)"
                  variant="ghost"
                  size="icon"
                  class="text-[#286AD8] hover:text-[#435574] hover:bg-blue-50"
                >
                  <Edit class="h-4 w-4" />
                </Button>
                <Button
                  @click="addressToDelete = address"
                  variant="ghost"
                  size="icon"
                  class="text-red-500 hover:text-red-700 hover:bg-red-50"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>
      </Card>

      <!-- Danger Zone -->
      <Card class="p-6 border-red-200">
        <h3 class="text-lg font-semibold text-red-600 mb-2">Zona de Perigo</h3>
        <p class="text-sm text-[#435574] mb-4">
          Ações irreversíveis. Tenha certeza antes de prosseguir.
        </p>

        <Separator class="my-4" />

        <div class="flex items-start justify-between">
          <div>
            <h4 class="font-medium text-[#263238] mb-1">Excluir Conta</h4>
            <p class="text-sm text-[#435574] max-w-md">
              Ao excluir sua conta, todos os seus dados serão permanentemente removidos,
              incluindo histórico de contratações, avaliações e mensagens. Esta ação não pode ser desfeita.
            </p>
          </div>
          <Button @click="showDeleteDialog = true" variant="destructive">
            <Trash2 class="h-4 w-4 mr-2" />
            Excluir Conta
          </Button>
        </div>
      </Card>
    </template>

    <!-- Add/Edit Address Dialog -->
    <AlertDialog :open="showAddressDialog" @update:open="(val) => { if (!val) closeAddressDialog() }">
      <AlertDialogContent class="max-w-lg">
        <AlertDialogHeader>
          <AlertDialogTitle>
            {{ editingAddress ? 'Editar Endereço' : 'Adicionar Endereço' }}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {{ editingAddress ? 'Atualize as informações do endereço.' : 'Preencha os dados do novo endereço.' }}
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        <div class="space-y-4 py-4">
          <!-- Identificação -->
          <div class="flex flex-col gap-1">
            <Label for="address-label">Identificação *</Label>
            <Input
              id="address-label"
              :model-value="newAddress.label"
              @update:model-value="(val) => { newAddress.label = val as string; addressErrors.label = '' }"
              placeholder="Ex: Casa, Trabalho, etc."
              :class="{ 'border-red-500 focus:ring-red-500': addressErrors.label }"
            />
            <p v-if="addressErrors.label" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ addressErrors.label }}
            </p>
          </div>

          <!-- CEP primeiro para auto-preenchimento via ViaCEP -->
          <div class="flex flex-col gap-1">
            <Label for="address-zipcode">CEP *</Label>
            <div class="relative">
              <Input
                id="address-zipcode"
                :model-value="newAddress.zipCode"
                @update:model-value="(val) => { handleCEPInput(val as string); addressErrors.zipCode = '' }"
                placeholder="00000-000"
                maxlength="9"
                :disabled="isLoadingCEP"
                :class="{ 'border-red-500 focus:ring-red-500': addressErrors.zipCode }"
              />
              <div v-if="isLoadingCEP" class="absolute right-3 top-1/2 -translate-y-1/2">
                <div class="animate-spin h-4 w-4 border-2 border-[#286AD8] border-t-transparent rounded-full"></div>
              </div>
            </div>
            <p v-if="addressErrors.zipCode" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ addressErrors.zipCode }}
            </p>
            <p v-else class="text-xs text-[#435574]">Digite o CEP para preencher automaticamente</p>
          </div>

          <div class="grid grid-cols-3 gap-4">
            <!-- Rua -->
            <div class="col-span-2 flex flex-col gap-1">
              <Label for="address-street">Rua *</Label>
              <Input
                id="address-street"
                :model-value="newAddress.street"
                @update:model-value="(val) => { newAddress.street = val as string; addressErrors.street = '' }"
                placeholder="Nome da rua"
                :class="{ 'bg-gray-50': isLoadingCEP, 'border-red-500 focus:ring-red-500': addressErrors.street }"
              />
              <p v-if="addressErrors.street" class="text-xs text-red-500 flex items-center gap-1">
                <AlertTriangle class="h-3 w-3" />
                {{ addressErrors.street }}
              </p>
            </div>
            <!-- Número -->
            <div class="flex flex-col gap-1">
              <Label for="address-number">Número *</Label>
              <Input
                id="address-number"
                :model-value="newAddress.number"
                @update:model-value="(val) => { newAddress.number = val as string; addressErrors.number = '' }"
                placeholder="123"
                :class="{ 'border-red-500 focus:ring-red-500': addressErrors.number }"
              />
              <p v-if="addressErrors.number" class="text-xs text-red-500 flex items-center gap-1">
                <AlertTriangle class="h-3 w-3" />
                {{ addressErrors.number }}
              </p>
            </div>
          </div>

          <!-- Complemento (sem validação obrigatória) -->
          <div class="flex flex-col gap-1">
            <Label for="address-complement">Complemento</Label>
            <Input
              id="address-complement"
              :model-value="newAddress.complement"
              @update:model-value="(val) => newAddress.complement = val as string"
              placeholder="Apto, Bloco, etc. (opcional)"
            />
          </div>

          <!-- Bairro -->
          <div class="flex flex-col gap-1">
            <Label for="address-neighborhood">Bairro *</Label>
            <Input
              id="address-neighborhood"
              :model-value="newAddress.neighborhood"
              @update:model-value="(val) => { newAddress.neighborhood = val as string; addressErrors.neighborhood = '' }"
              placeholder="Nome do bairro"
              :class="{ 'bg-gray-50': isLoadingCEP, 'border-red-500 focus:ring-red-500': addressErrors.neighborhood }"
            />
            <p v-if="addressErrors.neighborhood" class="text-xs text-red-500 flex items-center gap-1">
              <AlertTriangle class="h-3 w-3" />
              {{ addressErrors.neighborhood }}
            </p>
          </div>

          <div class="grid grid-cols-3 gap-4">
            <!-- Cidade -->
            <div class="col-span-2 flex flex-col gap-1">
              <Label for="address-city">Cidade *</Label>
              <Input
                id="address-city"
                :model-value="newAddress.city"
                @update:model-value="(val) => { newAddress.city = val as string; addressErrors.city = '' }"
                placeholder="Nome da cidade"
                :class="{ 'bg-gray-50': isLoadingCEP, 'border-red-500 focus:ring-red-500': addressErrors.city }"
              />
              <p v-if="addressErrors.city" class="text-xs text-red-500 flex items-center gap-1">
                <AlertTriangle class="h-3 w-3" />
                {{ addressErrors.city }}
              </p>
            </div>
            <!-- Estado -->
            <div class="flex flex-col gap-1">
              <Label for="address-state">Estado *</Label>
              <Input
                id="address-state"
                :model-value="newAddress.state"
                @update:model-value="(val) => { newAddress.state = (val as string).toUpperCase().slice(0, 2); addressErrors.state = '' }"
                placeholder="SP"
                maxlength="2"
                :class="{ 'bg-gray-50': isLoadingCEP, 'border-red-500 focus:ring-red-500': addressErrors.state }"
              />
              <p v-if="addressErrors.state" class="text-xs text-red-500 flex items-center gap-1">
                <AlertTriangle class="h-3 w-3" />
                {{ addressErrors.state }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <input
              type="checkbox"
              id="address-default"
              :checked="newAddress.isDefault"
              @change="(e) => newAddress.isDefault = (e.target as HTMLInputElement).checked"
              class="h-4 w-4 rounded border-gray-300"
            />
            <Label for="address-default" class="cursor-pointer">Definir como endereço padrão</Label>
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel @click="closeAddressDialog">Cancelar</AlertDialogCancel>
          <AlertDialogAction 
            @click="handleSaveAddress" 
            class="bg-primary hover:bg-primary/90"
            :disabled="isSavingAddress"
          >
            {{ isSavingAddress ? 'Salvando...' : (editingAddress ? 'Salvar Alterações' : 'Adicionar') }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Delete Address Dialog -->
    <AlertDialog :open="!!addressToDelete" @update:open="(val) => addressToDelete = val ? addressToDelete : null">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Remover Endereço</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja remover o endereço "{{ addressToDelete?.label }}"?
            Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="addressToDelete = null">Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleDeleteAddress" class="bg-destructive hover:bg-destructive/90">
            Sim, Remover
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Delete Account Dialogs -->
    <AlertDialog :open="showDeleteDialog" @update:open="(val) => showDeleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <div class="flex items-center gap-3 mb-2">
            <div class="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
              <AlertTriangle class="h-6 w-6 text-red-600" />
            </div>
            <AlertDialogTitle>Excluir Conta Permanentemente</AlertDialogTitle>
          </div>
          <AlertDialogDescription>
            <strong class="text-red-600">Atenção:</strong> Esta ação é irreversível e resultará em:
            <ul class="list-disc list-inside mt-2 space-y-1 text-sm">
              <li>Exclusão permanente de todos os seus dados pessoais</li>
              <li>Perda do histórico de contratações e avaliações</li>
              <li>Remoção de todas as mensagens e conversas</li>
              <li>Cancelamento de contratos em andamento</li>
            </ul>
            <p class="mt-3 text-sm">
              Conforme a LGPD, seus dados serão completamente removidos de nossos sistemas
              em até 30 dias.
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showDeleteDialog = false">Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="showDeleteDialog = false; showDeleteConfirmation = true"
            class="bg-destructive hover:bg-destructive/90"
          >
            Prosseguir com Exclusão
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog :open="showDeleteConfirmation" @update:open="(val) => showDeleteConfirmation = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmação Final</AlertDialogTitle>
          <AlertDialogDescription>
            Digite sua senha para confirmar a exclusão da conta:
          </AlertDialogDescription>
        </AlertDialogHeader>
        <div class="py-4">
          <Input
            type="password"
            placeholder="Digite sua senha"
            :model-value="deletePassword"
            @update:model-value="(val) => deletePassword = val as string"
          />
        </div>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showDeleteConfirmation = false; deletePassword = ''">Cancelar</AlertDialogCancel>
          <AlertDialogAction 
            @click="handleDeleteAccount" 
            class="bg-destructive hover:bg-destructive/90"
            :disabled="isDeletingAccount"
          >
            {{ isDeletingAccount ? 'Excluindo...' : 'Confirmar Exclusão' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

<style scoped>
/* Responsividade para Avatar Section */
@media (max-width: 768px) {
  .max-w-4xl {
    padding: 1rem !important;
  }
}

@media (max-width: 480px) {
  .max-w-4xl {
    padding: 0.75rem !important;
  }

  /* Avatar Section - empilhar verticalmente */
  :deep(.p-6) .flex.items-center.gap-6 {
    flex-direction: column;
    text-align: center;
    gap: 1rem !important;
  }

  /* Centralizar avatar */
  :deep(.p-6) .flex.items-center.gap-6 > div:first-child {
    margin: 0 auto;
  }

  /* Container de texto e botão */
  :deep(.p-6) .flex-1 {
    width: 100%;
  }

  /* Quebrar email com word-break */
  :deep(.p-6) .flex-1 p {
    word-break: break-all;
    overflow-wrap: break-word;
    font-size: 0.875rem;
  }

  /* Título menor */
  :deep(.p-6) .flex-1 h2 {
    font-size: 1.125rem;
    word-break: break-word;
  }

  /* Botão Alterar Foto responsivo */
  :deep(.avatar-upload-btn) {
    width: 100%;
    max-width: 250px;
    margin: 0 auto;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 0.875rem;
    padding: 0.625rem 1rem;
  }

  :deep(.avatar-upload-btn svg) {
    width: 1rem;
    height: 1rem;
  }
}

@media (max-width: 408px) {
  /* Ajustes específicos para 408px e abaixo */
  :deep(.p-6) .flex-1 h2 {
    font-size: 1rem;
  }

  :deep(.p-6) .flex-1 p {
    font-size: 0.8125rem;
    margin-bottom: 0.75rem !important;
  }

  /* Botão menor em telas muito pequenas */
  :deep(.avatar-upload-btn) {
    max-width: 200px;
    font-size: 0.8125rem;
    padding: 0.5rem 0.875rem;
  }

  :deep(.avatar-upload-btn svg) {
    width: 0.875rem;
    height: 0.875rem;
    margin-right: 0.375rem;
  }

  /* Reduzir tamanho do avatar */
  :deep(.h-24.w-24) {
    height: 5rem !important;
    width: 5rem !important;
  }

  :deep(.h-24.w-24 .text-2xl) {
    font-size: 1.5rem !important;
  }
}

@media (max-width: 375px) {
  /* Botão Adicionar Endereço abaixo do título */
  .address-header {
    flex-direction: column !important;
    align-items: flex-start !important;
    gap: 0.75rem;
  }

  .add-address-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
