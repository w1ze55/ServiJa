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
  const pollingInterval = ref<ReturnType<typeof setInterval> | null>(null);
  const currentPaymentId = ref<number | null>(null);

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
      
      // Parar polling se estava ativo
      stopPolling();

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
      // Iniciar polling como fallback
      startPolling();
    };

    stompClient.onWebSocketClose = () => {
      console.log('❌ WebSocket desconectado');
      connected.value = false;
      // Iniciar polling como fallback
      startPolling();
    };

    stompClient.activate();
    client.value = stompClient;
  };

  // Polling como fallback quando WebSocket não funciona
  const startPolling = () => {
    if (pollingInterval.value) return;
    
    console.log('🔄 Iniciando polling como fallback...');
    
    pollingInterval.value = setInterval(async () => {
      if (currentPaymentId.value) {
        await checkPaymentStatus(currentPaymentId.value);
      }
    }, 5000); // Verificar a cada 5 segundos
  };

  const stopPolling = () => {
    if (pollingInterval.value) {
      clearInterval(pollingInterval.value);
      pollingInterval.value = null;
      console.log('🛑 Polling parado');
    }
  };

  // Verificar status do pagamento via API
  const checkPaymentStatus = async (paymentId: number) => {
    try {
      const apiUrl = import.meta.env.VITE_API_URL;
      const token = localStorage.getItem('token');
      
      const response = await fetch(`${apiUrl}/api/payments/${paymentId}/status`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        console.log('📊 Status do pagamento (polling):', data);
        
        // Se o status mudou para COMPLETED, notificar
        if (data.status === 'COMPLETED') {
          const notification: PaymentNotification = {
            type: 'PIX_CONFIRMED',
            paymentId: paymentId,
            status: 'COMPLETED',
            message: 'Pagamento confirmado!',
            timestamp: Date.now(),
          };
          
          lastNotification.value = notification;
          
          window.dispatchEvent(
            new CustomEvent('payment-notification', { detail: notification })
          );
          
          // Parar polling após confirmação
          stopPolling();
        } else if (data.status === 'FAILED' || data.status === 'CANCELLED') {
          const notification: PaymentNotification = {
            type: data.status === 'FAILED' ? 'PAYMENT_FAILED' : 'PAYMENT_CANCELLED',
            paymentId: paymentId,
            status: data.status,
            message: data.status === 'FAILED' ? 'Pagamento falhou' : 'Pagamento cancelado',
            timestamp: Date.now(),
          };
          
          lastNotification.value = notification;
          
          window.dispatchEvent(
            new CustomEvent('payment-notification', { detail: notification })
          );
          
          stopPolling();
        }
      }
    } catch (error) {
      console.error('Erro ao verificar status do pagamento:', error);
    }
  };

  // Método para definir o ID do pagamento atual (para polling)
  const setPaymentId = (paymentId: number) => {
    currentPaymentId.value = paymentId;
    console.log('💳 Payment ID definido para monitoramento:', paymentId);
  };

  const disconnect = () => {
    stopPolling();
    
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
    setPaymentId,
    startPolling,
    stopPolling,
  };
}
