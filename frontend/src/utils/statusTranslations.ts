// Mapeamento de status para português
export const STATUS_LABELS: Record<string, string> = {
    // Status de Propostas/Solicitações
    'pending': 'Pendente',
    'PENDING': 'Pendente',
    'sent': 'Enviada',
    'SENT': 'Enviada',
    'negotiating': 'Negociando',
    'NEGOTIATING': 'Negociando',
    'accepted': 'Aceita',
    'ACCEPTED': 'Aceita',
    'in_progress': 'Em Andamento',
    'IN_PROGRESS': 'Em Andamento',
    'delivered': 'Entregue',
    'DELIVERED': 'Entregue',
    'rejected': 'Recusada',
    'REJECTED': 'Recusada',
    'completed': 'Concluída',
    'COMPLETED': 'Concluída',
    'archived': 'Arquivada',
    'CANCELLED': 'Cancelada',

    // Status de Pagamento
    'PROCESSING': 'Processando',
    'PAID': 'Pago',
    'CONFIRMED': 'Confirmado',
    'PIX_CONFIRMED': 'PIX Confirmado',
    'FAILED': 'Falhou',
    'REFUNDED': 'Reembolsado'
};

/**
 * Retorna o label traduzido para um status.
 * @param status O status em inglês (ex: 'pending', 'DELIVERED')
 * @returns O status traduzido ou o próprio status caso não encontre tradução.
 */
export const getStatusLabel = (status: string): string => {
    if (!status) return '';
    return STATUS_LABELS[status] || status;
};
