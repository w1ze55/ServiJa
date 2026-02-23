import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export interface Proposal {
  id: string;
  clientName: string;
  clientAvatar?: string;
  clientLocation: string;
  clientEmail?: string;
  clientPhone?: string;
  service: string;
  category: string;
  description: string;
  budget: number;
  deadline: string;
  priority: 'low' | 'medium' | 'high';
  status: 'pending' | 'negotiating' | 'accepted' | 'rejected' | 'completed' | 'archived';
  receivedAt: string;
  attachments?: ProposalAttachment[];
  viewed: boolean;
  address?: string;
  desiredDate?: string;
  urgency?: string;
  freelancerResponse?: FreelancerResponse;
  messages?: ProposalMessage[];
  auditLog?: AuditLogEntry[];
}

export interface ProposalAttachment {
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
  milestones?: Milestone[];
  attachments?: ProposalAttachment[];
  sentAt: string;
}

export interface Milestone {
  id: string;
  title: string;
  description: string;
  deadline: string;
  value: number;
  status: 'pending' | 'in_progress' | 'completed';
}

export interface ProposalMessage {
  id: string;
  sender: 'freelancer' | 'client';
  senderName: string;
  message: string;
  timestamp: string;
  attachments?: ProposalAttachment[];
}

export interface AuditLogEntry {
  id: string;
  action: string;
  user: string;
  timestamp: string;
  details?: string;
}

export interface ProposalStats {
  pending: number;
  negotiating: number;
  accepted: number;
  completed: number;
  rejected: number;
}

const generateMockData = (): Proposal[] => [
  {
    id: '1',
    clientName: 'Maria Silva',
    clientAvatar: undefined,
    clientLocation: 'São Paulo, SP',
    clientEmail: 'maria.silva@email.com',
    clientPhone: '(11) 99999-0001',
    service: 'Instalação Elétrica Residencial',
    category: 'Eletricista',
    description:
      'Preciso de instalação completa do sistema elétrico em um apartamento de 80m². Inclui pontos de luz, tomadas e quadro de distribuição.\n\nO apartamento é novo e precisa de toda a infraestrutura elétrica. Gostaria de receber um orçamento detalhado.',
    budget: 2500,
    deadline: '15 dias',
    priority: 'high',
    status: 'pending',
    receivedAt: '2026-02-20T14:30:00',
    address: 'Rua das Flores, 123 - Jardim Paulista, São Paulo - SP',
    desiredDate: '2026-03-10',
    urgency: 'Alta',
    attachments: [
      { id: 'att1', name: 'planta-eletrica.pdf', size: '2.4 MB', url: '#', type: 'application/pdf' },
      { id: 'att2', name: 'foto-quadro.jpg', size: '1.2 MB', url: '#', type: 'image/jpeg' },
      { id: 'att3', name: 'projeto-luminotecnia.pdf', size: '856 KB', url: '#', type: 'application/pdf' },
    ],
    viewed: false,
    messages: [
      {
        id: 'msg1',
        sender: 'client',
        senderName: 'Maria Silva',
        message: 'Olá! Gostaria de saber se você consegue começar na próxima semana.',
        timestamp: '2026-02-20T15:00:00',
      },
    ],
    auditLog: [
      {
        id: 'log1',
        action: 'Solicitação criada',
        user: 'Maria Silva (Cliente)',
        timestamp: '2026-02-20T14:30:00',
        details: 'Cliente criou a solicitação de orçamento',
      },
      {
        id: 'log2',
        action: 'Anexos adicionados',
        user: 'Maria Silva (Cliente)',
        timestamp: '2026-02-20T14:35:00',
        details: '3 arquivos anexados',
      },
    ],
  },
  {
    id: '2',
    clientName: 'Carlos Mendes',
    clientAvatar: undefined,
    clientLocation: 'Guarulhos, SP',
    clientEmail: 'carlos.mendes@email.com',
    clientPhone: '(11) 98888-0002',
    service: 'Manutenção Preventiva',
    category: 'Eletricista',
    description:
      'Necessito de revisão completa do sistema elétrico de uma casa com 150m². Verificação de disjuntores e fiação.',
    budget: 800,
    deadline: '7 dias',
    priority: 'medium',
    status: 'pending',
    receivedAt: '2026-02-21T10:15:00',
    address: 'Av. Paulista, 456 - Guarulhos, SP',
    desiredDate: '2026-03-01',
    urgency: 'Média',
    attachments: [
      { id: 'att4', name: 'foto-disjuntor.jpg', size: '890 KB', url: '#', type: 'image/jpeg' },
    ],
    viewed: true,
    messages: [],
    auditLog: [
      {
        id: 'log3',
        action: 'Solicitação criada',
        user: 'Carlos Mendes (Cliente)',
        timestamp: '2026-02-21T10:15:00',
        details: 'Cliente criou a solicitação de orçamento',
      },
    ],
  },
  {
    id: '3',
    clientName: 'Ana Costa',
    clientAvatar: undefined,
    clientLocation: 'Osasco, SP',
    clientEmail: 'ana.costa@email.com',
    clientPhone: '(11) 97777-0003',
    service: 'Automação Residencial',
    category: 'Eletricista',
    description:
      'Instalação de sistema de automação residencial completo, incluindo iluminação inteligente, controle de temperatura e segurança.',
    budget: 5000,
    deadline: '30 dias',
    priority: 'medium',
    status: 'negotiating',
    receivedAt: '2026-02-18T16:45:00',
    address: 'Rua das Orquídeas, 789 - Osasco, SP',
    desiredDate: '2026-04-01',
    urgency: 'Média',
    attachments: [
      { id: 'att5', name: 'projeto-automacao.pdf', size: '3.2 MB', url: '#', type: 'application/pdf' },
      { id: 'att6', name: 'planta-casa.pdf', size: '1.8 MB', url: '#', type: 'application/pdf' },
    ],
    viewed: true,
    freelancerResponse: {
      value: 4800,
      executionDays: 25,
      scope: 'Instalação completa do sistema de automação incluindo:\n- Iluminação inteligente em todos os cômodos\n- Controle de temperatura (ar-condicionado)\n- Sistema de segurança com câmeras\n- Integração com assistente virtual',
      notes: 'Inclui garantia de 1 ano para todos os equipamentos.',
      sentAt: '2026-02-19T10:00:00',
      milestones: [
        { id: 'ms1', title: 'Preparação e cabeamento', description: 'Instalação da infraestrutura', deadline: '2026-03-15', value: 1500, status: 'pending' },
        { id: 'ms2', title: 'Instalação de equipamentos', description: 'Montagem dos dispositivos', deadline: '2026-03-25', value: 2000, status: 'pending' },
        { id: 'ms3', title: 'Configuração e testes', description: 'Programação e validação', deadline: '2026-04-01', value: 1300, status: 'pending' },
      ],
    },
    messages: [
      {
        id: 'msg2',
        sender: 'client',
        senderName: 'Ana Costa',
        message: 'Recebi sua proposta. Podemos negociar um desconto para pagamento à vista?',
        timestamp: '2026-02-19T14:00:00',
      },
      {
        id: 'msg3',
        sender: 'freelancer',
        senderName: 'Você',
        message: 'Olá Ana! Sim, para pagamento à vista posso fazer 5% de desconto, ficando R$ 4.560,00.',
        timestamp: '2026-02-19T16:30:00',
      },
    ],
    auditLog: [
      {
        id: 'log4',
        action: 'Solicitação criada',
        user: 'Ana Costa (Cliente)',
        timestamp: '2026-02-18T16:45:00',
        details: 'Cliente criou a solicitação de orçamento',
      },
      {
        id: 'log5',
        action: 'Proposta enviada',
        user: 'Você (Freelancer)',
        timestamp: '2026-02-19T10:00:00',
        details: 'Valor: R$ 4.800,00 - Prazo: 25 dias',
      },
      {
        id: 'log6',
        action: 'Status alterado',
        user: 'Sistema',
        timestamp: '2026-02-19T10:00:00',
        details: 'Pendente → Negociando',
      },
    ],
  },
  {
    id: '4',
    clientName: 'Roberto Lima',
    clientAvatar: undefined,
    clientLocation: 'São Paulo, SP',
    clientEmail: 'roberto.lima@email.com',
    clientPhone: '(11) 96666-0004',
    service: 'Reparo de Circuitos',
    category: 'Eletricista',
    description: 'Preciso de reparo urgente em curto-circuito. Disjuntor caindo constantemente.',
    budget: 350,
    deadline: '2 dias',
    priority: 'high',
    status: 'accepted',
    receivedAt: '2026-02-22T09:00:00',
    address: 'Rua Augusta, 1000 - São Paulo, SP',
    desiredDate: '2026-02-24',
    urgency: 'Alta',
    attachments: [],
    viewed: true,
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
        senderName: 'Roberto Lima',
        message: 'Perfeito! Pode vir amanhã às 9h?',
        timestamp: '2026-02-22T12:00:00',
      },
      {
        id: 'msg5',
        sender: 'freelancer',
        senderName: 'Você',
        message: 'Confirmado! Estarei aí amanhã às 9h.',
        timestamp: '2026-02-22T12:30:00',
      },
    ],
    auditLog: [
      {
        id: 'log7',
        action: 'Solicitação criada',
        user: 'Roberto Lima (Cliente)',
        timestamp: '2026-02-22T09:00:00',
        details: 'Cliente criou a solicitação de orçamento',
      },
      {
        id: 'log8',
        action: 'Proposta enviada',
        user: 'Você (Freelancer)',
        timestamp: '2026-02-22T11:00:00',
        details: 'Valor: R$ 350,00 - Prazo: 1 dia',
      },
      {
        id: 'log9',
        action: 'Proposta aceita',
        user: 'Roberto Lima (Cliente)',
        timestamp: '2026-02-22T12:00:00',
        details: 'Cliente aceitou a proposta',
      },
    ],
  },
  {
    id: '5',
    clientName: 'Julia Fernandes',
    clientAvatar: undefined,
    clientLocation: 'Campinas, SP',
    clientEmail: 'julia.fernandes@email.com',
    clientPhone: '(19) 95555-0005',
    service: 'Instalação de Painéis Solares',
    category: 'Eletricista',
    description:
      'Instalação de sistema de energia solar fotovoltaica residencial. Casa de 120m² com consumo médio de 450kWh/mês.',
    budget: 18000,
    deadline: '45 dias',
    priority: 'low',
    status: 'rejected',
    receivedAt: '2026-02-15T11:20:00',
    address: 'Rua das Palmeiras, 200 - Campinas, SP',
    desiredDate: '2026-04-15',
    urgency: 'Baixa',
    attachments: [
      { id: 'att7', name: 'conta-luz.pdf', size: '450 KB', url: '#', type: 'application/pdf' },
    ],
    viewed: true,
    auditLog: [
      {
        id: 'log10',
        action: 'Solicitação criada',
        user: 'Julia Fernandes (Cliente)',
        timestamp: '2026-02-15T11:20:00',
        details: 'Cliente criou a solicitação de orçamento',
      },
      {
        id: 'log11',
        action: 'Solicitação recusada',
        user: 'Você (Freelancer)',
        timestamp: '2026-02-16T09:00:00',
        details: 'Motivo: Não trabalho com instalação de painéis solares',
      },
    ],
    messages: [],
  },
  {
    id: '6',
    clientName: 'Pedro Oliveira',
    clientAvatar: undefined,
    clientLocation: 'São Paulo, SP',
    clientEmail: 'pedro.oliveira@email.com',
    clientPhone: '(11) 94444-0006',
    service: 'Instalação de Chuveiro Elétrico',
    category: 'Eletricista',
    description: 'Instalação de chuveiro elétrico 220V com fiação adequada.',
    budget: 250,
    deadline: '3 dias',
    priority: 'medium',
    status: 'completed',
    receivedAt: '2026-02-10T15:10:00',
    address: 'Rua dos Lírios, 50 - São Paulo, SP',
    desiredDate: '2026-02-15',
    urgency: 'Média',
    attachments: [],
    viewed: true,
    freelancerResponse: {
      value: 280,
      executionDays: 1,
      scope: 'Instalação de chuveiro elétrico 220V com fiação 6mm e disjuntor dedicado.',
      sentAt: '2026-02-11T08:00:00',
    },
    messages: [],
    auditLog: [
      {
        id: 'log12',
        action: 'Solicitação criada',
        user: 'Pedro Oliveira (Cliente)',
        timestamp: '2026-02-10T15:10:00',
        details: 'Cliente criou a solicitação de orçamento',
      },
      {
        id: 'log13',
        action: 'Proposta aceita',
        user: 'Pedro Oliveira (Cliente)',
        timestamp: '2026-02-11T10:00:00',
        details: 'Cliente aceitou a proposta',
      },
      {
        id: 'log14',
        action: 'Serviço concluído',
        user: 'Você (Freelancer)',
        timestamp: '2026-02-12T16:00:00',
        details: 'Instalação realizada com sucesso',
      },
    ],
  },
];

export const useProposalStore = defineStore('proposal', () => {
  const proposals = ref<Proposal[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const stats = computed<ProposalStats>(() => ({
    pending: proposals.value.filter((p) => p.status === 'pending').length,
    negotiating: proposals.value.filter((p) => p.status === 'negotiating').length,
    accepted: proposals.value.filter((p) => p.status === 'accepted').length,
    completed: proposals.value.filter((p) => p.status === 'completed' || p.status === 'archived').length,
    rejected: proposals.value.filter((p) => p.status === 'rejected').length,
  }));

  const loadProposals = async () => {
    isLoading.value = true;
    error.value = null;
    try {
      // TODO: Substituir por chamada real à API
      // const response = await api.get('/proposals/received');
      // proposals.value = response.data;
      
      // Mock data para desenvolvimento
      await new Promise((resolve) => setTimeout(resolve, 500));
      proposals.value = generateMockData();
    } catch (err) {
      error.value = 'Erro ao carregar propostas';
      console.error('Erro ao carregar propostas:', err);
    } finally {
      isLoading.value = false;
    }
  };

  const getProposalById = (id: string): Proposal | undefined => {
    return proposals.value.find((p) => p.id === id);
  };

  const markAsViewed = async (id: string) => {
    const proposal = proposals.value.find((p) => p.id === id);
    if (proposal && !proposal.viewed) {
      proposal.viewed = true;
      // TODO: Chamar API para persistir
      // await api.patch(`/proposals/${id}/viewed`);
    }
  };

  const sendResponse = async (
    id: string,
    response: Omit<FreelancerResponse, 'sentAt'>
  ): Promise<boolean> => {
    try {
      const proposal = proposals.value.find((p) => p.id === id);
      if (!proposal) return false;

      // TODO: Chamar API
      // await api.post(`/proposals/${id}/respond`, response);
      
      proposal.freelancerResponse = {
        ...response,
        sentAt: new Date().toISOString(),
      };
      proposal.status = 'negotiating';
      
      proposal.auditLog = proposal.auditLog || [];
      proposal.auditLog.push({
        id: `log-${Date.now()}`,
        action: 'Proposta enviada',
        user: 'Você (Freelancer)',
        timestamp: new Date().toISOString(),
        details: `Valor: R$ ${response.value.toLocaleString('pt-BR', { minimumFractionDigits: 2 })} - Prazo: ${response.executionDays} dias`,
      });

      return true;
    } catch (err) {
      console.error('Erro ao enviar resposta:', err);
      return false;
    }
  };

  const updateStatus = async (
    id: string,
    newStatus: Proposal['status'],
    reason?: string
  ): Promise<boolean> => {
    try {
      const proposal = proposals.value.find((p) => p.id === id);
      if (!proposal) return false;

      const oldStatus = proposal.status;
      proposal.status = newStatus;

      // TODO: Chamar API
      // await api.patch(`/proposals/${id}/status`, { status: newStatus, reason });

      const statusLabels: Record<string, string> = {
        pending: 'Pendente',
        negotiating: 'Negociando',
        accepted: 'Aceita',
        rejected: 'Recusada',
        completed: 'Concluída',
        archived: 'Arquivada',
      };

      proposal.auditLog = proposal.auditLog || [];
      proposal.auditLog.push({
        id: `log-${Date.now()}`,
        action: 'Status alterado',
        user: 'Você (Freelancer)',
        timestamp: new Date().toISOString(),
        details: `${statusLabels[oldStatus]} → ${statusLabels[newStatus]}${reason ? ` - Motivo: ${reason}` : ''}`,
      });

      return true;
    } catch (err) {
      console.error('Erro ao atualizar status:', err);
      return false;
    }
  };

  const rejectProposal = async (id: string, reason: string): Promise<boolean> => {
    return updateStatus(id, 'rejected', reason);
  };

  const acceptProposal = async (id: string): Promise<boolean> => {
    return updateStatus(id, 'accepted');
  };

  const completeProposal = async (id: string): Promise<boolean> => {
    return updateStatus(id, 'completed');
  };

  const archiveProposal = async (id: string): Promise<boolean> => {
    return updateStatus(id, 'archived');
  };

  const sendMessage = async (
    id: string,
    message: string,
    attachments?: File[]
  ): Promise<boolean> => {
    try {
      const proposal = proposals.value.find((p) => p.id === id);
      if (!proposal) return false;

      // TODO: Chamar API com upload de arquivos
      // const formData = new FormData();
      // formData.append('message', message);
      // attachments?.forEach((file) => formData.append('attachments', file));
      // await api.post(`/proposals/${id}/messages`, formData);

      const newMessage: ProposalMessage = {
        id: `msg-${Date.now()}`,
        sender: 'freelancer',
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

      proposal.messages = proposal.messages || [];
      proposal.messages.push(newMessage);

      proposal.auditLog = proposal.auditLog || [];
      proposal.auditLog.push({
        id: `log-${Date.now()}`,
        action: 'Mensagem enviada',
        user: 'Você (Freelancer)',
        timestamp: new Date().toISOString(),
      });

      return true;
    } catch (err) {
      console.error('Erro ao enviar mensagem:', err);
      return false;
    }
  };

  const updateMilestoneStatus = async (
    proposalId: string,
    milestoneId: string,
    status: Milestone['status']
  ): Promise<boolean> => {
    try {
      const proposal = proposals.value.find((p) => p.id === proposalId);
      if (!proposal?.freelancerResponse?.milestones) return false;

      const milestone = proposal.freelancerResponse.milestones.find((m) => m.id === milestoneId);
      if (!milestone) return false;

      milestone.status = status;

      // TODO: Chamar API
      // await api.patch(`/proposals/${proposalId}/milestones/${milestoneId}`, { status });

      const statusLabels: Record<string, string> = {
        pending: 'Pendente',
        in_progress: 'Em andamento',
        completed: 'Concluído',
      };

      proposal.auditLog = proposal.auditLog || [];
      proposal.auditLog.push({
        id: `log-${Date.now()}`,
        action: 'Marco atualizado',
        user: 'Você (Freelancer)',
        timestamp: new Date().toISOString(),
        details: `${milestone.title} → ${statusLabels[status]}`,
      });

      return true;
    } catch (err) {
      console.error('Erro ao atualizar marco:', err);
      return false;
    }
  };

  return {
    proposals,
    isLoading,
    error,
    stats,
    loadProposals,
    getProposalById,
    markAsViewed,
    sendResponse,
    updateStatus,
    rejectProposal,
    acceptProposal,
    completeProposal,
    archiveProposal,
    sendMessage,
    updateMilestoneStatus,
  };
});
