<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Shield, ShieldCheck } from 'lucide-vue-next'
import { twoFactorService, type TwoFactorStatus } from '@/api/services/twoFactorService'
import Button from '../ui/button.vue'
import Card from '../ui/card.vue'
import BackupCodesDialog from './BackupCodesDialog.vue'
import TwoFactorDisableDialog from './TwoFactorDisableDialog.vue'
import TwoFactorSetupDialog from './TwoFactorSetupDialog.vue'

interface Props {
  interactive?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  interactive: false,
})

const status = ref<TwoFactorStatus | null>(null)
const loading = ref(true)
const setupOpen = ref(false)
const disableOpen = ref(false)
const backupCodes = ref<string[]>([])
const backupOpen = ref(false)

const isEnabled = computed(() => Boolean(status.value?.enabled))
const canAct = computed(() => !loading.value && status.value !== null)
const canInteract = computed(() => props.interactive && canAct.value)
const actionLabel = computed(() => (isEnabled.value ? 'Desativar' : 'Ativar'))
const iconComponent = computed(() => (isEnabled.value ? ShieldCheck : Shield))
const cardClasses = computed(() => [
  'p-6',
  props.interactive ? 'bg-white transition-all hover:shadow-md hover:border-[#286AD8]' : '',
  canInteract.value ? 'cursor-pointer' : props.interactive ? 'cursor-default' : '',
])

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

function handleCardClick() {
  if (!canInteract.value) {
    return
  }

  openCurrentAction()
}

function openCurrentAction() {
  if (!canAct.value) {
    return
  }

  if (isEnabled.value) {
    disableOpen.value = true
    return
  }

  setupOpen.value = true
}
</script>

<template>
  <Card
    :class="cardClasses"
    :role="canInteract ? 'button' : undefined"
    :tabindex="canInteract ? 0 : undefined"
    :aria-disabled="props.interactive && !canInteract ? 'true' : undefined"
    @click="handleCardClick"
    @keydown.enter.prevent="handleCardClick"
    @keydown.space.prevent="handleCardClick"
  >
    <div class="flex items-center justify-between gap-4">
      <div class="flex items-center gap-4">
        <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-blue-100">
          <component
            :is="iconComponent"
            class="h-6 w-6"
            :class="isEnabled ? 'text-green-600' : 'text-[#286AD8]'"
          />
        </div>
        <div class="flex-1">
          <h3 class="mb-1 font-semibold text-[#263238]">Autenticação em dois fatores (2FA)</h3>
          <p class="text-sm text-[#435574]">
            Proteja sua conta exigindo um código do Google Authenticator no login.
          </p>
          <p v-if="isEnabled" class="mt-2 text-sm text-green-700">
            Ativo. Você tem {{ status?.unusedBackupCodes }} códigos de backup não usados.
          </p>
        </div>
      </div>

      <div v-if="!loading && status" class="shrink-0">
        <Button
          :variant="isEnabled ? 'destructive' : 'default'"
          size="sm"
          type="button"
          @click.stop="openCurrentAction"
        >
          {{ actionLabel }}
        </Button>
      </div>
    </div>
  </Card>

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
</template>
