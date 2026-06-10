<script lang="ts">
// Compartilhado entre todas as instancias: garante que apenas um audio toca por vez.
let currentlyPlaying: HTMLAudioElement | null = null;
</script>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount } from 'vue';
import { Play, Pause } from 'lucide-vue-next';

interface Props {
  src: string;
  isOwn?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isOwn: false,
});

const audioRef = ref<HTMLAudioElement | null>(null);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const isSeeking = ref(false);
const needsDurationFix = ref(false);

function formatSeconds(value: number) {
  if (!isFinite(value) || value < 0) return '0:00';
  const total = Math.floor(value);
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

const displayTime = computed(() =>
  isPlaying.value || currentTime.value > 0
    ? formatSeconds(currentTime.value)
    : formatSeconds(duration.value)
);

function togglePlay() {
  const audio = audioRef.value;
  if (!audio) return;

  if (audio.paused) {
    void audio.play();
  } else {
    audio.pause();
  }
}

function handleLoadedMetadata() {
  const audio = audioRef.value;
  if (!audio) return;

  if (audio.duration === Infinity || isNaN(audio.duration)) {
    // Força o browser a percorrer o áudio e recalcular a duração real.
    needsDurationFix.value = true;
    audio.currentTime = 1e101;
    return;
  }

  duration.value = audio.duration;
}

function handleTimeUpdate() {
  const audio = audioRef.value;
  if (!audio) return;

  if (needsDurationFix.value) {
    needsDurationFix.value = false;
    if (isFinite(audio.duration)) {
      duration.value = audio.duration;
    }
    audio.currentTime = 0;
    return;
  }

  if (!isSeeking.value) {
    currentTime.value = audio.currentTime;
  }
}

function handlePlay() {
  isPlaying.value = true;
  // Pausa qualquer outro audio que esteja tocando.
  if (currentlyPlaying && currentlyPlaying !== audioRef.value) {
    currentlyPlaying.pause();
  }
  currentlyPlaying = audioRef.value;
}

function handlePause() {
  isPlaying.value = false;
  if (currentlyPlaying === audioRef.value) {
    currentlyPlaying = null;
  }
}

function handleEnded() {
  isPlaying.value = false;
  currentTime.value = 0;
  if (currentlyPlaying === audioRef.value) {
    currentlyPlaying = null;
  }
}

function onSeekInput(event: Event) {
  isSeeking.value = true;
  currentTime.value = Number((event.target as HTMLInputElement).value);
}

function onSeekCommit(event: Event) {
  const audio = audioRef.value;
  const value = Number((event.target as HTMLInputElement).value);
  if (audio && isFinite(value)) {
    audio.currentTime = value;
  }
  isSeeking.value = false;
}

onBeforeUnmount(() => {
  if (currentlyPlaying === audioRef.value) {
    currentlyPlaying = null;
  }
  audioRef.value?.pause();
});
</script>

<template>
  <div
    class="flex items-center gap-3 rounded-full px-3 py-2 max-w-[18rem]"
    :class="isOwn ? 'bg-white/15' : 'bg-white border border-gray-200'"
  >
    <audio
      ref="audioRef"
      :src="src"
      preload="metadata"
      class="hidden"
      @loadedmetadata="handleLoadedMetadata"
      @timeupdate="handleTimeUpdate"
      @ended="handleEnded"
      @play="handlePlay"
      @pause="handlePause"
    />

    <button
      type="button"
      class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full transition-colors"
      :class="
        isOwn
          ? 'bg-white text-[#286AD8] hover:bg-white/90'
          : 'bg-[#286AD8] text-white hover:bg-[#1f57b3]'
      "
      :title="isPlaying ? 'Pausar' : 'Reproduzir'"
      @click="togglePlay"
    >
      <Pause v-if="isPlaying" class="h-4 w-4 fill-current" />
      <Play v-else class="h-4 w-4 fill-current" />
    </button>

    <div class="flex flex-1 flex-col gap-1">
      <input
        type="range"
        min="0"
        :max="duration || 0"
        step="0.01"
        :value="currentTime"
        class="audio-range"
        :class="isOwn ? 'audio-range-own' : 'audio-range-other'"
        :disabled="!duration"
        @input="onSeekInput"
        @change="onSeekCommit"
      />
      <span
        class="text-left text-xs tabular-nums"
        :class="isOwn ? 'text-white/80' : 'text-[#435574]'"
      >
        {{ displayTime }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.audio-range {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 4px;
  border-radius: 9999px;
  cursor: pointer;
  outline: none;
}

.audio-range:disabled {
  cursor: default;
  opacity: 0.6;
}

.audio-range-own {
  background: rgba(255, 255, 255, 0.35);
}

.audio-range-other {
  background: #d1dae8;
}

.audio-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
}

.audio-range-own::-webkit-slider-thumb {
  background: #ffffff;
}

.audio-range-other::-webkit-slider-thumb {
  background: #286ad8;
}

.audio-range::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
}

.audio-range-own::-moz-range-thumb {
  background: #ffffff;
}

.audio-range-other::-moz-range-thumb {
  background: #286ad8;
}
</style>
