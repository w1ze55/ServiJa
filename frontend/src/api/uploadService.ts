import api from './config/axios';

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

  const response = await api.put<UploadResponse>('/users/me/avatar/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });

  return response.data.url;
};

export default {
  uploadImage,
  uploadImages,
  deleteImage,
  uploadAvatar,
};
