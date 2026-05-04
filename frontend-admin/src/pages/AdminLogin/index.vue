<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Shield, Eye, EyeOff } from 'lucide-vue-next'
import { authService } from '@/api/services/authService'
import { useUserStore } from '@/stores/userStore'
import { toast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  if (route.query.expired === 'true') {
    toast.error('Sua sessão expirou. Por favor, faça login novamente.')
  }
  if (route.query.denied === 'true') {
    errorMessage.value = 'Acesso negado: esta área é exclusiva para administradores.'
  }
})

const handleLogin = async () => {
  errorMessage.value = ''

  if (!email.value.trim() || !password.value) {
    errorMessage.value = 'Preencha email e senha.'
    return
  }

  isLoading.value = true
  try {
    const user = await authService.login({
      email: email.value.trim(),
      password: password.value,
    })

    if (user.type !== 'admin') {
      errorMessage.value = 'Acesso negado: esta área é exclusiva para administradores.'
      return
    }

    if (user.token) {
      localStorage.setItem('token', user.token)
    }

    userStore.setUser({
      id: user.id,
      name: user.name,
      email: user.email,
      type: 'admin',
      avatar: user.avatar,
    })

    router.push('/users')
  } catch (err: unknown) {
    const error = err as { response?: { status?: number; data?: { message?: string } } }
    if (error.response?.status === 401 || error.response?.status === 403) {
      errorMessage.value = 'Email ou senha inválidos.'
    } else {
      errorMessage.value =
        error.response?.data?.message || 'Erro ao realizar login. Tente novamente.'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-slate-50 p-4">
    <div class="w-full max-w-sm">
      <div class="mb-8 text-center">
        <div class="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-[#10243d]">
          <Shield class="h-7 w-7 text-white" />
        </div>
        <h1 class="text-2xl font-bold text-slate-900">ServiJá Admin</h1>
        <p class="mt-1 text-sm text-slate-500">Acesso exclusivo para administradores</p>
      </div>

      <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <form class="space-y-4" @submit.prevent="handleLogin">
          <div
            v-if="errorMessage"
            class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"
          >
            {{ errorMessage }}
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Email</label>
            <input
              v-model="email"
              type="email"
              placeholder="seu@email.com"
              autocomplete="email"
              class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Senha</label>
            <div class="relative">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                autocomplete="current-password"
                class="w-full rounded-lg border border-slate-200 px-3 py-2.5 pr-10 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 transition hover:text-slate-600"
                @click="showPassword = !showPassword"
              >
                <Eye v-if="!showPassword" class="h-4 w-4" />
                <EyeOff v-else class="h-4 w-4" />
              </button>
            </div>
          </div>

          <button
            type="submit"
            :disabled="isLoading"
            class="w-full rounded-lg bg-[#10243d] px-4 py-2.5 text-sm font-medium text-white transition hover:bg-[#1a3a5c] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {{ isLoading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>
      </div>

      <p class="mt-6 text-center text-xs text-slate-400">ServiJá © {{ new Date().getFullYear() }}</p>
    </div>
  </div>
</template>
