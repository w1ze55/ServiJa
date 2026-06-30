import { AxiosError } from 'axios';
import api from './config/axios';

/**
 * Extensões de imagem aceitas pelo backend (mantido em sincronia com
 * FileStorageService.IMAGE_EXTENSIONS). Usado no atributo `accept` e na
 * validação client-side por extensão (alguns formatos, como .jfif, chegam
 * com `file.type` vazio dependendo do SO/navegador).
 */
export const ACCEPTED_IMAGE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.jfif', '.webp', '.gif'];

export const ACCEPTED_IMAGE_ACCEPT_ATTR = `${ACCEPTED_IMAGE_EXTENSIONS.join(',')},image/*`;

/**
 * Verifica se um arquivo é uma imagem aceita, por MIME type ou extensão.
 */
export const isAcceptedImageFile = (file: File): boolean => {
  if (file.type.startsWith('image/')) {
    return true;
  }
  const name = file.name.toLowerCase();
  return ACCEPTED_IMAGE_EXTENSIONS.some((ext) => name.endsWith(ext));
};

/**
 * Traduz um erro de upload (AxiosError) em uma mensagem amigável e específica,
 * distinguindo tamanho (413), validação do backend (400) e falha de servidor.
 */
export const getUploadErrorMessage = (error: unknown): string => {
  const axiosError = error as AxiosError<{ error?: string; message?: string }>;
  const status = axiosError?.response?.status;
  const data = axiosError?.response?.data;

  if (status === 413) {
    return 'Arquivo muito grande. O tamanho máximo permitido é 10MB por arquivo.';
  }

  if (status === 400) {
    return data?.error || data?.message || 'Arquivo inválido. Verifique o formato e o tamanho.';
  }

  return 'Não foi possível enviar o arquivo. Tente novamente.';
};

export interface UploadResponse {
  url: string;
}

export interface MultipleUploadResponse {
  urls: string[];
}

/**
 * Upload de uma única imagem
 */
export const uploadImage = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.post<UploadResponse>('/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data.url;
};

/**
 * Upload de múltiplas imagens
 */
export const uploadImages = async (files: File[]): Promise<string[]> => {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<MultipleUploadResponse>('/upload/images', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data.urls;
};

/**
 * Deletar uma imagem
 */
export const deleteImage = async (url: string): Promise<void> => {
  await api.delete('/upload/image', {
    params: { url },
  });
};

/**
 * Upload de avatar do usuário atual
 */
export const uploadAvatar = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await api.put<{ avatarUrl: string }>('/users/me/avatar/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data.avatarUrl;
};

/**
 * Upload de arquivos genéricos (imagens, PDFs, docs, etc.)
 */
export const uploadFiles = async (files: File[]): Promise<string[]> => {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });

  const response = await api.post<MultipleUploadResponse>('/upload/files', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data.urls;
};

export const deleteFile = async (url: string): Promise<void> => {
  await deleteImage(url);
};

export default {
  uploadImage,
  uploadImages,
  uploadFiles,
  deleteFile,
  deleteImage,
  uploadAvatar,
};
