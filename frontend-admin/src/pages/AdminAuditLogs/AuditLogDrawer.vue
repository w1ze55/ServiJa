<script setup lang="ts">
import { computed } from "vue";
import { AlertTriangle, Check, Copy, LoaderCircle, X } from "lucide-vue-next";
import type { ActorType, AuditLogEntry } from "@/api/services/auditLogService";

type JsonPrimitive = string | number | boolean | null;
interface JsonArray extends Array<JsonValue> {}
interface JsonObject {
  [key: string]: JsonValue;
}
type JsonValue = JsonPrimitive | JsonArray | JsonObject;

interface JsonEntry {
  key: string;
  value: JsonValue;
  isLast: boolean;
}

const props = withDefaults(
  defineProps<{
    log: AuditLogEntry | null;
    open: boolean;
    loading?: boolean;
  }>(),
  {
    loading: false,
  },
);

const emit = defineEmits<{
  close: [];
}>();

const EMPTY_VALUE = "Não informado";

const ACTOR_TYPE_LABEL: Record<ActorType, string> = {
  CONTRACTOR: "Contratante",
  FREELANCER: "Freelancer",
  ADMIN: "Admin",
  GUEST: "Anônimo",
};

const ENTITY_LABEL: Record<string, string> = {
  USER: "Usuário",
  SERVICE: "Serviço",
  SERVICE_REQUEST: "Solicitação",
  REVIEW: "Avaliação",
  PAYMENT: "Pagamento",
  WITHDRAWAL: "Saque",
  BANK_ACCOUNT: "Conta bancária",
  ADDRESS: "Endereço",
  PORTFOLIO: "Portfólio",
  FAVORITE: "Favorito",
  SUPPORT_TICKET: "Suporte",
  AUTH: "Autenticação",
  ADJUSTMENT: "Ajuste",
  SYSTEM: "Sistema",
  CATEGORY: "Categoria",
  SUBCATEGORY: "Subcategoria",
};

const formattedTimestamp = computed(() => {
  if (!props.log) return "";
  const date = new Date(props.log.timestamp);
  return (
    date.toLocaleDateString("pt-BR") +
    " às " +
    date.toLocaleTimeString("pt-BR")
  );
});

const entityLabel = computed(() => {
  if (!props.log?.entityType) return EMPTY_VALUE;
  return ENTITY_LABEL[props.log.entityType] ?? props.log.entityType;
});

const payloadValue = computed(() => parseJsonValue(props.log?.payload));
const oldValue = computed(() => parseJsonValue(props.log?.oldValue));
const newValue = computed(() => parseJsonValue(props.log?.newValue));

const showPayload = computed(() => !isEmptyJson(payloadValue.value));
const showDiff = computed(
  () => !isEmptyJson(oldValue.value) || !isEmptyJson(newValue.value),
);

const beforeEntries = computed(() => toEntries(oldValue.value));
const afterEntries = computed(() => toEntries(newValue.value));

const changedKeys = computed(() => {
  const beforeObject = toObject(oldValue.value);
  const afterObject = toObject(newValue.value);
  const keys = new Set([
    ...Object.keys(beforeObject),
    ...Object.keys(afterObject),
  ]);

  return new Set(
    Array.from(keys).filter(
      (key) =>
        JSON.stringify(beforeObject[key] ?? null) !==
        JSON.stringify(afterObject[key] ?? null),
    ),
  );
});

const formattedPayload = computed(() => {
  if (!showPayload.value) return "";
  return JSON.stringify(payloadValue.value, null, 2);
});

const copyPayload = () => {
  if (formattedPayload.value) {
    void navigator.clipboard.writeText(formattedPayload.value);
  }
};

function parseJsonValue(raw?: string | null): JsonValue | null {
  if (!raw) return null;

  try {
    return JSON.parse(raw) as JsonValue;
  } catch {
    return raw;
  }
}

function isEmptyJson(value: JsonValue | null): boolean {
  if (value == null) return true;
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === "object") return Object.keys(value).length === 0;
  return false;
}

function toObject(value: JsonValue | null): JsonObject {
  if (value == null) return {};
  if (Array.isArray(value)) {
    return { value };
  }
  if (typeof value === "object") {
    return value as JsonObject;
  }
  return { value };
}

function toEntries(value: JsonValue | null): JsonEntry[] {
  const entries = Object.entries(toObject(value));
  return entries.map(([key, entryValue], index) => ({
    key,
    value: entryValue,
    isLast: index === entries.length - 1,
  }));
}

function isScalar(value: JsonValue): value is JsonPrimitive {
  return value == null || typeof value !== "object";
}

function formatScalar(value: JsonPrimitive): string {
  if (typeof value === "string") return `"${value}"`;
  if (value === null) return "null";
  return String(value);
}

function formatStructured(value: Exclude<JsonValue, JsonPrimitive>): string {
  return JSON.stringify(value, null, 2);
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
        <div
          class="flex h-20 shrink-0 items-center justify-between border-b border-slate-200 px-6"
        >
          <div>
            <p class="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Detalhes do log
            </p>
            <p class="mt-0.5 text-base font-semibold text-slate-900">
              {{ log ? `#LOG-${String(log.id).padStart(4, "0")}` : "" }}
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
            <div v-if="loading" class="flex items-center gap-2 text-sm text-slate-500">
              <LoaderCircle class="h-4 w-4 animate-spin" />
              Carregando detalhes completos...
            </div>

            <div>
              <div class="mb-3 flex flex-wrap items-center gap-2">
                <span
                  class="inline-flex items-center gap-1.5 rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700"
                >
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
                {{ log.entityLabel ?? entityLabel ?? log.action }}
              </h2>
              <p class="mt-1 text-sm text-slate-500">{{ formattedTimestamp }}</p>
            </div>

            <div class="rounded-xl border border-slate-200 bg-slate-50/60 p-4">
              <dl class="grid grid-cols-2 gap-x-4 gap-y-3">
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Usuário
                  </dt>
                  <dd class="mt-1 text-sm font-medium text-slate-900">
                    {{ log.actorName ?? EMPTY_VALUE }}
                  </dd>
                  <dd class="text-xs text-slate-500">
                    {{ log.actorEmail ?? EMPTY_VALUE }}
                  </dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Tipo
                  </dt>
                  <dd class="mt-1 text-sm font-medium text-slate-900">
                    {{ log.actorType ? ACTOR_TYPE_LABEL[log.actorType] : EMPTY_VALUE }}
                  </dd>
                  <dd class="text-xs text-slate-500">ID: {{ log.actorId ?? EMPTY_VALUE }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Entidade
                  </dt>
                  <dd class="mt-1 text-sm font-medium text-slate-900">{{ entityLabel }}</dd>
                  <dd class="text-xs text-slate-500">{{ log.entityId ?? EMPTY_VALUE }}</dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Requisição
                  </dt>
                  <dd class="mt-1 text-xs font-mono text-slate-900">
                    {{ log.httpMethod ?? EMPTY_VALUE }} {{ log.httpPath ?? "" }}
                  </dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">
                    Endereço IP
                  </dt>
                  <dd class="mt-1 text-sm font-mono text-slate-900">
                    {{ log.ipAddress ?? EMPTY_VALUE }}
                  </dd>
                </div>
                <div>
                  <dt class="text-xs font-medium uppercase tracking-wide text-slate-500">
                    User Agent
                  </dt>
                  <dd class="mt-1 break-words text-xs text-slate-700">
                    {{ log.userAgent ?? EMPTY_VALUE }}
                  </dd>
                </div>
              </dl>
            </div>

            <div v-if="showDiff" class="space-y-3">
              <h3 class="text-sm font-semibold text-slate-900">Alterações</h3>
              <div class="space-y-4">
                <section class="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-600">
                    Antes
                  </p>
                  <p v-if="!beforeEntries.length" class="text-sm text-slate-500">
                    Registro inexistente
                  </p>
                  <div v-else class="space-y-1 overflow-x-auto font-mono text-xs text-slate-700">
                    <div class="text-slate-400">{</div>
                    <div
                      v-for="entry in beforeEntries"
                      :key="`before-${entry.key}`"
                      class="break-words pl-4"
                      :class="changedKeys.has(entry.key) ? 'font-semibold' : ''"
                    >
                      <span class="text-slate-700">"{{ entry.key }}"</span>
                      <span class="text-slate-400">: </span>
                      <span v-if="isScalar(entry.value)" class="text-slate-700">
                        {{ formatScalar(entry.value) }}
                      </span>
                      <pre
                        v-else
                        class="inline whitespace-pre-wrap break-words text-slate-700"
                      >{{ formatStructured(entry.value) }}</pre>
                      <span v-if="!entry.isLast" class="text-slate-400">,</span>
                    </div>
                    <div class="text-slate-400">}</div>
                  </div>
                </section>

                <section class="rounded-xl border border-slate-200 bg-slate-50 p-4">
                  <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-600">
                    Depois
                  </p>
                  <p v-if="!afterEntries.length" class="text-sm text-slate-500">
                    Registro removido
                  </p>
                  <div v-else class="space-y-1 overflow-x-auto font-mono text-xs text-slate-700">
                    <div class="text-slate-400">{</div>
                    <div
                      v-for="entry in afterEntries"
                      :key="`after-${entry.key}`"
                      class="break-words pl-4"
                      :class="changedKeys.has(entry.key) ? 'font-semibold' : ''"
                    >
                      <span class="text-slate-700">"{{ entry.key }}"</span>
                      <span class="text-slate-400">: </span>
                      <span v-if="isScalar(entry.value)" class="text-slate-700">
                        {{ formatScalar(entry.value) }}
                      </span>
                      <pre
                        v-else
                        class="inline whitespace-pre-wrap break-words text-slate-700"
                      >{{ formatStructured(entry.value) }}</pre>
                      <span v-if="!entry.isLast" class="text-slate-400">,</span>
                    </div>
                    <div class="text-slate-400">}</div>
                  </div>
                </section>
              </div>
            </div>

            <div v-if="showPayload">
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
              <pre
                class="overflow-x-auto rounded-xl border border-slate-200 bg-slate-900 p-4 text-xs leading-relaxed text-slate-100"
              >{{ formattedPayload }}</pre>
            </div>
          </div>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.25s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
