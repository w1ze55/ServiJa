import { ref, onMounted, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface ServiceRequestNotification {
  type: 'NEW_SERVICE_REQUEST' | 'SERVICE_REQUEST_STATUS_CHANGED' | 'NEW_PROPOSAL' | 'COUNTER_PROPOSAL' | 'PROPOSAL_ACCEPTED' | 'SERVICE_REQUEST_CANCELLED' | 'NEW_REVIEW' | 'SERVICE_COMPLETED_REVIEW_PENDING' | 'COUNTER_PROPOSAL_REJECTED' | 'ADJUSTMENTS_REQUESTED' | 'ADJUSTMENT_STATUS_CHANGED' | 'ADJUSTMENT_DELETED';
  serviceRequestId: number;
  contractorName?: string;
  freelancerName?: string;
  serviceName?: string;
  status?: string;
  proposalAmount?: string;
  counterProposalAmount?: string;
  reason?: string;
  reviewId?: number;
  reviewerName?: string;
  rating?: number;
  actorName?: string;
  message: string;
  timestamp: number;
}


export function useServiceRequestWebSocket(userEmail: string) {
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const lastNotification = ref<ServiceRequestNotification | null>(null);
  const connectionPromise = ref<Promise<void> | null>(null);

  const connect = () => {
    if (client.value?.connected) {
      console.log('WebSocket de solicitações já está conectado');
      return Promise.resolve();
    }

    // Se já existe uma tentativa de conexão em andamento, retornar a promise existente
    if (connectionPromise.value) {
      console.log('WebSocket já está tentando conectar, aguardando...');
      return connectionPromise.value;
    }

    // Construir URL do WebSocket a partir da API URL
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const wsUrl = `${apiUrl}/ws`;

    // Criar uma promise que resolve quando a conexão for estabelecida
    connectionPromise.value = new Promise<void>((resolve, reject) => {
      const stompClient = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        debug: (str) => {
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.onConnect = () => {
        console.log('✅ WebSocket de solicitações conectado');
        connected.value = true;

        // Subscrever ao tópico de solicitações do usuário
        stompClient.subscribe(`/topic/service-request/${userEmail}`, (message) => {
          console.log('📩 Nova notificação de solicitação recebida:', message.body);
          
          try {
            const notification: ServiceRequestNotification = JSON.parse(message.body);
            lastNotification.value = notification;

            // Disparar evento customizado para o componente
            const event = new CustomEvent('service-request-notification', {
              detail: notification
            });
            window.dispatchEvent(event);

            console.log('🔔 Notificação processada:', notification.type);
          } catch (error) {
            console.error('❌ Erro ao processar notificação:', error);
          }
        });

        // Resolver a promise quando conectado e subscrito
        resolve();
      };

      stompClient.onStompError = (frame) => {
        connected.value = false;
        connectionPromise.value = null;
        reject(new Error('Erro ao conectar WebSocket'));
      };

      stompClient.onWebSocketClose = () => {
        connected.value = false;
        connectionPromise.value = null;
      };

      stompClient.activate();
      client.value = stompClient;
    });

    return connectionPromise.value;
  };

  const disconnect = () => {
    if (client.value) {
      client.value.deactivate();
      client.value = null;
      connected.value = false;
    }
  };

  // Auto-disconnect ao desmontar o componente
  onUnmounted(() => {
    disconnect();
  });

  return {
    client,
    connected,
    lastNotification,
    connect,
    disconnect
  };
}
