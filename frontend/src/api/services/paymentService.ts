import api from '../config/axios';

export interface PaymentRequest {
  serviceRequestId: string;
  paymentMethod: 'pix' | 'credit-card' | 'wallet';
  amount: number;
  cardData?: {
    number: string;
    name: string;
    expiry: string;
    cvv: string;
    installmentCount: number;
  };
}

export interface Payment {
  id: string;
  serviceRequestId: string;
  amount: number;
  platformFee?: number;
  gatewayFee?: number;
  totalAmount?: number;
  paymentMethod: string;
  installmentCount?: number;
  status: 'pending' | 'processing' | 'completed' | 'failed' | 'refunded';
  transactionId?: string;
  pixQrCode?: string;
  pixCopyPaste?: string;
  createdAt: string;
  completedAt?: string;
}

export interface PaymentReceipt {
  id: string;
  paymentId: string;
  serviceRequestId: string;
  amount: number;
  platformFee: number;
  totalAmount: number;
  paymentMethod: string;
  freelancerName: string;
  serviceName: string;
  contractorName: string;
  issuedAt: string;
  receiptUrl: string;
}

export const paymentService = {
  /**
   * Cria um novo pagamento
   */
  async createPayment(data: PaymentRequest): Promise<Payment> {
    const response = await api.post('/payments', data);
    return response.data;
  },

  /**
   * Obtém detalhes de um pagamento
   */
  async getPayment(paymentId: string): Promise<Payment> {
    const response = await api.get(`/payments/${paymentId}`);
    return response.data;
  },

  /**
   * Lista pagamentos do usuário
   */
  async getMyPayments(): Promise<Payment[]> {
    const response = await api.get('/payments/my');
    return response.data;
  },

  /**
   * Verifica status de pagamento PIX
   */
  async checkPixPaymentStatus(paymentId: string): Promise<Payment> {
    const response = await api.get(`/payments/${paymentId}/status`);
    return response.data;
  },

  /**
   * Obtém recibo de pagamento
   */
  async getReceipt(paymentId: string): Promise<PaymentReceipt> {
    const response = await api.get(`/payments/${paymentId}/receipt`);
    return response.data;
  },

  /**
   * Download do recibo em PDF
   */
  async downloadReceiptPdf(paymentId: string): Promise<Blob> {
    const response = await api.get(`/payments/${paymentId}/receipt/pdf`, {
      responseType: 'blob',
    });
    return response.data;
  },

  /**
   * Obtém URL do recibo/comprovante via Asaas
   */
  async getReceiptUrl(paymentId: string): Promise<{ transactionReceiptUrl?: string; invoiceUrl?: string }> {
    const response = await api.get(`/payments/${paymentId}/receipt-url`);
    return response.data;
  },

  /**
   * Solicita reembolso
   */
  async requestRefund(paymentId: string, reason: string): Promise<Payment> {
    const response = await api.post(`/payments/${paymentId}/refund`, { reason });
    return response.data;
  },
};
