<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { FolderPlus, Pencil, Archive, RotateCcw, Plus } from 'lucide-vue-next';
import AdminLayout from '@/components/layout/AdminLayout.vue';
import Accordion from '@/components/ui/accordion.vue';
import AccordionContent from '@/components/ui/accordion-content.vue';
import AccordionItem from '@/components/ui/accordion-item.vue';
import AccordionTrigger from '@/components/ui/accordion-trigger.vue';
import Card from '@/components/ui/card.vue';
import Button from '@/components/ui/button.vue';
import Dialog from '@/components/ui/dialog.vue';
import DialogContent from '@/components/ui/dialog-content.vue';
import DialogHeader from '@/components/ui/dialog-header.vue';
import DialogTitle from '@/components/ui/dialog-title.vue';
import DialogFooter from '@/components/ui/dialog-footer.vue';
import Input from '@/components/ui/input.vue';
import Label from '@/components/ui/label.vue';
import { toast } from '@/utils/toast';
import { categoryService, type AdminCategoryItem, type CategoryPayload, type SubcategoryPayload } from '@/api/services/categoryService';
import { useCategoryCatalogStore } from '@/stores/categoryCatalogStore';

const catalogStore = useCategoryCatalogStore();
const isLoading = ref(false);
const categories = computed(() => catalogStore.adminCategories);

const categoryDialogOpen = ref(false);
const categoryForm = ref<CategoryPayload>({ name: '', sortOrder: 0 });
const editingCategoryId = ref<number | null>(null);

const subcategoryDialogOpen = ref(false);
const subcategoryForm = ref<SubcategoryPayload>({ categoryId: 0, name: '', sortOrder: 0 });
const editingSubcategoryId = ref<number | null>(null);

const resetCategoryForm = () => {
  editingCategoryId.value = null;
  categoryForm.value = { name: '', sortOrder: 0 };
};

const resetSubcategoryForm = (categoryId = 0) => {
  editingSubcategoryId.value = null;
  subcategoryForm.value = { categoryId, name: '', sortOrder: 0 };
};

const loadCategories = async (force = false) => {
  isLoading.value = true;
  try {
    await catalogStore.loadAdminCategories(force);
  } catch (error) {
    console.error(error);
    toast.error('Erro ao carregar categorias');
  } finally {
    isLoading.value = false;
  }
};

const openCreateCategory = () => {
  resetCategoryForm();
  categoryDialogOpen.value = true;
};

const openEditCategory = (category: AdminCategoryItem) => {
  editingCategoryId.value = category.id;
  categoryForm.value = { name: category.name, sortOrder: category.sortOrder };
  categoryDialogOpen.value = true;
};

const saveCategory = async () => {
  try {
    if (editingCategoryId.value) {
      await categoryService.updateCategory(editingCategoryId.value, categoryForm.value);
      toast.success('Categoria atualizada');
    } else {
      await categoryService.createCategory(categoryForm.value);
      toast.success('Categoria criada');
    }
    categoryDialogOpen.value = false;
    resetCategoryForm();
    await loadCategories(true);
  } catch (error) {
    console.error(error);
    toast.error('Nao foi possivel salvar a categoria');
  }
};

const toggleCategory = async (category: AdminCategoryItem) => {
  try {
    if (category.active) {
      await categoryService.archiveCategory(category.id);
      toast.success('Categoria arquivada');
    } else {
      await categoryService.restoreCategory(category.id);
      toast.success('Categoria restaurada');
    }
    await loadCategories(true);
  } catch (error) {
    console.error(error);
    toast.error('Nao foi possivel atualizar a categoria');
  }
};

const openCreateSubcategory = (category: AdminCategoryItem) => {
  resetSubcategoryForm(category.id);
  subcategoryDialogOpen.value = true;
};

const openEditSubcategory = (categoryId: number, subcategory: { id: number; name: string; sortOrder: number }) => {
  editingSubcategoryId.value = subcategory.id;
  subcategoryForm.value = { categoryId, name: subcategory.name, sortOrder: subcategory.sortOrder };
  subcategoryDialogOpen.value = true;
};

const saveSubcategory = async () => {
  try {
    if (editingSubcategoryId.value) {
      await categoryService.updateSubcategory(editingSubcategoryId.value, subcategoryForm.value);
      toast.success('Subcategoria atualizada');
    } else {
      await categoryService.createSubcategory(subcategoryForm.value);
      toast.success('Subcategoria criada');
    }
    subcategoryDialogOpen.value = false;
    resetSubcategoryForm();
    await loadCategories(true);
  } catch (error) {
    console.error(error);
    toast.error('Nao foi possivel salvar a subcategoria');
  }
};

const toggleSubcategory = async (subcategory: { id: number; active: boolean }) => {
  try {
    if (subcategory.active) {
      await categoryService.archiveSubcategory(subcategory.id);
      toast.success('Subcategoria arquivada');
    } else {
      await categoryService.restoreSubcategory(subcategory.id);
      toast.success('Subcategoria restaurada');
    }
    await loadCategories(true);
  } catch (error) {
    console.error(error);
    toast.error('Nao foi possivel atualizar a subcategoria');
  }
};

onMounted(() => {
  loadCategories();
});
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-slate-900">Catálogo de Categorias</h1>
          <p class="text-sm text-slate-600">Cadastre, edite, arquive e restaure categorias e subcategorias.</p>
        </div>
        <Button @click="openCreateCategory">
          <FolderPlus class="mr-2 h-4 w-4" />
          Nova categoria
        </Button>
      </div>

      <div v-if="isLoading" class="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-slate-500">
        Carregando catalogo...
      </div>

      <Accordion v-else type="multiple" :default-value="[]" class="grid gap-4">
        <Card v-for="category in categories" :key="category.id" class="overflow-hidden border border-slate-200 bg-white">
          <AccordionItem :value="String(category.id)" class="border-none">
            <div class="flex flex-col gap-4 p-5 md:flex-row md:items-start md:justify-between">
              <div>
                <div class="flex flex-wrap items-center gap-3">
                  <h2 class="text-lg font-semibold text-slate-900">{{ category.name }}</h2>
                  <span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="category.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'">
                    {{ category.active ? 'Ativa' : 'Arquivada' }}
                  </span>
                  <span class="text-xs text-slate-500">slug: {{ category.slug }}</span>
                </div>
                <p class="mt-2 text-sm text-slate-600">Ordem: {{ category.sortOrder }}</p>
              </div>

              <div class="flex flex-wrap gap-2">
                <Button variant="outline" size="sm" @click="openEditCategory(category)">
                  <Pencil class="mr-2 h-4 w-4" />
                  Editar
                </Button>
                <Button variant="outline" size="sm" @click="openCreateSubcategory(category)">
                  <Plus class="mr-2 h-4 w-4" />
                  Subcategoria
                </Button>
                <Button variant="outline" size="sm" @click="toggleCategory(category)">
                  <Archive v-if="category.active" class="mr-2 h-4 w-4" />
                  <RotateCcw v-else class="mr-2 h-4 w-4" />
                  {{ category.active ? 'Arquivar' : 'Restaurar' }}
                </Button>
              </div>
            </div>

            <AccordionTrigger class="border-t border-slate-200 px-5 py-4 text-xs font-semibold uppercase tracking-[0.16em] text-slate-500 hover:no-underline">
              <span>Subcategorias ({{ category.subcategories?.length || 0 }})</span>
            </AccordionTrigger>

            <AccordionContent>
              <div class="bg-slate-50/70 p-5">
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
                        <span class="rounded-full px-2 py-0.5 text-[11px] font-semibold" :class="subcategory.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'">
                          {{ subcategory.active ? 'Ativa' : 'Arquivada' }}
                        </span>
                        <span class="text-[11px] text-slate-500">slug: {{ subcategory.slug }}</span>
                      </div>
                      <p class="mt-1 text-xs text-slate-500">Ordem: {{ subcategory.sortOrder }}</p>
                    </div>

                    <div class="flex flex-wrap gap-2">
                      <Button variant="outline" size="sm" @click="openEditSubcategory(category.id, subcategory)">
                        <Pencil class="mr-2 h-4 w-4" />
                        Editar
                      </Button>
                      <Button variant="outline" size="sm" @click="toggleSubcategory(subcategory)">
                        <Archive v-if="subcategory.active" class="mr-2 h-4 w-4" />
                        <RotateCcw v-else class="mr-2 h-4 w-4" />
                        {{ subcategory.active ? 'Arquivar' : 'Restaurar' }}
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </AccordionContent>
          </AccordionItem>
        </Card>
      </Accordion>
    </div>

    <Dialog :open="categoryDialogOpen" @update:open="(value) => { categoryDialogOpen = value; if (!value) resetCategoryForm(); }">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>{{ editingCategoryId ? 'Editar categoria' : 'Nova categoria' }}</DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-2">
          <div class="space-y-2">
            <Label for="category-name">Nome</Label>
            <Input id="category-name" v-model="categoryForm.name" placeholder="Ex: Tecnologia e Informatica" />
          </div>
          <div class="space-y-2">
            <Label for="category-sort">Ordem</Label>
            <Input id="category-sort" type="number" v-model="categoryForm.sortOrder" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="categoryDialogOpen = false">Cancelar</Button>
          <Button @click="saveCategory">Salvar</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <Dialog :open="subcategoryDialogOpen" @update:open="(value) => { subcategoryDialogOpen = value; if (!value) resetSubcategoryForm(); }">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>{{ editingSubcategoryId ? 'Editar subcategoria' : 'Nova subcategoria' }}</DialogTitle>
        </DialogHeader>
        <div class="space-y-4 py-2">
          <div class="space-y-2">
            <Label for="subcategory-name">Nome</Label>
            <Input id="subcategory-name" v-model="subcategoryForm.name" placeholder="Ex: Desenvolvimento Web" />
          </div>
          <div class="space-y-2">
            <Label for="subcategory-sort">Ordem</Label>
            <Input id="subcategory-sort" type="number" v-model="subcategoryForm.sortOrder" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" @click="subcategoryDialogOpen = false">Cancelar</Button>
          <Button @click="saveSubcategory">Salvar</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </AdminLayout>
</template>
