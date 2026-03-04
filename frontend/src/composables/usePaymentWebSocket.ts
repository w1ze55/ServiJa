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
  const connectionError = ref<string | null>(null);

  const connect = () => {
    if (client.value?.connected) {
      console.log('WebSocket de pagamentos já está conectado');
      return;
    }

    if (!userEmail) {
      console.error('❌ userEmail não fornecido para WebSocket de pagamentos');
      connectionError.value = 'Email do usuário não disponível';
      return;
    }

    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const wsUrl = `${apiUrl}/ws`;

    console.log('🔌 Conectando WebSocket de pagamentos em:', wsUrl);
    console.log('📧 Email do usuário:', userEmail);

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      debug: (str) => {
        if (str.includes('ERROR') || str.includes('CONNECTED') || str.includes('DISCONNECT') || str.includes('SUBSCRIBE')) {
          console.log('STOMP (Pagamentos):', str);
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = () => {
      console.log('✅ WebSocket de pagamentos conectado');
      connected.value = true;
      connectionError.value = null;

      // Subscrever ao tópico de pagamentos do usuário (mesmo padrão das solicitações)
      const destination = `/topic/payment/${userEmail}`;
      console.log('📡 Inscrevendo em:', destination);
      
      stompClient.subscribe(destination, (message) => {
        console.log('📨 Notificação de pagamento recebida:', message.body);
        
        try {
          const notification: PaymentNotification = JSON.parse(message.body);
          lastNotification.value = notification;

          // Disparar evento customizado para o componente
          window.dispatchEvent(
            new CustomEvent('payment-notification', { detail: notification })
          );

          console.log('🔔 Notificação de pagamento processada:', notification.type);
        } catch (error) {
          console.error('❌ Erro ao processar notificação de pagamento:', error);
        }
      });

      console.log('📡 Inscrito em', destination);
    };

    stompClient.onStompError = (frame) => {
      console.error('❌ Erro STOMP (Pagamentos):', frame.headers['message']);
      console.error('Detalhes:', frame.body);
      connectionError.value = frame.headers['message'] || 'Erro STOMP';
      connected.value = false;
    };

    stompClient.onWebSocketClose = () => {
      console.log('🔌 WebSocket de pagamentos desconectado');
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
      console.log('🔌 WebSocket de pagamentos desconectado manualmente');
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
