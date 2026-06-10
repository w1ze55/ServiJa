export type ChatAttachmentKind = 'image' | 'audio' | 'file';
export type ChatMessageType = 'text' | 'image' | 'file' | 'audio' | 'system';

const IMAGE_EXT_RE = /\.(jpg|jpeg|png|gif|webp|bmp|svg)(\?.*)?$/i;
const AUDIO_EXT_RE = /\.(mp3|m4a|wav|ogg|webm|aac)(\?.*)?$/i;

const RECORDER_MIME_CANDIDATES = [
  'audio/webm;codecs=opus',
  'audio/webm',
  'audio/mp4;codecs=mp4a.40.2',
  'audio/mp4',
  'audio/ogg',
  'audio/aac',
] as const;

export const CHAT_ATTACHMENT_ACCEPT =
  'image/*,audio/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt';

export function isImageUrl(url?: string | null) {
  return !!url && IMAGE_EXT_RE.test(url);
}

export function isAudioUrl(url?: string | null) {
  return !!url && AUDIO_EXT_RE.test(url);
}

export function isImageFile(file: File) {
  return file.type.startsWith('image/') || IMAGE_EXT_RE.test(file.name);
}

export function isAudioFile(file: File) {
  return file.type.startsWith('audio/') || AUDIO_EXT_RE.test(file.name);
}

export function resolveAttachmentKindFromUrl(url?: string | null): ChatAttachmentKind {
  if (isImageUrl(url)) {
    return 'image';
  }
  if (isAudioUrl(url)) {
    return 'audio';
  }
  return 'file';
}

export function resolveAttachmentKindFromFile(file: File): ChatAttachmentKind {
  if (isImageFile(file)) {
    return 'image';
  }
  if (isAudioFile(file)) {
    return 'audio';
  }
  return 'file';
}

export function resolveChatMessageType(
  type?: string | null,
  attachmentUrl?: string | null
): ChatMessageType {
  if (type === 'text' || type === 'image' || type === 'file' || type === 'audio' || type === 'system') {
    return type;
  }
  if (attachmentUrl) {
    return resolveAttachmentKindFromUrl(attachmentUrl);
  }
  return 'text';
}

export function getAttachmentPlaceholder(type: ChatMessageType) {
  switch (type) {
    case 'image':
      return 'Imagem';
    case 'audio':
      return 'Audio';
    case 'file':
      return 'Arquivo';
    default:
      return '';
  }
}

export function shouldHideAttachmentPlaceholder(
  content: string | null | undefined,
  type: ChatMessageType,
  hasAttachment: boolean
) {
  if (!content || !hasAttachment) {
    return false;
  }
  return content === getAttachmentPlaceholder(type);
}

export function pickSupportedAudioMimeType() {
  if (typeof MediaRecorder === 'undefined') {
    return null;
  }

  if (typeof MediaRecorder.isTypeSupported !== 'function') {
    return RECORDER_MIME_CANDIDATES[0];
  }

  for (const mimeType of RECORDER_MIME_CANDIDATES) {
    if (MediaRecorder.isTypeSupported(mimeType)) {
      return mimeType;
    }
  }

  return null;
}

export function isAudioRecordingSupported() {
  return (
    typeof window !== 'undefined' &&
    typeof navigator !== 'undefined' &&
    !!navigator.mediaDevices?.getUserMedia &&
    typeof MediaRecorder !== 'undefined'
  );
}

export function getAudioRecordingUnavailableReason() {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return 'Gravacao de audio indisponivel neste contexto.';
  }
  if (!window.isSecureContext) {
    return 'O microfone so funciona em HTTPS ou localhost.';
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    return 'O navegador nao disponibiliza acesso ao microfone.';
  }
  if (typeof MediaRecorder === 'undefined') {
    return 'Este navegador nao suporta gravacao de audio.';
  }
  return 'Gravacao de audio indisponivel neste navegador.';
}

export function getMicrophoneAccessErrorMessage(error: unknown) {
  if (typeof window !== 'undefined' && !window.isSecureContext) {
    return 'O microfone so funciona em HTTPS ou localhost.';
  }

  const errorName =
    typeof error === 'object' && error !== null && 'name' in error
      ? String((error as { name?: unknown }).name)
      : '';

  switch (errorName) {
    case 'NotAllowedError':
    case 'PermissionDeniedError':
      return 'Permissao do microfone negada ou bloqueada pelo navegador.';
    case 'SecurityError':
      return 'O navegador bloqueou o acesso ao microfone nesta pagina.';
    case 'NotFoundError':
    case 'DevicesNotFoundError':
      return 'Nenhum microfone foi encontrado no dispositivo.';
    case 'NotReadableError':
    case 'TrackStartError':
      return 'O microfone esta em uso por outro aplicativo ou indisponivel.';
    case 'OverconstrainedError':
    case 'ConstraintNotSatisfiedError':
      return 'Nao foi possivel iniciar o microfone com as configuracoes atuais.';
    case 'AbortError':
      return 'A captura de audio foi interrompida antes de iniciar.';
    case 'TypeError':
      return 'O microfone so funciona em HTTPS ou localhost.';
    default:
      return 'Nao foi possivel acessar o microfone.';
  }
}

export function logAudioRecordingError(scope: string, error: unknown) {
  const browserContext =
    typeof window === 'undefined' || typeof navigator === 'undefined'
      ? {}
      : {
          isSecureContext: window.isSecureContext,
          hasMediaDevices: !!navigator.mediaDevices,
          hasGetUserMedia: !!navigator.mediaDevices?.getUserMedia,
          hasMediaRecorder: typeof MediaRecorder !== 'undefined',
          preferredMimeType: pickSupportedAudioMimeType(),
        };

  console.error(`[${scope}] Falha ao iniciar gravacao de audio`, {
    error,
    ...browserContext,
  });
}

function extensionFromMimeType(mimeType: string) {
  if (mimeType.includes('mp4')) {
    return '.m4a';
  }
  if (mimeType.includes('ogg')) {
    return '.ogg';
  }
  if (mimeType.includes('aac')) {
    return '.aac';
  }
  if (mimeType.includes('wav')) {
    return '.wav';
  }
  if (mimeType.includes('mpeg') || mimeType.includes('mp3')) {
    return '.mp3';
  }
  return '.webm';
}

function padFilePart(value: number) {
  return value.toString().padStart(2, '0');
}

export function buildRecordedAudioFile(blob: Blob, recordedAt = new Date()) {
  const mimeType = blob.type || pickSupportedAudioMimeType() || 'audio/webm';
  const extension = extensionFromMimeType(mimeType);
  const timestamp = [
    recordedAt.getFullYear(),
    padFilePart(recordedAt.getMonth() + 1),
    padFilePart(recordedAt.getDate()),
  ].join('');
  const time = [
    padFilePart(recordedAt.getHours()),
    padFilePart(recordedAt.getMinutes()),
    padFilePart(recordedAt.getSeconds()),
  ].join('');

  return new File([blob], `audio_${timestamp}_${time}${extension}`, {
    type: mimeType,
    lastModified: recordedAt.getTime(),
  });
}
