<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Shield, ShieldCheck } from 'lucide-vue-next'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { twoFactorService, type TwoFactorStatus } from '@/api/services/twoFactorService'
import TwoFactorSetupDialog from '@/components/security/TwoFactorSetupDialog.vue'
import TwoFactorDisableDialog from '@/components/security/TwoFactorDisableDialog.vue'
import BackupCodesDialog from '@/components/security/BackupCodesDialog.vue'

const status = ref<TwoFactorStatus | null>(null)
const loading = ref(true)
const setupOpen = ref(false)
const disableOpen = ref(false)
const backupCodes = ref<string[]>([])
const backupOpen = ref(false)

async function reload() {
  loading.value = true
  try {
    status.value = await twoFactorService.status()
  } finally {
    loading.value = false
  }
}

onMounted(reload)

function onActivated(codes: string[]) {
  setupOpen.value = false
  backupCodes.value = codes
  backupOpen.value = true
}

function onBackupClosed() {
  backupOpen.value = false
  backupCodes.value = []
  reload()
}

function onDisabled() {
  disableOpen.value = false
  reload()
}
</script>

<template>
  <AdminLayout>
    <div class="mx-auto max-w-3xl space-y-6">
      <header>
        <h1 class="text-2xl font-semibold text-slate-900">Segurança</h1>
        <p class="text-sm text-slate-500">Gerencie como você acessa o painel administrativo.</p>
      </header>

    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <div class="flex items-start justify-between gap-4">
        <div class="flex items-start gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-blue-50">
            <component
              :is="status?.enabled ? ShieldCheck : Shield"
              class="h-5 w-5"
              :class="status?.enabled ? 'text-green-600' : 'text-blue-600'"
            />
          </div>
          <div>
            <h3 class="text-lg font-semibold text-slate-900">
              Autenticação em dois fatores (2FA)
            </h3>
            <p class="text-sm text-slate-600">
              Proteja sua conta exigindo um código do Google Authenticator no login.
            </p>
            <p v-if="status?.enabled" class="mt-2 text-sm text-green-700">
              Ativo. {{ status.unusedBackupCodes }} códigos de backup disponíveis.
            </p>
          </div>
        </div>

        <div v-if="!loading" class="shrink-0">
          <button
            v-if="!status?.enabled"
            type="button"
            class="rounded-lg bg-[#10243d] px-4 py-2 text-sm font-medium text-white transition hover:bg-[#1a3a5c]"
            @click="setupOpen = true"
          >
            Ativar
          </button>
          <button
            v-else
            type="button"
            class="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700"
            @click="disableOpen = true"
          >
            Desativar
          </button>
        </div>
      </div>
    </div>

    <TwoFactorSetupDialog
      :open="setupOpen"
      :on-close="() => (setupOpen = false)"
      :on-activated="onActivated"
    />
    <TwoFactorDisableDialog
      :open="disableOpen"
      :on-close="() => (disableOpen = false)"
      :on-disabled="onDisabled"
    />
      <BackupCodesDialog :open="backupOpen" :codes="backupCodes" :on-close="onBackupClosed" />
    </div>
  </AdminLayout>
</template>
