<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AlertDialog from '@/components/ui/alert-dialog.vue';
import AlertDialogAction from '@/components/ui/alert-dialog-action.vue';
import AlertDialogCancel from '@/components/ui/alert-dialog-cancel.vue';
import AlertDialogContent from '@/components/ui/alert-dialog-content.vue';
import AlertDialogDescription from '@/components/ui/alert-dialog-description.vue';
import AlertDialogFooter from '@/components/ui/alert-dialog-footer.vue';
import AlertDialogHeader from '@/components/ui/alert-dialog-header.vue';
import AlertDialogTitle from '@/components/ui/alert-dialog-title.vue';
import Tabs from '@/components/ui/tabs.vue';
import TabsContent from '@/components/ui/tabs-content.vue';
import TabsList from '@/components/ui/tabs-list.vue';
import TabsTrigger from '@/components/ui/tabs-trigger.vue';
import NotificationActionsBar from '@/components/notifications/NotificationActionsBar.vue';
import NotificationEmptyState from '@/components/notifications/NotificationEmptyState.vue';
import NotificationList from '@/components/notifications/NotificationList.vue';
import NotificationSkeleton from '@/components/notifications/NotificationSkeleton.vue';
import { useNotificationStore } from '@/stores/notificationStore';
import type { NotificationCategory, NotificationItem } from '@/types/notifications';
import {
  getNotificationCategoryLabel,
  groupNotificationsByCategory,
  notificationCategoryOrder,
  resolveNotificationRoute,
  type NotificationUserType,
} from '@/utils/notifications';

interface Props {
  userType: NotificationUserType;
}

const props = defineProps<Props>();

const router = useRouter();
const notificationStore = useNotificationStore();
const showClearDialog = ref(false);
const activeTab = ref<'all' | NotificationCategory>('all');

const groupedNotifications = computed(() =>
  groupNotificationsByCategory(notificationStore.sortedNotifications)
);

const hasAnyNotifications = computed(() => notificationStore.sortedNotifications.length > 0);
const activeTabLabel = computed(() =>
  activeTab.value === 'all' ? 'Todas' : getNotificationCategoryLabel(activeTab.value)
);
const currentNotifications = computed(() =>
  activeTab.value === 'all'
    ? notificationStore.sortedNotifications
    : groupedNotifications.value[activeTab.value]
);

const openNotification = async (notification: NotificationItem) => {
  if (!notification.isRead) {
    await notificationStore.markNotificationAsRead(notification.id);
  }

  const target = resolveNotificationRoute(notification, props.userType);
  router.push({
    path: target.path,
    query: target.query,
  });
};

const handleClearAll = async () => {
  showClearDialog.value = false;
  await notificationStore.clearAllNotifications();
};

onMounted(async () => {
  await notificationStore.loadPageNotifications();
});
</script>

<template>
  <div class="mx-auto max-w-5xl space-y-6">
    <div class="space-y-2">
      <h1 class="text-3xl font-semibold text-[#263238]">Notificações</h1>
      <p class="text-[#435574]">
        Acompanhe propostas, pagamentos, atualizações de projeto e futuras notificações de mensagens em um só lugar.
      </p>
    </div>

    <NotificationActionsBar
      :total-count="notificationStore.sortedNotifications.length"
      :unread-count="notificationStore.unreadCount"
      :is-busy="notificationStore.isMutating"
      @mark-all-read="notificationStore.markAllNotificationsAsRead"
      @clear-all="showClearDialog = true"
    />

    <NotificationSkeleton
      v-if="notificationStore.isLoadingPage && !hasAnyNotifications"
      :count="5"
    />

    <NotificationEmptyState
      v-else-if="!hasAnyNotifications"
      title="Nenhuma notificação por enquanto"
      description="Assim que houver novas propostas, pagamentos ou atualizações de projeto, elas aparecerão aqui."
    />

    <Tabs
      v-else
      v-model="activeTab"
      default-value="all"
      class="space-y-5"
    >
      <TabsList class="tabs-list grid w-full grid-cols-4">
          <TabsTrigger value="all">
            Todas
          </TabsTrigger>
          <TabsTrigger
            v-for="category in notificationCategoryOrder"
            :key="category"
            :value="category"
          >
            {{ getNotificationCategoryLabel(category) }}
          </TabsTrigger>
      </TabsList>

      <TabsContent :value="activeTab">
        <section class="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-[#263238]">
                {{ activeTabLabel }}
              </h2>
              <p class="text-sm text-[#435574]">
                {{ currentNotifications.length }} {{ currentNotifications.length === 1 ? 'notificação' : 'notificações' }}
              </p>
            </div>
          </div>

          <NotificationList
            :notifications="currentNotifications"
            :show-category="activeTab === 'all'"
            empty-title="Nada por aqui"
            empty-description="Quando houver novas atualizações nesta categoria, elas aparecerão aqui."
          :pending-ids="notificationStore.pendingNotificationIds"
          @open="openNotification"
          @mark-read="notificationStore.markNotificationAsRead($event.id)"
          @delete="notificationStore.deleteNotification($event.id)"
          />
        </section>
      </TabsContent>
    </Tabs>

    <AlertDialog :open="showClearDialog" @update:open="showClearDialog = $event">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Limpar todas as notificações</AlertDialogTitle>
          <AlertDialogDescription>
            Essa ação remove todas as notificações, lidas e não lidas. Não será possível desfazer.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction class="bg-destructive hover:bg-destructive/90" @click="handleClearAll">
            Limpar tudo
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .tabs-list {
    grid-template-columns: repeat(2, 1fr) !important;
    gap: 0.25rem;
  }
}
</style>
