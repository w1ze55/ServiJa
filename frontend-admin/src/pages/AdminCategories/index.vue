<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  FolderPlus,
  Pencil,
  Archive,
  RotateCcw,
  Plus,
  ChevronDown,
  ChevronRight,
} from 'lucide-vue-next'
import AdminLayout from '@/layouts/AdminLayout.vue'
import {
  categoryService,
  type AdminCategoryItem,
  type CategoryPayload,
  type SubcategoryPayload,
} from '@/api/services/categoryService'
import { useCategoryCatalogStore } from '@/stores/categoryCatalogStore'
import { toast } from '@/utils/toast'

const catalogStore = useCategoryCatalogStore()
const isLoading = ref(false)
const categories = computed(() => catalogStore.adminCategories)
const expandedCategories = ref<Set<number>>(new Set())

const categoryDialogOpen = ref(false)
const categoryForm = ref<CategoryPayload>({ name: '', sortOrder: 0 })
const editingCategoryId = ref<number | null>(null)

const subcategoryDialogOpen = ref(false)
const subcategoryForm = ref<SubcategoryPayload>({ categoryId: 0, name: '', sortOrder: 0 })
const editingSubcategoryId = ref<number | null>(null)

const resetCategoryForm = () => {
  editingCategoryId.value = null
  categoryForm.value = { name: '', sortOrder: 0 }
}

const resetSubcategoryForm = (categoryId = 0) => {
  editingSubcategoryId.value = null
  subcategoryForm.value = { categoryId, name: '', sortOrder: 0 }
}

const loadCategories = async (force = false) => {
  isLoading.value = true
  try {
    await catalogStore.loadAdminCategories(force)
  } catch (error) {
    console.error(error)
    toast.error('Erro ao carregar categorias')
  } finally {
    isLoading.value = false
  }
}

const toggleExpand = (id: number) => {
  if (expandedCategories.value.has(id)) {
    expandedCategories.value.delete(id)
  } else {
    expandedCategories.value.add(id)
  }
}

const openCreateCategory = () => {
  resetCategoryForm()
  categoryDialogOpen.value = true
}

const openEditCategory = (category: AdminCategoryItem) => {
  editingCategoryId.value = category.id
  categoryForm.value = { name: category.name, sortOrder: category.sortOrder }
  categoryDialogOpen.value = true
}

const saveCategory = async () => {
  try {
    if (editingCategoryId.value) {
      await categoryService.updateCategory(editingCategoryId.value, categoryForm.value)
      toast.success('Categoria atualizada')
    } else {
      await categoryService.createCategory(categoryForm.value)
      toast.success('Categoria criada')
    }
    categoryDialogOpen.value = false
    resetCategoryForm()
    await loadCategories(true)
  } catch (error) {
    console.error(error)
    toast.error('Não foi possível salvar a categoria')
  }
}

const toggleCategory = async (category: AdminCategoryItem) => {
  try {
    if (category.active) {
      await categoryService.archiveCategory(category.id)
      toast.success('Categoria arquivada')
    } else {
      await categoryService.restoreCategory(category.id)
      toast.success('Categoria restaurada')
    }
    await loadCategories(true)
  } catch (error) {
    console.error(error)
    toast.error('Não foi possível atualizar a categoria')
  }
}

const openCreateSubcategory = (category: AdminCategoryItem) => {
  resetSubcategoryForm(category.id)
  subcategoryDialogOpen.value = true
}

const openEditSubcategory = (
  categoryId: number,
  subcategory: { id: number; name: string; sortOrder: number }
) => {
  editingSubcategoryId.value = subcategory.id
  subcategoryForm.value = { categoryId, name: subcategory.name, sortOrder: subcategory.sortOrder }
  subcategoryDialogOpen.value = true
}

const saveSubcategory = async () => {
  try {
    if (editingSubcategoryId.value) {
      await categoryService.updateSubcategory(editingSubcategoryId.value, subcategoryForm.value)
      toast.success('Subcategoria atualizada')
    } else {
      await categoryService.createSubcategory(subcategoryForm.value)
      toast.success('Subcategoria criada')
    }
    subcategoryDialogOpen.value = false
    resetSubcategoryForm()
    await loadCategories(true)
  } catch (error) {
    console.error(error)
    toast.error('Não foi possível salvar a subcategoria')
  }
}

const toggleSubcategory = async (subcategory: { id: number; active: boolean }) => {
  try {
    if (subcategory.active) {
      await categoryService.archiveSubcategory(subcategory.id)
      toast.success('Subcategoria arquivada')
    } else {
      await categoryService.restoreSubcategory(subcategory.id)
      toast.success('Subcategoria restaurada')
    }
    await loadCategories(true)
  } catch (error) {
    console.error(error)
    toast.error('Não foi possível atualizar a subcategoria')
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-slate-900">Catálogo de Categorias</h1>
          <p class="text-sm text-slate-600">
            Cadastre, edite, arquive e restaure categorias e subcategorias.
          </p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 rounded-lg bg-[#10243d] px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
          @click="openCreateCategory"
        >
          <FolderPlus class="h-4 w-4" />
          Nova categoria
        </button>
      </div>

      <div
        v-if="isLoading"
        class="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500"
      >
        Carregando catálogo...
      </div>

      <div v-else class="grid gap-4">
        <div
          v-for="category in categories"
          :key="category.id"
          class="overflow-hidden rounded-xl border border-slate-200 bg-white"
        >
          <div class="flex flex-col gap-4 p-5 md:flex-row md:items-start md:justify-between">
            <div>
              <div class="flex flex-wrap items-center gap-3">
                <h2 class="text-lg font-semibold text-slate-900">{{ category.name }}</h2>
                <span
                  class="rounded-full px-2.5 py-1 text-xs font-semibold"
                  :class="category.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'"
                >
                  {{ category.active ? 'Ativa' : 'Arquivada' }}
                </span>
                <span class="text-xs text-slate-500">slug: {{ category.slug }}</span>
              </div>
              <p class="mt-2 text-sm text-slate-600">Ordem: {{ category.sortOrder }}</p>
            </div>

            <div class="flex flex-wrap gap-2">
              <button
                type="button"
                class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click="openEditCategory(category)"
              >
                <Pencil class="h-3.5 w-3.5" />
                Editar
              </button>
              <button
                type="button"
                class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click="openCreateSubcategory(category)"
              >
                <Plus class="h-3.5 w-3.5" />
                Subcategoria
              </button>
              <button
                type="button"
                class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click="toggleCategory(category)"
              >
                <Archive v-if="category.active" class="h-3.5 w-3.5" />
                <RotateCcw v-else class="h-3.5 w-3.5" />
                {{ category.active ? 'Arquivar' : 'Restaurar' }}
              </button>
            </div>
          </div>

          <button
            type="button"
            class="flex w-full items-center gap-2 border-t border-slate-200 px-5 py-3 text-left text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 transition hover:bg-slate-50"
            @click="toggleExpand(category.id)"
          >
            <ChevronDown v-if="expandedCategories.has(category.id)" class="h-4 w-4" />
            <ChevronRight v-else class="h-4 w-4" />
            Subcategorias ({{ category.subcategories?.length || 0 }})
          </button>

          <div v-if="expandedCategories.has(category.id)" class="bg-slate-50/70 p-5">
            <div v-if="!category.subcategories?.length" class="text-sm text-slate-500">
              Nenhuma subcategoria cadastrada.
            </div>
            <div v-else class="grid gap-2">
              <div
                v-for="subcategory in category.subcategories"
                :key="subcategory.id"
                class="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 md:flex-row md:items-center md:justify-between"
              >
                <div>
                  <div class="flex flex-wrap items-center gap-2">
                    <p class="font-medium text-slate-900">{{ subcategory.name }}</p>
                    <span
                      class="rounded-full px-2 py-0.5 text-[11px] font-semibold"
                      :class="
                        subcategory.active
                          ? 'bg-emerald-50 text-emerald-700'
                          : 'bg-slate-100 text-slate-600'
                      "
                    >
                      {{ subcategory.active ? 'Ativa' : 'Arquivada' }}
                    </span>
                    <span class="text-[11px] text-slate-500">slug: {{ subcategory.slug }}</span>
                  </div>
                  <p class="mt-1 text-xs text-slate-500">Ordem: {{ subcategory.sortOrder }}</p>
                </div>

                <div class="flex flex-wrap gap-2">
                  <button
                    type="button"
                    class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
                    @click="openEditSubcategory(category.id, subcategory)"
                  >
                    <Pencil class="h-3 w-3" />
                    Editar
                  </button>
                  <button
                    type="button"
                    class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
                    @click="toggleSubcategory(subcategory)"
                  >
                    <Archive v-if="subcategory.active" class="h-3 w-3" />
                    <RotateCcw v-else class="h-3 w-3" />
                    {{ subcategory.active ? 'Arquivar' : 'Restaurar' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="categoryDialogOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div class="absolute inset-0 bg-black/40" @click="(categoryDialogOpen = false), resetCategoryForm()" />
      <div class="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
        <h2 class="mb-4 text-base font-semibold text-slate-900">
          {{ editingCategoryId ? 'Editar categoria' : 'Nova categoria' }}
        </h2>
        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Nome</label>
            <input
              v-model="categoryForm.name"
              type="text"
              placeholder="Ex: Tecnologia e Informática"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Ordem</label>
            <input
              v-model.number="categoryForm.sortOrder"
              type="number"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>
        </div>
        <div class="mt-6 flex gap-3">
          <button
            type="button"
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            @click="(categoryDialogOpen = false), resetCategoryForm()"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
            @click="saveCategory"
          >
            Salvar
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="subcategoryDialogOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div
        class="absolute inset-0 bg-black/40"
        @click="(subcategoryDialogOpen = false), resetSubcategoryForm()"
      />
      <div class="relative w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl">
        <h2 class="mb-4 text-base font-semibold text-slate-900">
          {{ editingSubcategoryId ? 'Editar subcategoria' : 'Nova subcategoria' }}
        </h2>
        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Nome</label>
            <input
              v-model="subcategoryForm.name"
              type="text"
              placeholder="Ex: Desenvolvimento Web"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-slate-700">Ordem</label>
            <input
              v-model.number="subcategoryForm.sortOrder"
              type="number"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>
        </div>
        <div class="mt-6 flex gap-3">
          <button
            type="button"
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            @click="(subcategoryDialogOpen = false), resetSubcategoryForm()"
          >
            Cancelar
          </button>
          <button
            type="button"
            class="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700"
            @click="saveSubcategory"
          >
            Salvar
          </button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>
