/**
 * QueueService - Serviço para adicionar jobs às filas (Versão com Redis/BullMQ)
 * 
 * Esta versão usa Redis e BullMQ para produção
 */

const { Queue } = require('bullmq');

class QueueService {
  constructor() {
    this.connection = {
      host: process.env.REDIS_HOST || 'localhost',
      port: parseInt(process.env.REDIS_PORT || '6379'),
      password: process.env.REDIS_PASSWORD || undefined
    };

    this.queues = this.initializeQueues();
  }

  initializeQueues() {
    console.log('🔧 Inicializando filas BullMQ com Redis...');
    console.log(`   Redis: ${this.connection.host}:${this.connection.port}`);
    
    return {
      email: new Queue('email-send', { connection: this.connection }),
      freelancer: new Queue('freelancer-operations', { connection: this.connection }),
      portfolio: new Queue('portfolio-operations', { connection: this.connection }),
      notification: new Queue('notifications', { connection: this.connection })
    };
  }

  // ============================================================================
  // EMAIL JOBS
  // ============================================================================

  /**
   * Enviar email de verificação
   */
  async sendVerificationEmail(email, userName, verificationCode) {
    return await this.queues.email.add('verification', {
      type: 'verification',
      data: {
        email,
        userName,
        verificationCode
      }
    }, {
      attempts: 3,
      backoff: {
        type: 'exponential',
        delay: 2000
      }
    });
  }

  /**
   * Enviar email de recuperação de senha
   */
  async sendPasswordResetEmail(email, userName, resetToken) {
    return await this.queues.email.add('password-reset', {
      type: 'password-reset',
      data: {
        email,
        userName,
        resetToken
      }
    }, {
      attempts: 3,
      backoff: {
        type: 'exponential',
        delay: 2000
      }
    });
  }

  /**
   * Enviar email de notificação genérica
   */
  async sendNotificationEmail(email, userName, subject, message) {
    return await this.queues.email.add('notification', {
      type: 'notification',
      data: {
        email,
        userName,
        subject,
        message
      }
    }, {
      attempts: 3,
      backoff: {
        type: 'exponential',
        delay: 2000
      }
    });
  }

  /**
   * Enviar email de boas-vindas
   */
  async sendWelcomeEmail(email, userName) {
    return await this.queues.email.add('welcome', {
      type: 'welcome',
      data: {
        email,
        userName
      }
    }, {
      attempts: 3,
      backoff: {
        type: 'exponential',
        delay: 2000
      }
    });
  }

  // ============================================================================
  // FREELANCER JOBS
  // ============================================================================

  /**
   * Atualizar perfil de freelancer
   */
  async updateFreelancerProfile(freelancerId, updates) {
    return await this.queues.freelancer.add('update-profile', {
      type: 'update-profile',
      data: {
        freelancerId,
        updates
      }
    });
  }

  /**
   * Atualizar localização do freelancer
   */
  async updateFreelancerLocation(freelancerId, latitude, longitude) {
    return await this.queues.freelancer.add('update-location', {
      type: 'update-location',
      data: {
        freelancerId,
        latitude,
        longitude
      }
    });
  }

  /**
   * Calcular estatísticas do freelancer
   */
  async calculateFreelancerStatistics(freelancerId) {
    return await this.queues.freelancer.add('calculate-statistics', {
      type: 'calculate-statistics',
      data: {
        freelancerId
      }
    }, {
      attempts: 2
    });
  }

  /**
   * Atualizar rating do freelancer
   */
  async updateFreelancerRating(freelancerId, rating, reviewId) {
    return await this.queues.freelancer.add('update-rating', {
      type: 'update-rating',
      data: {
        freelancerId,
        rating,
        reviewId
      }
    });
  }

  /**
   * Sincronizar freelancers próximos
   */
  async syncNearbyFreelancers(latitude, longitude, radiusKm, category = null) {
    return await this.queues.freelancer.add('sync-nearby', {
      type: 'sync-nearby',
      data: {
        latitude,
        longitude,
        radiusKm,
        category
      }
    });
  }

  // ============================================================================
  // PORTFOLIO JOBS
  // ============================================================================

  /**
   * Processar imagem do portfólio
   */
  async processPortfolioImage(projectId, imageId, imageUrl, options = {}) {
    return await this.queues.portfolio.add('process-image', {
      type: 'process-image',
      data: {
        projectId,
        imageId,
        imageUrl,
        options
      }
    }, {
      attempts: 2,
      backoff: {
        type: 'exponential',
        delay: 5000
      }
    });
  }

  /**
   * Gerar thumbnail
   */
  async generateThumbnail(imageUrl, width = 300, height = 300) {
    return await this.queues.portfolio.add('generate-thumbnail', {
      type: 'generate-thumbnail',
      data: {
        imageUrl,
        width,
        height
      }
    });
  }

  /**
   * Otimizar imagens de um projeto
   */
  async optimizeProjectImages(projectId) {
    return await this.queues.portfolio.add('optimize-images', {
      type: 'optimize-images',
      data: {
        projectId
      }
    }, {
      attempts: 1
    });
  }

  /**
   * Processar vídeo do portfólio
   */
  async processPortfolioVideo(projectId, videoUrl) {
    return await this.queues.portfolio.add('process-video', {
      type: 'process-video',
      data: {
        projectId,
        videoUrl
      }
    }, {
      attempts: 1,
      timeout: 300000
    });
  }

  /**
   * Sincronizar projeto
   */
  async syncProject(projectId, freelancerId) {
    return await this.queues.portfolio.add('sync-project', {
      type: 'sync-project',
      data: {
        projectId,
        freelancerId
      }
    });
  }

  /**
   * Deletar assets do portfólio
   */
  async deletePortfolioAssets(projectId, assetIds) {
    return await this.queues.portfolio.add('delete-assets', {
      type: 'delete-assets',
      data: {
        projectId,
        assetIds
      }
    }, {
      attempts: 2
    });
  }

  // ============================================================================
  // NOTIFICATION JOBS
  // ============================================================================

  /**
   * Enviar push notification
   */
  async sendPushNotification(userId, title, body, data = {}) {
    return await this.queues.notification.add('push', {
      type: 'push',
      data: {
        userId,
        title,
        body,
        data
      }
    }, {
      attempts: 3,
      backoff: {
        type: 'exponential',
        delay: 1000
      }
    });
  }

  /**
   * Enviar notificação in-app
   */
  async sendInAppNotification(userId, title, message, type, actionUrl = null) {
    return await this.queues.notification.add('in-app', {
      type: 'in-app',
      data: {
        userId,
        title,
        message,
        type,
        actionUrl
      }
    });
  }

  /**
   * Enviar broadcast notification
   */
  async sendBroadcastNotification(userIds, title, body, data = {}) {
    return await this.queues.notification.add('broadcast', {
      type: 'broadcast',
      data: {
        userIds,
        title,
        body,
        data
      }
    }, {
      attempts: 2
    });
  }

  /**
   * Agendar notificação
   */
  async scheduleNotification(notificationId, scheduledFor) {
    const scheduledDate = new Date(scheduledFor);
    const now = new Date();
    const delay = scheduledDate.getTime() - now.getTime();

    if (delay < 0) {
      throw new Error('A data agendada deve ser no futuro');
    }

    return await this.queues.notification.add('scheduled', {
      type: 'scheduled',
      data: {
        notificationId,
        scheduledFor
      }
    }, {
      delay: delay
    });
  }

  /**
   * Enviar lembrete
   */
  async sendReminder(userId, reminderType, data = {}) {
    return await this.queues.notification.add('reminder', {
      type: 'reminder',
      data: {
        userId,
        reminderType,
        data
      }
    });
  }

  // ============================================================================
  // UTILITY METHODS
  // ============================================================================

  /**
   * Obter estatísticas de uma fila
   */
  async getQueueStats(queueName) {
    const queue = this.queues[queueName];
    if (!queue) {
      throw new Error(`Fila ${queueName} não encontrada`);
    }

    const [waiting, active, completed, failed, delayed] = await Promise.all([
      queue.getWaitingCount(),
      queue.getActiveCount(),
      queue.getCompletedCount(),
      queue.getFailedCount(),
      queue.getDelayedCount()
    ]);

    return {
      queueName,
      waiting,
      active,
      completed,
      failed,
      delayed,
      total: waiting + active + delayed
    };
  }

  /**
   * Obter todas as estatísticas
   */
  async getAllStats() {
    const stats = {};
    for (const [name, queue] of Object.entries(this.queues)) {
      stats[name] = await this.getQueueStats(name);
    }
    return stats;
  }

  /**
   * Limpar jobs completados (limpeza)
   */
  async cleanCompletedJobs(queueName, olderThanMs = 24 * 60 * 60 * 1000) {
    const queue = this.queues[queueName];
    if (!queue) {
      throw new Error(`Fila ${queueName} não encontrada`);
    }

    await queue.clean(olderThanMs, 100, 'completed');
    await queue.clean(olderThanMs, 100, 'failed');
  }

  /**
   * Fechar todas as conexões
   */
  async close() {
    for (const queue of Object.values(this.queues)) {
      await queue.close();
    }
  }
}

module.exports = QueueService;
