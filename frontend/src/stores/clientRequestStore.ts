import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api from '@/api/config/axios';
import { normalizeWorkModel, type WorkModelValue } from '@/utils/workModel';

export interface ServiceRequestAttachment {
    id: string;
    name: string;
    size: string;
    url: string;
    type: string;
    description?: string;
}

export interface FreelancerResponse {
    value: number;
    executionDays: number;
    scope: string;
    notes?: string;
    sentAt: string;
    milestones?: any[];
    attachments?: ServiceRequestAttachment[];
}

export interface RequestMessage {
    id: string;
    sender: 'client' | 'freelancer';
    senderName: string;
    message: string;
    timestamp: string;
    attachments?: ServiceRequestAttachment[];
}

export interface AuditLogEntry {
    id: string;
    action: string;
    operationKey?: string;
    user: string;
    timestamp: string;
    details?: string;
}

export interface ServiceRequest {
    id: string;
    freelancerId?: string;
    freelancerName?: string;
    freelancerAvatar?: string;
    freelancerLocation?: string;
    service: string;
    categoryId?: number | null;
    subcategoryId?: number | null;
    category: string;
    subcategory?: string;
    description: string;
    budget?: number | null;
    deadline: string;
    workModel?: WorkModelValue | null;
    status: 'pending' | 'sent' | 'negotiating' | 'accepted' | 'in_progress' | 'delivered' | 'rejected' | 'completed' | 'cancelled';
    createdAt: string;
    updatedAt: string;
    address?: string;
    desiredDate?: string;
    attachments?: ServiceRequestAttachment[];
    freelancerAttachments?: ServiceRequestAttachment[];
    freelancerResponse?: FreelancerResponse;
    counterValue?: number;
    counterDays?: number;
    counterNotes?: string;
    cancelReason?: string;
    rejectReason?: string;
    messages?: RequestMessage[];
    auditLog?: AuditLogEntry[];
}

export interface CreateServiceRequestInput {
    freelancerId?: string;
    service: string;
    categoryId?: number | null;
    subcategoryId?: number | null;
    category: string;
    subcategory?: string;
    description: string;
    deadline: string;
    workModel: WorkModelValue;
    address?: string;
    desiredDate?: string;
    attachments?: File[];
}

export interface RequestStats {
    pending: number;
    negotiating: number;
    inProgress: number;
    completed: number;
    cancelled: number;
}

export const useClientRequestStore = defineStore('clientRequest', () => {
    const requests = ref<ServiceRequest[]>([]);
    const isLoading = ref(false);
    const error = ref<string | null>(null);

    const mapApiStatusToLocal = (status: string): ServiceRequest['status'] => {
        const mapping: Record<string, ServiceRequest['status']> = {
            'PENDING': 'pending',
            'SENT': 'sent',
            'NEGOTIATING': 'negotiating',
            'ACCEPTED': 'accepted',
            'IN_PROGRESS': 'in_progress',
            'DELIVERED': 'delivered',
            'REJECTED': 'rejected',
            'COMPLETED': 'completed',
            'CANCELLED': 'cancelled',
        };
        return mapping[status] || 'pending';
    };

    const extractFileName = (url: string): string => {
        try {
            const path = new URL(url).pathname;
            const encoded = path.split('/').pop() || 'arquivo';
            return decodeURIComponent(encoded);
        } catch {
            return url.split('/').pop() || 'arquivo';
        }
    };

    const getFileType = (url: string): string => {
        const ext = url.split('.').pop()?.toLowerCase().split('?')[0] || '';
        const types: Record<string, string> = {
            pdf: 'application/pdf',
            jpg: 'image/jpeg', jpeg: 'image/jpeg',
            png: 'image/png', gif: 'image/gif', webp: 'image/webp',
            doc: 'application/msword',
            docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        };
        return types[ext] || 'application/octet-stream';
    };

    const mapAttachments = (
        urls: string[] | undefined,
        prefix: string,
        requestId: string | number
    ): ServiceRequestAttachment[] => (urls || []).map((url: string, idx: number) => ({
        id: `${prefix}-${requestId}-${idx}`,
        name: extractFileName(url),
        size: '',
        url,
        type: getFileType(url),
    }));

    const mapDetailedAttachments = (
        attachments: Array<{ url: string; name?: string; description?: string | null }> | undefined,
        prefix: string,
        requestId: string | number
    ): ServiceRequestAttachment[] => (attachments || [])
        .filter((attachment) => attachment?.url)
        .map((attachment, idx) => ({
            id: `${prefix}-${requestId}-${idx}`,
            name: attachment.name?.trim() || extractFileName(attachment.url),
            size: '',
            url: attachment.url,
            type: getFileType(attachment.url),
            description: attachment.description?.trim() || undefined,
        }));

    const mapFreelancerAttachments = (
        req: any,
        prefix: string,
        requestId: string | number
    ): ServiceRequestAttachment[] => {
        if (req.freelancerAttachmentDetails?.length) {
            return mapDetailedAttachments(req.freelancerAttachmentDetails, prefix, requestId);
        }

        return mapAttachments(req.freelancerAttachments, prefix, requestId);
    };

    const mapRequestResponse = (req: any, existingRequest?: ServiceRequest): ServiceRequest => ({
        ...req,
        id: String(req.id),
        service: req.serviceName,
        categoryId: req.categoryId ?? null,
        subcategoryId: req.subcategoryId ?? null,
        category: req.categoryLabel || req.category || '',
        subcategory: req.subcategoryLabel || req.subcategory || '',
        budget: req.budget ?? existingRequest?.budget ?? null,
        workModel: normalizeWorkModel(req.workModel),
        status: mapApiStatusToLocal(req.status),
        updatedAt: req.updatedAt || req.createdAt,
        attachments: mapAttachments(req.attachments, 'att', req.id),
        freelancerAttachments: mapFreelancerAttachments(req, 'fatt', req.id),
        freelancerAvatar: req.freelancerAvatar,
        freelancerResponse: req.responseValue ? {
            value: req.responseValue,
            executionDays: req.responseDays,
            scope: req.responseScope,
            notes: req.responseNotes,
            sentAt: req.respondedAt,
            milestones: req.milestones,
            attachments: mapAttachments(req.responseAttachments, 'ratt', req.id),
        } : undefined,
        counterValue: req.counterValue,
        counterDays: req.counterDays,
        counterNotes: req.counterNotes,
        rejectReason: req.rejectReason,
        cancelReason: req.cancelReason,
        messages: existingRequest?.messages,
        auditLog: existingRequest?.auditLog,
    });

    const getRequestById = (id: string): ServiceRequest | undefined => {
        return requests.value.find((r) => r.id === String(id));
    };

    const upsertRequest = (request: ServiceRequest): ServiceRequest => {
        const index = requests.value.findIndex((existing) => existing.id === request.id);
        if (index === -1) {
            requests.value.unshift(request);
            return requests.value[0];
        }

        requests.value[index] = request;
        return requests.value[index];
    };

    const stats = computed<RequestStats>(() => ({
        pending: requests.value.filter((r) => r.status === 'pending' || r.status === 'sent').length,
        negotiating: requests.value.filter((r) => r.status === 'negotiating' || r.status === 'accepted').length,
        inProgress: requests.value.filter((r) => r.status === 'in_progress' || r.status === 'delivered').length,
        completed: requests.value.filter((r) => r.status === 'completed').length,
        cancelled: requests.value.filter((r) => r.status === 'cancelled' || r.status === 'rejected').length,
    }));

    const loadRequests = async () => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.get('/service-requests/my');
            const existingRequests = new Map(requests.value.map((request) => [request.id, request]));
            requests.value = response.data.map((req: any) =>
                mapRequestResponse(req, existingRequests.get(String(req.id)))
            );
        } catch (err) {
            error.value = 'Erro ao carregar solicitações';
            console.error('Erro ao carregar solicitações:', err);
        } finally {
            isLoading.value = false;
        }
    };

    const loadRequestById = async (id: string): Promise<ServiceRequest | null> => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.get(`/service-requests/${id}`);
            const mappedRequest = mapRequestResponse(response.data, getRequestById(id));
            return upsertRequest(mappedRequest);
        } catch (err) {
            error.value = 'Erro ao carregar detalhes da solicitação';
            console.error(`Erro ao carregar solicitação ${id}:`, err);
            return null;
        } finally {
            isLoading.value = false;
        }
    };

    const createRequest = async (input: CreateServiceRequestInput): Promise<ServiceRequest | null> => {
        try {
            const requestData = {
                serviceName: input.service,
                categoryId: input.categoryId,
                subcategoryId: input.subcategoryId,
                category: input.category,
                subcategory: input.subcategory,
                description: input.description,
                deadline: input.deadline,
                workModel: input.workModel,
                address: input.address,
                desiredDate: input.desiredDate,
                freelancerId: input.freelancerId
            };

            const response = await api.post('/service-requests', requestData);
            const newRequest = mapRequestResponse(response.data);

            requests.value.unshift(newRequest);
            return newRequest;
        } catch (err) {
            console.error('Erro ao criar solicitação:', err);
            return null;
        }
    };

    const cancelRequest = async (id: string, reason?: string): Promise<boolean> => {
        try {
            const request = requests.value.find((r) => r.id === id);
            if (!request) return false;

            const response = await api.patch(`/service-requests/${id}/status`, {
                status: 'CANCELLED',
                reason
            });

            request.status = 'cancelled';
            request.updatedAt = response.data.updatedAt || new Date().toISOString();
            request.auditLog = request.auditLog || [];
            request.auditLog.push({
                id: `log-${Date.now()}`,
                action: 'Solicitação cancelada',
                user: 'Você (Cliente)',
                timestamp: new Date().toISOString(),
                details: reason ? `Motivo: ${reason}` : undefined,
            });

            return true;
        } catch (err) {
            console.error('Erro ao cancelar solicitação:', err);
            return false;
        }
    };

    const acceptFreelancerResponse = async (id: string): Promise<boolean> => {
        try {
            const request = requests.value.find((r) => r.id === id);
            if (!request) return false;

            const response = await api.patch(`/service-requests/${id}/status`, {
                status: 'ACCEPTED'
            });

            request.status = 'accepted';
            request.updatedAt = response.data.updatedAt || new Date().toISOString();
            request.auditLog = request.auditLog || [];
            request.auditLog.push({
                id: `log-${Date.now()}`,
                action: 'Proposta aceita',
                user: 'Você (Cliente)',
                timestamp: new Date().toISOString(),
                details: `Valor aceito: R$ ${request.freelancerResponse?.value?.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
            });

            return true;
        } catch (err) {
            console.error('Erro ao aceitar proposta:', err);
            return false;
        }
    };

    const rejectFreelancerResponse = async (id: string, reason: string): Promise<boolean> => {
        try {
            const request = requests.value.find((r) => r.id === id);
            if (!request) return false;

            const response = await api.patch(`/service-requests/${id}/status`, {
                status: 'REJECTED',
                reason
            });

            request.status = 'rejected';
            request.updatedAt = response.data.updatedAt || new Date().toISOString();
            request.auditLog = request.auditLog || [];
            request.auditLog.push({
                id: `log-${Date.now()}`,
                action: 'Proposta recusada',
                user: 'Você (Cliente)',
                timestamp: new Date().toISOString(),
                details: `Motivo: ${reason}`,
            });

            return true;
        } catch (err) {
            console.error('Erro ao recusar proposta:', err);
            return false;
        }
    };

    const sendCounterProposal = async (
        id: string,
        value: number,
        executionDays: number,
        notes: string
    ): Promise<boolean> => {
        const request = requests.value.find((r) => r.id === id);
        if (!request) return false;

        const response = await api.patch(`/service-requests/${id}/counter-proposal`, {
            value,
            executionDays,
            notes
        });

        const updatedReq = response.data;
        request.status = 'negotiating';
        request.updatedAt = updatedReq.updatedAt || new Date().toISOString();
        request.counterValue = updatedReq.counterValue;
        request.counterDays = updatedReq.counterDays;
        request.counterNotes = updatedReq.counterNotes;

        request.auditLog = request.auditLog || [];
        request.auditLog.unshift({
            id: `log-${Date.now()}`,
            action: 'Contra proposta enviada',
            user: 'Você (Cliente)',
            timestamp: new Date().toISOString(),
            details: `Valor proposto: R$ ${value.toLocaleString('pt-BR', { minimumFractionDigits: 2 })} | Prazo: ${executionDays} dias`,
        });

        return true;
    };

    const completeService = async (id: string): Promise<boolean> => {
        try {
            const request = requests.value.find((r) => r.id === id);
            if (!request) return false;

            const response = await api.patch(`/service-requests/${id}/complete`);

            request.status = 'completed';
            request.updatedAt = response.data.updatedAt || new Date().toISOString();

            request.auditLog = request.auditLog || [];
            request.auditLog.unshift({
                id: `log-${Date.now()}`,
                action: 'Serviço concluído',
                operationKey: 'SERVICE_COMPLETED',
                user: 'Você (Cliente)',
                timestamp: new Date().toISOString(),
                details: 'Serviço marcado como concluído pelo cliente',
            });

            return true;
        } catch (err) {
            console.error('Erro ao concluir serviço:', err);
            return false;
        }
    };

    const sendMessage = async (
        id: string,
        message: string,
        attachments?: File[]
    ): Promise<boolean> => {
        try {
            const request = requests.value.find((r) => r.id === id);
            if (!request) return false;

            const newMessage: RequestMessage = {
                id: `msg-${Date.now()}`,
                sender: 'client',
                senderName: 'Você',
                message,
                timestamp: new Date().toISOString(),
                attachments: attachments?.map((file, idx) => ({
                    id: `att-${Date.now()}-${idx}`,
                    name: file.name,
                    size: `${(file.size / 1024).toFixed(1)} KB`,
                    url: URL.createObjectURL(file),
                    type: file.type,
                })),
            };

            request.messages = request.messages || [];
            request.messages.push(newMessage);

            request.auditLog = request.auditLog || [];
            request.auditLog.push({
                id: `log-${Date.now()}`,
                action: 'Mensagem enviada',
                user: 'Você (Cliente)',
                timestamp: new Date().toISOString(),
            });

            return true;
        } catch (err) {
            console.error('Erro ao enviar mensagem:', err);
            return false;
        }
    };

    const loadHistory = async (requestId: string) => {
        try {
            const response = await api.get(`/service-requests/${requestId}/history`);
            const request = requests.value.find((r) => r.id === requestId);
            if (request) {
                request.auditLog = response.data.map((entry: any) => ({
                    id: String(entry.id),
                    action: entry.operationType,
                    operationKey: entry.operationKey,
                    user: entry.userName,
                    timestamp: entry.formattedDateTime,
                    details: entry.details,
                }));
            }
        } catch (err) {
            console.error('Erro ao carregar histórico:', err);
        }
    };

    return {
        requests,
        isLoading,
        error,
        stats,
        loadRequests,
        loadRequestById,
        getRequestById,
        createRequest,
        cancelRequest,
        acceptFreelancerResponse,
        rejectFreelancerResponse,
        sendCounterProposal,
        completeService,
        sendMessage,
        loadHistory,
    };
});
