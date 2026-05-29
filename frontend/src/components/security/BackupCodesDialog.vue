<script setup lang="ts">
import { ref } from 'vue'
import Button from '../ui/button.vue'
import AlertDialog from '../ui/alert-dialog.vue'
import AlertDialogContent from '../ui/alert-dialog-content.vue'
import AlertDialogDescription from '../ui/alert-dialog-description.vue'
import { Copy, Check, Download } from 'lucide-vue-next'

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
    'Códigos de backup ServiJá — guarde em local seguro.\nCada código pode ser usado uma única vez.\n\n' +
    props.codes.join('\n')
  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'servija-backup-codes.txt'
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <AlertDialog :open="props.open">
    <AlertDialogContent>
      <h3 class="text-lg font-semibold">Salve seus códigos de backup</h3>
      <AlertDialogDescription>
        Estes códigos só serão exibidos uma vez. Guarde-os em um lugar seguro,
        cada código pode substituir o aplicativo autenticador uma única vez.
      </AlertDialogDescription>

      <div class="my-4 grid grid-cols-2 gap-2 rounded-lg bg-gray-50 p-4 font-mono text-sm">
        <div v-for="code in props.codes" :key="code">{{ code }}</div>
      </div>

      <div class="flex gap-2">
        <Button variant="outline" @click="copyAll" type="button">
          <component :is="copied ? Check : Copy" class="h-4 w-4" />
          {{ copied ? 'Copiado!' : 'Copiar todos' }}
        </Button>
        <Button variant="outline" @click="download" type="button">
          <Download class="h-4 w-4" />
          Baixar .txt
        </Button>
      </div>

      <label class="mt-4 flex items-center gap-2 text-sm">
        <input type="checkbox" v-model="acknowledged" />
        Salvei meus códigos em local seguro
      </label>

      <div class="mt-4 flex justify-end">
        <Button :disabled="!acknowledged" @click="props.onClose" type="button">
          Concluir
        </Button>
      </div>
    </AlertDialogContent>
  </AlertDialog>
</template>
