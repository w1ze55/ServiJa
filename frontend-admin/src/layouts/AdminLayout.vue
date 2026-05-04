<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { FolderTree, LogOut, Shield, Users } from 'lucide-vue-next'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const currentUser = computed(() => userStore.user)

const isCategoriesActive = computed(() => route.path.startsWith('/categories'))
const isUsersActive = computed(() => route.path.startsWith('/users'))

const logout = () => {
  userStore.clearUser()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <aside
      class="fixed inset-y-0 left-0 hidden w-64 border-r border-slate-200 bg-[#10243d] text-white lg:flex lg:flex-col"
    >
      <div class="flex h-20 shrink-0 items-center border-b border-white/10 px-6">
        <div class="flex items-center gap-3">
          <div class="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/10">
            <Shield class="h-5 w-5" />
          </div>
          <div>
            <h1 class="text-lg font-semibold">Servi Já</h1>
            <p class="text-xs uppercase tracking-[0.24em] text-slate-300">Admin</p>
          </div>
        </div>
      </div>

      <nav class="flex-1 space-y-1 px-4 py-6">
        <button
          :class="[
            'flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-medium transition-colors',
            isCategoriesActive
              ? 'bg-white/20 text-white'
              : 'text-white/70 hover:bg-white/10 hover:text-white',
          ]"
          type="button"
          @click="router.push('/categories')"
        >
          <FolderTree class="h-4 w-4" />
          Catálogo de categorias
        </button>
        <button
          :class="[
            'flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left text-sm font-medium transition-colors',
            isUsersActive
              ? 'bg-white/20 text-white'
              : 'text-white/70 hover:bg-white/10 hover:text-white',
          ]"
          type="button"
          @click="router.push('/users')"
        >
          <Users class="h-4 w-4" />
          Usuários
        </button>
      </nav>

      <div class="border-t border-white/10 px-6 py-4 text-sm text-slate-300">
        <p class="font-medium text-white">{{ currentUser?.name }}</p>
        <p>{{ currentUser?.email }}</p>
      </div>
    </aside>

    <div class="lg:pl-64">
      <header class="sticky top-0 z-30 border-b border-slate-200 bg-white/90 backdrop-blur">
        <div class="mx-auto flex h-20 max-w-7xl items-center justify-end px-4 sm:px-6">
          <button
            type="button"
            class="flex items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            @click="logout"
          >
            <LogOut class="h-4 w-4" />
            Sair
          </button>
        </div>
      </header>

      <main class="mx-auto max-w-7xl px-4 py-6 sm:px-6">
        <slot />
      </main>
    </div>
  </div>
</template>
