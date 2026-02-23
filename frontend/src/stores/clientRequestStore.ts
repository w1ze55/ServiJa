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

const generateMockData = (): ServiceRequest[] => [
    {
        id: '1',
        freelancerName: 'João Eletricista',
        freelancerLocation: 'São Paulo, SP',
        service: 'Instalação Elétrica Residencial',
        category: 'Eletricista',
        description:
            'Preciso de instalação completa do sistema elétrico em um apartamento de 80m². Inclui pontos de luz, tomadas e quadro de distribuição.\n\nO apartamento é novo e precisa de toda a infraestrutura elétrica.',
        budget: 2500,
        deadline: '15 dias',
        priority: 'high',
        status: 'pending',
        createdAt: '2026-02-20T14:30:00',
        address: 'Rua das Flores, 123 - Jardim Paulista, São Paulo - SP',
        desiredDate: '2026-03-10',
        urgency: 'Alta',
        attachments: [
            { id: 'att1', name: 'planta-eletrica.pdf', size: '2.4 MB', url: '#', type: 'application/pdf' },
            { id: 'att2', name: 'foto-quadro.jpg', size: '1.2 MB', url: '#', type: 'image/jpeg' },
        ],
        messages: [
            {
                id: 'msg1',
                sender: 'client',
                senderName: 'Você',
                message: 'Olá! Gostaria de saber se você consegue começar na próxima semana.',
                timestamp: '2026-02-20T15:00:00',
            },
        ],
        auditLog: [
            {
                id: 'log1',
                action: 'Solicitação criada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-20T14:30:00',
                details: 'Solicitação de orçamento enviada',
            },
        ],
    },
    {
        id: '2',
        freelancerName: 'João Eletricista',
        freelancerLocation: 'São Paulo, SP',
        service: 'Manutenção Preventiva',
        category: 'Eletricista',
        description:
            'Necessito de revisão completa do sistema elétrico de uma casa com 150m². Verificação de disjuntores e fiação.',
        budget: 800,
        deadline: '7 dias',
        priority: 'medium',
        status: 'sent',
        createdAt: '2026-02-21T10:15:00',
        address: 'Av. Paulista, 456 - Guarulhos, SP',
        desiredDate: '2026-03-01',
        urgency: 'Média',
        attachments: [
            { id: 'att4', name: 'foto-disjuntor.jpg', size: '890 KB', url: '#', type: 'image/jpeg' },
        ],
        messages: [],
        auditLog: [
            {
                id: 'log3',
                action: 'Solicitação criada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-21T10:15:00',
                details: 'Solicitação de orçamento enviada',
            },
        ],
    },
    {
        id: '3',
        freelancerName: 'João Eletricista',
        freelancerLocation: 'São Paulo, SP',
        service: 'Automação Residencial',
        category: 'Eletricista',
        description:
            'Instalação de sistema de automação residencial completo, incluindo iluminação inteligente, controle de temperatura e segurança.',
        budget: 5000,
        deadline: '30 dias',
        priority: 'medium',
        status: 'negotiating',
        createdAt: '2026-02-18T16:45:00',
        address: 'Rua das Orquídeas, 789 - Osasco, SP',
        desiredDate: '2026-04-01',
        urgency: 'Média',
        freelancerResponse: {
            value: 4800,
            executionDays: 25,
            scope: 'Instalação completa do sistema de automação incluindo:\n- Iluminação inteligente em todos os cômodos\n- Controle de temperatura (ar-condicionado)\n- Sistema de segurança com câmeras\n- Integração com assistente virtual',
            notes: 'Inclui garantia de 1 ano para todos os equipamentos.',
            sentAt: '2026-02-19T10:00:00',
        },
        messages: [
            {
                id: 'msg2',
                sender: 'client',
                senderName: 'Você',
                message: 'Recebi sua proposta. Podemos negociar um desconto para pagamento à vista?',
                timestamp: '2026-02-19T14:00:00',
            },
            {
                id: 'msg3',
                sender: 'freelancer',
                senderName: 'João Eletricista',
                message: 'Olá! Sim, para pagamento à vista posso fazer 5% de desconto, ficando R$ 4.560,00.',
                timestamp: '2026-02-19T16:30:00',
            },
        ],
        auditLog: [
            {
                id: 'log4',
                action: 'Solicitação criada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-18T16:45:00',
            },
            {
                id: 'log5',
                action: 'Resposta do freelancer',
                user: 'João Eletricista (Freelancer)',
                timestamp: '2026-02-19T10:00:00',
                details: 'Valor: R$ 4.800,00 - Prazo: 25 dias',
            },
            {
                id: 'log6',
                action: 'Status alterado',
                user: 'Sistema',
                timestamp: '2026-02-19T10:00:00',
                details: 'Enviada → Negociando',
            },
        ],
    },
    {
        id: '4',
        freelancerName: 'João Eletricista',
        freelancerLocation: 'São Paulo, SP',
        service: 'Reparo de Circuitos',
        category: 'Eletricista',
        description: 'Preciso de reparo urgente em curto-circuito. Disjuntor caindo constantemente.',
        budget: 350,
        deadline: '2 dias',
        priority: 'high',
        status: 'accepted',
        createdAt: '2026-02-22T09:00:00',
        address: 'Rua Augusta, 1000 - São Paulo, SP',
        desiredDate: '2026-02-24',
        urgency: 'Alta',
        freelancerResponse: {
            value: 350,
            executionDays: 1,
            scope: 'Diagnóstico e reparo do curto-circuito. Troca de componentes danificados se necessário.',
            sentAt: '2026-02-22T11:00:00',
        },
        messages: [
            {
                id: 'msg4',
                sender: 'client',
                senderName: 'Você',
                message: 'Perfeito! Pode vir amanhã às 9h?',
                timestamp: '2026-02-22T12:00:00',
            },
            {
                id: 'msg5',
                sender: 'freelancer',
                senderName: 'João Eletricista',
                message: 'Confirmado! Estarei aí amanhã às 9h.',
                timestamp: '2026-02-22T12:30:00',
            },
        ],
        auditLog: [
            {
                id: 'log7',
                action: 'Solicitação criada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-22T09:00:00',
            },
            {
                id: 'log8',
                action: 'Resposta do freelancer',
                user: 'João Eletricista (Freelancer)',
                timestamp: '2026-02-22T11:00:00',
                details: 'Valor: R$ 350,00 - Prazo: 1 dia',
            },
            {
                id: 'log9',
                action: 'Proposta aceita',
                user: 'Você (Cliente)',
                timestamp: '2026-02-22T12:00:00',
            },
        ],
    },
    {
        id: '5',
        freelancerName: 'Carlos Pintor',
        freelancerLocation: 'Campinas, SP',
        service: 'Pintura Completa de Apartamento',
        category: 'Pintor',
        description: 'Pintura interna completa de apartamento de 3 quartos (100m²). Paredes e teto, com massa corrida.',
        budget: 3500,
        deadline: '10 dias',
        priority: 'low',
        status: 'completed',
        createdAt: '2026-02-10T15:10:00',
        address: 'Rua dos Lírios, 50 - Campinas, SP',
        desiredDate: '2026-02-20',
        urgency: 'Baixa',
        freelancerResponse: {
            value: 3200,
            executionDays: 7,
            scope: 'Pintura interna com massa corrida em paredes e teto. Inclui material (tinta Suvinil Acrílica).',
            sentAt: '2026-02-11T08:00:00',
        },
        messages: [],
        auditLog: [
            {
                id: 'log10',
                action: 'Solicitação criada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-10T15:10:00',
            },
            {
                id: 'log11',
                action: 'Proposta aceita',
                user: 'Você (Cliente)',
                timestamp: '2026-02-11T10:00:00',
            },
            {
                id: 'log12',
                action: 'Serviço concluído',
                user: 'Carlos Pintor (Freelancer)',
                timestamp: '2026-02-18T16:00:00',
            },
        ],
    },
    {
        id: '6',
        freelancerName: 'Ana Encanadora',
        freelancerLocation: 'São Paulo, SP',
        service: 'Desentupimento de Esgoto',
        category: 'Encanador',
        description: 'Pia da cozinha entupida. Já tentei desentupidor manual sem sucesso.',
        budget: 200,
        deadline: '1 dia',
        priority: 'high',
        status: 'cancelled',
        createdAt: '2026-02-19T08:00:00',
        address: 'Rua Consolação, 300 - São Paulo, SP',
        desiredDate: '2026-02-19',
        urgency: 'Alta',
        messages: [],
        auditLog: [
            {
                id: 'log13',
                action: 'Solicitação criada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-19T08:00:00',
            },
            {
                id: 'log14',
                action: 'Solicitação cancelada',
                user: 'Você (Cliente)',
                timestamp: '2026-02-19T09:30:00',
                details: 'Motivo: Consegui resolver o problema sozinho',
            },
        ],
    },
];

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
            requests.value = generateMockData();
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
