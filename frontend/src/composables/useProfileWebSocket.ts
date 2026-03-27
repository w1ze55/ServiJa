import { ref } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useUserStore } from '@/stores/userStore';

export interface ProfileUpdateEvent {
  type: 'PROFILE_IMAGE_UPDATED';
  avatarUrl: string;
  timestamp: number;
}

export function useProfileWebSocket(userEmail: string) {
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

        const destination = `/topic/profile-update/${userEmail}`;
        stompClient.subscribe(destination, (message) => {
          try {
            const event: ProfileUpdateEvent = JSON.parse(message.body);

            if (event.type === 'PROFILE_IMAGE_UPDATED' && event.avatarUrl) {
              const userStore = useUserStore();
              userStore.updateUser({ avatar: event.avatarUrl });

              window.dispatchEvent(
                new CustomEvent('profile-image-updated', { detail: event })
              );
            }
          } catch (error) {
            console.error('Erro ao processar evento de perfil:', error);
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
