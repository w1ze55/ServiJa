import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api from '@/api/config/axios';

export interface ServiceRequestAttachment {
    id: string;
    name: string;
    size: string;
    url: string;
    type: string;
}

export interface FreelancerResponse {
    value: number;
    executionDays: number;
    scope: string;
    notes?: string;
    sentAt: string;
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
    category: string;
    description: string;
    budget: number;
    deadline: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    status: 'pending' | 'sent' | 'negotiating' | 'accepted' | 'rejected' | 'completed' | 'cancelled';
    createdAt: string;
    address?: string;
    desiredDate?: string;
    attachments?: ServiceRequestAttachment[];
    freelancerResponse?: FreelancerResponse;
    cancelReason?: string;
    rejectReason?: string;
    messages?: RequestMessage[];
    auditLog?: AuditLogEntry[];
}

export interface CreateServiceRequestInput {
    freelancerId?: string;
    service: string;
    category: string;
    description: string;
    budget: number;
    deadline: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    address?: string;
    desiredDate?: string;
    attachments?: File[];
}

export interface RequestStats {
    pending: number;
    negotiating: number;
    accepted: number;
    completed: number;
    cancelled: number;
}

export const useClientRequestStore = defineStore('clientRequest', () => {
    const requests = ref<ServiceRequest[]>([]);
    const isLoading = ref(false);
    const error = ref<string | null>(null);

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

    const stats = computed<RequestStats>(() => ({
        pending: requests.value.filter((r) => r.status === 'pending' || r.status === 'sent').length,
        negotiating: requests.value.filter((r) => r.status === 'negotiating').length,
        accepted: requests.value.filter((r) => r.status === 'accepted').length,
        completed: requests.value.filter((r) => r.status === 'completed').length,
        cancelled: requests.value.filter((r) => r.status === 'cancelled' || r.status === 'rejected').length,
    }));

    const loadRequests = async () => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.get('/service-requests/my');
            // Mapear campos do backend para o frontend se necessário
            requests.value = response.data.map((req: any) => ({
                ...req,
                id: String(req.id),
                service: req.serviceName, // Backend usa serviceName
                priority: req.priority || 'MEDIUM',
                status: (req.status || 'PENDING').toLowerCase(),
                attachments: (req.attachments || []).map((url: string, idx: number) => ({
                    id: `att-${req.id}-${idx}`,
                    name: extractFileName(url),
                    size: '',
                    url,
                    type: getFileType(url),
                })),
                freelancerAvatar: req.freelancerAvatar,
                freelancerResponse: req.responseValue ? {
                    value: req.responseValue,
                    executionDays: req.responseDays,
                    scope: req.responseScope,
                    notes: req.responseNotes,
                    sentAt: req.respondedAt
                } : undefined
            }));
        } catch (err) {
            error.value = 'Erro ao carregar solicitações';
            console.error('Erro ao carregar solicitações:', err);
        } finally {
            isLoading.value = false;
        }
    };

    const getRequestById = (id: string): ServiceRequest | undefined => {
        return requests.value.find((r) => r.id === String(id));
    };

    const createRequest = async (input: CreateServiceRequestInput): Promise<ServiceRequest | null> => {
        try {
            // Adequar nomes de campos para o backend
            const requestData = {
                serviceName: input.service,
                category: input.category,
                description: input.description,
                budget: input.budget,
                deadline: input.deadline,
                priority: input.priority,
                address: input.address,
                desiredDate: input.desiredDate,
                freelancerId: input.freelancerId
            };

            const response = await api.post('/service-requests', requestData);
            const newRequest = {
                ...response.data,
                id: String(response.data.id),
                service: response.data.serviceName,
                priority: response.data.priority || 'MEDIUM',
                status: (response.data.status || 'PENDING').toLowerCase(),
                freelancerAvatar: response.data.freelancerAvatar,
            };

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

            // TODO: Chamar API
            // await api.patch(`/service-requests/${id}/status`, { status: 'cancelled', reason });

            request.status = 'cancelled';
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

            // TODO: Chamar API
            // await api.patch(`/service-requests/${id}/status`, { status: 'accepted' });

            request.status = 'accepted';
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

            request.status = 'rejected';
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
        getRequestById,
        createRequest,
        cancelRequest,
        acceptFreelancerResponse,
        rejectFreelancerResponse,
        sendMessage,
        loadHistory,
    };
});
