<script setup lang="ts">
import { computed } from 'vue';
import { User, Settings, Shield, FileText, Bell, HelpCircle, LogOut, ChevronRight } from 'lucide-vue-next';
import Card from '../ui/card.vue';

interface Props {
  userType: 'contractor' | 'provider';
  onNavigate: (page: string, params?: any) => void;
  onLogout: () => void;
}

const props = defineProps<Props>();

interface SettingsOption {
  id: string;
  icon: any;
  title: string;
  description: string;
  onClick: () => void;
}

const settingsOptions = computed<SettingsOption[]>(() => {
  if (props.userType === 'contractor') {
    return [
      {
        id: 'profile',
        icon: User,
        title: 'Perfil',
        description: 'Gerencie suas informações pessoais',
        onClick: () => props.onNavigate('profile'),
      },
      {
        id: 'notifications',
        icon: Bell,
        title: 'Notificações',
        description: 'Configure suas preferências de notificações',
        onClick: () => props.onNavigate('notifications'),
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
        onClick: () => props.onNavigate('freelancer-profile-settings'),
      },
      {
        id: 'kyc',
        icon: Shield,
        title: 'Verificação de Identidade',
        description: 'Gerencie seus documentos KYC',
        onClick: () => props.onNavigate('freelancer-kyc'),
      },
      {
        id: 'account',
        icon: Settings,
        title: 'Configurações de Conta',
        description: 'Senha, 2FA e exclusão de conta',
        onClick: () => props.onNavigate('freelancer-account-settings'),
      },
      {
        id: 'notifications',
        icon: Bell,
        title: 'Notificações',
        description: 'Configure suas preferências de notificações',
        onClick: () => props.onNavigate('notifications'),
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
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Configurações</h1>
      <p class="text-[#435574]">Gerencie sua conta e preferências</p>
    </div>

    <!-- Settings Options -->
    <div class="space-y-3">
      <Card
        v-for="option in settingsOptions"
        :key="option.id"
        class="p-6 hover:shadow-md transition-all cursor-pointer hover:border-[#286AD8]"
        @click="option.onClick"
      >
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
            <component :is="option.icon" class="h-6 w-6 text-[#286AD8]" />
          </div>
          <div class="flex-1">
            <h3 class="text-[#263238] mb-1">{{ option.title }}</h3>
            <p class="text-sm text-[#435574]">{{ option.description }}</p>
          </div>
          <ChevronRight class="w-5 h-5 text-[#435574]" />
        </div>
      </Card>

      <!-- Logout -->
      <Card
        class="p-6 hover:shadow-md transition-all cursor-pointer hover:border-red-300"
        @click="props.onLogout"
      >
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
            <LogOut class="h-6 w-6 text-red-600" />
          </div>
          <div class="flex-1">
            <h3 class="text-red-600 mb-1">Sair da Conta</h3>
            <p class="text-sm text-[#435574]">Encerrar sua sessão atual</p>
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>




