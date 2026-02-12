/**
 * EmailWorker - Worker para processamento assíncrono de emails (Versão Redis/BullMQ)
 * 
 * Responsabilidades:
 * - Enviar emails de verificação
 * - Enviar emails de recuperação de senha
 * - Enviar notificações por email
 * - Retry automático em caso de falha
 */

const { Worker } = require('bullmq');

class EmailWorker {
  constructor(services) {
    this.queueService = services.queueService;
    this.emailService = services.emailService;
    this.worker = null;
    this.queueName = 'email-send';
  }

  async start() {
    console.log(`📧 Iniciando EmailWorker (fila: ${this.queueName})...`);

    this.worker = new Worker(
      this.queueName,
      async (job) => {
        const { type, data } = job.data;
        console.log(`📧 Processando email tipo: ${type} - Job ID: ${job.id}`);

        try {
          switch (type) {
            case 'verification':
              await this.sendVerificationEmail(data);
              break;
            
            case 'password-reset':
              await this.sendPasswordResetEmail(data);
              break;
            
            case 'notification':
              await this.sendNotificationEmail(data);
              break;
            
            case 'welcome':
              await this.sendWelcomeEmail(data);
              break;

            default:
              throw new Error(`Tipo de email desconhecido: ${type}`);
          }

          console.log(`✅ Email ${type} enviado com sucesso - Job ID: ${job.id}`);
          return { success: true, type, timestamp: new Date().toISOString() };

        } catch (error) {
          console.error(`❌ Erro ao processar email ${type}:`, error.message);
          throw error; // BullMQ vai fazer retry automaticamente
        }
      },
      {
        connection: this.queueService.connection,
        concurrency: 5, // Processa até 5 emails simultaneamente
        limiter: {
          max: 10, // Máximo 10 jobs
          duration: 1000 // Por segundo (rate limiting)
        }
      }
    );

    // Event listeners
    this.worker.on('completed', (job) => {
      console.log(`✅ EmailWorker - Job ${job.id} completado`);
    });

    this.worker.on('failed', (job, err) => {
      console.error(`❌ EmailWorker - Job ${job.id} falhou:`, err.message);
    });

    this.worker.on('error', (err) => {
      console.error('❌ EmailWorker - Erro no worker:', err);
    });

    console.log(`✅ EmailWorker iniciado (concurrency: 5)`);
  }

  async sendVerificationEmail({ email, userName, verificationCode }) {
    console.log(`📧 Enviando email de verificação para ${email}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Email de verificação para ${email}:`);
      console.log(`   Código: ${verificationCode}`);
      console.log(`   Nome: ${userName}`);
      return;
    }

    // Implementação real do envio de email
    await this.emailService.sendVerification({
      to: email,
      subject: 'Verificação de Email - ServiJá',
      template: 'verification',
      data: {
        userName,
        verificationCode
      }
    });
  }

  async sendPasswordResetEmail({ email, userName, resetToken }) {
    console.log(`📧 Enviando email de recuperação de senha para ${email}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Email de recuperação para ${email}:`);
      console.log(`   Token: ${resetToken}`);
      console.log(`   Nome: ${userName}`);
      return;
    }

    await this.emailService.sendPasswordReset({
      to: email,
      subject: 'Recuperação de Senha - ServiJá',
      template: 'password-reset',
      data: {
        userName,
        resetToken
      }
    });
  }

  async sendNotificationEmail({ email, userName, subject, message }) {
    console.log(`📧 Enviando notificação por email para ${email}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Notificação para ${email}:`);
      console.log(`   Assunto: ${subject}`);
      console.log(`   Mensagem: ${message}`);
      return;
    }

    await this.emailService.send({
      to: email,
      subject,
      template: 'notification',
      data: {
        userName,
        message
      }
    });
  }

  async sendWelcomeEmail({ email, userName }) {
    console.log(`📧 Enviando email de boas-vindas para ${email}`);
    
    if (process.env.TEST_MODE === 'true') {
      console.log(`[TEST MODE] Email de boas-vindas para ${email}`);
      console.log(`   Nome: ${userName}`);
      return;
    }

    await this.emailService.send({
      to: email,
      subject: 'Bem-vindo ao ServiJá!',
      template: 'welcome',
      data: {
        userName
      }
    });
  }

  async stop() {
    if (this.worker) {
      console.log(`🔄 Parando EmailWorker...`);
      await this.worker.close();
      console.log(`✅ EmailWorker parado`);
    }
  }
}

module.exports = EmailWorker;
