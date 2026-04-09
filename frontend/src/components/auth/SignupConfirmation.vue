<template>
  <div :class="props.inline ? '' : 'min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4'">
    <div :class="props.inline ? '' : 'w-full max-w-4xl bg-white rounded-lg shadow-xl p-8'">
      <Button
        v-if="!props.inline"
        variant="ghost"
        @click="onBack"
        class="mb-6"
        :disabled="isLoading"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar
      </Button>

      <div v-if="!props.inline" class="mb-8">
        <div class="flex items-center justify-between mb-2">
          <h2 class="text-gray-900">Criar conta</h2>
          <span class="text-sm text-gray-500">Passo 3 de 3</span>
        </div>
        <Progress :value="100" class="h-2" />
      </div>

      <div v-if="!props.inline" class="mb-8">
        <h3 class="text-gray-900 mb-2">Confirme seus dados</h3>
        <p class="text-gray-600">Revise as informações antes de criar sua conta</p>
      </div>

      <div class="grid lg:grid-cols-2 gap-8 mb-8">
        <!-- COLUNA 1 -->
        <div class="space-y-6">
          <div>
            <h4 class="text-gray-900 mb-4">Informações da conta</h4>

            <div class="space-y-4">

              <div class="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <div class="p-2 bg-blue-100 rounded-lg">
                  <User class="h-5 w-5 text-blue-600" />
                </div>
                <div class="flex-1">
                  <p class="text-sm text-gray-500">Tipo de conta</p>
                  <p class="text-gray-900">
                    {{ accountType === 'contractor' ? 'Contratante' : 'Freelancer' }}
                  </p>
                </div>
              </div>

              <div class="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <div class="p-2 bg-blue-100 rounded-lg">
                  <User class="h-5 w-5 text-blue-600" />
                </div>
                <div class="flex-1">
                  <p class="text-sm text-gray-500">Nome completo</p>
                  <p class="text-gray-900">{{ personalData.name }}</p>
                </div>
              </div>

              <div class="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <div class="p-2 bg-blue-100 rounded-lg">
                  <Mail class="h-5 w-5 text-blue-600" />
                </div>
                <div class="flex-1">
                  <p class="text-sm text-gray-500">E-mail</p>
                  <p class="text-gray-900">{{ personalData.email }}</p>
                </div>
              </div>

              <div class="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <div class="p-2 bg-blue-100 rounded-lg">
                  <Phone class="h-5 w-5 text-blue-600" />
                </div>
                <div class="flex-1">
                  <p class="text-sm text-gray-500">Telefone</p>
                  <p class="text-gray-900">{{ personalData.phone }}</p>
                </div>
              </div>

              <div class="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <div class="p-2 bg-blue-100 rounded-lg">
                  <CreditCard class="h-5 w-5 text-blue-600" />
                </div>
                <div class="flex-1">
                  <p class="text-sm text-gray-500">CPF</p>
                  <p class="text-gray-900">{{ maskCPF(personalData.cpf) }}</p>
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- COLUNA 2 -->
        <div class="space-y-6">
          <div>
            <h4 class="text-gray-900 mb-4">Próximos passos</h4>

            <div class="space-y-3">
              <div class="flex items-start gap-3">
                <div class="p-1 bg-blue-100 rounded-full mt-0.5">
                  <Check class="h-3 w-3 text-blue-600" />
                </div>
                <div>
                  <p class="text-gray-900">Verificação de e-mail</p>
                  <p class="text-sm text-gray-600">
                    Enviaremos um código para {{ maskEmail(personalData.email) }}
                  </p>
                </div>
              </div>

              <div class="flex items-start gap-3">
                <div class="p-1 bg-blue-100 rounded-full mt-0.5">
                  <Check class="h-3 w-3 text-blue-600" />
                </div>
                <div>
                  <p class="text-gray-900">Complete seu perfil</p>
                  <p class="text-sm text-gray-600">
                    {{
                      accountType === 'contractor'
                        ? 'Adicione mais informações para começar a solicitar orçamentos'
                        : 'Configure seus serviços e portfólio para receber propostas'
                    }}
                  </p>
                </div>
              </div>

              <div class="flex items-start gap-3">
                <div class="p-1 bg-blue-100 rounded-full mt-0.5">
                  <Check class="h-3 w-3 text-blue-600" />
                </div>
                <div>
                  <p class="text-gray-900">Comece a usar</p>
                  <p class="text-sm text-gray-600">
                    {{
                      accountType === 'contractor'
                        ? 'Busque profissionais e solicite orçamentos'
                        : 'Receba solicitações e construa sua reputação'
                    }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div class="p-4 bg-blue-50 rounded-lg border border-blue-200">
            <h5 class="text-gray-900 mb-2">Proteção de dados</h5>
            <p class="text-sm text-gray-700">
              Seus dados estão protegidos de acordo com a LGPD. Você pode atualizar ou excluir suas informações a qualquer momento.
            </p>
          </div>
        </div>
      </div>

      <!-- ERRO -->
      <Alert v-if="error" variant="destructive" class="mb-6">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <!-- ACEITE DOS TERMOS -->
      <div class="border-t pt-6">
        <Button
          class="w-full"
          size="lg"
          :disabled="isLoading"
          @click="handleConfirm"
        >
          {{ isLoading ? 'Criando conta...' : 'Criar conta' }}
        </Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Button from '../ui/button.vue'

import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import Progress from '../ui/progress.vue'
import api from '@/api/config/axios'
import {
  AlertCircle,
  ArrowLeft,
  Check,
  Mail,
  Phone,
  User,
  CreditCard,
} from 'lucide-vue-next'

interface PersonalData {
  name: string
  email: string
  phone: string
  cpf: string
  password: string
}

interface Props {
  accountType: 'contractor' | 'provider'
  personalData: PersonalData
  onConfirm: () => void
  onBack: () => void
  inline?: boolean
}

const props = defineProps<Props>()
const isLoading = ref(false)
const error = ref('')

const handleConfirm = async () => {
  error.value = ''
  isLoading.value = true

  try {
    // Mapear tipo de conta para o formato da API
    const userType = props.accountType === 'provider' ? 'FREELANCER' : 'CONTRACTOR'
    
    // Remover formatação do CPF e telefone
    const cpfClean = props.personalData.cpf.replace(/\D/g, '')
    const phoneClean = props.personalData.phone.replace(/\D/g, '')
    
    await api.post('/auth/register', {
      name: props.personalData.name,
      email: props.personalData.email,
      cpf: cpfClean,
      phone: phoneClean,
      password: props.personalData.password,
      userType: userType,
      acceptTerms: true
    })
    
    // Registro bem-sucedido, ir para verificação de email
    props.onConfirm()
  } catch (err: any) {
    const errorMessage = err.response?.data?.message || 
                         err.response?.data?.error ||
                         'Erro ao criar conta. Tente novamente.'
    error.value = errorMessage
  } finally {
    isLoading.value = false
  }
}

const maskCPF = (cpf: string) => {
  const numbers = cpf.replace(/\D/g, '')
  return `***.***.${numbers.slice(6, 9)}-**`
}

const maskEmail = (email: string) => {
  const [username, domain] = email.split('@')
  if (username.length <= 3) return email
  return `${username.slice(0, 3)}***@${domain}`
}

const { personalData, accountType, onBack } = props
</script>
