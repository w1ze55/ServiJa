import api from '@/api/config/axios';

export type ActorType = 'CONTRACTOR' | 'FREELANCER' | 'ADMIN' | 'GUEST';
export type AuditResult = 'SUCCESS' | 'FAILURE';
export type AuditSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface AuditLogEntry {
  id: number;
  timestamp: string;
  actorId: number | null;
  actorName: string | null;
  actorEmail: string | null;
  actorType: ActorType | null;
  action: string;
  entityType: string | null;
  entityId: string | null;
  entityLabel: string | null;
  httpMethod: string | null;
  httpPath: string | null;
  httpStatus: number | null;
  result: AuditResult;
  severity?: AuditSeverity | null;
  ipAddress: string | null;
  userAgent: string | null;
  payload: string | null;
}

export interface AuditLogPageResponse {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface AuditLogListParams {
  search?: string;
  action?: string;
  entityType?: string;
  actorType?: ActorType | '';
  result?: AuditResult | '';
  severity?: AuditSeverity | '';
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

export const auditLogService = {
  async listLogs(params: AuditLogListParams = {}): Promise<AuditLogPageResponse> {
    const clean: Record<string, unknown> = {};
    if (params.search) clean.search = params.search;
    if (params.action) clean.action = params.action;
    if (params.entityType) clean.entityType = params.entityType;
    if (params.actorType) clean.actorType = params.actorType;
    if (params.result) clean.result = params.result;
    if (params.severity) clean.severity = params.severity;
    if (params.dateFrom) clean.dateFrom = params.dateFrom;
    if (params.dateTo) clean.dateTo = params.dateTo;
    if (params.page !== undefined) clean.page = params.page;
    if (params.size !== undefined) clean.size = params.size;

    const response = await api.get<AuditLogPageResponse>('/admin/audit-logs', { params: clean });
    return response.data;
  },
};
