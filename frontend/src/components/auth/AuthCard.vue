<template>
  <div class="auth-card-container">
    <div class="auth-card-wrapper">
      <Button
        v-if="onBackToLanding"
        variant="ghost"
        @click="onBackToLanding"
        class="mb-4"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar para a página inicial
      </Button>

      <div class="auth-card-main">
        <div class="auth-card-grid">
          <div
            class="auth-card-form-section"
            :class="[isSignUp ? 'hidden-mobile' : 'visible']"
          >
            <div class="auth-card-form-container">
              <h2 class="auth-card-form-title">Entrar</h2>
              <p class="auth-card-form-subtitle">
                Bem-vindo de volta ao Servi Já
              </p>

              <form @submit.prevent="handleLoginSubmit" class="space-y-4">
                <Alert v-if="loginError" variant="destructive">
                  <AlertCircle class="h-4 w-4" />
                  <AlertDescription>{{ loginError }}</AlertDescription>
                </Alert>

                <div>
                  <Label for="login-email">Email</Label>
                  <Input
                    id="login-email"
                    type="email"
                    placeholder="seu@email.com"
                    v-model="loginEmail"
                    :disabled="isLoggingIn"
                  />
                </div>

                <div>
                  <Label for="login-password">Senha</Label>
                  <div class="relative">
                    <Input
                      id="login-password"
                      :type="showPassword ? 'text' : 'password'"
                      placeholder="••••••••"
                      v-model="loginPassword"
                      :disabled="isLoggingIn"
                    />
                    <button
                      type="button"
                      @click="showPassword = !showPassword"
                      class="auth-card-password-toggle"
                    >
                      <component
                        :is="showPassword ? EyeOff : Eye"
                        class="h-4 w-4"
                      />
                    </button>
                  </div>
                </div>

                <div class="auth-card-forgot-password">
                  <button type="button" @click="onForgotPassword">
                    Esqueci minha senha
                  </button>
                </div>

                <Button
                  type="submit"
                  class="w-full"
                  size="lg"
                  :disabled="isLoggingIn"
                >
                  {{ isLoggingIn ? "Entrando..." : "Entrar" }}
                </Button>

                <div class="md:hidden text-center mt-6">
                  <p class="auth-card-switch-text">
                    Não tem uma conta?
                    <Button
                      type="button"
                      variant="link"
                      @click="handleSwitchToSignup"
                    >
                      Cadastre-se
                    </Button>
                  </p>
                </div>
              </form>
            </div>
          </div>

          <div
            class="auth-card-signup-section"
            :class="[!isSignUp ? 'hidden-mobile' : 'visible']"
          >
            <div class="w-full max-w-full md:max-w-2xl mx-auto my-auto">
              <div v-if="isSignUp" class="mb-6">
                <div class="auth-card-progress-header">
                  <Button
                    v-if="signupStep !== 'type'"
                    variant="ghost"
                    size="sm"
                    @click="handlePreviousSignupStep"
                  >
                    <ArrowLeft class="h-4 w-4" />
                    Voltar
                  </Button>
                  <div class="flex items-center gap-2">
                    <h2 class="text-gray-900 text-center md:text-left">
                      Criar conta
                    </h2>
                  </div>
                  <span class="auth-card-progress-step">
                    Passo {{ getSignupStepNumber() }} de 4
                  </span>
                </div>
                <Progress :value="getSignupProgress()" class="h-2" />
              </div>

              <template v-if="signupStep === 'type'">
                <p class="text-gray-600 mb-6 text-center md:text-left">
                  Escolha como deseja usar o Servi Já
                </p>

                <form
                  @submit.prevent="handleAccountTypeSelect"
                  class="space-y-4"
                >
                  <div class="space-y-3">
                    <button
                      v-for="account in accountTypes"
                      :key="account.type"
                      type="button"
                      @click="selectedAccountType = account.type"
                      class="w-full p-4 rounded-lg border-2 transition-all text-left flex items-center gap-4"
                      :class="
                        selectedAccountType === account.type
                          ? 'border-blue-600 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      "
                    >
                      <div
                        class="auth-card-account-icon"
                        :class="{ selected: selectedAccountType === account.type }"
                      >
                        <component
                          :is="account.icon"
                          class="h-6 w-6"
                          :class="{ selected: selectedAccountType === account.type }"
                        />
                      </div>
                      <div class="flex-1">
                        <h4 class="auth-card-account-title">
                          {{ account.title }}
                        </h4>
                        <p class="auth-card-account-description">
                          {{ account.description }}
                        </p>
                      </div>
                    </button>
                  </div>

                  <Button
                    type="submit"
                    class="w-full"
                    size="lg"
                    :disabled="!selectedAccountType"
                  >
                    Continuar
                  </Button>

                  <div class="md:hidden text-center mt-6">
                    <p class="text-gray-600">
                      Já tem uma conta?
                      <Button
                        type="button"
                        variant="link"
                        @click="handleSwitchToLogin"
                      >
                        Entrar
                      </Button>
                    </p>
                  </div>
                </form>
              </template>

              <template v-else-if="signupStep === 'data' && selectedAccountType">
                <div class="w-full">
                  <div class="mb-6">
                    <h3 class="text-gray-900 mb-2">Dados pessoais</h3>
                    <p class="text-gray-600">
                      Preencha seus dados para criar sua conta de
                      {{
                        selectedAccountType === "contractor"
                          ? "Contratante"
                          : "Freelancer"
                      }}
                    </p>
                  </div>

                  <SignupPersonalData
                    :account-type="selectedAccountType"
                    :inline="true"
                    :initial-data="formData"
                    @next="handlePersonalDataSubmit"
                    @back="signupStep = 'type'"
                  />
                </div>
              </template>

              <template v-else-if="signupStep === 'address'">
                <div class="w-full">
                  <div class="mb-6">
                    <h3 class="text-gray-900 mb-2">Endereço</h3>
                    <p class="text-gray-600">
                      Informe o endereço principal que ficará salvo na sua conta.
                    </p>
                  </div>

                  <SignupAddress
                    :inline="true"
                    :initial-data="addressData"
                    @next="handleAddressSubmit"
                    @back="signupStep = 'data'"
                  />
                </div>
              </template>

              <template
                v-else-if="
                  signupStep === 'confirmation' &&
                  selectedAccountType &&
                  formData
                "
              >
                <div class="w-full">
                  <SignupConfirmation
                    :account-type="selectedAccountType"
                    :personal-data="formData"
                    :address-data="addressData"
                    :inline="true"
                    @confirm="handleSignupConfirm"
                    @back="signupStep = 'address'"
                  />
                </div>
              </template>
            </div>
          </div>

          <div
            class="auth-card-overlay"
            :class="[isSignUp ? 'signup' : 'login']"
          >
            <div class="auth-card-overlay-content">
              <Transition name="fade-slide" mode="out-in">
                <div v-if="!isSignUp" key="signup">
                  <h2 class="auth-card-overlay-title">Bem-vindo de volta!</h2>
                  <p class="auth-card-overlay-text">
                    Faça login usando suas credenciais
                  </p>
                  <Button
                    variant="outline"
                    size="lg"
                    @click="handleSwitchToSignup"
                    class="auth-card-overlay-button"
                  >
                    Cadastre-se
                  </Button>
                </div>

                <div v-else key="login">
                  <h2 class="auth-card-overlay-title">Seja bem-vindo(a)!</h2>
                  <p class="auth-card-overlay-text">
                    Cadastre-se para encontrar os melhores profissionais ou
                    oferecer seus serviços de forma simples e rápida.
                  </p>
                  <Button
                    variant="outline"
                    size="lg"
                    @click="handleSwitchToLogin"
                    class="auth-card-overlay-button"
                  >
                    Entrar
                  </Button>
                </div>
              </Transition>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import Alert from "../ui/alert.vue";
import AlertDescription from "../ui/alert-description.vue";
import Input from "../ui/input.vue";
import Button from "../ui/button.vue";
import Label from "../ui/label.vue";
import Progress from "../ui/progress.vue";
import SignupPersonalData from "./SignupPersonalData.vue";
import SignupAddress from "./SignupAddress.vue";
import SignupConfirmation from "./SignupConfirmation.vue";
import {
  AlertCircle,
  Eye,
  EyeOff,
  ArrowLeft,
  UserCircle,
  Briefcase,
} from "lucide-vue-next";

export interface PersonalData {
  name: string;
  email: string;
  phone: string;
  birthDate: string;
  cpf: string;
  password: string;
  acceptTerms: boolean;
}

export interface AddressData {
  label: string;
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
  isDefault: boolean;
}

const props = defineProps<{
  onLogin: (
    email: string,
    password: string,
    onError?: (error: string) => void,
  ) => void;
  onSignupComplete: (
    accountType: "contractor" | "provider",
    personalData: PersonalData,
  ) => void;
  onForgotPassword: () => void;
  onBackToLanding?: () => void;
  initialPage?: "login" | "signup";
}>();

const isSignUp = ref(props.initialPage === "signup");
const signupStep = ref<"type" | "data" | "address" | "confirmation">("type");
const showPassword = ref(false);

const loginEmail = ref("");
const loginPassword = ref("");
const loginError = ref("");
const isLoggingIn = ref(false);

const selectedAccountType = ref<"contractor" | "provider" | null>(null);

const formData = reactive<PersonalData>({
  name: "",
  email: "",
  phone: "",
  birthDate: "",
  cpf: "",
  password: "",
  acceptTerms: false,
});

const addressData = reactive<AddressData>({
  label: "Principal",
  zipCode: "",
  street: "",
  number: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: "",
  isDefault: true,
});

function handleLoginSubmit() {
  const email = loginEmail.value?.trim() || "";
  const password = loginPassword.value?.trim() || "";

  if (!email || !password) {
    loginError.value = "Por favor, preencha todos os campos";
    return;
  }

  loginError.value = "";
  isLoggingIn.value = true;

  props.onLogin(email, password, (error: string) => {
    loginError.value = error;
    isLoggingIn.value = false;
  });

  setTimeout(() => {
    if (!loginError.value) {
      isLoggingIn.value = false;
    }
  }, 600);
}

function handleSwitchToSignup() {
  isSignUp.value = true;
  signupStep.value = "type";
  selectedAccountType.value = null;
}

function handleSwitchToLogin() {
  isSignUp.value = false;
  signupStep.value = "type";
}

function handlePreviousSignupStep() {
  if (signupStep.value === "confirmation") {
    signupStep.value = "address";
    return;
  }

  if (signupStep.value === "address") {
    signupStep.value = "data";
    return;
  }

  if (signupStep.value === "data") {
    signupStep.value = "type";
  }
}

function handleAccountTypeSelect() {
  if (selectedAccountType.value) {
    signupStep.value = "data";
  }
}

function handlePersonalDataSubmit(data: PersonalData) {
  Object.assign(formData, data);
  signupStep.value = "address";
}

function handleAddressSubmit(data: AddressData) {
  Object.assign(addressData, data);
  signupStep.value = "confirmation";
}

function handleSignupConfirm() {
  if (selectedAccountType.value) {
    props.onSignupComplete(selectedAccountType.value, formData);
  }
}

function getSignupProgress() {
  return signupStep.value === "type"
    ? 25
    : signupStep.value === "data"
      ? 50
      : signupStep.value === "address"
        ? 75
        : 100;
}

function getSignupStepNumber() {
  return signupStep.value === "type"
    ? "1"
    : signupStep.value === "data"
      ? "2"
      : signupStep.value === "address"
        ? "3"
        : "4";
}

const accountTypes: Array<{
  type: "contractor" | "provider";
  title: string;
  icon: any;
  description: string;
}> = [
  {
    type: "contractor",
    title: "Contratante",
    icon: UserCircle,
    description: "Preciso contratar serviços",
  },
  {
    type: "provider",
    title: "Prestador",
    icon: Briefcase,
    description: "Quero oferecer meus serviços",
  },
];
</script>

<style scoped>
@import "@/styles/auth/auth-card.css";
</style>
