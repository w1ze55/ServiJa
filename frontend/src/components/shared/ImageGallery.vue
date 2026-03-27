<script setup lang="ts">
import { ref, watch } from 'vue';
import { ChevronLeft, ChevronRight, Briefcase } from 'lucide-vue-next';

interface Props {
  images: string[];
  alt?: string;
}

const props = withDefaults(defineProps<Props>(), {
  alt: 'Imagem',
});

const activeImageIndex = ref(0);

watch(
  () => props.images,
  () => {
    activeImageIndex.value = 0;
  }
);

const prevImage = () => {
  if (!props.images.length) return;
  activeImageIndex.value = (activeImageIndex.value - 1 + props.images.length) % props.images.length;
};

const nextImage = () => {
  if (!props.images.length) return;
  activeImageIndex.value = (activeImageIndex.value + 1) % props.images.length;
};
</script>

<template>
  <div class="relative rounded-2xl overflow-hidden bg-gray-900 shadow-xl">
    <!-- Main image -->
    <div class="relative aspect-video">
      <img
        v-if="images && images.length > 0"
        :src="images[activeImageIndex]"
        :alt="alt"
        class="w-full h-full object-cover"
      />
      <div v-else class="w-full h-full flex flex-col items-center justify-center text-white/30">
        <Briefcase class="h-20 w-20 mb-4" />
        <span>Nenhuma imagem disponível</span>
      </div>

      <!-- Navigation arrows -->
      <template v-if="images && images.length > 1">
        <button
          @click="prevImage"
          class="absolute left-4 top-1/2 -translate-y-1/2 p-2.5 bg-black/40 hover:bg-black/70 text-white rounded-full transition-all backdrop-blur-sm"
        >
          <ChevronLeft class="h-5 w-5" />
        </button>
        <button
          @click="nextImage"
          class="absolute right-4 top-1/2 -translate-y-1/2 p-2.5 bg-black/40 hover:bg-black/70 text-white rounded-full transition-all backdrop-blur-sm"
        >
          <ChevronRight class="h-5 w-5" />
        </button>
        <!-- Indicators -->
        <div class="absolute bottom-4 left-1/2 -translate-x-1/2 flex gap-1.5">
          <span
            v-for="(_, idx) in images"
            :key="idx"
            class="h-1.5 rounded-full transition-all duration-300"
            :class="activeImageIndex === idx ? 'w-6 bg-white' : 'w-1.5 bg-white/50'"
          />
        </div>
      </template>
    </div>

    <!-- Thumbnails row -->
    <div v-if="images && images.length > 1" class="flex gap-2 p-3 bg-black/20 overflow-x-auto">
      <button
        v-for="(img, idx) in images"
        :key="idx"
        @click="activeImageIndex = idx"
        class="w-16 h-16 rounded-lg overflow-hidden border-2 shrink-0 transition-all"
        :class="activeImageIndex === idx ? 'border-[#286AD8] scale-105' : 'border-transparent opacity-60 hover:opacity-100'"
      >
        <img :src="img" class="w-full h-full object-cover" />
      </button>
    </div>
  </div>
</template>
