/**
 * WorkerManager - Gerenciador central de todos os workers
 * 
 * Responsabilidades:
 * - Inicializar todos os workers
 * - Gerenciar lifecycle dos workers
 * - Monitorar saúde dos workers
 * - Graceful shutdown
 */

const EmailWorker = require('./EmailWorker');
const FreelancerWorker = require('./FreelancerWorker');
const PortfolioWorker = require('./PortfolioWorker');
const NotificationWorker = require('./NotificationWorker');

class WorkerManager {
  constructor(services) {
    this.services = services;
    this.workers = [];
    this.isRunning = false;
  }

  async startAllWorkers() {
    console.log('🚀 Iniciando todos os workers...\n');

    try {
      // Criar instâncias dos workers
      const emailWorker = new EmailWorker(this.services);
      const freelancerWorker = new FreelancerWorker(this.services);
      const portfolioWorker = new PortfolioWorker(this.services);
      const notificationWorker = new NotificationWorker(this.services);

      // Adicionar à lista
      this.workers = [
        { name: 'EmailWorker', instance: emailWorker, queueName: emailWorker.queueName },
        { name: 'FreelancerWorker', instance: freelancerWorker, queueName: freelancerWorker.queueName },
        { name: 'PortfolioWorker', instance: portfolioWorker, queueName: portfolioWorker.queueName },
        { name: 'NotificationWorker', instance: notificationWorker, queueName: notificationWorker.queueName }
      ];

      // Iniciar todos os workers
      for (const worker of this.workers) {
        try {
          await worker.instance.start();
          console.log(`✅ ${worker.name} iniciado com sucesso\n`);
        } catch (error) {
          console.error(`❌ Erro ao iniciar ${worker.name}:`, error.message);
          throw error;
        }
      }

      this.isRunning = true;
      console.log('✅ Todos os workers iniciados com sucesso!\n');

    } catch (error) {
      console.error('❌ Erro ao iniciar workers:', error);
      throw error;
    }
  }

  async stopAllWorkers() {
    if (!this.isRunning) {
      console.log('⚠️  Workers já estão parados');
      return;
    }

    console.log('🔄 Parando todos os workers...\n');

    for (const worker of this.workers) {
      try {
        await worker.instance.stop();
        console.log(`✅ ${worker.name} parado com sucesso`);
      } catch (error) {
        console.error(`❌ Erro ao parar ${worker.name}:`, error.message);
      }
    }

    this.isRunning = false;
    console.log('\n✅ Todos os workers parados com sucesso!');
  }

  getWorkersStatus() {
    return this.workers.map(worker => ({
      name: worker.name,
      running: this.isRunning,
      queueName: worker.instance.queueName
    }));
  }
}

module.exports = WorkerManager;
