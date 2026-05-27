<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Button from '../ui/button.vue'
import Card from '../ui/card.vue'
import { Shield, ShieldCheck } from 'lucide-vue-next'
import { twoFactorService, type TwoFactorStatus } from '@/api/services/twoFactorService'
import TwoFactorSetupDialog from './TwoFactorSetupDialog.vue'
import TwoFactorDisableDialog from './TwoFactorDisableDialog.vue'
import BackupCodesDialog from './BackupCodesDialog.vue'

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
  <Card class="p-6">
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
          <h3 class="text-lg font-semibold">Autenticação em dois fatores (2FA)</h3>
          <p class="text-sm text-gray-600">
            Proteja sua conta exigindo um código do Google Authenticator no login.
          </p>
          <p v-if="status?.enabled" class="mt-2 text-sm text-green-700">
            Ativo. Você tem {{ status.unusedBackupCodes }} códigos de backup não usados.
          </p>
        </div>
      </div>

      <div v-if="!loading" class="shrink-0">
        <Button v-if="!status?.enabled" @click="setupOpen = true" type="button">
          Ativar
        </Button>
        <Button v-else variant="destructive" @click="disableOpen = true" type="button">
          Desativar
        </Button>
      </div>
    </div>

    <TwoFactorSetupDialog
      :open="setupOpen"
      :onClose="() => (setupOpen = false)"
      :onActivated="onActivated"
    />

    <TwoFactorDisableDialog
      :open="disableOpen"
      :onClose="() => (disableOpen = false)"
      :onDisabled="onDisabled"
    />

    <BackupCodesDialog :open="backupOpen" :codes="backupCodes" :onClose="onBackupClosed" />
  </Card>
</template>
