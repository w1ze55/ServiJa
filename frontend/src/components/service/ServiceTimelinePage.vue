<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  ArrowLeft,
  CheckCircle2,
  Circle,
  Clock,
  MessageSquare,
  Paperclip,
  Send,
  Download,
  AlertCircle,
  User,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Textarea from '../ui/textarea.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Separator from '../ui/separator.vue';

interface Milestone {
  id: string;
  title: string;
  description: string;
  status: 'pending' | 'in-progress' | 'completed' | 'revision-requested';
  dueDate: string;
  completedDate?: string;
  comments: Comment[];
  files: FileAttachment[];
}

interface Comment {
  id: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  isFreelancer: boolean;
  message: string;
  timestamp: string;
}

interface FileAttachment {
  id: string;
  name: string;
  size: string;
  url: string;
  uploadedBy: string;
  uploadedAt: string;
}

interface Props {
  serviceId: string;
  serviceName: string;
  freelancerName: string;
  freelancerAvatar?: string;
  milestones: Milestone[];
  onBack: () => void;
  onAddComment: (milestoneId: string, comment: string) => void;
  onUploadFile: (milestoneId: string, file: File) => void;
  onRequestRevision: (milestoneId: string, reason: string) => void;
}

const props = defineProps<Props>();

const selectedMilestone = ref<string | null>(null);
const newComment = ref('');
const revisionReason = ref('');
const showRevisionForm = ref<string | null>(null);

const getStatusColor = (status: Milestone['status']) => {
  switch (status) {
    case 'completed':
      return 'bg-green-100 text-green-700';
    case 'in-progress':
      return 'bg-blue-100 text-blue-700';
    case 'revision-requested':
      return 'bg-yellow-100 text-yellow-700';
    default:
      return 'bg-gray-100 text-gray-700';
  }
};

const getStatusLabel = (status: Milestone['status']) => {
  switch (status) {
    case 'completed':
      return 'Concluído';
    case 'in-progress':
      return 'Em andamento';
    case 'revision-requested':
      return 'Revisão solicitada';
    default:
      return 'Pendente';
  }
};

const handleAddComment = (milestoneId: string) => {
  if (newComment.value.trim()) {
    props.onAddComment(milestoneId, newComment.value);
    newComment.value = '';
  }
};

const handleRequestRevision = (milestoneId: string) => {
  if (revisionReason.value.trim()) {
    props.onRequestRevision(milestoneId, revisionReason.value);
    revisionReason.value = '';
    showRevisionForm.value = null;
  }
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const completedMilestones = computed(() =>
  props.milestones.filter((m) => m.status === 'completed').length
);
const progress = computed(() => (completedMilestones.value / props.milestones.length) * 100);

const handleFileUpload = (milestoneId: string) => {
  const input = document.createElement('input');
  input.type = 'file';
  input.onchange = (e: Event) => {
    const target = e.target as HTMLInputElement;
    const file = target.files?.[0];
    if (file) props.onUploadFile(milestoneId, file);
  };
  input.click();
};
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center gap-4">
      <Button
        @click="props.onBack"
        variant="ghost"
        size="icon"
        class="hover:bg-gray-100"
      >
        <ArrowLeft class="h-5 w-5" />
      </Button>
      <div class="flex-1">
        <h1 class="text-[#263238]">Acompanhamento do Serviço</h1>
        <p class="text-[#435574]">{{ props.serviceName }}</p>
      </div>
    </div>

    <!-- Service Info Card -->
    <Card class="p-6">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-3">
          <Avatar class="h-12 w-12">
            <AvatarImage :src="props.freelancerAvatar" :alt="props.freelancerName" />
            <AvatarFallback class="bg-[#286AD8] text-white">
              {{ getInitials(props.freelancerName) }}
            </AvatarFallback>
          </Avatar>
          <div>
            <p class="text-[#263238]">{{ props.freelancerName }}</p>
            <p class="text-sm text-[#435574]">Freelancer</p>
          </div>
        </div>
        <Badge class="bg-blue-100 text-blue-700 hover:bg-blue-100">Em execução</Badge>
      </div>

      <!-- Progress Bar -->
      <div class="space-y-2">
        <div class="flex justify-between text-sm">
          <span class="text-[#435574]">Progresso geral</span>
          <span class="text-[#263238]"
            >{{ completedMilestones }} de {{ props.milestones.length }} marcos concluídos</span
          >
        </div>
        <div class="w-full bg-gray-200 rounded-full h-2">
          <div
            class="bg-[#286AD8] h-2 rounded-full transition-all duration-500"
            :style="{ width: `${progress}%` }"
          />
        </div>
      </div>
    </Card>

    <!-- Timeline -->
    <div class="space-y-6">
      <Card v-for="milestone in props.milestones" :key="milestone.id" class="p-6">
        <!-- Milestone Header -->
        <div class="flex items-start gap-4 mb-4">
          <!-- Status Icon -->
          <div class="flex-shrink-0 mt-1">
            <div
              v-if="milestone.status === 'completed'"
              class="bg-green-100 rounded-full p-2"
            >
              <CheckCircle2 class="h-6 w-6 text-green-600" />
            </div>
            <div
              v-else-if="milestone.status === 'in-progress'"
              class="bg-blue-100 rounded-full p-2 animate-pulse"
            >
              <Clock class="h-6 w-6 text-blue-600" />
            </div>
            <div
              v-else-if="milestone.status === 'revision-requested'"
              class="bg-yellow-100 rounded-full p-2"
            >
              <AlertCircle class="h-6 w-6 text-yellow-600" />
            </div>
            <div v-else class="bg-gray-100 rounded-full p-2">
              <Circle class="h-6 w-6 text-gray-400" />
            </div>
          </div>

          <!-- Milestone Info -->
          <div class="flex-1">
            <div class="flex items-start justify-between mb-2">
              <div>
                <h3 class="text-[#263238] mb-1">{{ milestone.title }}</h3>
                <p class="text-sm text-[#435574]">{{ milestone.description }}</p>
              </div>
              <Badge :class="getStatusColor(milestone.status)">
                {{ getStatusLabel(milestone.status) }}
              </Badge>
            </div>

            <!-- Dates -->
            <div class="flex items-center gap-4 text-sm text-[#435574] mb-4">
              <span>Prazo: {{ milestone.dueDate }}</span>
              <span v-if="milestone.completedDate" class="text-green-600"
                >Concluído em: {{ milestone.completedDate }}</span
              >
            </div>

            <!-- Files -->
            <div v-if="milestone.files.length > 0" class="mb-4">
              <p class="text-sm text-[#435574] mb-2">Arquivos anexados:</p>
              <div class="space-y-2">
                <div
                  v-for="file in milestone.files"
                  :key="file.id"
                  class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div class="flex items-center gap-2">
                    <Paperclip class="h-4 w-4 text-[#435574]" />
                    <div>
                      <p class="text-sm text-[#263238]">{{ file.name }}</p>
                      <p class="text-xs text-[#435574]">
                        {{ file.size }} • {{ file.uploadedBy }} • {{ file.uploadedAt }}
                      </p>
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    @click="window.open(file.url, '_blank')"
                  >
                    <Download class="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </div>

            <!-- Comments -->
            <div v-if="milestone.comments.length > 0" class="mb-4">
              <p class="text-sm text-[#435574] mb-3">Comentários:</p>
              <div class="space-y-3">
                <div v-for="comment in milestone.comments" :key="comment.id" class="flex gap-3">
                  <Avatar class="h-8 w-8">
                    <AvatarImage :src="comment.userAvatar" :alt="comment.userName" />
                    <AvatarFallback class="bg-gray-200 text-[#435574] text-xs">
                      {{ getInitials(comment.userName) }}
                    </AvatarFallback>
                  </Avatar>
                  <div class="flex-1 bg-gray-50 rounded-lg p-3">
                    <div class="flex items-center justify-between mb-1">
                      <div class="flex items-center gap-2">
                        <span class="text-sm text-[#263238]">{{ comment.userName }}</span>
                        <Badge v-if="comment.isFreelancer" variant="secondary" class="text-xs">
                          Freelancer
                        </Badge>
                      </div>
                      <span class="text-xs text-[#435574]">{{ comment.timestamp }}</span>
                    </div>
                    <p class="text-sm text-[#435574]">{{ comment.message }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Add Comment Form -->
            <div
              v-if="selectedMilestone === milestone.id"
              class="space-y-3 p-4 bg-blue-50 rounded-lg"
            >
              <Textarea
                placeholder="Escreva seu comentário..."
                v-model="newComment"
                :rows="3"
              />
              <div class="flex gap-2">
                <Button
                  @click="handleAddComment(milestone.id)"
                  class="bg-[#286AD8] hover:bg-[#435574]"
                  size="sm"
                >
                  <Send class="h-4 w-4 mr-2" />
                  Enviar
                </Button>
                <Button
                  @click="
                    selectedMilestone = null;
                    newComment = '';
                  "
                  variant="ghost"
                  size="sm"
                >
                  Cancelar
                </Button>
              </div>
            </div>

            <!-- Request Revision Form -->
            <div
              v-if="showRevisionForm === milestone.id"
              class="space-y-3 p-4 bg-yellow-50 rounded-lg border border-yellow-200"
            >
              <p class="text-sm text-[#263238]">Solicitar revisão</p>
              <Textarea
                placeholder="Descreva os ajustes necessários..."
                v-model="revisionReason"
                :rows="3"
              />
              <div class="flex gap-2">
                <Button
                  @click="handleRequestRevision(milestone.id)"
                  class="bg-yellow-600 hover:bg-yellow-700"
                  size="sm"
                >
                  Solicitar Revisão
                </Button>
                <Button
                  @click="
                    showRevisionForm = null;
                    revisionReason = '';
                  "
                  variant="ghost"
                  size="sm"
                >
                  Cancelar
                </Button>
              </div>
            </div>

            <!-- Actions -->
            <div v-if="!selectedMilestone && !showRevisionForm" class="flex gap-2 mt-4">
              <Button
                @click="selectedMilestone = milestone.id"
                variant="outline"
                size="sm"
                class="border-[#286AD8] text-[#286AD8]"
              >
                <MessageSquare class="h-4 w-4 mr-2" />
                Adicionar Comentário
              </Button>

              <Button
                v-if="milestone.status === 'in-progress'"
                @click="showRevisionForm = milestone.id"
                variant="outline"
                size="sm"
              >
                <AlertCircle class="h-4 w-4 mr-2" />
                Solicitar Revisão
              </Button>

              <Button
                @click="handleFileUpload(milestone.id)"
                variant="outline"
                size="sm"
              >
                <Paperclip class="h-4 w-4 mr-2" />
                Anexar Arquivo
              </Button>
            </div>
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>




