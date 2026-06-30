<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useReviewStore } from '@/stores/reviewStore';
import type { PendingReview } from '@/api/services/reviewService';
import { Star, X, ArrowRight } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';

const router = useRouter();
const route = useRoute();
const reviewStore = useReviewStore();

const isOnRatingPage = computed(() => {
  return route.path === '/rate-freelancer';
});

const showBanner = ref(true);
const showModal = ref(false);
const currentPendingIndex = ref(0);
let reminderInterval: ReturnType<typeof setInterval> | null = null;

const REMINDER_INTERVAL_MS = 120_000; // 2 minutos
const SNOOZE_KEY = 'pending_review_snoozed_until';

const currentPending = ref<PendingReview | null>(reviewStore.pendingReviews[0] || null);

watch(
  () => reviewStore.pendingReviews,
  (pending) => {
    if (pending.length > 0) {
      currentPending.value = pending[currentPendingIndex.value] || pending[0];
      checkAndShowModal();
    } else {
      currentPending.value = null;
      showModal.value = false;
    }
  },
  { deep: true }
);

watch(
  () => route.path,
  () => {
    if (isOnRatingPage.value) {
      showModal.value = false;
    }
  }
);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const isSnoozed = (): boolean => {
  const snoozedUntil = localStorage.getItem(SNOOZE_KEY);
  if (!snoozedUntil) return false;
  return Date.now() < parseInt(snoozedUntil);
};

const checkAndShowModal = () => {
  if (reviewStore.hasPendingReviews && !isSnoozed() && !isOnRatingPage.value) {
    showModal.value = true;
  }
};

const goToReview = (serviceRequestId: number) => {
  showModal.value = false;
  router.push('/rate-freelancer?requestId=' + serviceRequestId);
};

const snoozeReminder = () => {
  const snoozeUntil = Date.now() + 5 * 60 * 1000; // 5 minutos
  localStorage.setItem(SNOOZE_KEY, String(snoozeUntil));
  showModal.value = false;
};

const dismissBanner = () => {
  showBanner.value = false;
};

const startReminder = () => {
  if (reminderInterval) clearInterval(reminderInterval);
  reminderInterval = setInterval(() => {
    if (reviewStore.hasPendingReviews) {
      checkAndShowModal();
    }
  }, REMINDER_INTERVAL_MS);
};

onMounted(async () => {
  if (!reviewStore.pendingLoaded) {
    await reviewStore.loadPendingReviews();
  }
  if (reviewStore.hasPendingReviews) {
    currentPending.value = reviewStore.pendingReviews[0];
    checkAndShowModal();
  }
  startReminder();
});

onUnmounted(() => {
  if (reminderInterval) {
    clearInterval(reminderInterval);
  }
});
</script>

<template>
  <!-- Banner fixo no topo (esconde na página de avaliação) -->
  <div
    v-if="reviewStore.hasPendingReviews && showBanner && !isOnRatingPage"
    class="bg-gradient-to-r from-amber-50 to-yellow-50 border-b border-amber-200 px-4 py-3"
  >
    <div class="max-w-7xl mx-auto flex items-center justify-between gap-4">
      <div class="flex items-center gap-3 min-w-0">
        <div class="bg-amber-100 rounded-full p-2 flex-shrink-0">
          <Star class="h-4 w-4 text-amber-600 fill-amber-600" />
        </div>
        <p class="text-sm text-amber-900 truncate">
          <strong>{{ reviewStore.pendingCount }}</strong>
          {{ reviewStore.pendingCount === 1 ? 'avaliação pendente' : 'avaliações pendentes' }}
          — Sua opinião ajuda a comunidade!
        </p>
      </div>
      <div class="flex items-center gap-2 flex-shrink-0">
        <Button
          variant="warning"
          size="sm"
          class="text-xs"
          @click="currentPending && goToReview(currentPending.serviceRequestId)"
        >
          Avaliar agora
          <ArrowRight class="h-3 w-3 ml-1" />
        </Button>
        <button @click="dismissBanner" class="text-amber-600 hover:text-amber-800 p-1">
          <X class="h-4 w-4" />
        </button>
      </div>
    </div>
  </div>

  <!-- Modal de lembrete -->
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="showModal && currentPending"
        class="fixed inset-0 z-[9999] flex items-center justify-center p-4"
      >
        <!-- Overlay -->
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm" @click="snoozeReminder" />

        <!-- Modal -->
        <div class="relative bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 space-y-5 animate-in zoom-in-95">
          <!-- Header com estrelas decorativas -->
          <div class="text-center">
            <div class="flex justify-center gap-1 mb-3">
              <Star
                v-for="i in 5"
                :key="i"
                class="h-6 w-6 text-yellow-400 fill-yellow-400 animate-pulse"
                :style="{ animationDelay: `${i * 100}ms` }"
              />
            </div>
            <h2 class="text-xl font-bold text-[#263238]">Avalie seu serviço!</h2>
            <p class="text-sm text-[#435574] mt-1">
              Sua avaliação ajuda outros usuários a encontrar os melhores profissionais
            </p>
          </div>

          <!-- Card do serviço pendente -->
          <div class="bg-gray-50 rounded-xl p-4">
            <div class="flex items-center gap-3">
              <Avatar class="h-12 w-12 flex-shrink-0">
                <AvatarImage :src="currentPending.targetAvatar" :alt="currentPending.targetName" />
                <AvatarFallback class="bg-[#286AD8] text-white">
                  {{ getInitials(currentPending.targetName) }}
                </AvatarFallback>
              </Avatar>
              <div class="min-w-0">
                <p class="font-semibold text-[#263238] truncate">{{ currentPending.targetName }}</p>
                <p class="text-sm text-[#435574] truncate">{{ currentPending.serviceName }}</p>
              </div>
            </div>
          </div>

          <!-- Info sobre pendentes -->
          <div v-if="reviewStore.pendingCount > 1" class="text-center">
            <p class="text-xs text-[#435574]">
              + {{ reviewStore.pendingCount - 1 }} {{ reviewStore.pendingCount - 1 === 1 ? 'outra avaliação pendente' : 'outras avaliações pendentes' }}
            </p>
          </div>

          <!-- Botões -->
          <div class="space-y-3">
            <Button
              variant="default"
              class="w-full"
              size="lg"
              @click="goToReview(currentPending.serviceRequestId)"
            >
              <Star class="h-4 w-4 mr-2" />
              Avaliar agora
            </Button>
            <Button
              variant="ghost"
              class="w-full text-[#435574] hover:text-[#263238]"
              @click="snoozeReminder"
            >
              Lembrar mais tarde
            </Button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}

@keyframes zoom-in-95 {
  from {
    transform: scale(0.95);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.animate-in {
  animation: zoom-in-95 0.3s ease forwards;
}
</style>
