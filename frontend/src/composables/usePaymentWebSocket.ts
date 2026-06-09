import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import {
  createSockJsFactory,
  describeSockJsCandidate,
  getStompConnectHeaders,
} from '@/utils/websocket';

interface PaymentNotification {
  type: string;
  paymentId: number;
  status?: string;
  message?: string;
  timestamp: number;
}

export function usePaymentWebSocket(userEmail: string) {
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const lastNotification = ref<PaymentNotification | null>(null);
  const connectionError = ref<string | null>(null);

  const connect = () => {
    if (client.value?.connected || client.value?.active) {
      return;
    }

    if (!userEmail) {
      connectionError.value = 'Email do usuario nao disponivel';
      return;
    }

    const socketFactory = createSockJsFactory();
    let hasConnected = false;

    const stompClient = new Client({
      webSocketFactory: socketFactory.webSocketFactory,
      connectHeaders: getStompConnectHeaders(),
      debug: (message) => {
        if (
          message.includes('ERROR') ||
          message.includes('CONNECTED') ||
          message.includes('DISCONNECT') ||
          message.includes('SUBSCRIBE')
        ) {
          return;
        }
      },
      reconnectDelay: 1000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = () => {
      hasConnected = true;
      connected.value = true;
      connectionError.value = null;

      const destination = `/topic/payment/${userEmail}`;
      stompClient.subscribe(destination, (message) => {
        try {
          const notification: PaymentNotification = JSON.parse(message.body);
          lastNotification.value = notification;
          window.dispatchEvent(
            new CustomEvent('payment-notification', { detail: notification })
          );
        } catch (error) {
          console.error('Erro ao processar notificacao de pagamento:', error);
        }
      });
    };

    stompClient.onStompError = (frame) => {
      connectionError.value = frame.headers['message'] || 'Erro STOMP';
      connected.value = false;
    };

    stompClient.onWebSocketClose = () => {
      connected.value = false;

      if (hasConnected) {
        return;
      }

      const failedCandidate = socketFactory.getCurrentCandidate();
      if (!socketFactory.advanceCandidate()) {
        connectionError.value = 'Erro ao conectar WebSocket';
        return;
      }

      console.warn(
        `[payment-ws] Falha ao conectar via ${describeSockJsCandidate(failedCandidate)}. Tentando ${describeSockJsCandidate(socketFactory.getCurrentCandidate())}.`
      );
    };

    stompClient.activate();
    client.value = stompClient;
  };

  const disconnect = () => {
    if (client.value) {
      client.value.deactivate();
      client.value = null;
      connected.value = false;
    }
  };

  onUnmounted(() => {
    disconnect();
  });

  return {
    connected,
    lastNotification,
    connectionError,
    connect,
    disconnect,
  };
}
