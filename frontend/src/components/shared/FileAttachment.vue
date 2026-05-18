<script setup lang="ts">
import { 
  FileText, 
  Eye, 
  Download,
  Upload,
  Image as ImageIcon,
  Film,
  FileArchive,
  FileCode,
  File as FileIcon
} from 'lucide-vue-next';
import Button from '../ui/button.vue';

interface Props {
  name: string;
  url?: string;
  size?: string;
  type?: string;
  description?: string;
  senderInfo?: string;
  variant?: 'default' | 'amber' | 'blue' | 'compact';
  mode?: 'display' | 'picker';
  actionLabel?: string;
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  mode: 'display',
  actionLabel: 'Selecionar',
});

const emit = defineEmits<{
  (e: 'action'): void;
}>();

const getFileIcon = (fileName: string, mimeType?: string) => {
  if (props.mode === 'picker') {
    return Upload;
  }

  const extension = fileName.split('.').pop()?.toLowerCase();
  
  if (mimeType?.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension || '')) {
    return ImageIcon;
  }
  if (mimeType?.startsWith('video/') || ['mp4', 'webm', 'mov', 'avi'].includes(extension || '')) {
    return Film;
  }
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension || '')) {
    return FileArchive;
  }
  if (['js', 'ts', 'html', 'css', 'json', 'py', 'java', 'c', 'cpp'].includes(extension || '')) {
    return FileCode;
  }
  if (['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'].includes(extension || '')) {
    return FileText;
  }
  
  return FileIcon;
};

const handleView = () => {
  if (!props.url) return;
  window.open(props.url, '_blank');
};

const handleDownload = async () => {
  if (!props.url) return;

  try {
    const response = await fetch(props.url);
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = props.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(blobUrl);
  } catch {
    window.open(props.url, '_blank');
  }
};

const variantClasses = {
  default: 'bg-gray-50 border-gray-100 hover:bg-white',
  amber: 'bg-blue-50 border-blue-100 hover:bg-white',
  blue: 'bg-blue-50 border-blue-100 hover:bg-white',
  compact: 'bg-transparent border-transparent p-0 shadow-none hover:bg-gray-50',
};

const iconClasses = {
  default: 'bg-gray-100 text-gray-600',
  amber: 'bg-blue-100 text-blue-600',
  blue: 'bg-blue-100 text-blue-600',
  compact: 'bg-gray-100 text-gray-500',
};

const rootClasses = {
  display: 'shadow-sm',
  picker: 'cursor-pointer shadow-none hover:border-[#286AD8] hover:bg-blue-50/40',
};

const handleAction = () => {
  emit('action');
};
</script>

<template>
  <div 
    :class="[
      'flex items-center justify-between p-3 rounded-xl border transition-all group shadow-sm',
      variantClasses[variant],
      rootClasses[mode]
    ]"
    @click="mode === 'picker' && handleAction()"
  >
    <div class="flex items-center gap-3 overflow-hidden">
      <div 
        :class="[
          'w-10 h-10 rounded-lg flex items-center justify-center shrink-0',
          iconClasses[variant]
        ]"
      >
        <component :is="getFileIcon(name, type)" class="h-5 w-5" />
      </div>
      <div class="min-w-0">
        <p class="text-sm font-semibold text-[#263238] truncate" :title="name">
          {{ name }}
        </p>
        <div class="flex items-center gap-2 mt-0.5">
          <span v-if="size" class="text-xs text-[#435574]">{{ size }}</span>
          <span v-if="size && senderInfo" class="w-1 h-1 rounded-full bg-gray-300"></span>
          <span v-if="senderInfo" class="text-xs text-[#435574]">{{ senderInfo }}</span>
        </div>
        <p v-if="description" class="text-xs text-[#435574] mt-1 whitespace-pre-line break-words">
          {{ description }}
        </p>
      </div>
    </div>
    
    <div v-if="mode === 'display'" class="flex items-center gap-1 shrink-0">
      <Button 
        variant="ghost" 
        size="icon" 
        class="h-8 w-8 text-muted-foreground hover:text-primary hover:bg-blue-50"
        title="Visualizar"
        @click="handleView"
      >
        <Eye class="h-4 w-4" />
      </Button>
      <Button 
        variant="ghost" 
        size="icon" 
        class="h-8 w-8 text-muted-foreground hover:text-primary hover:bg-blue-50"
        title="Baixar"
        @click="handleDownload"
      >
        <Download class="h-4 w-4" />
      </Button>
    </div>
    <div v-else class="shrink-0">
      <Button
        type="button"
        variant="outline"
        class="pointer-events-none"
        @click="handleAction"
      >
        {{ actionLabel }}
      </Button>
    </div>
  </div>
</template>

<style scoped>
.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
