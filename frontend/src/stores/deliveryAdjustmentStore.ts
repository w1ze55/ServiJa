import { defineStore } from 'pinia';
import { ref } from 'vue';
import api from '@/api/config/axios';

export interface DeliveryAdjustment {
    id: number;
    serviceRequestId: number;
    requestedByName: string;
    requestedByEmail: string;
    reason: string;
    status: 'PENDING' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';
    freelancerResponse?: string;
    respondedAt?: string;
    createdAt: string;
}

export interface DeliveryAdjustmentRequest {
    reason: string;
}

export interface AdjustmentStatusUpdate {
    status: string;
    response?: string;
}

export const useDeliveryAdjustmentStore = defineStore('deliveryAdjustment', () => {
    const adjustments = ref<DeliveryAdjustment[]>([]);
    const isLoading = ref(false);
    const error = ref<string | null>(null);

    const requestAdjustment = async (serviceRequestId: number, reason: string): Promise<boolean> => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.post(`/delivery-adjustments/service-request/${serviceRequestId}`, {
                reason
            });
            
            if (response.data) {
                console.log('Ajuste solicitado com sucesso:', response.data);
                return true;
            }
            return false;
        } catch (err: any) {
            console.error('Erro ao solicitar ajuste:', err);
            error.value = err.response?.data?.message || 'Erro ao solicitar ajuste';
            return false;
        } finally {
            isLoading.value = false;
        }
    };

    const getAdjustmentsByServiceRequest = async (serviceRequestId: number): Promise<void> => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.get(`/delivery-adjustments/service-request/${serviceRequestId}`);
            adjustments.value = response.data || [];
        } catch (err: any) {
            console.error('Erro ao carregar ajustes:', err);
            error.value = err.response?.data?.message || 'Erro ao carregar ajustes';
            adjustments.value = [];
        } finally {
            isLoading.value = false;
        }
    };

    const getMyRequests = async (): Promise<void> => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.get('/delivery-adjustments/my-requests');
            adjustments.value = response.data || [];
        } catch (err: any) {
            console.error('Erro ao carregar meus pedidos de ajuste:', err);
            error.value = err.response?.data?.message || 'Erro ao carregar ajustes';
            adjustments.value = [];
        } finally {
            isLoading.value = false;
        }
    };

    const getReceived = async (): Promise<void> => {
        isLoading.value = true;
        error.value = null;
        try {
            const response = await api.get('/delivery-adjustments/received');
            adjustments.value = response.data || [];
        } catch (err: any) {
            console.error('Erro ao carregar ajustes recebidos:', err);
            error.value = err.response?.data?.message || 'Erro ao carregar ajustes';
            adjustments.value = [];
        } finally {
            isLoading.value = false;
        }
    };

    const updateStatus = async (adjustmentId: number, status: string, response?: string): Promise<boolean> => {
        isLoading.value = true;
        error.value = null;
        try {
            const payload: AdjustmentStatusUpdate = {
                status,
                response
            };
            
            await api.patch(`/delivery-adjustments/${adjustmentId}/status`, payload);
            
            // Atualizar localmente
            const index = adjustments.value.findIndex(a => a.id === adjustmentId);
            if (index !== -1) {
                adjustments.value[index].status = status as any;
                if (response) {
                    adjustments.value[index].freelancerResponse = response;
                }
            }
            
            return true;
        } catch (err: any) {
            console.error('Erro ao atualizar status do ajuste:', err);
            error.value = err.response?.data?.message || 'Erro ao atualizar ajuste';
            return false;
        } finally {
            isLoading.value = false;
        }
    };

    const deleteAdjustment = async (adjustmentId: number): Promise<boolean> => {
        isLoading.value = true;
        error.value = null;
        try {
            await api.delete(`/delivery-adjustments/${adjustmentId}`);
            
            // Remover localmente
            adjustments.value = adjustments.value.filter(a => a.id !== adjustmentId);
            
            return true;
        } catch (err: any) {
            console.error('Erro ao deletar ajuste:', err);
            error.value = err.response?.data?.message || 'Erro ao deletar ajuste';
            return false;
        } finally {
            isLoading.value = false;
        }
    };

    return {
        adjustments,
        isLoading,
        error,
        requestAdjustment,
        getAdjustmentsByServiceRequest,
        getMyRequests,
        getReceived,
        updateStatus,
        deleteAdjustment
    };
});
