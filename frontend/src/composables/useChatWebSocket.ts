import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import type { MessageDTO, ReadReceiptDTO } from '@/api/services/chatService';
import {
  createSockJsFactory,
  describeSockJsCandidate,
  getStompConnectHeaders,
} from '@/utils/websocket';

export function useChatWebSocket() {
  const client = ref<Client | null>(null);
  const connected = ref(false);
  const onMessage = ref<((msg: MessageDTO) => void) | null>(null);
  const onReadReceipt = ref<((receipt: ReadReceiptDTO) => void) | null>(null);

  const connect = (conversationId: string) => {
    if (client.value?.connected || client.value?.active) return;

    const socketFactory = createSockJsFactory();
    let hasConnected = false;
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
      stompClient.subscribe(`/topic/chat/${conversationId}`, (frame) => {
        try {
          const msg: MessageDTO = JSON.parse(frame.body);
          onMessage.value?.(msg);
        } catch (e) {
          console.error('Erro ao processar mensagem de chat:', e);
        }
      });
      stompClient.subscribe(`/topic/chat/${conversationId}/read`, (frame) => {
        try {
          const receipt: ReadReceiptDTO = JSON.parse(frame.body);
          onReadReceipt.value?.(receipt);
        } catch (e) {
          console.error('Erro ao processar read receipt:', e);
        }
      });
    };

    stompClient.onWebSocketClose = () => {
      connected.value = false;
      if (hasConnected) {
        return;
      }

      const failedCandidate = socketFactory.getCurrentCandidate();
      if (!socketFactory.advanceCandidate()) {
        return;
      }

      console.warn(
        `[chat-ws] Falha ao conectar via ${describeSockJsCandidate(failedCandidate)}. Tentando ${describeSockJsCandidate(socketFactory.getCurrentCandidate())}.`
      );
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

  return { connected, onMessage, onReadReceipt, connect, disconnect };
}
