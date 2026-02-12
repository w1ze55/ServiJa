<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { User, Mail, Phone, CreditCard, Camera, Save, X, CheckCircle, AlertCircle, Edit2 } from 'lucide-vue-next'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Card from '../ui/card.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import { fetchWithAuth, handleApiError } from '../../utils/api'
import { API_ENDPOINTS } from '../../utils/api'
import type { User as UserType } from '../../App.vue'

interface Props {
  user: UserType
  onNavigate: (page: string, params?: any) => void
}

const props = defineProps<Props>()

// Estados
const isLoading = ref(false)
const isEditing = ref(false)
const error = ref('')
const success = ref('')

// Dados do formulário
const formData = ref({
  name: props.user.name,
  email: props.user.email,
  phone: '',
  cpf: '',
})

// CPF formatado
const formattedCpf = computed(() => {
  const cpf = formData.value.cpf.replace(/\D/g, '')
  return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
})

// Telefone formatado
const formattedPhone = computed(() => {
  const phone = formData.value.phone.replace(/\D/g, '')
  if (phone.length === 11) {
    return phone.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
  } else if (phone.length === 10) {
    return phone.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3')
  }
  return phone
})

// Carregar dados do usuário
onMounted(async () => {
  await loadUserData()
})

async function loadUserData() {
  try {
    isLoading.value = true
    error.value = ''
    
    const response = await fetchWithAuth(API_ENDPOINTS.GET_CURRENT_USER)
    
    if (response.ok) {
      const userData = await response.json()
      formData.value.name = userData.name || props.user.name
      formData.value.email = userData.email || props.user.email
      formData.value.phone = userData.phone || ''
      formData.value.cpf = userData.cpf || ''
    } else {
      // Fallback para localStorage se a API falhar
      const storedUser = localStorage.getItem('user')
      if (storedUser) {
        const localUserData = JSON.parse(storedUser)
        formData.value.name = localUserData.name || props.user.name
        formData.value.email = localUserData.email || props.user.email
        formData.value.phone = localUserData.phone || ''
        formData.value.cpf = localUserData.cpf || ''
      }
    }
  } catch (err) {
    console.error('Erro ao carregar dados:', err)
    error.value = 'Erro ao carregar dados do usuário'
    // Fallback para localStorage
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      const userData = JSON.parse(storedUser)
      formData.value.name = userData.name || props.user.name
      formData.value.email = userData.email || props.user.email
      formData.value.phone = userData.phone || ''
      formData.value.cpf = userData.cpf || ''
    }
  } finally {
    isLoading.value = false
  }
}

function handleEdit() {
  isEditing.value = true
  error.value = ''
  success.value = ''
}

function handleCancel() {
  isEditing.value = false
  // Restaurar valores originais
  formData.value = {
    name: props.user.name,
    email: props.user.email,
    phone: '',
    cpf: '',
  }
  error.value = ''
  success.value = ''
  loadUserData()
}

async function handleSave() {
  try {
    isLoading.value = true
    error.value = ''
    success.value = ''

    // Validações
    if (!formData.value.name || formData.value.name.trim().length < 3) {
      error.value = 'Nome deve ter no mínimo 3 caracteres'
      isLoading.value = false
      return
    }

    // Preparar dados para envio (remover formatação)
    const updateData = {
      name: formData.value.name.trim(),
      phone: formData.value.phone.replace(/\D/g, ''),
      cpf: formData.value.cpf.replace(/\D/g, ''),
    }

    const response = await fetchWithAuth(API_ENDPOINTS.UPDATE_USER, {
      method: 'PUT',
      body: JSON.stringify(updateData),
    })

    if (response.ok) {
      const updatedUserData = await response.json()
      
      // Atualizar localStorage com os dados atualizados
      const updatedUser = {
        ...props.user,
        name: updatedUserData.name,
        email: updatedUserData.email,
      }
      localStorage.setItem('user', JSON.stringify(updatedUser))

      success.value = 'Dados atualizados com sucesso!'
      isEditing.value = false

      // Recarregar após 2 segundos
      setTimeout(() => {
        success.value = ''
        loadUserData()
      }, 2000)
    } else {
      const errorData = await response.json()
      error.value = errorData.message || 'Erro ao atualizar dados'
    }
  } catch (err) {
    error.value = handleApiError(err)
  } finally {
    isLoading.value = false
  }
}

function handlePhoneInput(event: Event) {
  const input = event.target as HTMLInputElement
  let value = input.value.replace(/\D/g, '')
  if (value.length > 11) value = value.slice(0, 11)
  formData.value.phone = value
}

function handleCpfInput(event: Event) {
  const input = event.target as HTMLInputElement
  let value = input.value.replace(/\D/g, '')
  if (value.length > 11) value = value.slice(0, 11)
  formData.value.cpf = value
}

const userTypeLabel = computed(() => {
  return props.user.type === 'contractor' ? 'Contratante' : 'Prestador'
})
</script>

<template>
  <div class="my-account-page">
    <div class="account-container">
      <!-- Header -->
      <div class="account-header">
        <div>
          <h1 class="account-title">Minha Conta</h1>
          <p class="account-subtitle">Gerencie suas informações pessoais e preferências</p>
        </div>
        <Button
          v-if="!isEditing"
          @click="handleEdit"
          class="btn-edit"
        >
          <Edit2 :size="18" />
          Editar Perfil
        </Button>
      </div>

      <!-- Alertas -->
      <Alert v-if="error" variant="destructive" class="alert">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <Alert v-if="success" class="alert-success">
        <CheckCircle class="h-4 w-4" />
        <AlertDescription>{{ success }}</AlertDescription>
      </Alert>

      <div class="account-grid">
        <!-- Informações Pessoais -->
        <Card class="account-card">
          <div class="card-header">
            <div class="card-icon">
              <User :size="24" />
            </div>
            <div>
              <h2 class="card-title">Informações Pessoais</h2>
              <p class="card-description">Suas informações básicas de conta</p>
            </div>
          </div>

          <div class="card-content">
            <div class="form-group">
              <Label for="name">Nome Completo</Label>
              <Input
                id="name"
                v-model="formData.name"
                :disabled="!isEditing"
                placeholder="Seu nome completo"
                class="input-field"
              />
            </div>

            <div class="form-group">
              <Label for="email">
                <Mail :size="16" />
                E-mail
              </Label>
              <Input
                id="email"
                v-model="formData.email"
                type="email"
                disabled
                placeholder="seu@email.com"
                class="input-field"
              />
              <p class="field-note">
                O e-mail não pode ser alterado por questões de segurança
              </p>
            </div>

            <div class="form-group">
              <Label for="phone">
                <Phone :size="16" />
                Telefone
              </Label>
              <Input
                id="phone"
                :value="formattedPhone"
                @input="handlePhoneInput"
                :disabled="!isEditing"
                placeholder="(00) 00000-0000"
                maxlength="15"
                class="input-field"
              />
            </div>

            <div class="form-group">
              <Label for="cpf">
                <CreditCard :size="16" />
                CPF
              </Label>
              <Input
                id="cpf"
                :value="formattedCpf"
                @input="handleCpfInput"
                :disabled="!isEditing"
                placeholder="000.000.000-00"
                maxlength="14"
                class="input-field"
              />
            </div>

            <div class="form-group">
              <Label>Tipo de Conta</Label>
              <div class="account-type-badge">
                {{ userTypeLabel }}
              </div>
            </div>
          </div>
        </Card>

        <!-- Foto de Perfil -->
        <Card class="account-card account-card-avatar">
          <div class="card-header">
            <div class="card-icon">
              <Camera :size="24" />
            </div>
            <div>
              <h2 class="card-title">Foto de Perfil</h2>
              <p class="card-description">Atualize sua foto de perfil</p>
            </div>
          </div>

          <div class="avatar-section">
            <div class="avatar-container">
              <img
                v-if="user.avatar"
                :src="user.avatar"
                :alt="user.name"
                class="avatar-image"
              />
              <div v-else class="avatar-placeholder">
                <User :size="48" />
              </div>
            </div>

            <div class="avatar-actions">
              <Button
                variant="outline"
                :disabled="!isEditing"
                class="btn-avatar"
              >
                <Camera :size="18" />
                Alterar Foto
              </Button>
              <p class="avatar-note">
                JPG, PNG ou GIF. Máximo 5MB
              </p>
            </div>
          </div>
        </Card>
      </div>

      <!-- Ações -->
      <div v-if="isEditing" class="account-actions">
        <Button
          variant="outline"
          @click="handleCancel"
          :disabled="isLoading"
          class="btn-cancel"
        >
          <X :size="18" />
          Cancelar
        </Button>
        <Button
          @click="handleSave"
          :disabled="isLoading"
          class="btn-save"
        >
          <Save :size="18" />
          {{ isLoading ? 'Salvando...' : 'Salvar Alterações' }}
        </Button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-account-page {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f9fafb 0%, #ffffff 100%);
  padding: 2rem 1.5rem;
}

.account-container {
  max-width: 1200px;
  margin: 0 auto;
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.account-title {
  font-size: 2rem;
  font-weight: 800;
  color: #111827;
  margin-bottom: 0.5rem;
}

@media (min-width: 768px) {
  .account-title {
    font-size: 2.5rem;
  }
}

.account-subtitle {
  color: #6b7280;
  font-size: 1rem;
}

.btn-edit {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  font-weight: 600;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-edit:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.alert {
  margin-bottom: 1.5rem;
}

.alert-success {
  margin-bottom: 1.5rem;
  background: #d1fae5;
  border-color: #10b981;
  color: #065f46;
}

.account-grid {
  display: grid;
  gap: 1.5rem;
  grid-template-columns: 1fr;
}

@media (min-width: 1024px) {
  .account-grid {
    grid-template-columns: 2fr 1fr;
  }
}

.account-card {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.account-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.card-icon {
  width: 3rem;
  height: 3rem;
  background: linear-gradient(135deg, #dbeafe 0%, #e9d5ff 100%);
  border-radius: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
  flex-shrink: 0;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 0.25rem;
}

.card-description {
  color: #6b7280;
  font-size: 0.875rem;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
}

.input-field {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: all 0.2s ease;
}

.input-field:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.input-field:disabled {
  background-color: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.field-note {
  font-size: 0.75rem;
  color: #9ca3af;
}

.account-type-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #dbeafe 0%, #e9d5ff 100%);
  color: #3b82f6;
  font-weight: 600;
  border-radius: 0.5rem;
  font-size: 0.875rem;
}

.account-card-avatar {
  height: fit-content;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.avatar-container {
  width: 8rem;
  height: 8rem;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid #e5e7eb;
  background: linear-gradient(135deg, #dbeafe 0%, #e9d5ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #3b82f6;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.btn-avatar {
  width: 100%;
  justify-content: center;
}

.avatar-note {
  font-size: 0.75rem;
  color: #9ca3af;
  text-align: center;
}

.account-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e5e7eb;
  flex-wrap: wrap;
}

.btn-cancel,
.btn-save {
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.btn-cancel {
  background: white;
  border: 2px solid #e5e7eb;
  color: #374151;
}

.btn-cancel:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.btn-save {
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

