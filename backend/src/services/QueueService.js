/**
 * QueueService - Serviço para adicionar jobs às filas (Versão Local - In-Memory)
 * 
 * Esta versão funciona em memória sem precisar de Redis
 * Ideal para desenvolvimento local
 */

// Filas em memória (simula Redis)
const inMemoryQueues = {
  'email-send': [],
  'freelancer-operations': [],
  'portfolio-operations': [],
  'notifications': []
};

class QueueService {
  constructor() {
    this.connection = {
      host: process.env.REDIS_HOST || 'localhost',
      port: parseInt(process.env.REDIS_PORT || '6379'),
      password: process.env.REDIS_PASSWORD || undefined
    };

    // Usar filas em memória
    this.inMemoryQueues = inMemoryQueues;
  }

  // Método para adicionar job à fila (simula BullMQ)
  async addJob(queueName, jobData, options = {}) {
    const jobId = Math.random().toString(36).substring(7);
    const job = {
      id: jobId,
      data: jobData,
      options,
      timestamp: new Date().toISOString()
    };
    
    if (!this.inMemoryQueues[queueName]) {
      this.inMemoryQueues[queueName] = [];
    }
    
    this.inMemoryQueues[queueName].push(job);
    console.log(`📥 [QueueService] Job adicionado à fila ${queueName} (ID: ${jobId})`);
    
    return { id: jobId };
  }

  // Método para obter um job da fila
  getJob(queueName) {
    const queue = this.inMemoryQueues[queueName];
    return queue && queue.length > 0 ? queue.shift() : null;
  }

  // Método para obter tamanho da fila
  getQueueLength(queueName) {
    return this.inMemoryQueues[queueName]?.length || 0;
  }

  // ============================================================================
  // EMAIL JOBS
  // ============================================================================

  /**
   * Enviar email de verificação
   */
  async sendVerificationEmail(email, userName, verificationCode) {
    return await this.addJob('email-send', {
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
    return await this.addJob('email-send', {
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
    return await this.addJob('email-send', {
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
    return await this.addJob('email-send', {
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
    return await this.addJob('freelancer-operations', {
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
    return await this.addJob('freelancer-operations', {
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
    return await this.addJob('freelancer-operations', {
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
    return await this.addJob('freelancer-operations', {
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
    return await this.addJob('freelancer-operations', {
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
    return await this.addJob('portfolio-operations', {
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
    return await this.addJob('portfolio-operations', {
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
    return await this.addJob('portfolio-operations', {
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
    return await this.addJob('portfolio-operations', {
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
    return await this.addJob('portfolio-operations', {
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
    return await this.addJob('portfolio-operations', {
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
    return await this.addJob('notifications', {
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
    return await this.addJob('notifications', {
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
    return await this.addJob('notifications', {
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

    return await this.addJob('notifications', {
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
    return await this.addJob('notifications', {
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
   * Obter estatísticas de uma fila (versão simplificada)
   */
  async getQueueStats(queueName) {
    if (!this.inMemoryQueues[queueName]) {
      throw new Error(`Fila ${queueName} não encontrada`);
    }

    const waiting = this.inMemoryQueues[queueName].length;

    return {
      queueName,
      waiting,
      active: 0,
      completed: 0,
      failed: 0,
      delayed: 0,
      total: waiting
    };
  }

  /**
   * Obter todas as estatísticas
   */
  async getAllStats() {
    const stats = {};
    for (const queueName of Object.keys(this.inMemoryQueues)) {
      stats[queueName] = await this.getQueueStats(queueName);
    }
    return stats;
  }

  /**
   * Limpar jobs completados (limpeza) - simplificado para modo local
   */
  async cleanCompletedJobs(queueName, olderThanMs = 24 * 60 * 60 * 1000) {
    if (!this.inMemoryQueues[queueName]) {
      throw new Error(`Fila ${queueName} não encontrada`);
    }
    // Em modo local, jobs são processados e removidos automaticamente
    console.log(`Limpeza de fila ${queueName} (modo local - não é necessário)`);
  }

  /**
   * Fechar todas as conexões - simplificado para modo local
   */
  async close() {
    console.log('Fechando QueueService (modo local)');
    // Em modo local, não há conexões para fechar
  }
}

module.exports = QueueService;
