<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Lock,
  Shield,
  Trash2,
  Eye,
  EyeOff,
  CheckCircle,
  AlertTriangle,
  Smartphone,
  Key,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Checkbox from '../ui/checkbox.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import { toast } from '../../utils/toast';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  onLogout: () => void;
}

const props = defineProps<Props>();

const twoFactorEnabled = ref(false);
const showPasswordDialog = ref(false);
const showTwoFactorDialog = ref(false);
const showDeleteDialog = ref(false);
const showConfirmDeleteDialog = ref(false);

// Password change form
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

// 2FA setup
const verificationCode = ref('');
const twoFactorCode = ref('ABCD-1234-EFGH-5678');

// Delete account
const deleteReason = ref('');
const confirmEmail = ref('');
const confirmPasswordDelete = ref('');
const agreeToTerms = ref(false);

const handleChangePassword = () => {
  if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
    toast.error('Preencha todos os campos');
    return;
  }

  if (newPassword.value !== confirmPassword.value) {
    toast.error('As senhas não coincidem');
    return;
  }

  if (newPassword.value.length < 8) {
    toast.error('A senha deve ter no mínimo 8 caracteres');
    return;
  }

  // Simular mudança de senha
  toast.success('Senha alterada com sucesso!');
  showPasswordDialog.value = false;
  currentPassword.value = '';
  newPassword.value = '';
  confirmPassword.value = '';
};

const handleEnable2FA = () => {
  if (!verificationCode.value || verificationCode.value.length !== 6) {
    toast.error('Digite o código de 6 dígitos');
    return;
  }

  // Simular ativação do 2FA
  twoFactorEnabled.value = true;
  showTwoFactorDialog.value = false;
  verificationCode.value = '';
  toast.success('Autenticação em duas etapas ativada com sucesso!');
};

const handleDisable2FA = () => {
  twoFactorEnabled.value = false;
  toast.success('Autenticação em duas etapas desativada');
};

const handleRequestDeleteAccount = () => {
  showDeleteDialog.value = true;
};

const handleConfirmDelete = () => {
  if (!deleteReason.value) {
    toast.error('Por favor, informe o motivo da exclusão');
    return;
  }
  showDeleteDialog.value = false;
  showConfirmDeleteDialog.value = true;
};

const handleFinalDeleteAccount = () => {
  if (!confirmEmail.value || !confirmPasswordDelete.value) {
    toast.error('Preencha todos os campos de confirmação');
    return;
  }

  if (confirmEmail.value !== 'joao.silva@email.com') {
    toast.error('E-mail incorreto');
    return;
  }

  if (!agreeToTerms.value) {
    toast.error('Você deve concordar com os termos de exclusão');
    return;
  }

  // Simular exclusão
  toast.success('Solicitação de exclusão enviada. Você receberá uma confirmação por e-mail.');
  showConfirmDeleteDialog.value = false;
  
  // Fazer logout após 2 segundos
  setTimeout(() => {
    props.onLogout();
  }, 2000);
};

const passwordStrength = (password: string) => {
  let strength = 0;
  if (password.length >= 8) strength++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++;
  if (/\d/.test(password)) strength++;
  if (/[^a-zA-Z\d]/.test(password)) strength++;
  return strength;
};

const getPasswordStrengthLabel = (strength: number) => {
  if (strength === 0) return { label: 'Muito fraca', color: 'bg-red-500' };
  if (strength === 1) return { label: 'Fraca', color: 'bg-orange-500' };
  if (strength === 2) return { label: 'Média', color: 'bg-yellow-500' };
  if (strength === 3) return { label: 'Forte', color: 'bg-green-500' };
  return { label: 'Muito forte', color: 'bg-green-600' };
};

const strength = computed(() => passwordStrength(newPassword.value));
const strengthData = computed(() => getPasswordStrengthLabel(strength.value));
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Configurações de Conta</h1>
      <p class="text-[#435574]">Gerencie sua senha, segurança e privacidade</p>
    </div>

    <!-- Change Password -->
    <Card class="p-6">
      <div class="flex items-start gap-4">
        <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
          <Lock class="h-6 w-6 text-[#286AD8]" />
        </div>
        <div class="flex-1">
          <h3 class="text-[#263238] mb-2">Alterar Senha</h3>
          <p class="text-sm text-[#435574] mb-4">
            Recomendamos alterar sua senha regularmente para manter sua conta segura
          </p>
          <Button
            @click="showPasswordDialog = true"
            variant="outline"
            class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
          >
            <Key class="h-4 w-4 mr-2" />
            Alterar Senha
          </Button>
        </div>
      </div>
    </Card>

    <!-- Two-Factor Authentication -->
    <Card class="p-6">
      <div class="flex items-start gap-4">
        <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
          <Shield class="h-6 w-6 text-green-600" />
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-2">
            <h3 class="text-[#263238]">Autenticação em Duas Etapas (2FA)</h3>
            <Badge v-if="twoFactorEnabled" class="bg-green-100 text-green-700 hover:bg-green-100">
              <CheckCircle class="h-3 w-3 mr-1" />
              Ativado
            </Badge>
          </div>
          <p class="text-sm text-[#435574] mb-4">
            Adicione uma camada extra de segurança exigindo um código do seu celular além da
            senha
          </p>
          <div v-if="twoFactorEnabled" class="space-y-3">
            <div class="p-3 bg-green-50 border border-green-200 rounded-lg">
              <div class="flex items-center gap-2">
                <Smartphone class="h-4 w-4 text-green-600" />
                <p class="text-sm text-green-800">
                  A autenticação em duas etapas está protegendo sua conta
                </p>
              </div>
            </div>
            <Button @click="handleDisable2FA" variant="outline" size="sm">
              Desativar 2FA
            </Button>
          </div>
          <Button
            v-else
            @click="showTwoFactorDialog = true"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            <Shield class="h-4 w-4 mr-2" />
            Ativar 2FA
          </Button>
        </div>
      </div>
    </Card>

    <!-- Delete Account -->
    <Card class="p-6 border-red-200">
      <div class="flex items-start gap-4">
        <div class="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
          <Trash2 class="h-6 w-6 text-red-600" />
        </div>
        <div class="flex-1">
          <h3 class="text-[#263238] mb-2">Excluir Conta</h3>
          <p class="text-sm text-[#435574] mb-4">
            A exclusão da conta é permanente e irreversível. Todos os seus dados serão removidos
            conforme a LGPD.
          </p>
          <div class="p-3 bg-red-50 border border-red-200 rounded-lg mb-4">
            <div class="flex items-start gap-2">
              <AlertTriangle class="h-4 w-4 text-red-600 flex-shrink-0 mt-0.5" />
              <p class="text-sm text-red-800">
                Ao excluir sua conta, você perderá permanentemente:
              </p>
            </div>
            <ul class="text-sm text-red-700 mt-2 ml-6 space-y-1 list-disc">
              <li>Seu perfil e avaliações</li>
              <li>Histórico de serviços</li>
              <li>Conversas e propostas</li>
              <li>Dados de verificação</li>
            </ul>
          </div>
          <Button
            @click="handleRequestDeleteAccount"
            variant="outline"
            class="border-red-600 text-red-600 hover:bg-red-50"
          >
            <Trash2 class="h-4 w-4 mr-2" />
            Excluir Minha Conta
          </Button>
        </div>
      </div>
    </Card>

    <!-- Activity Logs Info -->
    <Card class="p-6 bg-gray-50">
      <div class="flex items-start gap-3">
        <AlertTriangle class="h-5 w-5 text-[#435574] flex-shrink-0 mt-0.5" />
        <div>
          <h4 class="text-[#263238] mb-2">Logs de Atividade</h4>
          <p class="text-sm text-[#435574]">
            Todas as alterações de configurações são registradas com data, hora e dispositivo
            para sua segurança. Em caso de atividade suspeita, você será notificado
            imediatamente.
          </p>
        </div>
      </div>
    </Card>

    <!-- Change Password Dialog -->
    <Dialog :open="showPasswordDialog" @update:open="(val) => showPasswordDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Alterar Senha</DialogTitle>
          <DialogDescription>
            Insira sua senha atual e escolha uma nova senha forte
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="current-password">Senha Atual *</Label>
            <div class="relative">
              <Input
                id="current-password"
                :type="showCurrentPassword ? 'text' : 'password'"
                :model-value="currentPassword"
                @update:model-value="(val) => currentPassword = val as string"
                placeholder="Digite sua senha atual"
              />
              <button
                type="button"
                @click="showCurrentPassword = !showCurrentPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[#435574] hover:text-[#263238]"
              >
                <EyeOff v-if="showCurrentPassword" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
          </div>

          <div class="space-y-2">
            <Label for="new-password">Nova Senha *</Label>
            <div class="relative">
              <Input
                id="new-password"
                :type="showNewPassword ? 'text' : 'password'"
                :model-value="newPassword"
                @update:model-value="(val) => newPassword = val as string"
                placeholder="Digite a nova senha"
              />
              <button
                type="button"
                @click="showNewPassword = !showNewPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[#435574] hover:text-[#263238]"
              >
                <EyeOff v-if="showNewPassword" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
            <div v-if="newPassword" class="space-y-1">
              <div class="flex items-center justify-between text-xs">
                <span class="text-[#435574]">Força da senha:</span>
                <span :class="`font-medium ${strengthData.color.replace('bg-', 'text-')}`">
                  {{ strengthData.label }}
                </span>
              </div>
              <div class="h-1 w-full bg-gray-200 rounded-full overflow-hidden">
                <div
                  :class="`h-full ${strengthData.color} transition-all`"
                  :style="{ width: `${(strength / 4) * 100}%` }"
                />
              </div>
            </div>
            <p class="text-xs text-[#435574]">
              Mínimo 8 caracteres, incluindo letras, números e símbolos
            </p>
          </div>

          <div class="space-y-2">
            <Label for="confirm-password">Confirmar Nova Senha *</Label>
            <div class="relative">
              <Input
                id="confirm-password"
                :type="showConfirmPassword ? 'text' : 'password'"
                :model-value="confirmPassword"
                @update:model-value="(val) => confirmPassword = val as string"
                placeholder="Digite novamente a nova senha"
              />
              <button
                type="button"
                @click="showConfirmPassword = !showConfirmPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[#435574] hover:text-[#263238]"
              >
                <EyeOff v-if="showConfirmPassword" class="h-4 w-4" />
                <Eye v-else class="h-4 w-4" />
              </button>
            </div>
            <p v-if="confirmPassword && confirmPassword !== newPassword" class="text-xs text-red-600">
              As senhas não coincidem
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showPasswordDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleChangePassword" class="bg-[#286AD8] hover:bg-[#435574]">
            Alterar Senha
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- 2FA Setup Dialog -->
    <Dialog :open="showTwoFactorDialog" @update:open="(val) => showTwoFactorDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Ativar Autenticação em Duas Etapas</DialogTitle>
          <DialogDescription>
            Escaneie o QR Code com seu aplicativo autenticador
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <!-- QR Code Placeholder -->
          <div class="p-8 bg-gray-100 rounded-lg flex flex-col items-center justify-center">
            <div class="w-48 h-48 bg-white border-4 border-gray-300 rounded-lg flex items-center justify-center mb-4">
              <p class="text-sm text-gray-400 text-center px-4">QR Code do 2FA</p>
            </div>
            <p class="text-sm text-[#435574] text-center">
              Use o Google Authenticator ou similar
            </p>
          </div>

          <div class="space-y-2">
            <Label>Código Manual</Label>
            <div class="p-3 bg-gray-100 rounded-lg">
              <p class="text-sm text-center font-mono text-[#263238]">{{ twoFactorCode }}</p>
            </div>
          </div>

          <div class="space-y-2">
            <Label for="verification-code">Código de Verificação *</Label>
            <Input
              id="verification-code"
              :model-value="verificationCode"
              @update:model-value="(val) => verificationCode = (val as string).replace(/\D/g, '')"
              placeholder="000000"
              maxlength="6"
              class="text-center text-lg tracking-widest"
            />
            <p class="text-xs text-[#435574]">
              Digite o código de 6 dígitos do aplicativo
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showTwoFactorDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleEnable2FA" class="bg-[#286AD8] hover:bg-[#435574]">
            Ativar 2FA
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Delete Account - Step 1 -->
    <AlertDialog :open="showDeleteDialog" @update:open="(val) => showDeleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Você tem certeza?</AlertDialogTitle>
          <AlertDialogDescription>
            Esta ação não pode ser desfeita. Por favor, nos conte o motivo da exclusão para
            melhorarmos nossos serviços.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div class="space-y-2">
          <Label for="delete-reason">Motivo da Exclusão</Label>
          <textarea
            id="delete-reason"
            :model-value="deleteReason"
            @update:model-value="(val: string) => deleteReason = val"
            placeholder="Digite o motivo..."
            class="w-full min-h-[100px] px-3 py-2 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-[#286AD8]"
          />
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleConfirmDelete"
            class="bg-red-600 hover:bg-red-700"
          >
            Continuar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Delete Account - Step 2 (Final Confirmation) -->
    <AlertDialog :open="showConfirmDeleteDialog" @update:open="(val) => showConfirmDeleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmação Final</AlertDialogTitle>
          <AlertDialogDescription>
            Para confirmar a exclusão, digite seu e-mail e senha.
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="confirm-email">E-mail *</Label>
            <Input
              id="confirm-email"
              type="email"
              :model-value="confirmEmail"
              @update:model-value="(val) => confirmEmail = val as string"
              placeholder="seu@email.com"
            />
          </div>

          <div class="space-y-2">
            <Label for="confirm-password-delete">Senha *</Label>
            <Input
              id="confirm-password-delete"
              type="password"
              :model-value="confirmPasswordDelete"
              @update:model-value="(val) => confirmPasswordDelete = val as string"
              placeholder="Digite sua senha"
            />
          </div>

          <div class="flex items-start gap-2">
            <Checkbox
              id="agree-terms"
              :checked="agreeToTerms"
              @update:checked="(val) => agreeToTerms = val as boolean"
            />
            <label for="agree-terms" class="text-sm text-[#435574] cursor-pointer">
              Entendo que esta ação é irreversível e todos os meus dados serão permanentemente
              excluídos conforme a LGPD
            </label>
          </div>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleFinalDeleteAccount"
            class="bg-red-600 hover:bg-red-700"
          >
            Excluir Definitivamente
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

