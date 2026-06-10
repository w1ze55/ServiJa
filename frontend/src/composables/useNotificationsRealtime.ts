import { ref } from 'vue';
import { Client } from '@stomp/stompjs';
import type { NotificationStreamEvent } from '@/types/notifications';
import {
  createSockJsFactory,
  describeSockJsCandidate,
  getStompConnectHeaders,
} from '@/utils/websocket';

export function useNotificationsRealtime(userEmail: string) {
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const connectionError = ref<string | null>(null);
  const connectionPromise = ref<Promise<void> | null>(null);

  const connect = () => {
    if (client.value?.connected || client.value?.active) {
      return connectionPromise.value ?? Promise.resolve();
    }

    if (!userEmail) {
      connectionError.value = 'Email do usuario nao disponivel';
      return Promise.resolve();
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
        reconnectDelay: 1000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      stompClient.onConnect = () => {
        hasConnected = true;
        connected.value = true;
        connectionError.value = null;

        const destination = `/topic/notifications/${userEmail}`;
        stompClient.subscribe(destination, (message) => {
          try {
            const event: NotificationStreamEvent = JSON.parse(message.body);
            window.dispatchEvent(
              new CustomEvent('notification-stream-event', { detail: event })
            );
          } catch (error) {
            console.error('Erro ao processar evento de notificacao:', error);
          }
        });

        if (!settled) {
          settled = true;
          resolve();
        }
      };

      stompClient.onStompError = (frame) => {
        connected.value = false;
        connectionError.value = frame.headers['message'] || 'Erro STOMP';
        connectionPromise.value = null;

        if (!settled) {
          settled = true;
          reject(new Error(connectionError.value));
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
            `[notifications-ws] Falha ao conectar via ${describeSockJsCandidate(failedCandidate)}. Tentando ${describeSockJsCandidate(socketFactory.getCurrentCandidate())}.`
          );
          return;
        }

        connectionError.value = 'Erro ao conectar WebSocket';
        connectionPromise.value = null;
        if (!settled) {
          settled = true;
          reject(new Error(connectionError.value));
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

  return {
    connected,
    connectionError,
    connect,
    disconnect,
  };
}
