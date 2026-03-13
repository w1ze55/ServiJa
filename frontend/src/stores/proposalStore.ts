import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api from '@/api/config/axios';

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
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'pending' | 'negotiating' | 'accepted' | 'in_progress' | 'delivered' | 'rejected' | 'completed' | 'archived';
  receivedAt: string;
  attachments?: ProposalAttachment[];
  freelancerAttachments?: ProposalAttachment[];
  viewed: boolean;
  address?: string;
  desiredDate?: string;
  freelancerResponse?: FreelancerResponse;
  counterValue?: number;
  counterDays?: number;
  counterNotes?: string;
  rejectReason?: string;
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
  operationKey?: string;
  user: string;
  timestamp: string;
  details?: string;
}

export interface ProposalStats {
  pending: number;
  negotiating: number;
  inProgress: number;
  completed: number;
  rejected: number;
}

export const useProposalStore = defineStore('proposal', () => {
  const proposals = ref<Proposal[]>([]);
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

  const stats = computed<ProposalStats>(() => ({
    pending: proposals.value.filter((p) => p.status === 'pending').length,
    negotiating: proposals.value.filter((p) => p.status === 'negotiating' || p.status === 'accepted').length,
    inProgress: proposals.value.filter((p) => p.status === 'in_progress' || p.status === 'delivered').length,
    completed: proposals.value.filter((p) => p.status === 'completed' || p.status === 'archived').length,
    rejected: proposals.value.filter((p) => p.status === 'rejected').length,
  }));

  const loadProposals = async () => {
    isLoading.value = true;
    error.value = null;
    try {
      const response = await api.get('/service-requests/received');

      // Mapear dados da API para o formato da store
      proposals.value = response.data.map((req: any) => ({
        id: req.id.toString(),
        clientName: req.contractorName,
        clientAvatar: req.contractorAvatar,
        clientLocation: req.address || 'Não informado',
        service: req.serviceName,
        category: req.category,
        description: req.description,
        budget: Number(req.budget),
        deadline: req.deadline,
        priority: req.priority || 'MEDIUM',
        status: mapApiStatusToLocal(req.status),
        receivedAt: req.createdAt,
        attachments: (req.attachments || []).map((url: string, idx: number) => ({
          id: `att-${req.id}-${idx}`,
          name: extractFileName(url),
          size: '',
          url,
          type: getFileType(url),
        })),
        viewed: true,
        address: req.address,
        desiredDate: req.desiredDate,
        freelancerResponse: req.responseValue ? {
          value: Number(req.responseValue),
          executionDays: req.responseDays,
          scope: req.responseScope,
          notes: req.responseNotes,
          sentAt: req.respondedAt,
          milestones: req.milestones,
          attachments: (req.responseAttachments || []).map((url: string, idx: number) => ({
            id: `ratt-${req.id}-${idx}`,
            name: extractFileName(url),
            size: '',
            url,
            type: getFileType(url),
          }))
        } : undefined,
        counterValue: req.counterValue,
        counterDays: req.counterDays,
        counterNotes: req.counterNotes,
        rejectReason: req.rejectReason,
        freelancerAttachments: (req.freelancerAttachments || []).map((url: string, idx: number) => ({
          id: `fatt-${req.id}-${idx}`,
          name: extractFileName(url),
          size: '',
          url,
          type: getFileType(url),
        })),
      }));
    } catch (err) {
      error.value = 'Erro ao carregar propostas';
      console.error('Erro ao carregar propostas:', err);
      proposals.value = [];
    } finally {
      isLoading.value = false;
    }
  };

  // Mapear status da API para formato local
  const mapApiStatusToLocal = (status: string): Proposal['status'] => {
    const mapping: Record<string, Proposal['status']> = {
      'PENDING': 'pending',
      'SENT': 'pending',
      'NEGOTIATING': 'negotiating',
      'ACCEPTED': 'accepted',
      'IN_PROGRESS': 'in_progress',
      'DELIVERED': 'delivered',
      'REJECTED': 'rejected',
      'COMPLETED': 'completed',
      'CANCELLED': 'archived',
    };
    return mapping[status] || 'pending';
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
      await api.patch(`/service-requests/${id}/respond`, {
        value: response.value,
        executionDays: response.executionDays,
        scope: response.scope,
        notes: response.notes,
        milestones: response.milestones,
        attachments: response.attachments?.map(a => a.url)
      });

      const proposal = proposals.value.find((p) => p.id === id);
      if (proposal) {
        proposal.freelancerResponse = {
          ...response,
          sentAt: new Date().toISOString(),
        };
        proposal.status = 'negotiating';
      }

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
      // Mapear status local para API
      const apiStatusMapping: Record<string, string> = {
        'pending': 'PENDING',
        'negotiating': 'NEGOTIATING',
        'accepted': 'ACCEPTED',
        'in_progress': 'IN_PROGRESS',
        'rejected': 'REJECTED',
        'completed': 'COMPLETED',
        'archived': 'CANCELLED',
      };

      await api.patch(`/service-requests/${id}/status`, {
        status: apiStatusMapping[newStatus],
        reason,
      });

      const proposal = proposals.value.find((p) => p.id === id);
      if (proposal) {
        proposal.status = newStatus;
      }

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
    try {
      const response = await api.patch(`/service-requests/${id}/complete`);
      const updatedReq = response.data;

      const proposal = proposals.value.find((p) => p.id === id);
      if (proposal) {
        proposal.status = mapApiStatusToLocal(updatedReq.status);
      }
      return true;
    } catch (err) {
      console.error('Erro ao completar proposta:', err);
      return false;
    }
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

  const loadHistory = async (proposalId: string) => {
    try {
      const response = await api.get(`/service-requests/${proposalId}/history`);
      const proposal = proposals.value.find((p) => p.id === proposalId);
      if (proposal) {
        proposal.auditLog = response.data.map((entry: any) => ({
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

  const acceptCounterProposal = async (proposalId: string) => {
    try {
      isLoading.value = true;
      const response = await api.patch(`/service-requests/${proposalId}/accept-counter`);
      const updatedRequest = response.data;

      const index = proposals.value.findIndex((p) => p.id === proposalId);
      if (index !== -1) {
        proposals.value[index] = {
          ...proposals.value[index],
          status: 'accepted',
          counterValue: undefined,
          counterDays: undefined,
          counterNotes: undefined,
          freelancerResponse: updatedRequest.freelancerResponse ? {
            value: updatedRequest.freelancerResponse.value,
            executionDays: updatedRequest.freelancerResponse.executionDays,
            scope: updatedRequest.freelancerResponse.scope,
            notes: updatedRequest.freelancerResponse.notes,
            sentAt: updatedRequest.freelancerResponse.sentAt,
          } : proposals.value[index].freelancerResponse
        };
      }
      return true;
    } catch (err) {
      console.error('Erro ao aceitar contra proposta:', err);
      return false;
    } finally {
      isLoading.value = false;
    }
  };

  const rejectCounterProposal = async (proposalId: string, reason?: string) => {
    try {
      isLoading.value = true;
      await api.patch(`/service-requests/${proposalId}/reject-counter`, { reason });

      const index = proposals.value.findIndex((p) => p.id === proposalId);
      if (index !== -1) {
        proposals.value[index] = {
          ...proposals.value[index],
          counterValue: undefined,
          counterDays: undefined,
          counterNotes: undefined,
        };
      }
      return true;
    } catch (err) {
      console.error('Erro ao rejeitar contra proposta:', err);
      return false;
    } finally {
      isLoading.value = false;
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
    loadHistory,
    acceptCounterProposal,
    rejectCounterProposal,
  };
});
