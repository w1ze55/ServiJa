<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Bell,
  DollarSign,
  MessageSquare,
  AlertCircle,
  CheckCircle,
  X,
  Trash2,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Card from '../ui/card.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';

interface Notification {
  id: string;
  type: 'financial' | 'message' | 'system';
  title: string;
  message: string;
  timestamp: string;
  isRead: boolean;
  actionLink?: {
    page: string;
    params?: any;
  };
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const showClearDialog = ref(false);
const activeTab = ref('all');

// Mock data - substituir por dados reais da API
const notifications = ref<Notification[]>([
  {
    id: '1',
    type: 'financial',
    title: 'Pagamento Aprovado',
    message: 'Seu pagamento de R$ 3.200,00 para João Silva foi aprovado com sucesso.',
    timestamp: '2025-11-11T10:30:00',
    isRead: false,
    actionLink: {
      page: 'service-timeline',
      params: { requestId: '1' },
    },
  },
  {
    id: '2',
    type: 'message',
    title: 'Nova Mensagem',
    message: 'Maria Santos enviou uma mensagem sobre o projeto de design.',
    timestamp: '2025-11-11T09:15:00',
    isRead: false,
    actionLink: {
      page: 'chat',
      params: { freelancerId: '2' },
    },
  },
  {
    id: '3',
    type: 'system',
    title: 'Proposta Recebida',
    message: 'Pedro Oliveira enviou uma proposta de R$ 450,00 para seu pedido de encanamento.',
    timestamp: '2025-11-10T16:45:00',
    isRead: false,
    actionLink: {
      page: 'quote-detail',
      params: { requestId: '3' },
    },
  },
  {
    id: '4',
    type: 'system',
    title: 'Serviço Concluído',
    message: 'João Silva marcou o serviço de instalação elétrica como concluído. Aprove a entrega.',
    timestamp: '2025-11-10T14:20:00',
    isRead: true,
    actionLink: {
      page: 'delivery-approval',
      params: { requestId: '1' },
    },
  },
  {
    id: '5',
    type: 'financial',
    title: 'Recibo Disponível',
    message: 'O recibo da contratação #1234 está disponível para download.',
    timestamp: '2025-11-09T11:00:00',
    isRead: true,
    actionLink: {
      page: 'contracts',
    },
  },
  {
    id: '6',
    type: 'message',
    title: 'Nova Mensagem',
    message: 'Ana Costa respondeu sua dúvida sobre o desenvolvimento do website.',
    timestamp: '2025-11-09T08:30:00',
    isRead: true,
    actionLink: {
      page: 'chat',
      params: { freelancerId: '4' },
    },
  },
  {
    id: '7',
    type: 'system',
    title: 'Avaliação Pendente',
    message: 'Não se esqueça de avaliar o trabalho de Carlos Mendes.',
    timestamp: '2025-11-08T17:00:00',
    isRead: true,
    actionLink: {
      page: 'rate-freelancer',
      params: { requestId: '5' },
    },
  },
]);

const getNotificationIcon = (type: Notification['type']) => {
  switch (type) {
    case 'financial':
      return DollarSign;
    case 'message':
      return MessageSquare;
    case 'system':
      return Bell;
  }
};

const getNotificationColor = (type: Notification['type']) => {
  switch (type) {
    case 'financial':
      return 'bg-green-100';
    case 'message':
      return 'bg-blue-100';
    case 'system':
      return 'bg-orange-100';
  }
};

const getNotificationLabel = (type: Notification['type']) => {
  switch (type) {
    case 'financial':
      return 'Financeiro';
    case 'message':
      return 'Mensagem';
    case 'system':
      return 'Sistema';
  }
};

const formatTimestamp = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInMs = now.getTime() - date.getTime();
  const diffInMinutes = Math.floor(diffInMs / 60000);
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);

  if (diffInMinutes < 1) return 'Agora';
  if (diffInMinutes < 60) return `há ${diffInMinutes} min`;
  if (diffInHours < 24) return `há ${diffInHours}h`;
  if (diffInDays === 1) return 'Ontem';
  if (diffInDays < 7) return `há ${diffInDays} dias`;

  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' });
};

const filteredNotifications = computed(() =>
  notifications.value.filter((notification) => {
    if (activeTab.value === 'all') return true;
    if (activeTab.value === 'unread') return !notification.isRead;
    return notification.type === activeTab.value;
  })
);

const unreadCount = computed(() => notifications.value.filter((n) => !n.isRead).length);

const handleMarkAsRead = (notificationId: string) => {
  notifications.value = notifications.value.map((n) =>
    n.id === notificationId ? { ...n, isRead: true } : n
  );
};

const handleMarkAllAsRead = () => {
  notifications.value = notifications.value.map((n) => ({ ...n, isRead: true }));
};

const handleClearAll = () => {
  notifications.value = notifications.value.filter((n) => !n.isRead);
  showClearDialog.value = false;
};

const handleNotificationClick = (notification: Notification) => {
  if (!notification.isRead) {
    handleMarkAsRead(notification.id);
  }
  if (notification.actionLink) {
    props.onNavigate(notification.actionLink.page, notification.actionLink.params);
  }
};

const handleDeleteNotification = (notificationId: string, e: MouseEvent) => {
  e.stopPropagation();
  notifications.value = notifications.value.filter((n) => n.id !== notificationId);
};
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-[#263238] mb-2">Notificações</h1>
        <p class="text-[#435574]">Acompanhe atualizações sobre seus serviços e mensagens</p>
      </div>

      <div v-if="notifications.length > 0" class="flex gap-2">
        <Button v-if="unreadCount > 0" @click="handleMarkAllAsRead" variant="outline" size="sm">
          <CheckCircle class="h-4 w-4 mr-2" />
          Marcar Todas como Lidas
        </Button>
        <Button
          @click="showClearDialog = true"
          variant="outline"
          size="sm"
          class="text-red-600 hover:text-red-700 hover:bg-red-50"
        >
          <Trash2 class="h-4 w-4 mr-2" />
          Limpar Lidas
        </Button>
      </div>
    </div>

    <!-- Stats -->
    <Card v-if="unreadCount > 0" class="p-4 bg-blue-50 border-blue-200">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-full bg-[#286AD8] flex items-center justify-center">
          <Bell class="h-5 w-5 text-white" />
        </div>
        <div>
          <p class="text-[#263238]">
            Você tem <strong>{{ unreadCount }}</strong>
            {{ unreadCount !== 1 ? 'notificações' : 'notificação' }} não
            {{ unreadCount !== 1 ? 'lidas' : 'lida' }}
          </p>
        </div>
      </div>
    </Card>

    <!-- Tabs -->
    <Tabs :model-value="activeTab" @update:model-value="(val) => activeTab = val as string">
      <TabsList class="grid w-full grid-cols-5">
        <TabsTrigger value="all">
          Todas
          <Badge v-if="notifications.length > 0" variant="secondary" class="ml-2">
            {{ notifications.length }}
          </Badge>
        </TabsTrigger>
        <TabsTrigger value="unread">
          Não Lidas
          <Badge v-if="unreadCount > 0" class="ml-2 bg-[#286AD8] hover:bg-[#286AD8]">
            {{ unreadCount }}
          </Badge>
        </TabsTrigger>
        <TabsTrigger value="financial">Financeiro</TabsTrigger>
        <TabsTrigger value="message">Mensagens</TabsTrigger>
        <TabsTrigger value="system">Sistema</TabsTrigger>
      </TabsList>

      <TabsContent :value="activeTab" class="space-y-3 mt-6">
        <Card v-if="filteredNotifications.length === 0" class="p-12 text-center">
          <div class="flex flex-col items-center gap-4">
            <div class="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center">
              <Bell class="h-8 w-8 text-gray-400" />
            </div>
            <div>
              <h3 class="text-[#263238] mb-2">Nenhuma notificação</h3>
              <p class="text-[#435574]">
                {{
                  activeTab === 'unread'
                    ? 'Você está em dia! Não há notificações não lidas.'
                    : 'Quando houver atualizações, você verá aqui.'
                }}
              </p>
            </div>
          </div>
        </Card>
        <Card
          v-for="notification in filteredNotifications"
          :key="notification.id"
          @click="handleNotificationClick(notification)"
          :class="`p-4 cursor-pointer hover:shadow-md transition-all ${
            !notification.isRead ? 'border-l-4 border-l-[#286AD8] bg-blue-50/30' : ''
          }`"
        >
          <div class="flex items-start gap-4">
            <!-- Icon -->
            <div
              :class="`w-10 h-10 rounded-full ${getNotificationColor(notification.type)} flex items-center justify-center flex-shrink-0`"
            >
              <component
                :is="getNotificationIcon(notification.type)"
                :class="`h-5 w-5 ${
                  notification.type === 'financial'
                    ? 'text-green-600'
                    : notification.type === 'message'
                      ? 'text-blue-600'
                      : 'text-orange-600'
                }`"
              />
            </div>

            <!-- Content -->
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between mb-1">
                <div class="flex items-center gap-2 flex-1">
                  <h3
                    :class="`${
                      !notification.isRead ? 'text-[#263238]' : 'text-[#435574]'
                    } truncate`"
                  >
                    {{ notification.title }}
                  </h3>
                  <Badge variant="secondary" class="text-xs">
                    {{ getNotificationLabel(notification.type) }}
                  </Badge>
                </div>
                <div class="flex items-center gap-2 ml-2">
                  <span class="text-xs text-[#435574] whitespace-nowrap">
                    {{ formatTimestamp(notification.timestamp) }}
                  </span>
                  <Button
                    @click.stop="handleDeleteNotification(notification.id, $event)"
                    variant="ghost"
                    size="icon"
                    class="h-8 w-8 text-gray-400 hover:text-red-600 hover:bg-red-50"
                  >
                    <X class="h-4 w-4" />
                  </Button>
                </div>
              </div>
              <p class="text-sm text-[#435574]">{{ notification.message }}</p>

              <Button
                v-if="notification.actionLink"
                variant="link"
                size="sm"
                class="text-[#286AD8] hover:text-[#435574] p-0 h-auto mt-2"
              >
                Ver detalhes →
              </Button>
            </div>

            <!-- Unread Indicator -->
            <div
              v-if="!notification.isRead"
              class="w-2 h-2 rounded-full bg-[#286AD8] flex-shrink-0 mt-2"
            />
          </div>
        </Card>
      </TabsContent>
    </Tabs>

    <!-- Clear Confirmation Dialog -->
    <AlertDialog :open="showClearDialog" @update:open="(val) => showClearDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Limpar Notificações Lidas</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja remover todas as notificações já lidas?
            Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleClearAll" class="bg-red-600 hover:bg-red-700">
            Sim, Limpar Tudo
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

