<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Package,
  Upload,
  Check,
  Clock,
  FileText,
  Download,
  Edit,
  Trash2,
  Plus,
  AlertCircle,
  Calendar,
  CheckCircle,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Progress from '../ui/progress.vue';
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
import { toast } from '../../utils/toast';

interface FileVersion {
  id: string;
  name: string;
  size: string;
  uploadedAt: string;
  version: number;
}

interface Milestone {
  id: string;
  title: string;
  description: string;
  deadline: string;
  status: 'pending' | 'in-progress' | 'submitted' | 'approved' | 'rejected';
  completedAt?: string;
  files: FileVersion[];
  clientFeedback?: string;
  criteria: string[];
}

interface Contract {
  id: string;
  service: string;
  clientName: string;
  totalAmount: number;
  milestones: Milestone[];
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const selectedContract = ref<string | null>(null);
const showMilestoneDialog = ref(false);
const showUploadDialog = ref(false);
const showDeleteDialog = ref(false);
const editingMilestone = ref<Milestone | null>(null);
const selectedMilestone = ref<Milestone | null>(null);
const milestoneToDelete = ref<Milestone | null>(null);
const uploadProgress = ref(0);
const isUploading = ref(false);

// Form states
const milestoneForm = ref({
  title: '',
  description: '',
  deadline: '',
  criteria: [''],
});

const uploadForm = ref({
  description: '',
  files: [] as File[],
});

// Mock data
const contracts = ref<Contract[]>([
  {
    id: '1',
    service: 'Instalação Elétrica Residencial',
    clientName: 'Maria Silva',
    totalAmount: 2500,
    milestones: [
      {
        id: 'm1',
        title: 'Planejamento e Projeto',
        description: 'Elaboração do projeto elétrico completo com diagrama unifilar',
        deadline: '2025-11-15',
        status: 'approved',
        completedAt: '2025-11-14 16:30',
        criteria: [
          'Diagrama unifilar completo',
          'Lista de materiais',
          'Cronograma de execução',
        ],
        files: [
          {
            id: 'f1',
            name: 'Projeto_Eletrico_v2.pdf',
            size: '2.3 MB',
            uploadedAt: '2025-11-14 16:30',
            version: 2,
          },
          {
            id: 'f2',
            name: 'Lista_Materiais.xlsx',
            size: '0.8 MB',
            uploadedAt: '2025-11-14 16:25',
            version: 1,
          },
        ],
        clientFeedback: 'Excelente trabalho! Projeto muito bem detalhado.',
      },
      {
        id: 'm2',
        title: 'Instalação dos Pontos de Luz',
        description: 'Instalação de todos os pontos de luz conforme projeto',
        deadline: '2025-11-20',
        status: 'submitted',
        completedAt: '2025-11-18 10:00',
        criteria: [
          'Instalação de 15 pontos de luz',
          'Fotos do trabalho concluído',
          'Testes de funcionamento',
        ],
        files: [
          {
            id: 'f3',
            name: 'Fotos_Instalacao.zip',
            size: '4.5 MB',
            uploadedAt: '2025-11-18 10:00',
            version: 1,
          },
        ],
      },
      {
        id: 'm3',
        title: 'Instalação de Tomadas',
        description: 'Instalação de todas as tomadas conforme projeto',
        deadline: '2025-11-25',
        status: 'in-progress',
        criteria: [
          'Instalação de 25 tomadas',
          'Testes de aterramento',
          'Documentação fotográfica',
        ],
        files: [],
      },
      {
        id: 'm4',
        title: 'Quadro de Distribuição',
        description: 'Montagem e instalação do quadro de distribuição',
        deadline: '2025-11-30',
        status: 'pending',
        criteria: [
          'Instalação do quadro',
          'Organização dos disjuntores',
          'Etiquetagem dos circuitos',
          'Testes finais',
        ],
        files: [],
      },
    ],
  },
]);

const currentContract = computed(() =>
  contracts.value.find((c) => c.id === (selectedContract.value || '1'))
);

const handleCreateMilestone = () => {
  if (!milestoneForm.value.title || !milestoneForm.value.description || !milestoneForm.value.deadline) {
    toast.error('Preencha todos os campos obrigatórios');
    return;
  }

  toast.success('Marco criado com sucesso!');
  showMilestoneDialog.value = false;
  milestoneForm.value = { title: '', description: '', deadline: '', criteria: [''] };
};

const handleUpdateMilestone = () => {
  if (!milestoneForm.value.title || !milestoneForm.value.description || !milestoneForm.value.deadline) {
    toast.error('Preencha todos os campos obrigatórios');
    return;
  }

  toast.success('Marco atualizado com sucesso!');
  showMilestoneDialog.value = false;
  editingMilestone.value = null;
  milestoneForm.value = { title: '', description: '', deadline: '', criteria: [''] };
};

const handleUploadFiles = () => {
  if (uploadForm.value.files.length === 0) {
    toast.error('Selecione pelo menos um arquivo');
    return;
  }

  // Check file sizes
  const oversizedFiles = uploadForm.value.files.filter((f) => f.size > 5 * 1024 * 1024);
  if (oversizedFiles.length > 0) {
    toast.error('Alguns arquivos excedem o limite de 5 MB');
    return;
  }

  isUploading.value = true;
  uploadProgress.value = 0;

  // Simulate upload progress
  const interval = setInterval(() => {
    uploadProgress.value += 10;
    if (uploadProgress.value >= 100) {
      clearInterval(interval);
      isUploading.value = false;
      toast.success('Arquivos enviados com sucesso!');
      showUploadDialog.value = false;
      uploadForm.value = { description: '', files: [] };
    }
  }, 200);
};

const handleRequestApproval = (milestone: Milestone) => {
  if (milestone.files.length === 0) {
    toast.error('Envie pelo menos um arquivo antes de solicitar aprovação');
    return;
  }
  toast.success('Solicitação de aprovação enviada ao cliente!');
};

const handleDeleteMilestone = () => {
  if (milestoneToDelete.value) {
    toast.success('Marco excluído com sucesso');
    milestoneToDelete.value = null;
    showDeleteDialog.value = false;
  }
};

const handleEditMilestone = (milestone: Milestone) => {
  editingMilestone.value = milestone;
  milestoneForm.value = {
    title: milestone.title,
    description: milestone.description,
    deadline: milestone.deadline,
    criteria: milestone.criteria,
  };
  showMilestoneDialog.value = true;
};

const addCriteria = () => {
  milestoneForm.value = {
    ...milestoneForm.value,
    criteria: [...milestoneForm.value.criteria, ''],
  };
};

const removeCriteria = (index: number) => {
  milestoneForm.value = {
    ...milestoneForm.value,
    criteria: milestoneForm.value.criteria.filter((_, i) => i !== index),
  };
};

const updateCriteria = (index: number, value: string) => {
  const newCriteria = [...milestoneForm.value.criteria];
  newCriteria[index] = value;
  milestoneForm.value = { ...milestoneForm.value, criteria: newCriteria };
};

// Status badge removed - using templates directly

const calculateProgress = computed(() => {
  if (!currentContract.value) return 0;
  const total = currentContract.value.milestones.length;
  const completed = currentContract.value.milestones.filter((m) => m.status === 'approved').length;
  return (completed / total) * 100;
});

const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files) {
    uploadForm.value = { ...uploadForm.value, files: Array.from(target.files) };
  }
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-[#263238] mb-2">Entregas & Marcos</h1>
        <p class="text-[#435574]">Gerencie os marcos de entrega dos seus contratos</p>
      </div>
      <Button
        @click="
          editingMilestone = null;
          milestoneForm = { title: '', description: '', deadline: '', criteria: [''] };
          showMilestoneDialog = true;
        "
        class="bg-[#286AD8] hover:bg-[#435574]"
      >
        <Plus class="h-4 w-4 mr-2" />
        Novo Marco
      </Button>
    </div>

    <!-- Contract Summary -->
    <Card v-if="currentContract" class="p-6">
      <div class="flex items-start justify-between mb-4">
        <div>
          <h3 class="text-[#263238] mb-1">{{ currentContract.service }}</h3>
          <p class="text-sm text-[#435574]">Cliente: {{ currentContract.clientName }}</p>
        </div>
        <div class="text-right">
          <p class="text-sm text-[#435574]">Valor Total</p>
          <p class="text-xl text-[#263238]">
            {{ new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(currentContract.totalAmount) }}
          </p>
        </div>
      </div>

      <div class="space-y-2">
        <div class="flex items-center justify-between text-sm">
          <span class="text-[#435574]">Progresso do Projeto</span>
          <span class="text-[#263238]">{{ Math.round(calculateProgress) }}%</span>
        </div>
        <Progress :value="calculateProgress" class="h-2" />
        <p class="text-xs text-[#435574]">
          {{ currentContract.milestones.filter((m) => m.status === 'approved').length }} de
          {{ currentContract.milestones.length }} marcos concluídos
        </p>
      </div>
    </Card>

    <!-- Milestones List -->
    <div class="space-y-4">
      <Card
        v-for="(milestone, index) in currentContract?.milestones"
        :key="milestone.id"
        class="p-6"
      >
        <div class="flex items-start gap-4">
          <!-- Timeline -->
          <div class="flex flex-col items-center">
            <div
              :class="`w-10 h-10 rounded-full flex items-center justify-center ${
                milestone.status === 'approved'
                  ? 'bg-green-100'
                  : milestone.status === 'submitted'
                  ? 'bg-yellow-100'
                  : milestone.status === 'in-progress'
                  ? 'bg-blue-100'
                  : 'bg-gray-100'
              }`"
            >
              <Check v-if="milestone.status === 'approved'" class="h-5 w-5 text-green-600" />
              <span v-else class="text-sm text-[#263238]">{{ index + 1 }}</span>
            </div>
            <div
              v-if="index < (currentContract?.milestones.length || 0) - 1"
              class="w-0.5 h-full bg-gray-200 my-2 flex-1"
            />
          </div>

          <!-- Content -->
          <div class="flex-1">
            <div class="flex items-start justify-between mb-3">
              <div class="flex-1">
                <div class="flex items-center gap-2 mb-1">
                  <h3 class="text-[#263238]">{{ milestone.title }}</h3>
                  <template v-if="milestone.status === 'pending'">
                    <Badge variant="secondary">
                      <Clock class="h-3 w-3 mr-1" />
                      Pendente
                    </Badge>
                  </template>
                  <template v-else-if="milestone.status === 'in-progress'">
                    <Badge class="bg-blue-100 text-blue-700 hover:bg-blue-100">
                      <Clock class="h-3 w-3 mr-1" />
                      Em Andamento
                    </Badge>
                  </template>
                  <template v-else-if="milestone.status === 'submitted'">
                    <Badge class="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">
                      <AlertCircle class="h-3 w-3 mr-1" />
                      Aguardando Aprovação
                    </Badge>
                  </template>
                  <template v-else-if="milestone.status === 'approved'">
                    <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                      <CheckCircle class="h-3 w-3 mr-1" />
                      Aprovado
                    </Badge>
                  </template>
                  <template v-else-if="milestone.status === 'rejected'">
                    <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
                      <AlertCircle class="h-3 w-3 mr-1" />
                      Rejeitado
                    </Badge>
                  </template>
                </div>
                <p class="text-sm text-[#435574] mb-2">{{ milestone.description }}</p>
                <div class="flex items-center gap-4 text-sm text-[#435574]">
                  <span class="flex items-center gap-1">
                    <Calendar class="h-4 w-4" />
                    Prazo: {{ new Date(milestone.deadline).toLocaleDateString('pt-BR') }}
                  </span>
                  <span
                    v-if="milestone.completedAt"
                    class="flex items-center gap-1 text-green-600"
                  >
                    <CheckCircle class="h-4 w-4" />
                    Concluído em {{ milestone.completedAt }}
                  </span>
                </div>
              </div>

              <!-- Actions -->
              <div v-if="milestone.status !== 'approved'" class="flex gap-2">
                <Button
                  @click="handleEditMilestone(milestone)"
                  variant="ghost"
                  size="icon"
                >
                  <Edit class="h-4 w-4" />
                </Button>
                <Button
                  @click="
                    milestoneToDelete = milestone;
                    showDeleteDialog = true;
                  "
                  variant="ghost"
                  size="icon"
                  class="text-red-600 hover:text-red-700 hover:bg-red-50"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </div>

            <!-- Criteria -->
            <div v-if="milestone.criteria.length > 0" class="mb-3 p-3 bg-gray-50 rounded-lg">
              <p class="text-sm text-[#263238] mb-2">Critérios de Conclusão:</p>
              <ul class="space-y-1">
                <li
                  v-for="(criteria, idx) in milestone.criteria"
                  :key="idx"
                  class="text-sm text-[#435574] flex items-start gap-2"
                >
                  <CheckCircle class="h-4 w-4 text-green-600 flex-shrink-0 mt-0.5" />
                  {{ criteria }}
                </li>
              </ul>
            </div>

            <!-- Files -->
            <div v-if="milestone.files.length > 0" class="mb-3">
              <p class="text-sm text-[#263238] mb-2">Arquivos Enviados:</p>
              <div class="space-y-2">
                <div
                  v-for="file in milestone.files"
                  :key="file.id"
                  class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div class="flex items-center gap-3">
                    <FileText class="h-5 w-5 text-[#286AD8]" />
                    <div>
                      <p class="text-sm text-[#263238]">{{ file.name }}</p>
                      <p class="text-xs text-[#435574]">
                        {{ file.size }} • v{{ file.version }} • {{ file.uploadedAt }}
                      </p>
                    </div>
                  </div>
                  <Button variant="ghost" size="icon">
                    <Download class="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </div>

            <!-- Client Feedback -->
            <div
              v-if="milestone.clientFeedback"
              class="p-3 bg-green-50 border border-green-200 rounded-lg mb-3"
            >
              <p class="text-sm text-[#263238] mb-1">Feedback do Cliente:</p>
              <p class="text-sm text-green-800">{{ milestone.clientFeedback }}</p>
            </div>

            <!-- Action Buttons -->
            <div v-if="milestone.status !== 'approved'" class="flex gap-2">
              <template v-if="milestone.status === 'pending' || milestone.status === 'in-progress'">
                <Button
                  @click="
                    selectedMilestone = milestone;
                    showUploadDialog = true;
                  "
                  size="sm"
                  variant="outline"
                  class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
                >
                  <Upload class="h-4 w-4 mr-2" />
                  Enviar Arquivos
                </Button>
                <Button
                  v-if="milestone.files.length > 0"
                  @click="handleRequestApproval(milestone)"
                  size="sm"
                  class="bg-[#286AD8] hover:bg-[#435574]"
                >
                  <Check class="h-4 w-4 mr-2" />
                  Solicitar Aprovação
                </Button>
              </template>
              <p v-else-if="milestone.status === 'submitted'" class="text-sm text-yellow-600">
                Aguardando aprovação do cliente...
              </p>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <!-- Create/Edit Milestone Dialog -->
    <Dialog :open="showMilestoneDialog" @update:open="(val) => showMilestoneDialog = val">
      <DialogContent class="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            {{ editingMilestone ? 'Editar Marco' : 'Criar Novo Marco' }}
          </DialogTitle>
          <DialogDescription>
            Defina os detalhes do marco de entrega
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="title">Título do Marco *</Label>
            <Input
              id="title"
              :model-value="milestoneForm.title"
              @update:model-value="(val) => milestoneForm.title = val as string"
              placeholder="Ex: Planejamento e Projeto"
            />
          </div>

          <div class="space-y-2">
            <Label for="description">Descrição *</Label>
            <Textarea
              id="description"
              :model-value="milestoneForm.description"
              @update:model-value="(val) => milestoneForm.description = val as string"
              placeholder="Descreva o que será entregue neste marco..."
              :rows="3"
            />
          </div>

          <div class="space-y-2">
            <Label for="deadline">Prazo de Entrega *</Label>
            <Input
              id="deadline"
              type="date"
              :model-value="milestoneForm.deadline"
              @update:model-value="(val) => milestoneForm.deadline = val as string"
            />
          </div>

          <div class="space-y-2">
            <Label>Critérios de Conclusão</Label>
            <div class="space-y-2">
              <div v-for="(criteria, index) in milestoneForm.criteria" :key="index" class="flex gap-2">
                <Input
                  :model-value="criteria"
                  @update:model-value="(val) => updateCriteria(index, val as string)"
                  placeholder="Ex: Instalação completa dos pontos de luz"
                />
                <Button
                  v-if="milestoneForm.criteria.length > 1"
                  @click="removeCriteria(index)"
                  variant="ghost"
                  size="icon"
                  class="text-red-600"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </div>
            <Button @click="addCriteria" variant="outline" size="sm" class="mt-2">
              <Plus class="h-4 w-4 mr-2" />
              Adicionar Critério
            </Button>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showMilestoneDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button
            @click="editingMilestone ? handleUpdateMilestone() : handleCreateMilestone()"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            {{ editingMilestone ? 'Atualizar' : 'Criar' }} Marco
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Upload Files Dialog -->
    <Dialog :open="showUploadDialog" @update:open="(val) => showUploadDialog = val">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Enviar Arquivos</DialogTitle>
          <DialogDescription>
            Faça upload dos arquivos relacionados ao marco "{{ selectedMilestone?.title }}"
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="file-description">Descrição (opcional)</Label>
            <Textarea
              id="file-description"
              :model-value="uploadForm.description"
              @update:model-value="(val) => uploadForm.description = val as string"
              placeholder="Adicione uma descrição sobre os arquivos..."
              :rows="2"
            />
          </div>

          <div class="space-y-2">
            <Label>Arquivos *</Label>
            <div class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-[#286AD8] transition-colors cursor-pointer">
              <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p class="text-[#263238] mb-2">
                Clique para fazer upload ou arraste os arquivos
              </p>
              <p class="text-sm text-[#435574] mb-4">
                PDF, JPG, PNG, ZIP • Máximo 5 MB por arquivo
              </p>
              <input
                type="file"
                multiple
                @change="handleFileSelect"
                class="hidden"
                id="file-upload"
              />
              <Button @click="document.getElementById('file-upload')?.click()" size="sm">
                Selecionar Arquivos
              </Button>
            </div>

            <div v-if="uploadForm.files.length > 0" class="mt-3 space-y-2">
              <p class="text-sm text-[#263238]">
                Arquivos selecionados ({{ uploadForm.files.length }}):
              </p>
              <div
                v-for="(file, index) in uploadForm.files"
                :key="index"
                class="flex items-center justify-between p-2 bg-gray-50 rounded"
              >
                <span class="text-sm text-[#263238]">{{ file.name }}</span>
                <span class="text-xs text-[#435574]">
                  {{ (file.size / (1024 * 1024)).toFixed(2) }} MB
                </span>
              </div>
            </div>
          </div>

          <div v-if="isUploading" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-[#435574]">Enviando arquivos...</span>
              <span class="text-[#263238]">{{ uploadProgress }}%</span>
            </div>
            <Progress :value="uploadProgress" class="h-2" />
          </div>
        </div>

        <DialogFooter>
          <Button @click="showUploadDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button
            @click="handleUploadFiles"
            :disabled="isUploading"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            <Upload class="h-4 w-4 mr-2" />
            Enviar Arquivos
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Delete Confirmation -->
    <AlertDialog :open="showDeleteDialog" @update:open="(val) => showDeleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Excluir Marco</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja excluir o marco "{{ milestoneToDelete?.title }}"?
            Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleDeleteMilestone"
            class="bg-red-600 hover:bg-red-700"
          >
            Excluir
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>

