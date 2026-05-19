<template>
  <div :class="props.inline ? '' : 'flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 p-4'">
    <div :class="props.inline ? '' : 'w-full max-w-5xl rounded-3xl bg-white p-4 shadow-xl sm:p-6 lg:p-8'">
      <Button
        v-if="!props.inline"
        variant="ghost"
        @click="props.onBack"
        class="mb-4 sm:mb-6"
        :disabled="isLoading"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar
      </Button>

      <div v-if="!props.inline" class="mb-6 sm:mb-8">
        <div class="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <h2 class="text-gray-900">Criar conta</h2>
          <span class="text-sm text-gray-500">Passo 3 de 3</span>
        </div>
        <Progress :value="100" class="h-2" />
      </div>

      <div class="mb-8 grid gap-6 [grid-template-columns:repeat(auto-fit,minmax(320px,1fr))]">
        <section class="space-y-4">
          <div class="flex items-center justify-between gap-3">
            <h4 class="text-base font-semibold text-gray-900 sm:text-lg">Informações da conta</h4>
            <span class="hidden rounded-full bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600 sm:inline-flex">
              {{ summaryItems.length }} itens revisados
            </span>
          </div>

          <div class="grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(220px,1fr))]">
            <article
              v-for="item in summaryItems"
              :key="item.label"
              class="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm transition-colors hover:border-blue-200 hover:bg-blue-50/40"
              :class="item.layoutClass"
            >
              <div class="flex items-start gap-3">
                <div class="rounded-xl p-2.5" :class="item.accentClass">
                  <component :is="item.icon" class="h-5 w-5" />
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm text-gray-500">{{ item.label }}</p>
                  <p class="mt-1 break-words text-base font-medium text-gray-900">{{ item.value }}</p>
                </div>
              </div>
            </article>
          </div>
        </section>

        <aside class="space-y-4">
          <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm">
            <h4 class="text-base font-semibold text-gray-900 sm:text-lg">Próximos passos</h4>
            <p class="mt-2 text-sm leading-6 text-gray-600">
              Assim que a conta for criada, você conclui a verificação e libera o acesso completo.
            </p>

            <div class="mt-5 space-y-4">
              <div
                v-for="(step, index) in nextSteps"
                :key="step.title"
                class="flex gap-3"
              >
                <div class="mt-0.5 flex h-7 w-7 flex-none items-center justify-center rounded-full bg-blue-100 text-blue-600">
                  <Check class="h-4 w-4" />
                </div>
                <div class="min-w-0">
                  <span class="text-xs font-semibold uppercase tracking-[0.18em] text-blue-700">
                    Etapa {{ index + 1 }}
                  </span>
                  <p class="mt-1 text-sm font-medium text-gray-900">{{ step.title }}</p>
                  <p class="mt-1 text-sm leading-6 text-gray-600">{{ step.description }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="rounded-2xl border border-blue-200 bg-blue-50 p-5">
            <div class="flex items-start gap-3">
              <div class="rounded-xl bg-white p-2 text-blue-600 shadow-sm">
                <ShieldCheck class="h-5 w-5" />
              </div>
              <div>
                <h5 class="text-sm font-semibold uppercase tracking-[0.18em] text-blue-700">Proteção de dados</h5>
                <p class="mt-2 text-sm leading-6 text-gray-700">
                  Seus dados ficam protegidos de acordo com a LGPD, com possibilidade de atualização ou exclusão sempre que necessário.
                </p>
              </div>
            </div>
          </div>
        </aside>
      </div>

      <Alert v-if="error" variant="destructive" class="mb-6">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <div class="border-t border-gray-200 pt-5 sm:pt-6">
        <div class="w-full">
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import Button from '../ui/button.vue'

import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import Progress from '../ui/progress.vue'
import api from '@/api/config/axios'
import {
  AlertCircle,
  ArrowLeft,
  Calendar,
  Check,
  CreditCard,
  Mail,
  Phone,
  ShieldCheck,
  User,
} from 'lucide-vue-next'

interface PersonalData {
  name: string
  email: string
  phone: string
  birthDate: string
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

const accountTypeLabel = computed(() =>
  props.accountType === 'contractor' ? 'Conta de contratante' : 'Conta de freelancer'
)

const summaryItems = computed(() => [
  {
    label: 'Tipo de conta',
    value: props.accountType === 'contractor' ? 'Contratante' : 'Freelancer',
    icon: User,
    accentClass: 'bg-blue-100 text-blue-600',
    layoutClass: 'col-span-full',
  },
  {
    label: 'Nome completo',
    value: props.personalData.name,
    icon: User,
    accentClass: 'bg-blue-100 text-blue-600',
    layoutClass: 'col-span-full',
  },
  {
    label: 'E-mail',
    value: props.personalData.email,
    icon: Mail,
    accentClass: 'bg-blue-100 text-blue-600',
    layoutClass: 'col-span-full',
  },
  {
    label: 'Telefone',
    value: props.personalData.phone,
    icon: Phone,
    accentClass: 'bg-blue-100 text-blue-600',
    layoutClass: '',
  },
  {
    label: 'Data de nascimento',
    value: formatBirthDate(props.personalData.birthDate),
    icon: Calendar,
    accentClass: 'bg-blue-100 text-blue-600',
    layoutClass: '',
  },
  {
    label: 'CPF',
    value: maskCPF(props.personalData.cpf),
    icon: CreditCard,
    accentClass: 'bg-blue-100 text-blue-600',
    layoutClass: '',
  },
])

const nextSteps = computed(() => [
  {
    title: 'Verificação de e-mail',
    description: `Enviaremos um código para ${maskEmail(props.personalData.email)}.`,
  },
  {
    title: 'Complete seu perfil',
    description:
      props.accountType === 'contractor'
        ? 'Adicione mais informações para começar a solicitar orçamentos.'
        : 'Configure seus serviços e portfólio para receber propostas.',
  },
  {
    title: 'Comece a usar',
    description:
      props.accountType === 'contractor'
        ? 'Busque profissionais e solicite orçamentos.'
        : 'Receba solicitações e construa sua reputação.',
  },
])

const handleConfirm = async () => {
  error.value = ''
  isLoading.value = true

  try {
    const userType = props.accountType === 'provider' ? 'FREELANCER' : 'CONTRACTOR'

    const cpfClean = props.personalData.cpf.replace(/\D/g, '')
    const phoneClean = props.personalData.phone.replace(/\D/g, '')

    await api.post('/auth/register', {
      name: props.personalData.name,
      email: props.personalData.email,
      cpf: cpfClean,
      phone: phoneClean,
      birthDate: props.personalData.birthDate,
      password: props.personalData.password,
      userType,
      acceptTerms: true,
    })

    props.onConfirm()
  } catch (err: any) {
    const errorMessage =
      err.response?.data?.message ||
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

const formatBirthDate = (birthDate: string) => {
  if (!birthDate) {
    return '-'
  }

  const [year, month, day] = birthDate.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))

  return new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(date)
}
</script>
