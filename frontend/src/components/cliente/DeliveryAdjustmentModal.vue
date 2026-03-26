<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { AlertCircle, MessageSquare } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';

interface Props {
  open?: boolean;
  serviceName?: string;
  adjustmentAlreadyUsed?: boolean;
  isLoading?: boolean;
  onSubmit: (reason: string) => boolean | void | Promise<boolean | void>;
  onGoToChat?: () => void;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isOpen = computed({
  get: () => props.open ?? false,
  set: (value) => emit('update:open', value),
});

const adjustmentUsed = ref(props.adjustmentAlreadyUsed ?? false);
const adjustmentsReason = ref('');
const showConfirmationDialog = ref(false);
const isSubmitting = ref(false);

const isBusy = computed(() => Boolean(props.isLoading) || isSubmitting.value);
const dialogDescription = computed(() =>
  props.serviceName
    ? `Descreva os ajustes necessarios para a entrega de ${props.serviceName}.`
    : 'Descreva os ajustes necessarios para a entrega.'
);

watch(
  () => props.adjustmentAlreadyUsed,
  (newValue) => {
    adjustmentUsed.value = Boolean(newValue);
  }
);

watch(isOpen, (open) => {
  if (!open) {
    adjustmentsReason.value = '';
    showConfirmationDialog.value = false;
  }
});

const handleOpenChange = (value: boolean) => {
  isOpen.value = value;
};

const closeModal = () => {
  isOpen.value = false;
};

const handleSubmit = async () => {
  if (!adjustmentsReason.value.trim() || isBusy.value) return;

  showConfirmationDialog.value = false;
  isSubmitting.value = true;

  try {
    const result = await props.onSubmit(adjustmentsReason.value.trim());

    if (result !== false) {
      adjustmentUsed.value = true;
      adjustmentsReason.value = '';
      isOpen.value = false;
    }
  } finally {
    isSubmitting.value = false;
  }
};

const handleGoToChat = () => {
  props.onGoToChat?.();
  isOpen.value = false;
};
</script>

<template>
  <Dialog :open="isOpen" @update:open="handleOpenChange">
    <DialogContent class="sm:max-w-xl">
      <DialogHeader>
        <DialogTitle>Solicitar Ajustes</DialogTitle>
        <DialogDescription>
          {{ dialogDescription }}
        </DialogDescription>
      </DialogHeader>

      <div
        v-if="adjustmentUsed"
        class="rounded-lg border border-yellow-300 bg-yellow-50 p-4 text-sm text-[#435574]"
      >
        <div class="flex items-start gap-3">
          <AlertCircle class="mt-0.5 h-5 w-5 flex-shrink-0 text-yellow-600" />
          <div class="space-y-3">
            <div>
              <p class="font-medium text-[#263238]">Solicitacao de ajuste ja enviada</p>
              <p>
                Voce ja utilizou sua unica solicitacao de ajuste para esta entrega.
                Se precisar alinhar novos detalhes, continue a conversa pelo chat.
              </p>
            </div>

            <DialogFooter class="sm:justify-between">
              <Button
                v-if="props.onGoToChat"
                @click="handleGoToChat"
                variant="outline"
                class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
              >
                <MessageSquare class="mr-2 h-4 w-4" />
                Ir para o Chat
              </Button>
              <Button @click="closeModal" variant="outline">
                Fechar
              </Button>
            </DialogFooter>
          </div>
        </div>
      </div>

      <template v-else>
        <div class="rounded-lg border border-yellow-200 bg-yellow-50 p-4 text-sm text-[#435574]">
          <div class="flex items-start gap-3">
            <AlertCircle class="mt-0.5 h-5 w-5 flex-shrink-0 text-yellow-600" />
            <div class="space-y-2">
              <p class="font-medium text-[#263238]">Revise a entrega com cuidado</p>
              <p>
                Explique com clareza o que precisa ser ajustado para que o profissional
                consiga corrigir a entrega sem ambiguidades.
              </p>
              <p class="text-xs font-medium text-yellow-700">
                Atencao: voce pode solicitar ajustes apenas uma vez por entrega.
              </p>
            </div>
          </div>
        </div>

        <Textarea
          v-model="adjustmentsReason"
          :rows="5"
          placeholder="Descreva os ajustes necessarios..."
        />

        <DialogFooter>
          <Button @click="closeModal" variant="outline">
            Cancelar
          </Button>
          <Button
            @click="showConfirmationDialog = true"
            :disabled="!adjustmentsReason.trim() || isBusy"
            class="bg-yellow-600 hover:bg-yellow-700"
          >
            Enviar Solicitacao
          </Button>
        </DialogFooter>
      </template>
    </DialogContent>
  </Dialog>

  <AlertDialog v-model:open="showConfirmationDialog">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>Confirmar Solicitacao de Ajuste</AlertDialogTitle>
        <AlertDialogDescription class="space-y-3">
          <span class="block">
            Voce tem direito a apenas <strong>uma unica solicitacao de ajuste</strong>.
            Depois do envio, nao sera possivel solicitar novos ajustes por aqui.
          </span>
          <span class="block">
            Se precisar alinhar novas alteracoes no futuro, voce e o freelancer
            deverao continuar pelo <strong>chat</strong>.
          </span>
          <span class="block font-medium text-yellow-700">
            Deseja realmente enviar esta solicitacao?
          </span>
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel @click="showConfirmationDialog = false">
          Voltar e Revisar
        </AlertDialogCancel>
        <AlertDialogAction
          @click="handleSubmit"
          :disabled="isBusy"
          class="bg-yellow-600 hover:bg-yellow-700"
        >
          Sim, Enviar Solicitacao
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
