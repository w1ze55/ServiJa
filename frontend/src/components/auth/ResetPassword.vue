<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import Button from "../ui/button.vue";
import Input from "../ui/input.vue";
import Label from "../ui/label.vue";
import Alert from "../ui/alert.vue";
import AlertDescription from "../ui/alert-description.vue";
import {
  AlertCircle,
  ArrowLeft,
  Check,
  CheckCircle,
  Clock,
  Eye,
  EyeOff,
  Lock,
  X,
} from "lucide-vue-next";
import api from "@/api/config/axios";
import { toast } from "@/utils/toast";

interface ResetPasswordProps {
  email: string;
  onSuccess: () => void;
  onBack: () => void;
}

type ResetStep = "code" | "password";

const props = defineProps<ResetPasswordProps>();

const currentStep = ref<ResetStep>("code");
const code = ref<string[]>(["", "", "", "", "", ""]);
const inputRefs = ref<HTMLInputElement[]>([]);

const password = ref("");
const confirmPassword = ref("");
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const errors = ref<{
  code?: string;
  password?: string;
  confirmPassword?: string;
}>({});

const isVerifyingCode = ref(false);
const isSubmitting = ref(false);
const success = ref(false);
const canResend = ref(false);
const timeLeft = ref(60);

const isLocked = ref(false);
const lockEndTime = ref<Date | null>(null);
const lockTimeRemaining = ref(0);
let lockInterval: ReturnType<typeof setInterval> | null = null;

const passwordRequirements = computed(() => ({
  minLength: password.value.length >= 8,
  hasUpperCase: /[A-Z]/.test(password.value),
  hasLowerCase: /[a-z]/.test(password.value),
  hasNumber: /[0-9]/.test(password.value),
  hasSpecial: /[!@#$%^&*(),.?":{}|<>]/.test(password.value),
}));

const isPasswordValid = computed(() =>
  Object.values(passwordRequirements.value).every(Boolean),
);

const isLoading = computed(() => isVerifyingCode.value || isSubmitting.value);
const fullCode = computed(() => code.value.join(""));
const canVerifyCode = computed(
  () => fullCode.value.length === 6 && !isLocked.value && !isLoading.value,
);
const hasCodeError = computed(() => Boolean(errors.value.code));

const maskedEmail = computed(() => {
  const [username, domain] = props.email.split("@");
  if (!domain || username.length <= 3) return props.email;
  return `${username.slice(0, 3)}***@${domain}`;
});

const lockTimeFormatted = computed(() => {
  const minutes = Math.floor(lockTimeRemaining.value / 60);
  const seconds = lockTimeRemaining.value % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
});

watch(
  [timeLeft, canResend],
  ([currentTime, resendEnabled], _, onCleanup) => {
    if (currentTime > 0 && !resendEnabled) {
      const timer = setTimeout(() => (timeLeft.value -= 1), 1000);
      onCleanup(() => clearTimeout(timer));
    } else if (currentTime === 0) {
      canResend.value = true;
    }
  },
  { immediate: true },
);

onMounted(() => {
  focusCodeInput();
});

onUnmounted(() => {
  clearLockTimer();
});

function clearLockTimer() {
  if (lockInterval) {
    clearInterval(lockInterval);
    lockInterval = null;
  }
}

function resetLockoutState() {
  clearLockTimer();
  isLocked.value = false;
  lockEndTime.value = null;
  lockTimeRemaining.value = 0;
}

function startLockoutTimer(lockUntil: string) {
  clearLockTimer();
  isLocked.value = true;
  lockEndTime.value = new Date(
    lockUntil.endsWith("Z") ? lockUntil : `${lockUntil}Z`,
  );

  const updateTimer = () => {
    if (!lockEndTime.value) return;

    const diff = Math.max(
      0,
      Math.floor((lockEndTime.value.getTime() - Date.now()) / 1000),
    );
    lockTimeRemaining.value = diff;

    if (diff <= 0) {
      resetLockoutState();
      errors.value.code = "";
      code.value = ["", "", "", "", "", ""];
      focusCodeInput();
    }
  };

  updateTimer();
  lockInterval = setInterval(updateTimer, 1000);
}

function focusCodeInput(index = 0) {
  nextTick(() => {
    inputRefs.value[index]?.focus();
  });
}

function resetCodeInputs() {
  code.value = ["", "", "", "", "", ""];
}

function handleChange(index: number, value: string) {
  if (!/^\d*$/.test(value)) return;

  const newCode = [...code.value];
  newCode[index] = value.slice(-1);
  code.value = newCode;
  errors.value.code = "";

  if (value && index < 5) {
    focusCodeInput(index + 1);
  }
}

function handleKeyDown(index: number, event: KeyboardEvent) {
  if (event.key === "Backspace" && !code.value[index] && index > 0) {
    focusCodeInput(index - 1);
  }
}

function handlePaste(event: ClipboardEvent) {
  event.preventDefault();

  const pastedData =
    event.clipboardData?.getData("text")?.replace(/\D/g, "").slice(0, 6) || "";

  const newCode = [...code.value];
  for (let i = 0; i < pastedData.length; i += 1) {
    newCode[i] = pastedData[i];
  }
  code.value = newCode;
  errors.value.code = "";

  if (pastedData.length < 6) {
    focusCodeInput(Math.min(pastedData.length, 5));
  }
}

function goToCodeStep(message?: string) {
  currentStep.value = "code";
  errors.value.code = message || "";
  resetCodeInputs();
  focusCodeInput();
}

async function handleVerifyCode() {
  if (fullCode.value.length !== 6) {
    errors.value.code = "Digite o código de 6 digitos";
    return;
  }

  isVerifyingCode.value = true;
  errors.value.code = "";

  try {
    await api.post("/auth/verify-reset-code", {
      email: props.email,
      code: fullCode.value,
    });

    resetLockoutState();
    currentStep.value = "password";
    toast.success("código validado! Agora defina sua nova senha.");
  } catch (err: any) {
    const status = err.response?.status;
    const message =
      err.response?.data?.message ||
      err.response?.data?.errors?.code ||
      "Erro ao validar código";

    if (status === 429 && err.response?.data?.lockUntil) {
      startLockoutTimer(err.response.data.lockUntil);
    }

    errors.value.code = message;
    resetCodeInputs();
    focusCodeInput();
    toast.error(message);
  } finally {
    isVerifyingCode.value = false;
  }
}

async function handleResend() {
  canResend.value = false;
  timeLeft.value = 60;
  errors.value = {};
  currentStep.value = "code";
  resetCodeInputs();
  password.value = "";
  confirmPassword.value = "";
  resetLockoutState();

  try {
    await api.post(
      `/auth/resend-reset-code?email=${encodeURIComponent(props.email)}`,
    );
    toast.success("Novo código enviado!");
  } catch (err: any) {
    const message =
      err.response?.data?.message ||
      "Erro ao reenviar código. Tente novamente.";
    errors.value.code = message;
    toast.error(message);
  } finally {
    focusCodeInput();
  }
}

function validatePassword(): boolean {
  const newErrors: { password?: string; confirmPassword?: string } = {};

  if (!password.value) {
    newErrors.password = "Senha e obrigatoria";
  } else if (password.value.length > 256) {
    newErrors.password = "Senha deve ter no maximo 256 caracteres";
  } else if (!isPasswordValid.value) {
    newErrors.password = "Senha nao atende aos requisitos";
  }

  if (!confirmPassword.value) {
    newErrors.confirmPassword = "Confirmacao de senha e obrigatoria";
  } else if (password.value !== confirmPassword.value) {
    newErrors.confirmPassword = "As senhas nao coincidem";
  }

  errors.value = {
    ...errors.value,
    password: newErrors.password,
    confirmPassword: newErrors.confirmPassword,
  };

  return Object.keys(newErrors).length === 0;
}

async function handleSubmit(event: Event) {
  event.preventDefault();

  if (!validatePassword()) return;

  if (fullCode.value.length !== 6) {
    goToCodeStep("Valide o código novamente para continuar");
    return;
  }

  isSubmitting.value = true;

  try {
    await api.post("/auth/reset-password", {
      email: props.email,
      code: fullCode.value,
      newPassword: password.value,
      confirmPassword: confirmPassword.value,
    });

    success.value = true;
    toast.success("Senha redefinida com sucesso!");
    setTimeout(() => props.onSuccess(), 2000);
  } catch (err: any) {
    const status = err.response?.status;
    const fieldErrors = err.response?.data?.errors || {};
    const message = err.response?.data?.message || "Erro ao redefinir senha";

    if (fieldErrors.newPassword) {
      errors.value.password = fieldErrors.newPassword;
    }
    if (fieldErrors.confirmPassword) {
      errors.value.confirmPassword = fieldErrors.confirmPassword;
    }

    if (status === 429 && err.response?.data?.lockUntil) {
      startLockoutTimer(err.response.data.lockUntil);
      goToCodeStep(message);
    } else if (fieldErrors.code || /código|expirad|inval/i.test(message)) {
      goToCodeStep(fieldErrors.code || message);
    }

    toast.error(
      fieldErrors.code ||
        fieldErrors.newPassword ||
        fieldErrors.confirmPassword ||
        message,
    );
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <div
    v-if="success"
    class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <div class="text-center">
        <div class="inline-flex p-4 bg-green-100 rounded-full mb-4">
          <CheckCircle class="h-8 w-8 text-green-600" />
        </div>
        <h2 class="text-gray-900 mb-2">Senha redefinida!</h2>
        <p class="text-gray-600 mb-4">Sua senha foi alterada com sucesso.</p>
        <p class="text-sm text-gray-500 mb-6">
          Voce sera redirecionado para o login em instantes...
        </p>

        <Alert class="mb-6 bg-blue-50 border-blue-200 text-left">
          <AlertCircle class="h-4 w-4 text-blue-600" />
          <AlertDescription class="text-gray-700">
            Por seguranca, voce precisara fazer login com sua nova senha.
          </AlertDescription>
        </Alert>

        <Button @click="props.onSuccess" class="w-full">
          Ir para o login
        </Button>
      </div>
    </div>
  </div>

  <div
    v-else
    class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <div class="mb-6 flex items-center justify-between gap-4">
        <Button variant="ghost" @click="props.onBack">
          <ArrowLeft class="h-4 w-4" />
          Voltar
        </Button>
        <p class="text-sm font-medium text-gray-500">
          {{ currentStep === "code" ? "Etapa 1 de 2" : "Etapa 2 de 2" }}
        </p>
      </div>
      <div class="text-center mb-6">
        <div class="inline-flex p-4 bg-red-100 rounded-full mb-4">
          <Lock class="h-8 w-8 text-red-600" />
        </div>
        <h1 class="text-3xl text-gray-900 mb-2 font-bold">Redefinir senha</h1>
        <p class="text-gray-600">
          Digite o código enviado para <strong>{{ maskedEmail }}</strong>
        </p>
      </div>

      <div
        v-if="isLocked"
        class="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-lg text-center"
      >
        <Clock class="h-6 w-6 text-amber-600 mx-auto mb-2" />
        <p class="text-amber-800 font-medium mb-1">Muitas tentativas</p>
        <p class="text-amber-700 text-sm mb-3">Aguarde para tentar novamente</p>
        <div class="text-2xl font-mono font-bold text-amber-900 tabular-nums">
          {{ lockTimeFormatted }}
        </div>
      </div>

      <Alert
        v-if="errors.code && currentStep === 'code' && !isLocked"
        variant="destructive"
        class="mb-6"
      >
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ errors.code }}</AlertDescription>
      </Alert>

      <div v-if="currentStep === 'code'">
        <div class="mb-6">
          <p class="text-sm text-gray-600 text-center mb-4">
            Código de recuperação
          </p>

          <div class="flex gap-2 justify-center" @paste="handlePaste">
            <input
              v-for="(digit, index) in code"
              :key="index"
              ref="inputRefs"
              type="text"
              inputmode="numeric"
              maxlength="1"
              :value="digit"
              @input="
                (e) => handleChange(index, (e.target as HTMLInputElement).value)
              "
              @keydown="(e) => handleKeyDown(index, e as KeyboardEvent)"
              :disabled="isLoading || success || isLocked"
              class="w-12 h-14 text-center text-xl font-bold border-2 rounded-lg transition-all focus:outline-none focus:ring-2 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed"
              :class="[
                hasCodeError
                  ? 'border-red-500 hover:border-red-500 focus:ring-red-300'
                  : digit
                    ? 'border-blue-600 hover:border-blue-500 focus:ring-blue-300'
                    : 'border-gray-300 hover:border-blue-300 focus:ring-blue-300',
              ]"
            />
          </div>

          <p class="text-xs text-gray-500 text-center mt-4">
            O código expira em 30 minutos
          </p>
        </div>

        <Button
          class="w-full"
          size="lg"
          :disabled="!canVerifyCode"
          @click="handleVerifyCode"
        >
          {{ isVerifyingCode ? "Validando..." : "Validar código" }}
        </Button>

        <div class="text-center mt-4">
          <p class="text-gray-600 text-sm mb-1">Não recebeu o código?</p>
          <Button
            v-if="canResend"
            variant="link"
            size="sm"
            @click="handleResend"
            :disabled="isLoading"
          >
            Reenviar código
          </Button>
          <p v-else class="text-sm text-gray-500">
            Reenviar código em {{ timeLeft }}s
          </p>
        </div>
      </div>

      <form v-else @submit="handleSubmit" class="space-y-4 border-t pt-6">
        <div class="rounded-lg border border-green-200 bg-green-50 px-4 py-3">
          <p
            class="text-sm text-green-700 flex items-center justify-center gap-2"
          >
            <Check class="h-4 w-4" />
            código verificado. Agora defina sua nova senha.
          </p>
        </div>

        <Alert v-if="errors.code && !isLocked" variant="destructive">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ errors.code }}</AlertDescription>
        </Alert>

        <div class="space-y-2">
          <Label for="password">Nova senha *</Label>
          <div class="relative">
            <Input
              id="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Digite sua nova senha"
              v-model="password"
              @input="errors.password = ''"
              :class="errors.password ? 'border-red-500 pr-10' : 'pr-10'"
              maxlength="256"
              :disabled="isLoading"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
              tabindex="-1"
            >
              <component :is="showPassword ? EyeOff : Eye" class="h-4 w-4" />
            </button>
          </div>

          <div v-if="password" class="mt-3 p-3 bg-gray-50 rounded-md space-y-2">
            <p class="text-sm text-gray-700">A senha deve conter:</p>
            <div
              class="flex items-center gap-2 text-sm"
              :class="
                passwordRequirements.minLength
                  ? 'text-green-600'
                  : 'text-gray-500'
              "
            >
              <component
                :is="passwordRequirements.minLength ? Check : X"
                class="h-3 w-3"
              />
              <span>Minimo 8 caracteres</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="
                passwordRequirements.hasUpperCase
                  ? 'text-green-600'
                  : 'text-gray-500'
              "
            >
              <component
                :is="passwordRequirements.hasUpperCase ? Check : X"
                class="h-3 w-3"
              />
              <span>1 letra maiuscula</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="
                passwordRequirements.hasLowerCase
                  ? 'text-green-600'
                  : 'text-gray-500'
              "
            >
              <component
                :is="passwordRequirements.hasLowerCase ? Check : X"
                class="h-3 w-3"
              />
              <span>1 letra minuscula</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="
                passwordRequirements.hasNumber
                  ? 'text-green-600'
                  : 'text-gray-500'
              "
            >
              <component
                :is="passwordRequirements.hasNumber ? Check : X"
                class="h-3 w-3"
              />
              <span>1 numero</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="
                passwordRequirements.hasSpecial
                  ? 'text-green-600'
                  : 'text-gray-500'
              "
            >
              <component
                :is="passwordRequirements.hasSpecial ? Check : X"
                class="h-3 w-3"
              />
              <span>1 caractere especial</span>
            </div>
          </div>

          <p
            v-if="errors.password"
            class="text-sm text-red-500 flex items-center gap-1"
          >
            <AlertCircle class="h-3 w-3" />
            {{ errors.password }}
          </p>
        </div>

        <div class="space-y-2">
          <Label for="confirmPassword">Confirmar nova senha *</Label>
          <div class="relative">
            <Input
              id="confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              placeholder="Digite novamente sua senha"
              v-model="confirmPassword"
              @input="errors.confirmPassword = ''"
              :class="errors.confirmPassword ? 'border-red-500 pr-10' : 'pr-10'"
              maxlength="256"
              :disabled="isLoading"
            />
            <button
              type="button"
              @click="showConfirmPassword = !showConfirmPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
              tabindex="-1"
            >
              <component
                :is="showConfirmPassword ? EyeOff : Eye"
                class="h-4 w-4"
              />
            </button>
          </div>

          <p
            v-if="errors.confirmPassword"
            class="text-sm text-red-500 flex items-center gap-1"
          >
            <AlertCircle class="h-3 w-3" />
            {{ errors.confirmPassword }}
          </p>

          <p
            v-if="confirmPassword && password === confirmPassword"
            class="text-sm text-green-600 flex items-center gap-1"
          >
            <Check class="h-3 w-3" />
            As senhas coincidem
          </p>
        </div>

        <Button type="submit" class="w-full" size="lg" :disabled="isLoading">
          {{ isSubmitting ? "Redefinindo..." : "Redefinir senha" }}
        </Button>

      </form>
    </div>
  </div>
</template>
