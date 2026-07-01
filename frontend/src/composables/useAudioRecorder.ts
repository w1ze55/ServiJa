import { computed, onUnmounted, ref } from 'vue';
import {
  buildRecordedAudioFile,
  getMicrophoneAccessErrorMessage,
  isAudioRecordingSupported,
  pickSupportedAudioMimeType,
} from '@/utils/chatAttachments';

export type AudioRecorderState = 'idle' | 'recording' | 'paused' | 'stopping' | 'error';

/**
 * Encapsula a gravação de áudio do chat (MediaRecorder + timer), com suporte a
 * pausar/retomar/cancelar. O áudio gravado é entregue como `File` pelo `stop()`,
 * mantendo o fluxo de envio via anexos.
 */
export function useAudioRecorder() {
  const recorderState = ref<AudioRecorderState>('idle');
  const recordingError = ref('');
  const recordingSeconds = ref(0);

  let mediaRecorder: MediaRecorder | null = null;
  let stream: MediaStream | null = null;
  let chunks: BlobPart[] = [];
  let timer: ReturnType<typeof setInterval> | null = null;
  let discard = false;
  let stopResolver: ((file: File | null) => void) | null = null;

  const isSupported = computed(() => isAudioRecordingSupported());

  const recordingElapsed = computed(() => {
    const total = recordingSeconds.value;
    const minutes = Math.floor(total / 60);
    const seconds = total % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  });

  function startTimer() {
    stopTimer();
    timer = setInterval(() => {
      recordingSeconds.value += 1;
    }, 1000);
  }

  function stopTimer() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }

  function cleanupStream() {
    stream?.getTracks().forEach((track) => track.stop());
    stream = null;
  }

  async function start(): Promise<boolean> {
    if (recorderState.value === 'recording' || recorderState.value === 'paused') {
      return false;
    }

    recordingError.value = '';

    if (!isSupported.value) {
      recorderState.value = 'error';
      recordingError.value = 'Gravação de áudio indisponível neste navegador.';
      return false;
    }

    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      chunks = [];
      discard = false;

      const preferredMimeType = pickSupportedAudioMimeType();
      try {
        mediaRecorder = preferredMimeType
          ? new MediaRecorder(stream, { mimeType: preferredMimeType })
          : new MediaRecorder(stream);
      } catch {
        mediaRecorder = new MediaRecorder(stream);
      }

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          chunks.push(event.data);
        }
      };

      mediaRecorder.onerror = () => {
        stopTimer();
        cleanupStream();
        recorderState.value = 'error';
        recordingError.value = 'Não foi possível gravar o áudio.';
        recordingSeconds.value = 0;
        const resolver = stopResolver;
        stopResolver = null;
        mediaRecorder = null;
        resolver?.(null);
      };

      mediaRecorder.onstop = () => {
        stopTimer();
        cleanupStream();

        const localChunks = chunks;
        chunks = [];
        const wasDiscarded = discard;
        const mimeType = mediaRecorder?.mimeType || preferredMimeType || 'audio/webm';
        mediaRecorder = null;

        if (recorderState.value !== 'error') {
          recorderState.value = 'idle';
        }
        recordingSeconds.value = 0;

        const resolver = stopResolver;
        stopResolver = null;

        if (wasDiscarded || localChunks.length === 0) {
          resolver?.(null);
          return;
        }

        const audioBlob = new Blob(localChunks, { type: mimeType });
        resolver?.(buildRecordedAudioFile(audioBlob));
      };

      mediaRecorder.start();
      startTimer();
      recorderState.value = 'recording';
      return true;
    } catch (error) {
      stopTimer();
      cleanupStream();
      mediaRecorder = null;
      recorderState.value = 'error';
      recordingError.value = getMicrophoneAccessErrorMessage(error);
      return false;
    }
  }

  function stop(): Promise<File | null> {
    if (!mediaRecorder || (recorderState.value !== 'recording' && recorderState.value !== 'paused')) {
      return Promise.resolve(null);
    }

    recorderState.value = 'stopping';
    stopTimer();

    return new Promise<File | null>((resolve) => {
      stopResolver = resolve;
      mediaRecorder?.stop();
    });
  }

  function cancel() {
    if (!mediaRecorder || (recorderState.value !== 'recording' && recorderState.value !== 'paused')) {
      recorderState.value = 'idle';
      recordingError.value = '';
      recordingSeconds.value = 0;
      return;
    }

    discard = true;
    recorderState.value = 'stopping';
    stopTimer();
    mediaRecorder.stop();
  }

  function pause() {
    if (mediaRecorder && recorderState.value === 'recording') {
      try {
        mediaRecorder.pause();
      } catch {
        return;
      }
      stopTimer();
      recorderState.value = 'paused';
    }
  }

  function resume() {
    if (mediaRecorder && recorderState.value === 'paused') {
      try {
        mediaRecorder.resume();
      } catch {
        return;
      }
      startTimer();
      recorderState.value = 'recording';
    }
  }

  function cleanup() {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.ondataavailable = null;
      mediaRecorder.onstop = null;
      mediaRecorder.onerror = null;
      mediaRecorder.stop();
    }
    stopTimer();
    cleanupStream();
    mediaRecorder = null;
    stopResolver = null;
  }

  onUnmounted(cleanup);

  return {
    recorderState,
    recordingError,
    recordingElapsed,
    isSupported,
    start,
    stop,
    pause,
    resume,
    cancel,
    cleanup,
  };
}
