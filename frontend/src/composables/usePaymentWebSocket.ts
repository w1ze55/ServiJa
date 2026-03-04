import { ref, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

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

  const connect = () => {
    if (client.value?.connected) {
      console.log('WebSocket já está conectado');
      return;
    }

    // Construir URL do WebSocket a partir da API URL
    const apiUrl = import.meta.env.VITE_API_URL;
    const wsUrl = `${apiUrl}/ws`;

    // Obter token JWT para autenticação
    const token = localStorage.getItem('token');

    console.log('🔌 Conectando WebSocket em:', wsUrl);
    console.log('📧 Email do usuário:', userEmail);
    console.log('🔑 Token presente:', !!token);

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: token ? `Bearer ${token}` : '',
        token: token || '',
      },
      debug: (str) => {
        if (str.includes('ERROR') || str.includes('CONNECTED') || str.includes('DISCONNECT')) {
          console.log('STOMP:', str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = () => {
      console.log('✅ WebSocket conectado e autenticado');
      connected.value = true;

      // Inscrever no tópico de pagamentos do usuário
      stompClient.subscribe(`/user/queue/payment`, (message) => {
        console.log('📨 Notificação de pagamento recebida:', message.body);
        const notification: PaymentNotification = JSON.parse(message.body);
        lastNotification.value = notification;

        // Disparar evento customizado
        window.dispatchEvent(
          new CustomEvent('payment-notification', { detail: notification })
        );
      });

      console.log('📡 Inscrito em /user/queue/payment');
    };

    stompClient.onStompError = (frame) => {
      console.error('❌ Erro STOMP:', frame.headers['message']);
      console.error('Detalhes:', frame.body);
    };

    stompClient.onWebSocketClose = () => {
      console.log('❌ WebSocket desconectado');
      connected.value = false;
    };

    stompClient.activate();
    client.value = stompClient;
  };

  const disconnect = () => {
    if (client.value) {
      client.value.deactivate();
      client.value = null;
      connected.value = false;
      console.log('WebSocket desconectado');
    }
  };

  // Desconectar automaticamente quando o componente for desmontado
  onUnmounted(() => {
    disconnect();
  });

  return {
    connected,
    lastNotification,
    connect,
    disconnect,
  };
}
