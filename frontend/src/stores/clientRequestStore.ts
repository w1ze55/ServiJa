import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

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
    priority: 'low' | 'medium' | 'high';
    status: 'pending' | 'sent' | 'negotiating' | 'accepted' | 'rejected' | 'completed' | 'cancelled';
    createdAt: string;
    address?: string;
    desiredDate?: string;
    urgency?: string;
    attachments?: ServiceRequestAttachment[];
    freelancerResponse?: FreelancerResponse;
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
    priority: 'low' | 'medium' | 'high';
    address?: string;
    desiredDate?: string;
    urgency?: string;
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
            // TODO: Substituir por chamada real à API
            // const response = await api.get('/service-requests/my');
            // requests.value = response.data;
            await new Promise((resolve) => setTimeout(resolve, 500));
            // Sem dados mock - começa vazio
            requests.value = [];
        } catch (err) {
            error.value = 'Erro ao carregar solicitações';
            console.error('Erro ao carregar solicitações:', err);
        } finally {
            isLoading.value = false;
        }
    };

    const getRequestById = (id: string): ServiceRequest | undefined => {
        return requests.value.find((r) => r.id === id);
    };

    const createRequest = async (input: CreateServiceRequestInput): Promise<ServiceRequest | null> => {
        try {
            // TODO: Chamar API
            // const response = await api.post('/service-requests', input);
            // const newRequest = response.data;

            const newRequest: ServiceRequest = {
                id: `req-${Date.now()}`,
                freelancerId: input.freelancerId,
                service: input.service,
                category: input.category,
                description: input.description,
                budget: input.budget,
                deadline: input.deadline,
                priority: input.priority,
                status: 'pending',
                createdAt: new Date().toISOString(),
                address: input.address,
                desiredDate: input.desiredDate,
                urgency: input.urgency,
                attachments: input.attachments?.map((file, idx) => ({
                    id: `att-${Date.now()}-${idx}`,
                    name: file.name,
                    size: `${(file.size / 1024).toFixed(1)} KB`,
                    url: URL.createObjectURL(file),
                    type: file.type,
                })),
                messages: [],
                auditLog: [
                    {
                        id: `log-${Date.now()}`,
                        action: 'Solicitação criada',
                        user: 'Você (Cliente)',
                        timestamp: new Date().toISOString(),
                        details: 'Solicitação de orçamento enviada',
                    },
                ],
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
    };
});
