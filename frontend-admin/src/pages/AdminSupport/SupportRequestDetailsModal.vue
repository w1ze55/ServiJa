<script setup lang="ts">
import { Calendar, ExternalLink, FileText, Loader2, Mail, Shield, Tag, User, X } from 'lucide-vue-next';
import type { SupportRequest } from '@/api/services/supportService';
import {
  formatSupportFileSize,
  getSupportCategoryLabel,
  getSupportStatusBadgeClass,
  getSupportStatusLabel,
  isImageAttachment,
} from '@/constants/support';

defineProps<{
  open: boolean;
  request: SupportRequest | null;
  isLoading?: boolean;
  canModerate?: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'approve'): void;
  (e: 'reject'): void;
}>();

const formatDate = (value?: string | null) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-[75] flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/40" @click="emit('close')" />
    <div class="relative flex max-h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <div>
          <h2 class="text-base font-semibold text-slate-900">
            {{ request ? `Solicitação #${request.id}` : 'Detalhes da solicitação' }}
          </h2>
          <p v-if="request" class="text-sm text-slate-500">
            {{ getSupportCategoryLabel(request.category) }}
          </p>
        </div>
        <button
          class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          @click="emit('close')"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-6">
        <div v-if="isLoading" class="flex min-h-[280px] items-center justify-center">
          <div class="flex items-center gap-3 text-slate-500">
            <Loader2 class="h-5 w-5 animate-spin" />
            Carregando detalhes...
          </div>
        </div>

        <div v-else-if="request" class="space-y-6">
          <div class="grid gap-4 md:grid-cols-2">
            <div class="rounded-xl border border-slate-200 bg-slate-50 p-5">
              <div class="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                <Shield class="h-4 w-4" />
                <p>Usuário</p>
                <div class="ml-auto">
                <p class="text-right text-sm text-slate-500">ID {{ request.userId }}</p>
                </div>
              </div>
              <p class="mt-2 break-words text-sm font-semibold leading-6 text-slate-900">{{ request.userName }}</p>
            </div>

            <div class="rounded-xl border border-slate-200 bg-slate-50 p-5">
              <div class="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                <Mail class="h-4 w-4" />
                Email
              </div>
              <p class="break-words text-sm font-semibold leading-6 text-slate-900">{{ request.userEmail }}</p>
            </div>

            <div class="rounded-xl border border-slate-200 bg-slate-50 p-5">
              <div class="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                <Tag class="h-4 w-4" />
                Categoria
              </div>
              <p class="break-words text-sm font-semibold leading-6 text-slate-900">
                {{ getSupportCategoryLabel(request.category) }}
              </p>
            </div>

            <div class="rounded-xl border border-slate-200 bg-slate-50 p-5">
              <div class="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">
                <User class="h-4 w-4" />
                Status
              </div>
              <span
                :class="['inline-flex rounded-full px-2.5 py-1 text-xs font-semibold', getSupportStatusBadgeClass(request.status)]"
              >
                {{ getSupportStatusLabel(request.status) }}
              </span>
            </div>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-5">
            <h3 class="text-sm font-semibold text-slate-900">Descrição completa</h3>
            <p class="mt-3 whitespace-pre-line text-sm leading-6 text-slate-700">
              {{ request.description }}
            </p>
          </div>

          <div class="rounded-2xl border border-slate-200 bg-white p-5">
            <div class="flex items-center justify-between gap-4">
              <h3 class="text-sm font-semibold text-slate-900">Anexos enviados</h3>
              <span class="text-xs text-slate-500">
                {{ request.attachments?.length || 0 }} arquivo{{ request.attachments?.length === 1 ? '' : 's' }}
              </span>
            </div>

            <div v-if="!request.attachments || request.attachments.length === 0" class="mt-4 rounded-xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-500">
              Nenhum anexo foi enviado nesta solicitação.
            </div>

            <div v-else class="mt-4 grid gap-4 md:grid-cols-2">
              <div
                v-for="(attachment, index) in request.attachments"
                :key="attachment.id || `${attachment.url}-${index}`"
                class="overflow-hidden rounded-xl border border-slate-200 bg-slate-50"
              >
                <div class="relative h-40 w-full overflow-hidden bg-white">
                  <img
                    v-if="isImageAttachment(attachment)"
                    :src="attachment.url"
                    :alt="attachment.name"
                    class="block h-full w-full object-cover"
                  />
                  <div
                    v-else
                    class="flex h-full w-full items-center justify-center text-slate-400"
                  >
                    <FileText class="h-10 w-10" />
                  </div>
                </div>

                <div class="space-y-3 p-4">
                  <div>
                    <p class="truncate text-sm font-medium text-slate-900">{{ attachment.name }}</p>
                    <p class="text-xs text-slate-500">
                      {{ attachment.type || 'Arquivo' }}
                      <span v-if="attachment.size"> • {{ formatSupportFileSize(attachment.size) }}</span>
                    </p>
                  </div>

                  <a
                    v-if="attachment.url"
                    :href="attachment.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                  >
                    <ExternalLink class="h-4 w-4" />
                    Abrir anexo
                  </a>
                </div>
              </div>
            </div>
          </div>

          <div class="grid gap-4 lg:grid-cols-2">
            <div class="rounded-2xl border border-slate-200 bg-white p-5">
              <h3 class="text-sm font-semibold text-slate-900">Resposta do admin</h3>
              <p v-if="request.adminResponse" class="mt-3 whitespace-pre-line text-sm leading-6 text-slate-700">
                {{ request.adminResponse }}
              </p>
              <p v-else class="mt-3 text-sm text-slate-500">
                Ainda não há resposta registrada para esta solicitação.
              </p>

              <div v-if="request.adminName || request.answeredAt" class="mt-4 border-t border-slate-200 pt-4 text-xs text-slate-500">
                <p v-if="request.adminName">Responsável: {{ request.adminName }}</p>
                <p v-if="request.answeredAt">Respondida em {{ formatDate(request.answeredAt) }}</p>
              </div>
            </div>

            <div class="rounded-2xl border border-slate-200 bg-white p-5">
              <h3 class="text-sm font-semibold text-slate-900">Datas</h3>
              <div class="mt-4 space-y-3 text-sm text-slate-700">
                <div class="flex items-start gap-3">
                  <Calendar class="mt-0.5 h-4 w-4 text-slate-400" />
                  <div>
                    <p class="font-medium text-slate-900">Criação</p>
                    <p>{{ formatDate(request.createdAt) }}</p>
                  </div>
                </div>
                <div class="flex items-start gap-3">
                  <Calendar class="mt-0.5 h-4 w-4 text-slate-400" />
                  <div>
                    <p class="font-medium text-slate-900">Última atualização</p>
                    <p>{{ formatDate(request.updatedAt) }}</p>
                  </div>
                </div>
                <div class="flex items-start gap-3">
                  <Calendar class="mt-0.5 h-4 w-4 text-slate-400" />
                  <div>
                    <p class="font-medium text-slate-900">Data de resposta</p>
                    <p>{{ formatDate(request.answeredAt) }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-3 border-t border-slate-200 px-6 py-4">
        <template v-if="canModerate">
          <button
            type="button"
            class="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-emerald-700"
            @click="emit('approve')"
          >
            Aprovar
          </button>
          <button
            type="button"
            class="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700"
            @click="emit('reject')"
          >
            Recusar
          </button>
        </template>
        <p v-else class="text-sm text-slate-500">
          Esta solicitação já foi respondida. Se necessário, o usuário deve abrir uma nova solicitação.
        </p>
      </div>
    </div>
  </div>
</template>
