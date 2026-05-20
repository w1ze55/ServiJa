<script setup lang="ts">
import { computed } from 'vue'
import { X, Check, AlertTriangle, Copy } from 'lucide-vue-next'
import type { AuditLogEntry, ActorType } from '@/api/services/auditLogService'

const props = defineProps<{
  log: AuditLogEntry | null
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const ACTOR_TYPE_LABEL: Record<ActorType, string> = {
  CONTRACTOR: 'Contratante',
  FREELANCER: 'Freelancer',
  ADMIN: 'Admin',
  GUEST: 'Anônimo',
}

const ENTITY_LABEL: Record<string, string> = {
  USER: 'Usuário',
  SERVICE: 'Serviço',
  SERVICE_REQUEST: 'Solicitação',
  REVIEW: 'Avaliação',
  PAYMENT: 'Pagamento',
  WITHDRAWAL: 'Saque',
  BANK_ACCOUNT: 'Conta bancária',
  PORTFOLIO: 'Portfólio',
  FAVORITE: 'Favorito',
  SUPPORT_TICKET: 'Suporte',
  AUTH: 'Autenticação',
  ADJUSTMENT: 'Ajuste',
  SYSTEM: 'Sistema',
  CATEGORY: 'Categoria',
}

const formattedTimestamp = computed(() => {
  if (!props.log) return ''
  const d = new Date(props.log.timestamp)
  return d.toLocaleDateString('pt-BR') + ' às ' + d.toLocaleTimeString('pt-BR')
})

const entityLabel = computed(() => {
  if (!props.log?.entityType) return '—'
  return ENTITY_LABEL[props.log.entityType] ?? props.log.entityType
})

const formattedPayload = computed(() => {
  if (!props.log?.payload) return ''
  try {
    return JSON.stringify(JSON.parse(props.log.payload), null, 2)
  } catch {
    return props.log.payload
  }
})

const copyPayload = () => {
  if (formattedPayload.value) {
    navigator.clipboard.writeText(formattedPayload.value)
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="open"
        class="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm"
        @click="emit('close')"
      />
    </Transition>

    <Transition name="slide">
      <aside
        v-if="open"
        class="fixed inset-y-0 right-0 z-50 flex w-full max-w-xl flex-col border-l border-slate-200 bg-white shadow-xl"
      >
        <div class="flex h-20 shrink-0 items-center justify-between border-b border-slate-200 px-6">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">Detalhes do log</p>
            <p class="mt-0.5 text-base font-semibold text-slate-900">
              {{ log ? `#LOG-${String(log.id).padStart(4, '0')}` : '' }}
            </p>
          </div>
          <button
            type="button"
            class="flex h-9 w-9 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50"
            @click="emit('close')"
          >
            <X class="h-4 w-4" />
          </button>
        </div>

        <div v-if="log" class="flex-1 overflow-y-auto px-6 py-6">
          <div class="space-y-6">
            <!-- Summary -->
            <div>
              <div class="mb-3 flex flex-wrap items-center gap-2">
                <span class="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700">
                  {{ log.action }}
                </span>
                <span
                  v-if="log.result === 'SUCCESS'"
                  class="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700"
                >
                  <Check class="h-3 w-3" /> Sucesso
                </span>
                <span
                  v-else
                  class="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-medium text-red-700"
                >
                  <AlertTriangle class="h-3 w-3" /> Falha
                </span>
                <span
                  v-if="log.httpStatus"
                  class="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700"
                >
                  HTTP {{ log.httpStatus }}
                </span>
              </div>
              <h2 class="text-lg font-semibold text-slate-900">
                {{ log.entityLabel ?? log.entityType ?? log.action }}
              </h2>
              <p class="mt-1 text-sm text-slate-500">{{ formattedTimestamp }}</p>
            </div>

            <!-- Actor / metadata -->
            <div class="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
              <dl class="grid grid-cols-2 gap-x-4 gap-y-3">
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Usuário</dt>
                  <dd class="mt-1 text-sm font-medium text-slate-900">{{ log.actorName ?? '—' }}</dd>
                  <dd class="text-xs text-slate-500">{{ log.actorEmail ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Tipo</dt>
                  <dd class="mt-1 text-sm font-medium text-slate-900">
                    {{ log.actorType ? ACTOR_TYPE_LABEL[log.actorType] : '—' }}
                  </dd>
                  <dd class="text-xs text-slate-500">ID: {{ log.actorId ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Entidade</dt>
                  <dd class="mt-1 text-sm font-medium text-slate-900">{{ entityLabel }}</dd>
                  <dd class="text-xs text-slate-500">{{ log.entityId ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Requisição</dt>
                  <dd class="mt-1 text-xs font-mono text-slate-900">
                    {{ log.httpMethod ?? '—' }} {{ log.httpPath ?? '' }}
                  </dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">Endereço IP</dt>
                  <dd class="mt-1 text-sm font-mono text-slate-900">{{ log.ipAddress ?? '—' }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">User Agent</dt>
                  <dd class="mt-1 text-xs text-slate-700 break-words">{{ log.userAgent ?? '—' }}</dd>
                </div>
              </dl>
            </div>

            <!-- Payload -->
            <div v-if="formattedPayload">
              <div class="mb-2 flex items-center justify-between">
                <h3 class="text-sm font-semibold text-slate-900">Payload</h3>
                <button
                  type="button"
                  class="flex items-center gap-1.5 text-xs font-medium text-slate-500 transition hover:text-slate-900"
                  @click="copyPayload"
                >
                  <Copy class="h-3.5 w-3.5" />
                  Copiar JSON
                </button>
              </div>
              <pre class="overflow-x-auto rounded-xl border border-slate-200 bg-slate-900 p-4 text-xs leading-relaxed text-slate-100">{{ formattedPayload }}</pre>
            </div>
          </div>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-enter-active, .slide-leave-active { transition: transform 0.25s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }
</style>
