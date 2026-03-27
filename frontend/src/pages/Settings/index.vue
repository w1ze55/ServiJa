<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { User, Settings, Shield, FileText, Bell, HelpCircle, LogOut, ChevronRight, MessageSquare, CreditCard, Construction } from 'lucide-vue-next';
import Card from '@/components/ui/card.vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import { useNavigation } from '@/utils/navigation';
import AlertDialog from '@/components/ui/alert-dialog.vue';
import AlertDialogAction from '@/components/ui/alert-dialog-action.vue';
import AlertDialogContent from '@/components/ui/alert-dialog-content.vue';
import AlertDialogDescription from '@/components/ui/alert-dialog-description.vue';
import AlertDialogFooter from '@/components/ui/alert-dialog-footer.vue';
import AlertDialogHeader from '@/components/ui/alert-dialog-header.vue';
import AlertDialogTitle from '@/components/ui/alert-dialog-title.vue';

const router = useRouter();
const { navigate, logout } = useNavigation();

const showDevelopmentModal = ref(false);
const developmentFeatureTitle = ref('');

const openDevelopmentModal = (title: string) => {
  developmentFeatureTitle.value = title;
  showDevelopmentModal.value = true;
};

// Obter usuário do localStorage
const userString = localStorage.getItem('user');
const user = userString ? JSON.parse(userString) : null;

interface SettingsOption {
  id: string;
  icon: any;
  title: string;
  description: string;
  onClick: () => void;
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
      {
        id: 'payment',
        icon: CreditCard,
        title: 'Formas de Pagamento',
        description: 'Gerencie seus cartões e métodos de pagamento',
        onClick: () => openDevelopmentModal('Formas de Pagamento'),
      },
      {
        id: 'help',
        icon: HelpCircle,
        title: 'Ajuda e Suporte',
        description: 'Central de ajuda e contato',
        onClick: () => openDevelopmentModal('Ajuda e Suporte'),
      },
    ];
  } else {
    return [
      {
        id: 'profile',
        icon: User,
        title: 'Configurações de Conta',
        description: 'Gerencie suas informações pessoais',
        onClick: () => router.push('/freelancer-profile-settings'),
      },
      {
        id: 'kyc',
        icon: Shield,
        title: 'Verificação de Identidade',
        description: 'Gerencie seus documentos KYC',
        onClick: () => openDevelopmentModal('Verificação de Identidade'),
      },
      {
        id: 'notifications',
        icon: Bell,
        title: 'Notificações',
        description: 'Visualize suas notificações',
        onClick: () => router.push('/notifications'),
      },
      {
        id: 'help',
        icon: HelpCircle,
        title: 'Ajuda e Suporte',
        description: 'Central de ajuda e contato',
        onClick: () => openDevelopmentModal('Ajuda e Suporte'),
      },
    ];
  }
});

// Navegação
const handleNavigate = (page: string) => {
  navigate(page, user?.type);
};

// Logout
const handleLogout = () => {
  logout();
};
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="settings"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
  >
    <div class="settings-container max-w-4xl mx-auto space-y-6">
      <!-- Header -->
      <div class="settings-header">
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Configurações</h1>
        <p class="text-[#435574]">Gerencie sua conta e preferências</p>
      </div>

      <!-- Settings Options -->
      <div class="space-y-3">
        <Card
          v-for="option in settingsOptions"
          :key="option.id"
          class="settings-card p-6 hover:shadow-md transition-all cursor-pointer hover:border-[#286AD8] bg-white"
          @click="option.onClick"
        >
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <component :is="option.icon" class="h-6 w-6 text-[#286AD8]" />
            </div>
            <div class="flex-1">
              <h3 class="font-semibold text-[#263238] mb-1">{{ option.title }}</h3>
              <p class="text-sm text-[#435574]">{{ option.description }}</p>
            </div>
            <ChevronRight class="chevron-icon w-5 h-5 text-[#435574]" />
          </div>
        </Card>

        <!-- Logout -->
        <Card
          class="settings-card p-6 hover:shadow-md transition-all cursor-pointer hover:border-red-300 bg-white"
          @click="handleLogout"
        >
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
              <LogOut class="h-6 w-6 text-red-600" />
            </div>
            <div class="flex-1">
              <h3 class="font-semibold text-red-600 mb-1">Sair da Conta</h3>
              <p class="text-sm text-[#435574]">Encerrar sua sessão atual</p>
            </div>
          </div>
        </Card>
      </div>
    </div>

    <!-- Modal de Funcionalidade em Desenvolvimento -->
    <AlertDialog :open="showDevelopmentModal" @update:open="(val) => showDevelopmentModal = val">
      <AlertDialogContent class="max-w-md">
        <AlertDialogHeader>
          <div class="flex justify-center mb-4">
            <div class="w-16 h-16 rounded-full bg-amber-100 flex items-center justify-center">
              <Construction class="w-8 h-8 text-amber-600" />
            </div>
          </div>
          <AlertDialogTitle class="text-center text-xl">
            {{ developmentFeatureTitle }}
          </AlertDialogTitle>
          <AlertDialogDescription class="text-center">
            Esta funcionalidade ainda está em desenvolvimento e estará disponível em breve.
            Agradecemos sua paciência!
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter class="sm:justify-center">
          <AlertDialogAction @click="showDevelopmentModal = false" class="bg-[#286AD8] hover:bg-[#1e5bb8]">
            Entendi
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
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
