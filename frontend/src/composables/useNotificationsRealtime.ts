import { ref } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { NotificationStreamEvent } from '@/types/notifications';

export function useNotificationsRealtime(userEmail: string) {
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const connectionError = ref<string | null>(null);
  const connectionPromise = ref<Promise<void> | null>(null);

  const connect = () => {
    if (client.value?.connected) {
      return Promise.resolve();
    }

    if (!userEmail) {
      connectionError.value = 'Email do usuário não disponível';
      return Promise.resolve();
    }

    if (connectionPromise.value) {
      return connectionPromise.value;
    }

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const wsUrl = `${apiUrl}/ws`;

    connectionPromise.value = new Promise<void>((resolve, reject) => {
      const stompClient = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.onConnect = () => {
        connected.value = true;
        connectionError.value = null;

        const destination = `/topic/notifications/${userEmail}`;
        stompClient.subscribe(destination, (message) => {
          try {
            const event: NotificationStreamEvent = JSON.parse(message.body);
            window.dispatchEvent(new CustomEvent('notification-stream-event', { detail: event }));
          } catch (error) {
            console.error('Erro ao processar evento de notificação:', error);
          }
        });

        resolve();
      };

      stompClient.onStompError = (frame) => {
        connected.value = false;
        connectionError.value = frame.headers['message'] || 'Erro STOMP';
        connectionPromise.value = null;
        reject(new Error(connectionError.value));
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
      connectionPromise.value = null;
    }
  };
  return {
    connected,
    connectionError,
    connect,
    disconnect,
  };
}
