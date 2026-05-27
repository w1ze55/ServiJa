<script setup lang="ts">
import { ref } from 'vue'
import { Copy, Check, Download } from 'lucide-vue-next'
import Modal from './Modal.vue'

interface Props {
  open: boolean
  codes: string[]
  onClose: () => void
}
const props = defineProps<Props>()

const copied = ref(false)
const acknowledged = ref(false)

async function copyAll() {
  await navigator.clipboard.writeText(props.codes.join('\n'))
  copied.value = true
  setTimeout(() => (copied.value = false), 2000)
}

function download() {
  const content =
    'Códigos de backup ServiJá Admin — guarde em local seguro.\nCada código pode ser usado uma única vez.\n\n' +
    props.codes.join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'servija-admin-backup-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <Modal :open="props.open">
    <h3 class="text-lg font-semibold text-slate-900">Salve seus códigos de backup</h3>
    <p class="mt-1 text-sm text-slate-600">
      Estes códigos só serão exibidos uma vez. Guarde-os em um lugar seguro, cada
      código pode substituir o aplicativo autenticador uma única vez.
    </p>

    <div class="mt-4 grid grid-cols-2 gap-2 rounded-lg bg-slate-50 p-4 font-mono text-sm">
      <div v-for="code in props.codes" :key="code">{{ code }}</div>
    </div>

    <div class="mt-3 flex gap-2">
      <button
        type="button"
        class="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        @click="copyAll"
      >
        <component :is="copied ? Check : Copy" class="h-4 w-4" />
        {{ copied ? 'Copiado!' : 'Copiar todos' }}
      </button>
      <button
        type="button"
        class="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        @click="download"
      >
        <Download class="h-4 w-4" />
        Baixar .txt
      </button>
    </div>

    <label class="mt-4 flex items-center gap-2 text-sm text-slate-700">
      <input type="checkbox" v-model="acknowledged" />
      Salvei meus códigos em local seguro
    </label>

    <div class="mt-6 flex justify-end">
      <button
        type="button"
        class="rounded-lg bg-[#10243d] px-4 py-2 text-sm font-medium text-white transition hover:bg-[#1a3a5c] disabled:opacity-60"
        :disabled="!acknowledged"
        @click="props.onClose"
      >
        Concluir
      </button>
    </div>
  </Modal>
</template>
