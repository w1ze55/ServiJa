<script setup lang="ts">
import { computed, ref } from 'vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Checkbox from '../ui/checkbox.vue';
import Alert from '../ui/alert.vue';
import AlertDescription from '../ui/alert-description.vue';
import Progress from '../ui/progress.vue';
import { AlertCircle, Eye, EyeOff, Check, X, ArrowLeft } from 'lucide-vue-next';
import { formatCPF, formatPhone, validateCPF, validatePhone } from '@/utils/masks';
import api from '@/api/config/axios';

export interface PersonalData {
  name: string;
  email: string;
  phone: string;
  birthDate: string;
  cpf: string;
  password: string;
  acceptTerms: boolean;
}

interface Props {
  accountType: 'contractor' | 'provider';
  inline?: boolean;
}

interface Emits {
  (e: 'next', data: PersonalData): void;
  (e: 'back'): void;
}

interface ValidationErrorResponse {
  errors?: Record<string, string>;
  message?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const formData = ref<PersonalData>({
  name: '',
  email: '',
  phone: '',
  birthDate: '',
  cpf: '',
  password: '',
  acceptTerms: false,
});

const showPassword = ref(false);
const touched = ref<Record<string, boolean>>({});
const errors = ref<Record<string, string>>({});
const serverError = ref('');
const isLoading = ref(false);

const formatDateForInput = (date: Date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
};

const maxBirthDate = computed(() => {
  return formatDateForInput(new Date());
});

const isAtLeast18YearsOld = (value: string) => {
  const birthDate = new Date(`${value}T00:00:00`);

  if (Number.isNaN(birthDate.getTime())) {
    return false;
  }

  const minimumBirthDate = new Date();
  minimumBirthDate.setHours(0, 0, 0, 0);
  minimumBirthDate.setFullYear(minimumBirthDate.getFullYear() - 18);

  return birthDate <= minimumBirthDate;
};

const passwordRequirements = computed(() => ({
  minLength: formData.value.password.length >= 8,
  hasUpperCase: /[A-Z]/.test(formData.value.password),
  hasLowerCase: /[a-z]/.test(formData.value.password),
  hasNumber: /[0-9]/.test(formData.value.password),
  hasSpecial: /[!@#$%^&*(),.?":{}|<>]/.test(formData.value.password),
}));

const isPasswordValid = computed(() =>
  Object.values(passwordRequirements.value).every(Boolean)
);

const validateField = (name: string, value: string): string | null => {
  switch (name) {
    case 'name':
      if (!value) return 'Nome é obrigatório';
      if (value.length < 3) return 'Nome deve ter pelo menos 3 caracteres';
      return null;

    case 'email':
      if (!value) return 'E-mail é obrigatório';
      if (value.length > 256) return 'E-mail deve ter no máximo 256 caracteres';
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'E-mail inválido';
      return null;

    case 'phone':
      if (!value) return 'Telefone é obrigatório';
      if (!validatePhone(value)) return 'Telefone inválido';
      return null;

    case 'birthDate':
      if (!value) return 'Data de nascimento é obrigatória';
      if (Number.isNaN(new Date(`${value}T00:00:00`).getTime())) return 'Data de nascimento inválida';
      if (!isAtLeast18YearsOld(value)) return 'Você precisa ter pelo menos 18 anos';
      return null;

    case 'cpf':
      if (!value) return 'CPF é obrigatório';
      if (!validateCPF(value)) return 'CPF inválido';
      return null;

    case 'password':
      if (!value) return 'Senha é obrigatória';
      if (value.length > 256) return 'Senha deve ter no máximo 256 caracteres';
      if (!isPasswordValid.value) return 'Senha não atende aos requisitos';
      return null;

    default:
      return null;
  }
};

const clearFieldError = (field: string) => {
  const newErrors = { ...errors.value };
  delete newErrors[field];
  errors.value = newErrors;
};

const handleBlur = (field: string) => {
  touched.value = { ...touched.value, [field]: true };
  const error = validateField(field, formData.value[field as keyof PersonalData] as string);

  if (error) {
    errors.value = { ...errors.value, [field]: error };
    return;
  }

  clearFieldError(field);
};

const handleChange = (field: keyof PersonalData, value: string | boolean) => {
  let processedValue = value;

  serverError.value = '';

  if (field === 'cpf' && typeof value === 'string') {
    processedValue = formatCPF(value);
  } else if (field === 'phone' && typeof value === 'string') {
    processedValue = formatPhone(value);
  }

  formData.value = { ...formData.value, [field]: processedValue };

  if (field === 'acceptTerms') {
    if (processedValue) {
      clearFieldError('acceptTerms');
    }
    return;
  }

  if (touched.value[field] && typeof processedValue === 'string') {
    const error = validateField(field, processedValue);
    if (error) {
      errors.value = { ...errors.value, [field]: error };
      return;
    }

    clearFieldError(field);
  }
};

const buildValidationErrors = () => {
  const newErrors: Record<string, string> = {};

  Object.keys(formData.value).forEach((key) => {
    if (key === 'acceptTerms') {
      return;
    }

    const error = validateField(key, formData.value[key as keyof PersonalData] as string);
    if (error) {
      newErrors[key] = error;
    }
  });

  if (!formData.value.acceptTerms) {
    newErrors.acceptTerms = 'Você deve aceitar os termos e condições';
  }

  return newErrors;
};

const applyServerValidationErrors = (responseData: ValidationErrorResponse) => {
  const apiErrors = responseData.errors;

  if (apiErrors && Object.keys(apiErrors).length > 0) {
    errors.value = { ...errors.value, ...apiErrors };
    touched.value = {
      ...touched.value,
      ...Object.keys(apiErrors).reduce((acc, key) => ({ ...acc, [key]: true }), {}),
    };
    return;
  }

  const message = responseData.message || 'Não foi possível validar os dados do cadastro.';

  if (message.toLowerCase().includes('e-mail')) {
    errors.value = { ...errors.value, email: message };
    touched.value = { ...touched.value, email: true };
    return;
  }

  if (message.toLowerCase().includes('cpf')) {
    errors.value = { ...errors.value, cpf: message };
    touched.value = { ...touched.value, cpf: true };
    return;
  }

  serverError.value = message;
};

const handleSubmit = async (e: Event) => {
  e.preventDefault();

  serverError.value = '';
  touched.value = Object.keys(formData.value).reduce(
    (acc, key) => ({ ...acc, [key]: true }),
    {}
  );

  const newErrors = buildValidationErrors();

  if (Object.keys(newErrors).length > 0) {
    errors.value = newErrors;
    return;
  }

  errors.value = {};
  isLoading.value = true;

  try {
    const userType = props.accountType === 'provider' ? 'FREELANCER' : 'CONTRACTOR';

    await api.post('/auth/validate-registration', {
      name: formData.value.name.trim(),
      email: formData.value.email.trim(),
      cpf: formData.value.cpf.replace(/\D/g, ''),
      phone: formData.value.phone.replace(/\D/g, ''),
      birthDate: formData.value.birthDate,
      password: formData.value.password,
      userType,
      acceptTerms: formData.value.acceptTerms,
    });

    emit('next', formData.value);
  } catch (err: any) {
    applyServerValidationErrors(err.response?.data || {});
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <div :class="props.inline ? '' : 'signup-personal-data-container'">
    <div :class="props.inline ? '' : 'signup-personal-data-card'">
      <Button
        v-if="!props.inline"
        variant="ghost"
        @click="emit('back')"
        class="mb-6"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar
      </Button>

      <div v-if="!props.inline" class="signup-personal-data-header">
        <div class="signup-personal-data-progress-header">
          <h2 class="signup-personal-data-title">Criar conta</h2>
          <span class="signup-personal-data-progress-step">Passo 2 de 3</span>
        </div>
        <Progress :value="66" class="h-2" />
      </div>

      <div v-if="!props.inline" class="mb-6">
        <h3 class="text-gray-900 mb-2">Dados pessoais</h3>
        <p class="text-gray-600">
          Preencha seus dados para criar sua conta de
          {{ accountType === 'contractor' ? 'Contratante' : 'Freelancer' }}
        </p>
      </div>

      <form @submit="handleSubmit" class="signup-personal-data-form">
        <Alert v-if="serverError" variant="destructive">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ serverError }}</AlertDescription>
        </Alert>

        <div class="signup-personal-data-field">
          <Label for="name">Nome completo *</Label>
          <Input
            id="name"
            type="text"
            placeholder="Digite seu nome completo"
            :value="formData.name"
            @input="(e: Event) => handleChange('name', (e.target as HTMLInputElement).value)"
            @blur="handleBlur('name')"
            :class="errors.name && touched.name ? 'signup-personal-data-input-error' : ''"
            :disabled="isLoading"
          />
          <p v-if="errors.name && touched.name" class="signup-personal-data-error-message">
            <AlertCircle class="h-3 w-3" />
            {{ errors.name }}
          </p>
        </div>

        <div class="signup-personal-data-grid">
          <div class="signup-personal-data-field">
            <Label for="email">E-mail *</Label>
            <Input
              id="email"
              type="email"
              placeholder="seu@email.com"
              :value="formData.email"
              @input="(e: Event) => handleChange('email', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('email')"
              :class="errors.email && touched.email ? 'signup-personal-data-input-error' : ''"
              maxlength="256"
              :disabled="isLoading"
            />
            <p v-if="errors.email && touched.email" class="signup-personal-data-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.email }}
            </p>
          </div>

          <div class="signup-personal-data-field">
            <Label for="phone">Telefone *</Label>
            <Input
              id="phone"
              type="tel"
              placeholder="(00) 00000-0000"
              :value="formData.phone"
              @input="(e: Event) => handleChange('phone', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('phone')"
              :class="errors.phone && touched.phone ? 'signup-personal-data-input-error' : ''"
              maxlength="15"
              :disabled="isLoading"
            />
            <p v-if="errors.phone && touched.phone" class="signup-personal-data-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.phone }}
            </p>
          </div>
        </div>

        <div class="signup-personal-data-grid">
          <div class="signup-personal-data-field">
            <Label for="birthDate">Data de nascimento *</Label>
            <Input
              id="birthDate"
              type="date"
              :value="formData.birthDate"
              :max="maxBirthDate"
              @input="(e: Event) => handleChange('birthDate', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('birthDate')"
              :class="[
                'signup-personal-data-date-input',
                errors.birthDate && touched.birthDate ? 'signup-personal-data-input-error' : '',
              ]"
              :disabled="isLoading"
            />
            <p v-if="errors.birthDate && touched.birthDate" class="signup-personal-data-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.birthDate }}
            </p>
          </div>

          <div class="signup-personal-data-field">
            <Label for="cpf">CPF *</Label>
            <Input
              id="cpf"
              type="text"
              placeholder="000.000.000-00"
              :value="formData.cpf"
              @input="(e: Event) => handleChange('cpf', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('cpf')"
              :class="errors.cpf && touched.cpf ? 'signup-personal-data-input-error' : ''"
              maxlength="14"
              :disabled="isLoading"
            />
            <p v-if="errors.cpf && touched.cpf" class="signup-personal-data-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.cpf }}
            </p>
          </div>
        </div>

        <div class="signup-personal-data-field">
          <Label for="password">Senha *</Label>
          <div class="signup-personal-data-password-wrapper">
            <Input
              id="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Digite sua senha"
              :value="formData.password"
              @input="(e: Event) => handleChange('password', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('password')"
              :class="[errors.password && touched.password ? 'signup-personal-data-input-error' : '', 'signup-personal-data-password-input']"
              maxlength="256"
              :disabled="isLoading"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="signup-personal-data-password-toggle"
              tabindex="-1"
            >
              <EyeOff v-if="showPassword" class="h-4 w-4" />
              <Eye v-else class="h-4 w-4" />
            </button>
          </div>

          <div v-if="formData.password" class="signup-personal-data-password-requirements">
            <p class="signup-personal-data-requirements-title">A senha deve conter:</p>
            <div
              class="signup-personal-data-requirement"
              :class="passwordRequirements.minLength ? 'met' : 'unmet'"
            >
              <Check v-if="passwordRequirements.minLength" class="h-3 w-3" />
              <X v-else class="h-3 w-3" />
              <span>Mínimo 8 caracteres</span>
            </div>
            <div
              class="signup-personal-data-requirement"
              :class="passwordRequirements.hasUpperCase ? 'met' : 'unmet'"
            >
              <Check v-if="passwordRequirements.hasUpperCase" class="h-3 w-3" />
              <X v-else class="h-3 w-3" />
              <span>1 letra maiúscula</span>
            </div>
            <div
              class="signup-personal-data-requirement"
              :class="passwordRequirements.hasLowerCase ? 'met' : 'unmet'"
            >
              <Check v-if="passwordRequirements.hasLowerCase" class="h-3 w-3" />
              <X v-else class="h-3 w-3" />
              <span>1 letra minúscula</span>
            </div>
            <div
              class="signup-personal-data-requirement"
              :class="passwordRequirements.hasNumber ? 'met' : 'unmet'"
            >
              <Check v-if="passwordRequirements.hasNumber" class="h-3 w-3" />
              <X v-else class="h-3 w-3" />
              <span>1 número</span>
            </div>
            <div
              class="signup-personal-data-requirement"
              :class="passwordRequirements.hasSpecial ? 'met' : 'unmet'"
            >
              <Check v-if="passwordRequirements.hasSpecial" class="h-3 w-3" />
              <X v-else class="h-3 w-3" />
              <span>1 caractere especial</span>
            </div>
          </div>
        </div>

        <div class="signup-personal-data-terms-wrapper">
          <Checkbox
            id="terms"
            :checked="formData.acceptTerms"
            @update:checked="(checked) => handleChange('acceptTerms', checked as boolean)"
            :class="errors.acceptTerms ? 'signup-personal-data-terms-checkbox-error' : 'signup-personal-data-terms-checkbox'"
            :disabled="isLoading"
          />
          <div class="signup-personal-data-terms-content">
            <Label for="terms" class="signup-personal-data-terms-label">
              Li e concordo com os
              <a href="/politicas?tab=termos" class="signup-personal-data-terms-link" @click.stop>
                Termos de Uso
              </a>
              e a
              <a href="/politicas" class="signup-personal-data-terms-link" @click.stop>
                Política de Privacidade
              </a>
              *
            </Label>
            <p v-if="errors.acceptTerms" class="signup-personal-data-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.acceptTerms }}
            </p>
          </div>
        </div>

        <Button type="submit" class="w-full" size="lg" :disabled="isLoading">
          {{ isLoading ? 'Validando...' : 'Continuar' }}
        </Button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.signup-personal-data-container {
  min-height: 100vh;
  background: linear-gradient(to bottom right, rgb(239, 246, 255), rgb(238, 242, 255));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.signup-personal-data-container.inline {
  min-height: auto;
  background: transparent;
  padding: 0;
}

.signup-personal-data-card {
  width: 100%;
  max-width: 42rem;
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  padding: 2rem;
}

.signup-personal-data-card.inline {
  max-width: none;
  box-shadow: none;
  padding: 0;
}

.signup-personal-data-header {
  margin-bottom: 2rem;
}

.signup-personal-data-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.signup-personal-data-progress-step {
  font-size: 0.875rem;
  color: rgb(107, 114, 128);
}

.signup-personal-data-title {
  color: rgb(17, 24, 39);
  margin-bottom: 0.5rem;
}

.signup-personal-data-subtitle {
  color: rgb(75, 85, 99);
}

.signup-personal-data-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.signup-personal-data-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.signup-personal-data-grid {
  display: grid;
  gap: 1rem;
}

@media (min-width: 1024px) {
  .signup-personal-data-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.signup-personal-data-input-error {
  border-color: rgb(239, 68, 68);
}

.signup-personal-data-date-input {
  padding-right: 2.75rem;
}

.signup-personal-data-date-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  margin-left: auto;
}

.signup-personal-data-date-input::-webkit-date-and-time-value {
  text-align: left;
}

.signup-personal-data-error-message {
  font-size: 0.875rem;
  color: rgb(239, 68, 68);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.signup-personal-data-password-wrapper {
  position: relative;
}

.signup-personal-data-password-input {
  padding-right: 2.5rem;
}

.signup-personal-data-password-toggle {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: rgb(107, 114, 128);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
  outline: none;
}

.signup-personal-data-password-toggle:hover {
  color: rgb(55, 65, 81);
}

.signup-personal-data-password-requirements {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background-color: rgb(249, 250, 251);
  border-radius: 0.375rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.signup-personal-data-requirements-title {
  font-size: 0.875rem;
  color: rgb(55, 65, 81);
}

.signup-personal-data-requirement {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.signup-personal-data-requirement.met {
  color: rgb(22, 163, 74);
}

.signup-personal-data-requirement.unmet {
  color: rgb(107, 114, 128);
}

.signup-personal-data-terms-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding-top: 1rem;
}

.signup-personal-data-terms-checkbox-error {
  border-color: rgb(239, 68, 68);
}

.signup-personal-data-terms-checkbox {
  border-color: rgba(0, 0, 0, 0.5);
}

.signup-personal-data-terms-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.signup-personal-data-terms-label {
  cursor: pointer;
}

.signup-personal-data-terms-link {
  color: rgb(37, 99, 235);
  text-decoration: none;
}

.signup-personal-data-terms-link:hover {
  text-decoration: underline;
}
</style>
