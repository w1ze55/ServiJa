// Exporta todos os serviços da API
export { authService } from './services/authService';
export { freelancerService } from './services/freelancerService';
export { clientService } from './services/clientService';
export { dashboardService } from './services/dashboardService';
export { favoriteService } from './services/favoriteService';

// Exporta a instância configurada do axios
export { default as api } from './config/axios';

// Exporta tipos
export type { LoginCredentials, RegisterData, User } from './services/authService';
export type { Freelancer, FreelancerProfile, Service, Review } from './services/freelancerService';
export type { Client, Quote, Contract } from './services/clientService';
export type { DashboardStats, RecentActivity } from './services/dashboardService';

