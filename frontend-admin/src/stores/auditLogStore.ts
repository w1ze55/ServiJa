import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  auditLogService,
  type AuditLogEntry,
  type AuditLogListParams,
} from '@/api/services/auditLogService';

export const useAuditLogStore = defineStore('auditLog', () => {
  const logs = ref<AuditLogEntry[]>([]);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const isLoading = ref(false);
  const selectedLog = ref<AuditLogEntry | null>(null);

  const filters = ref<AuditLogListParams>({
    search: '',
    action: '',
    entityType: '',
    actorType: '',
    result: '',
    dateFrom: '',
    dateTo: '',
    page: 0,
    size: 20,
  });

  const loadLogs = async (params?: AuditLogListParams) => {
    isLoading.value = true;
    try {
      const merged = { ...filters.value, ...params };
      const response = await auditLogService.listLogs(merged);
      logs.value = response.content;
      totalElements.value = response.totalElements;
      totalPages.value = response.totalPages;
      currentPage.value = response.currentPage;
    } finally {
      isLoading.value = false;
    }
  };

  const clearSelectedLog = () => {
    selectedLog.value = null;
  };

  return {
    logs,
    totalElements,
    totalPages,
    currentPage,
    isLoading,
    selectedLog,
    filters,
    loadLogs,
    clearSelectedLog,
  };
});
