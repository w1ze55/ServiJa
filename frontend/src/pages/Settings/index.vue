<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ChevronRight, Construction, CreditCard, HelpCircle, LogOut, User } from 'lucide-vue-next'
import MainLayout from '@/components/layout/MainLayout.vue'
import TwoFactorSection from '@/components/security/TwoFactorSection.vue'
import SupportRequestModal from '@/components/support/SupportRequestModal.vue'
import AlertDialog from '@/components/ui/alert-dialog.vue'
import AlertDialogAction from '@/components/ui/alert-dialog-action.vue'
import AlertDialogContent from '@/components/ui/alert-dialog-content.vue'
import AlertDialogDescription from '@/components/ui/alert-dialog-description.vue'
import AlertDialogFooter from '@/components/ui/alert-dialog-footer.vue'
import AlertDialogHeader from '@/components/ui/alert-dialog-header.vue'
import AlertDialogTitle from '@/components/ui/alert-dialog-title.vue'
import Card from '@/components/ui/card.vue'
import { useNavigation } from '@/utils/navigation'

const router = useRouter()
const { navigate, logout } = useNavigation()

const showDevelopmentModal = ref(false)
const developmentFeatureTitle = ref('')
const showSupportModal = ref(false)

const openDevelopmentModal = (title: string) => {
  developmentFeatureTitle.value = title
  showDevelopmentModal.value = true
}

const openSupportModal = () => {
  showSupportModal.value = true
}

const userString = localStorage.getItem('user')
const user = userString ? JSON.parse(userString) : null

interface SettingsOption {
  id: string
  icon: any
  title: string
  description: string
  onClick: () => void
}

const settingsOptions = computed<SettingsOption[]>(() => {
  if (user?.type === 'contractor') {
    return [
      {
        id: 'profile',
        icon: User,
        title: 'Perfil',
        description: 'Gerencie suas informações pessoais',
        onClick: () => router.push('/cliente-profile-settings'),
      },
      {
        id: 'notifications',
        icon: Bell,
        title: 'Notificações',
        description: 'Visualize suas notificações',
        onClick: () => router.push('/notifications'),
      },
    ]
  }

  return [
    {
      id: 'profile',
      icon: User,
      title: 'Configurações de Conta',
      description: 'Gerencie suas informações pessoais',
      onClick: () => router.push('/freelancer-profile-settings'),
    },
    {
      id: 'notifications',
      icon: Bell,
      title: 'Notificações',
      description: 'Visualize suas notificações',
      onClick: () => router.push('/notifications'),
    },
  ]
})

const handleNavigate = (page: string) => {
  navigate(page, user?.type)
}

const handleLogout = () => {
  logout()
}
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="settings"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
  >
    <div class="settings-container mx-auto max-w-4xl space-y-6">
      <div class="settings-header">
        <h1 class="mb-2 text-2xl font-bold text-[#263238]">Configurações</h1>
        <p class="text-[#435574]">Gerencie sua conta e preferências</p>
      </div>

      <div class="space-y-3">
        <Card
          v-for="option in settingsOptions"
          :key="option.id"
          class="settings-card cursor-pointer bg-white p-6 transition-all hover:border-[#286AD8] hover:shadow-md"
          @click="option.onClick"
        >
          <div class="flex items-center gap-4">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-blue-100">
              <component :is="option.icon" class="h-6 w-6 text-[#286AD8]" />
            </div>
            <div class="flex-1">
              <h3 class="mb-1 font-semibold text-[#263238]">{{ option.title }}</h3>
              <p class="text-sm text-[#435574]">{{ option.description }}</p>
            </div>
            <ChevronRight class="chevron-icon h-5 w-5 text-[#435574]" />
          </div>
        </Card>

        <TwoFactorSection interactive class="settings-card" />

        <Card
          class="settings-card cursor-pointer bg-white p-6 transition-all hover:border-[#286AD8] hover:shadow-md"
          @click="openSupportModal"
        >
          <div class="flex items-center gap-4">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-blue-100">
              <HelpCircle class="h-6 w-6 text-[#286AD8]" />
            </div>
            <div class="flex-1">
              <h3 class="mb-1 font-semibold text-[#263238]">Ajuda e Suporte</h3>
              <p class="text-sm text-[#435574]">Central de ajuda e contato</p>
            </div>
            <ChevronRight class="chevron-icon h-5 w-5 text-[#435574]" />
          </div>
        </Card>

        <Card
          class="settings-card cursor-pointer bg-white p-6 transition-all hover:border-red-300 hover:shadow-md"
          @click="handleLogout"
        >
          <div class="flex items-center gap-4">
            <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-red-100">
              <LogOut class="h-6 w-6 text-red-600" />
            </div>
            <div class="flex-1">
              <h3 class="mb-1 font-semibold text-red-600">Sair da Conta</h3>
              <p class="text-sm text-[#435574]">Encerrar sua sessão atual</p>
            </div>
          </div>
        </Card>
      </div>
    </div>

    <AlertDialog :open="showDevelopmentModal" @update:open="(val) => (showDevelopmentModal = val)">
      <AlertDialogContent class="max-w-md">
        <AlertDialogHeader>
          <div class="mb-4 flex justify-center">
            <div class="flex h-16 w-16 items-center justify-center rounded-full bg-amber-100">
              <Construction class="h-8 w-8 text-amber-600" />
            </div>
          </div>
          <AlertDialogTitle class="text-center text-xl">
            {{ developmentFeatureTitle }}
          </AlertDialogTitle>
          <AlertDialogDescription class="text-center">
            Esta funcionalidade ainda esta em desenvolvimento e estara disponivel em breve.
            Agradecemos sua paciencia!
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter class="sm:justify-center">
          <AlertDialogAction @click="showDevelopmentModal = false" class="bg-primary hover:bg-primary/90">
            Entendi
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <SupportRequestModal
      :open="showSupportModal"
      :on-close="() => { showSupportModal = false }"
    />
  </MainLayout>
</template>

<style scoped>
@media (max-width: 768px) {
  .settings-container {
    padding: 1rem;
    max-width: 100%;
  }

  .settings-header h1 {
    font-size: 1.5rem !important;
  }

  .settings-header p {
    font-size: 0.875rem !important;
  }

  .settings-card {
    padding: 1rem !important;
  }

  .settings-card .flex {
    gap: 0.75rem !important;
  }

  .settings-card h3 {
    font-size: 0.9375rem !important;
  }

  .settings-card p {
    font-size: 0.8125rem !important;
  }

  .settings-card .w-12 {
    width: 2.5rem !important;
    height: 2.5rem !important;
  }

  .settings-card .w-12 svg {
    width: 1.25rem !important;
    height: 1.25rem !important;
  }

  .chevron-icon {
    width: 1.125rem !important;
    height: 1.125rem !important;
  }
}

@media (max-width: 480px) {
  .settings-container {
    padding: 0.75rem;
  }

  .settings-header h1 {
    font-size: 1.25rem !important;
    margin-bottom: 0.5rem !important;
  }

  .settings-header p {
    font-size: 0.8125rem !important;
  }

  .settings-card {
    padding: 0.875rem !important;
  }

  .settings-card .flex {
    gap: 0.625rem !important;
  }

  .settings-card h3 {
    font-size: 0.875rem !important;
    margin-bottom: 0.25rem !important;
  }

  .settings-card p {
    font-size: 0.75rem !important;
    line-height: 1rem !important;
  }

  .settings-card .w-12 {
    width: 2.25rem !important;
    height: 2.25rem !important;
  }

  .settings-card .w-12 svg {
    width: 1.125rem !important;
    height: 1.125rem !important;
  }

  .chevron-icon {
    width: 1rem !important;
    height: 1rem !important;
  }

  .space-y-3 {
    gap: 0.625rem !important;
  }
}
</style>
