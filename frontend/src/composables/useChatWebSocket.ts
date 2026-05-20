import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { MessageDTO } from '@/api/services/chatService';

export function useChatWebSocket() {
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const onMessage = ref<((msg: MessageDTO) => void) | null>(null);

  const connect = (conversationId: string) => {
    if (client.value?.connected) return;

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`${apiUrl}/ws`),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = () => {
      connected.value = true;
      stompClient.subscribe(`/topic/chat/${conversationId}`, (frame) => {
        try {
          const msg: MessageDTO = JSON.parse(frame.body);
          onMessage.value?.(msg);
        } catch (e) {
          console.error('Erro ao processar mensagem de chat:', e);
        }
      });
    };

    stompClient.onWebSocketClose = () => {
      connected.value = false;
    };

    stompClient.activate();
    client.value = stompClient;
  };

  const disconnect = () => {
    client.value?.deactivate();
    client.value = null;
    connected.value = false;
  };

  onUnmounted(disconnect);

  return { connected, onMessage, connect, disconnect };
}
