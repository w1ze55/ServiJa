<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { User, Settings, Shield, FileText, Bell, HelpCircle, LogOut, ChevronRight, MessageSquare, CreditCard } from 'lucide-vue-next';
import Card from '@/components/ui/card.vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import { useNavigation } from '@/utils/navigation';

const router = useRouter();
const { navigate, logout } = useNavigation();

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
        description: 'Configure suas preferências de notificações',
        onClick: () => alert('Em desenvolvimento'),
      },
      {
        id: 'payment',
        icon: CreditCard,
        title: 'Formas de Pagamento',
        description: 'Gerencie seus cartões e métodos de pagamento',
        onClick: () => alert('Em desenvolvimento'),
      },
      {
        id: 'help',
        icon: HelpCircle,
        title: 'Ajuda e Suporte',
        description: 'Central de ajuda e contato',
        onClick: () => alert('Em desenvolvimento'),
      },
    ];
  } else {
    return [
      {
        id: 'profile',
        icon: User,
        title: 'Perfil Profissional',
        description: 'Edite sua biografia, serviços e portfólio',
        onClick: () => router.push('/freelancer-profile-settings'),
      },
      {
        id: 'kyc',
        icon: Shield,
        title: 'Verificação de Identidade',
        description: 'Gerencie seus documentos KYC',
        onClick: () => alert('Em desenvolvimento'),
      },
      {
        id: 'account',
        icon: Settings,
        title: 'Configurações de Conta',
        description: 'Senha, 2FA e exclusão de conta',
        onClick: () => router.push('/freelancer-profile-settings'),
      },
      {
        id: 'notifications',
        icon: Bell,
        title: 'Notificações',
        description: 'Configure suas preferências de notificações',
        onClick: () => alert('Em desenvolvimento'),
      },
      {
        id: 'help',
        icon: HelpCircle,
        title: 'Ajuda e Suporte',
        description: 'Central de ajuda e contato',
        onClick: () => alert('Em desenvolvimento'),
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
    <div class="max-w-4xl mx-auto space-y-6">
      <!-- Header -->
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Configurações</h1>
        <p class="text-[#435574]">Gerencie sua conta e preferências</p>
      </div>

      <!-- Settings Options -->
      <div class="space-y-3">
        <Card
          v-for="option in settingsOptions"
          :key="option.id"
          class="p-6 hover:shadow-md transition-all cursor-pointer hover:border-[#286AD8] bg-white"
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
            <ChevronRight class="w-5 h-5 text-[#435574]" />
          </div>
        </Card>

        <!-- Logout -->
        <Card
          class="p-6 hover:shadow-md transition-all cursor-pointer hover:border-red-300 bg-white"
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
  </MainLayout>
</template>
