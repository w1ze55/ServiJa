/**
 * NotificationWorker - Worker para processamento de notificações
 * 
 * Responsabilidades:
 * - Enviar notificações push
 * - Enviar notificações in-app
 * - Processar notificações agendadas
 * - Gerenciar preferências de notificação
 */

class NotificationWorker {
  constructor(services) {
    this.queueService = services.queueService;
    this.databaseService = services.databaseService;
    this.pushService = services.pushService;
    this.worker = null;
    this.queueName = 'notifications';
  }

  async start() {
    console.log(`🔔 Iniciando NotificationWorker (fila: ${this.queueName})...`);

    this.worker = setInterval(async () => {
      const job = this.queueService.getJob?.(this.queueName);
      
      if (!job) {
        return;
      }

      const { type, data } = job.data;
      console.log(`🔔 Processando notificação: ${type} - Job ID: ${job.id}`);

      try {
        switch (type) {
          case 'push':
            await this.sendPushNotification(data);
            break;
          
          case 'in-app':
            await this.sendInAppNotification(data);
            break;
          
          case 'broadcast':
            await this.sendBroadcastNotification(data);
            break;
          
          case 'scheduled':
            await this.processScheduledNotification(data);
            break;

          case 'reminder':
            await this.sendReminder(data);
            break;

          default:
            throw new Error(`Tipo de notificação desconhecido: ${type}`);
        }

        console.log(`✅ Notificação ${type} enviada - Job ID: ${job.id}`);

      } catch (error) {
        console.error(`❌ Erro ao processar notificação ${type}:`, error.message);
      }
    }, 1000);

    console.log(`✅ NotificationWorker iniciado (modo local)`);
  }

  async sendPushNotification({ userId, title, body, data = {} }) {
    console.log(`📱 Enviando push notification para usuário ${userId}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Push para ${userId}:`);
      console.log(`   Título: ${title}`);
      console.log(`   Corpo: ${body}`);
      console.log(`   Data:`, data);
      return;
    }

    // Verificar preferências do usuário
    // const preferences = await this.databaseService.getUserNotificationPreferences(userId);
    
    // if (!preferences.pushEnabled) {
    //   console.log(`⏭️  Usuário ${userId} desabilitou notificações push`);
    //   return;
    // }

    // Enviar notificação
    // await this.pushService.send({
    //   userId,
    //   title,
    //   body,
    //   data
    // });
  }

  async sendInAppNotification({ userId, title, message, type, actionUrl }) {
    console.log(`📬 Criando notificação in-app para usuário ${userId}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] In-app para ${userId}:`);
      console.log(`   Título: ${title}`);
      console.log(`   Mensagem: ${message}`);
      console.log(`   Tipo: ${type}`);
      return;
    }

    // Criar notificação no banco
    // await this.databaseService.createNotification({
    //   userId,
    //   title,
    //   message,
    //   type,
    //   actionUrl,
    //   read: false,
    //   createdAt: new Date()
    // });

    // Enviar via WebSocket se usuário estiver online
    // await this.sendRealtimeUpdate(userId, { type: 'new_notification' });
  }

  async sendBroadcastNotification({ userIds, title, body, data = {} }) {
    console.log(`📢 Enviando broadcast para ${userIds.length} usuários`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Broadcast:`);
      console.log(`   Usuários: ${userIds.length}`);
      console.log(`   Título: ${title}`);
      return;
    }

    // Enviar para todos os usuários
    for (const userId of userIds) {
      await this.sendPushNotification({ userId, title, body, data });
    }
  }

  async processScheduledNotification({ notificationId, scheduledFor }) {
    console.log(`⏰ Processando notificação agendada ${notificationId}`);
    
    const now = new Date();
    const scheduled = new Date(scheduledFor);

    if (scheduled > now) {
      // Reagendar para o futuro
      const delay = scheduled - now;
      console.log(`⏱️  Reagendando para ${scheduledFor} (${delay}ms)`);
      
      await this.queueService.addJob(
        this.queueName,
        {
          type: 'scheduled',
          data: { notificationId, scheduledFor }
        },
        { delay }
      );
      return;
    }

    // Processar notificação agora
    // const notification = await this.databaseService.getScheduledNotification(notificationId);
    // await this.sendPushNotification(notification);
    // await this.databaseService.markNotificationAsSent(notificationId);
  }

  async sendReminder({ userId, reminderType, data }) {
    console.log(`⏰ Enviando lembrete para usuário ${userId}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Lembrete tipo ${reminderType} para ${userId}`);
      return;
    }

    const reminders = {
      'profile-incomplete': {
        title: 'Complete seu perfil!',
        body: 'Adicione mais informações ao seu perfil para atrair mais clientes.',
        actionUrl: '/profile/edit'
      },
      'portfolio-empty': {
        title: 'Adicione projetos ao seu portfólio',
        body: 'Mostre seu trabalho e aumente suas chances de conseguir projetos.',
        actionUrl: '/portfolio/add'
      },
      'new-job': {
        title: 'Novo trabalho disponível!',
        body: 'Um novo trabalho na sua área foi publicado.',
        actionUrl: '/jobs/browse'
      }
    };

    const reminder = reminders[reminderType];
    if (reminder) {
      await this.sendInAppNotification({
        userId,
        ...reminder,
        type: 'reminder'
      });
    }
  }

  async stop() {
    if (this.worker) {
      console.log(`🔄 Parando NotificationWorker...`);
      clearInterval(this.worker);
      console.log(`✅ NotificationWorker parado`);
    }
  }
}

module.exports = NotificationWorker;
