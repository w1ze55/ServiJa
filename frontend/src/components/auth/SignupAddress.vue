<script setup lang="ts">
import { ref } from 'vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Alert from '../ui/alert.vue';
import AlertDescription from '../ui/alert-description.vue';
import Progress from '../ui/progress.vue';
import { AlertCircle, ArrowLeft, LoaderCircle, MapPin } from 'lucide-vue-next';
import api from '@/api/config/axios';

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

interface Props {
  inline?: boolean;
  initialData?: Partial<AddressData>;
}

interface Emits {
  (e: 'next', data: AddressData): void;
  (e: 'back'): void;
}

interface ValidationErrorResponse {
  errors?: Record<string, string>;
  message?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const formData = ref<AddressData>({
  label: 'Principal',
  zipCode: '',
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  isDefault: true,
  ...props.initialData,
});

const touched = ref<Record<string, boolean>>({});
const errors = ref<Record<string, string>>({});
const serverError = ref('');
const isLoading = ref(false);
const isLoadingCEP = ref(false);

const formatCEP = (value: string) => {
  const numbers = value.replace(/\D/g, '');
  return numbers.replace(/(\d{5})(\d{0,3})/, '$1-$2');
};

const validateField = (name: string, value: string): string | null => {
  switch (name) {
    case 'zipCode': {
      const cleanCEP = value.replace(/\D/g, '');
      if (!cleanCEP) return 'CEP é obrigatório';
      if (cleanCEP.length !== 8) return 'CEP deve ter 8 dígitos';
      return null;
    }
    case 'street':
      if (!value.trim()) return 'Rua é obrigatória';
      if (value.length > 255) return 'Rua deve ter no máximo 255 caracteres';
      return null;
    case 'number':
      if (!value.trim()) return 'Número é obrigatório';
      if (value.length > 20) return 'Número deve ter no máximo 20 caracteres';
      return null;
    case 'complement':
      if (value.length > 100) return 'Complemento deve ter no máximo 100 caracteres';
      return null;
    case 'neighborhood':
      if (!value.trim()) return 'Bairro é obrigatório';
      if (value.length > 100) return 'Bairro deve ter no máximo 100 caracteres';
      return null;
    case 'city':
      if (!value.trim()) return 'Cidade é obrigatória';
      if (value.length > 100) return 'Cidade deve ter no máximo 100 caracteres';
      return null;
    case 'state':
      if (!value.trim()) return 'Estado é obrigatório';
      if (value.trim().length !== 2) return 'Use a sigla com 2 letras';
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

const handleBlur = (field: keyof AddressData) => {
  if (field === 'label' || field === 'isDefault') {
    return;
  }

  touched.value = { ...touched.value, [field]: true };
  const error = validateField(field, String(formData.value[field] ?? ''));

  if (error) {
    errors.value = { ...errors.value, [field]: error };
    return;
  }

  clearFieldError(field);
};

const fetchAddressByCEP = async (cep: string) => {
  const cleanCEP = cep.replace(/\D/g, '');

  if (cleanCEP.length !== 8) {
    return;
  }

  isLoadingCEP.value = true;
  serverError.value = '';

  try {
    const response = await fetch(`https://viacep.com.br/ws/${cleanCEP}/json/`);
    const data = await response.json();

    if (data.erro) {
      errors.value = { ...errors.value, zipCode: 'CEP não encontrado' };
      touched.value = { ...touched.value, zipCode: true };
      return;
    }

    formData.value = {
      ...formData.value,
      street: data.logradouro || formData.value.street,
      neighborhood: data.bairro || formData.value.neighborhood,
      city: data.localidade || formData.value.city,
      state: data.uf || formData.value.state,
      complement: formData.value.complement || data.complemento || '',
    };

    ['zipCode', 'street', 'neighborhood', 'city', 'state'].forEach(clearFieldError);
  } catch (error) {
    serverError.value = 'Erro ao buscar CEP. Verifique sua conexão.';
  } finally {
    isLoadingCEP.value = false;
  }
};

const handleChange = (field: keyof AddressData, value: string | boolean) => {
  serverError.value = '';
  let processedValue = value;

  if (field === 'zipCode' && typeof value === 'string') {
    processedValue = formatCEP(value);
  }

  if (field === 'state' && typeof value === 'string') {
    processedValue = value.toUpperCase();
  }

  formData.value = {
    ...formData.value,
    [field]: processedValue,
  };

  if (field === 'zipCode' && typeof processedValue === 'string') {
    fetchAddressByCEP(processedValue);
  }

  if (field !== 'label' && field !== 'isDefault' && touched.value[field] && typeof processedValue === 'string') {
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

  ['zipCode', 'street', 'number', 'complement', 'neighborhood', 'city', 'state'].forEach((field) => {
    const error = validateField(field, String(formData.value[field as keyof AddressData] ?? ''));
    if (error) {
      newErrors[field] = error;
    }
  });

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

  serverError.value = responseData.message || 'Não foi possível validar o endereço informado.';
};

const handleSubmit = async (e: Event) => {
  e.preventDefault();

  serverError.value = '';
  touched.value = {
    ...touched.value,
    zipCode: true,
    street: true,
    number: true,
    complement: true,
    neighborhood: true,
    city: true,
    state: true,
  };

  const newErrors = buildValidationErrors();
  if (Object.keys(newErrors).length > 0) {
    errors.value = newErrors;
    return;
  }

  errors.value = {};
  isLoading.value = true;

  try {
    await api.post('/auth/validate-address', formData.value);
    emit('next', {
      ...formData.value,
      state: formData.value.state.toUpperCase(),
    });
  } catch (err: any) {
    applyServerValidationErrors(err.response?.data || {});
  } finally {
    isLoading.value = false;
  }
};
</script>

<template>
  <div :class="props.inline ? '' : 'signup-address-container'">
    <div :class="props.inline ? '' : 'signup-address-card'">
      <Button
        v-if="!props.inline"
        variant="ghost"
        @click="emit('back')"
        class="mb-6"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar
      </Button>

      <div v-if="!props.inline" class="signup-address-header">
        <div class="signup-address-progress-header">
          <h2 class="signup-address-title">Criar conta</h2>
          <span class="signup-address-progress-step">Passo 3 de 4</span>
        </div>
        <Progress :value="75" class="h-2" />
      </div>

      <div v-if="!props.inline" class="mb-6">
        <h3 class="text-gray-900 mb-2">Endereço</h3>
        <p class="text-gray-600">
          Informe seu endereço principal. Vamos usar o CEP para preencher os dados automaticamente quando possível.
        </p>
      </div>

      <form @submit="handleSubmit" class="signup-address-form">
        <Alert v-if="serverError" variant="destructive">
          <AlertCircle class="h-4 w-4" />
          <AlertDescription>{{ serverError }}</AlertDescription>
        </Alert>

        <div class="signup-address-grid">
          <div class="signup-address-field">
            <Label for="zipCode">CEP *</Label>
            <div class="signup-address-cep-wrapper">
              <Input
                id="zipCode"
                type="text"
                placeholder="00000-000"
                :value="formData.zipCode"
                @input="(e: Event) => handleChange('zipCode', (e.target as HTMLInputElement).value)"
                @blur="handleBlur('zipCode')"
                :class="errors.zipCode && touched.zipCode ? 'signup-address-input-error' : ''"
                maxlength="9"
                :disabled="isLoading"
              />
              <span v-if="isLoadingCEP" class="signup-address-cep-loading">
                <LoaderCircle class="h-4 w-4 animate-spin" />
              </span>
            </div>
            <p v-if="errors.zipCode && touched.zipCode" class="signup-address-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.zipCode }}
            </p>
          </div>

          <div class="signup-address-field">
            <Label for="number">Número *</Label>
            <Input
              id="number"
              type="text"
              placeholder="123"
              :value="formData.number"
              @input="(e: Event) => handleChange('number', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('number')"
              :class="errors.number && touched.number ? 'signup-address-input-error' : ''"
              maxlength="20"
              :disabled="isLoading"
            />
            <p v-if="errors.number && touched.number" class="signup-address-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.number }}
            </p>
          </div>
        </div>

        <div class="signup-address-field">
          <Label for="street">Rua *</Label>
          <Input
            id="street"
            type="text"
            placeholder="Rua, avenida, travessa..."
            :value="formData.street"
            @input="(e: Event) => handleChange('street', (e.target as HTMLInputElement).value)"
            @blur="handleBlur('street')"
            :class="errors.street && touched.street ? 'signup-address-input-error' : ''"
            maxlength="255"
            :disabled="isLoading"
          />
          <p v-if="errors.street && touched.street" class="signup-address-error-message">
            <AlertCircle class="h-3 w-3" />
            {{ errors.street }}
          </p>
        </div>

        <div class="signup-address-field">
          <Label for="complement">Complemento</Label>
          <Input
            id="complement"
            type="text"
            placeholder="Apartamento, bloco, referência..."
            :value="formData.complement"
            @input="(e: Event) => handleChange('complement', (e.target as HTMLInputElement).value)"
            @blur="handleBlur('complement')"
            :class="errors.complement && touched.complement ? 'signup-address-input-error' : ''"
            maxlength="100"
            :disabled="isLoading"
          />
          <p v-if="errors.complement && touched.complement" class="signup-address-error-message">
            <AlertCircle class="h-3 w-3" />
            {{ errors.complement }}
          </p>
        </div>

        <div class="signup-address-field">
          <Label for="neighborhood">Bairro *</Label>
          <Input
            id="neighborhood"
            type="text"
            placeholder="Seu bairro"
            :value="formData.neighborhood"
            @input="(e: Event) => handleChange('neighborhood', (e.target as HTMLInputElement).value)"
            @blur="handleBlur('neighborhood')"
            :class="errors.neighborhood && touched.neighborhood ? 'signup-address-input-error' : ''"
            maxlength="100"
            :disabled="isLoading"
          />
          <p v-if="errors.neighborhood && touched.neighborhood" class="signup-address-error-message">
            <AlertCircle class="h-3 w-3" />
            {{ errors.neighborhood }}
          </p>
        </div>

        <div class="signup-address-grid">
          <div class="signup-address-field">
            <Label for="city">Cidade *</Label>
            <Input
              id="city"
              type="text"
              placeholder="Sua cidade"
              :value="formData.city"
              @input="(e: Event) => handleChange('city', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('city')"
              :class="errors.city && touched.city ? 'signup-address-input-error' : ''"
              maxlength="100"
              :disabled="isLoading"
            />
            <p v-if="errors.city && touched.city" class="signup-address-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.city }}
            </p>
          </div>

          <div class="signup-address-field">
            <Label for="state">Estado *</Label>
            <Input
              id="state"
              type="text"
              placeholder="SP"
              :value="formData.state"
              @input="(e: Event) => handleChange('state', (e.target as HTMLInputElement).value)"
              @blur="handleBlur('state')"
              :class="errors.state && touched.state ? 'signup-address-input-error' : ''"
              maxlength="2"
              :disabled="isLoading"
            />
            <p v-if="errors.state && touched.state" class="signup-address-error-message">
              <AlertCircle class="h-3 w-3" />
              {{ errors.state }}
            </p>
          </div>
        </div>

        <Button type="submit" class="w-full" size="lg" :disabled="isLoading || isLoadingCEP">
          {{ isLoading ? 'Validando endereço...' : 'Continuar' }}
        </Button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.signup-address-container {
  min-height: 100vh;
  background: linear-gradient(to bottom right, rgb(239, 246, 255), rgb(238, 242, 255));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.signup-address-container.inline {
  min-height: auto;
  background: transparent;
  padding: 0;
}

.signup-address-card {
  width: 100%;
  max-width: 42rem;
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  padding: 2rem;
}

.signup-address-card.inline {
  max-width: none;
  box-shadow: none;
  padding: 0;
}

.signup-address-header {
  margin-bottom: 2rem;
}

.signup-address-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.signup-address-progress-step {
  font-size: 0.875rem;
  color: rgb(107, 114, 128);
}

.signup-address-title {
  color: rgb(17, 24, 39);
  margin-bottom: 0.5rem;
}

.signup-address-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.signup-address-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.signup-address-grid {
  display: grid;
  gap: 1rem;
}

@media (min-width: 1024px) {
  .signup-address-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.signup-address-cep-wrapper {
  position: relative;
}

.signup-address-cep-loading {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  color: rgb(59, 130, 246);
}

.signup-address-input-error {
  border-color: rgb(239, 68, 68);
}

.signup-address-error-message {
  font-size: 0.875rem;
  color: rgb(239, 68, 68);
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
