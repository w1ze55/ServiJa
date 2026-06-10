import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import {
  createSockJsFactory,
  describeSockJsCandidate,
  getStompConnectHeaders,
} from '@/utils/websocket';

export interface ServiceRequestNotification {
  type:
    | 'NEW_SERVICE_REQUEST'
    | 'SERVICE_REQUEST_STATUS_CHANGED'
    | 'NEW_PROPOSAL'
    | 'COUNTER_PROPOSAL'
    | 'PROPOSAL_ACCEPTED'
    | 'SERVICE_REQUEST_CANCELLED'
    | 'NEW_REVIEW'
    | 'SERVICE_COMPLETED_REVIEW_PENDING'
    | 'COUNTER_PROPOSAL_REJECTED'
    | 'ADJUSTMENTS_REQUESTED'
    | 'ADJUSTMENT_STATUS_CHANGED'
    | 'ADJUSTMENT_DELETED';
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
    if (client.value?.connected || client.value?.active) {
      return connectionPromise.value ?? Promise.resolve();
    }

    if (connectionPromise.value) {
      return connectionPromise.value;
    }

    connectionPromise.value = new Promise<void>((resolve, reject) => {
      const socketFactory = createSockJsFactory();
      let hasConnected = false;
      let settled = false;

      const stompClient = new Client({
        webSocketFactory: socketFactory.webSocketFactory,
        connectHeaders: getStompConnectHeaders(),
        debug: () => {},
        reconnectDelay: 1000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.onConnect = () => {
        hasConnected = true;
        connected.value = true;

        stompClient.subscribe(`/topic/service-request/${userEmail}`, (message) => {
          try {
            const notification: ServiceRequestNotification = JSON.parse(message.body);
            lastNotification.value = notification;

            window.dispatchEvent(
              new CustomEvent('service-request-notification', {
                detail: notification,
              })
            );
          } catch (error) {
            console.error('Erro ao processar notificacao de solicitacao:', error);
          }
        });

        if (!settled) {
          settled = true;
          resolve();
        }
      };

      stompClient.onStompError = () => {
        connected.value = false;
        connectionPromise.value = null;

        if (!settled) {
          settled = true;
          reject(new Error('Erro ao conectar WebSocket'));
        }
      };

      stompClient.onWebSocketClose = () => {
        connected.value = false;

        if (hasConnected) {
          connectionPromise.value = null;
          return;
        }

        const failedCandidate = socketFactory.getCurrentCandidate();
        if (socketFactory.advanceCandidate()) {
          console.warn(
            `[service-request-ws] Falha ao conectar via ${describeSockJsCandidate(failedCandidate)}. Tentando ${describeSockJsCandidate(socketFactory.getCurrentCandidate())}.`
          );
          return;
        }

        connectionPromise.value = null;
        if (!settled) {
          settled = true;
          reject(new Error('Erro ao conectar WebSocket'));
        }
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
      connectionPromise.value = null;
    }
  };

  onUnmounted(() => {
    disconnect();
  });

  return {
    client,
    connected,
    lastNotification,
    connect,
    disconnect,
  };
}
