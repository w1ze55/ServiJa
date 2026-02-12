import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';

// Importar as páginas
import LandingPage from '@/pages/LandingPage/index.vue';
import Login from '@/pages/Login/index.vue';
import Perfil from '@/pages/Perfil/index.vue';
import Dashboard from '@/pages/Dashboard/index.vue';
import FreelancerDashboard from '@/pages/FreelancerDashboard/index.vue';
import ClienteDashboard from '@/pages/ClienteDashboard/index.vue';
import FreelancerProfileSettings from '@/pages/FreelancerProfileSettings/index.vue';
import FreelancerProfessionalProfile from '@/pages/FreelancerProfessionalProfile/index.vue';
//import FreelancerPortfolio from '@/pages/FreelancerPortfolio/index.vue';
import ClienteProfileSettings from '@/pages/ClienteProfileSettings/index.vue';
//import MapPage from '@/pages/Map/index.vue';
//import SettingsPage from '@/pages/Settings/index.vue';
//import FreelancerServicesPage from '@/pages/FreelancerServices/index.vue';
//import FreelancerProposalsPage from '@/pages/FreelancerProposals/index.vue';
//import FreelancerDeliveriesPage from '@/pages/FreelancerDeliveries/index.vue';
//import FreelancerWalletPage from '@/pages/FreelancerWallet/index.vue';
//import FreelancerInvoicesPage from '@/pages/FreelancerInvoices/index.vue';
//import FreelancerSchedulePage from '@/pages/FreelancerSchedule/index.vue';
//import MessagesPage from '@/pages/Messages/index.vue';
//import NotificationsPage from '@/pages/Notifications/index.vue';
//import ExplorePage from '@/pages/Explore/index.vue';
//import SearchPage from '@/pages/Search/index.vue';
//import MyRequestsPage from '@/pages/MyRequests/index.vue';
//import FavoritesPage from '@/pages/Favorites/index.vue';
//import ContractsPage from '@/pages/Contracts/index.vue';
//import FreelancerKYCPage from '@/pages/FreelancerKYC/index.vue';
//import FreelancerAccountSettingsPage from '@/pages/FreelancerAccountSettings/index.vue';

const routes: RouteRecordRaw[] = [
  // ============ ROTAS PÚBLICAS ============
  {
    path: '/',
    name: 'landing',
    component: LandingPage,
    meta: { requiresAuth: false },
  },
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { requiresAuth: false },
  },

  // ============ ROTAS DE REDIRECIONAMENTO ============
  {
    path: '/dashboard',
    name: 'dashboard',
    component: Dashboard,
    meta: { requiresAuth: true },
    // Redireciona automaticamente para dashboard específico
  },
  {
    path: '/perfil',
    name: 'perfil',
    component: Perfil,
    meta: { requiresAuth: true },
    // Redireciona automaticamente para perfil específico
  },

  // ============ DASHBOARDS ESPECÍFICOS ============
  {
    path: '/freelancer-dashboard',
    name: 'freelancer-dashboard',
    component: FreelancerDashboard,
    meta: { requiresAuth: true, userType: 'provider' },
  },
  {
    path: '/cliente-dashboard',
    name: 'cliente-dashboard',
    component: ClienteDashboard,
    meta: { requiresAuth: true, userType: 'contractor' },
  },

  // ============ PROFILE SETTINGS ESPECÍFICOS ============
  {
    path: '/freelancer-profile-settings',
    name: 'freelancer-profile-settings',
    component: FreelancerProfileSettings,
    meta: { requiresAuth: true, userType: 'provider' },
  },
  {
    path: '/freelancer-professional-profile',
    name: 'freelancer-professional-profile',
    component: FreelancerProfessionalProfile,
    meta: { requiresAuth: true, userType: 'provider' },
  },
  {
    path: '/cliente-profile-settings',
    name: 'cliente-profile-settings',
    component: ClienteProfileSettings,
    meta: { requiresAuth: true, userType: 'contractor' },
  },

  // ============ MAPA ============
  /*{
    path: '/map',
    name: 'map',
    component: MapPage,
    meta: { requiresAuth: true },
  },

  // ============ FREELANCER SERVICES ============
  {
    path: '/freelancer-services',
    name: 'freelancer-services',
    component: FreelancerServicesPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ FREELANCER PROPOSALS ============
  {
    path: '/freelancer-proposals',
    name: 'freelancer-proposals',
    component: FreelancerProposalsPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ FREELANCER DELIVERIES ============
  {
    path: '/freelancer-deliveries',
    name: 'freelancer-deliveries',
    component: FreelancerDeliveriesPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ FREELANCER WALLET ============
  {
    path: '/freelancer-wallet',
    name: 'freelancer-wallet',
    component: FreelancerWalletPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ FREELANCER INVOICES ============
  {
    path: '/freelancer-invoices',
    name: 'freelancer-invoices',
    component: FreelancerInvoicesPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ FREELANCER SCHEDULE ============
  {
    path: '/freelancer-schedule',
    name: 'freelancer-schedule',
    component: FreelancerSchedulePage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ FREELANCER PORTFOLIO/GALLERY ============
  {
    path: '/freelancer-portfolio',
    name: 'freelancer-portfolio',
    component: FreelancerPortfolio,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // ============ MESSAGES ============
  {
    path: '/messages',
    name: 'messages',
    component: MessagesPage,
    meta: { requiresAuth: true },
  },

  // ============ CONFIGURAÇÕES ============
  {
    path: '/settings',
    name: 'settings',
    component: SettingsPage,
    meta: { requiresAuth: true },
  },

  // ============ ROTAS ADICIONAIS ============

  // Notifications
  {
    path: '/notifications',
    name: 'notifications',
    component: NotificationsPage,
    meta: { requiresAuth: true },
  },

   Explore (Cliente)
  {
    path: '/explore',
    name: 'explore',
    component: ExplorePage,
    meta: { requiresAuth: true, userType: 'contractor' },
  },

  // Search
  {
    path: '/search',
    name: 'search',
    component: SearchPage,
    meta: { requiresAuth: false },
  },

  // My Requests (Cliente)
  {
    path: '/my-requests',
    name: 'my-requests',
    component: MyRequestsPage,
    meta: { requiresAuth: true, userType: 'contractor' },
  },

  // Favorites (Cliente)
  {
    path: '/favorites',
    name: 'favorites',
    component: FavoritesPage,
    meta: { requiresAuth: true, userType: 'contractor' },
  },

  // Contracts (Cliente)
  {
    path: '/contracts',
    name: 'contracts',
    component: ContractsPage,
    meta: { requiresAuth: true, userType: 'contractor' },
  },

  // Freelancer KYC
  {
    path: '/freelancer-kyc',
    name: 'freelancer-kyc',
    component: FreelancerKYCPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },

  // Freelancer Account Settings
  {
    path: '/freelancer-account-settings',
    name: 'freelancer-account-settings',
    component: FreelancerAccountSettingsPage,
    meta: { requiresAuth: true, userType: 'provider' },
  },*/
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// Guard de navegação para verificar autenticação
router.beforeEach((to, from, next) => {
  const userString = localStorage.getItem('user');
  const user = userString ? JSON.parse(userString) : null;

  // Se a rota requer autenticação e o usuário não está logado
  if (to.meta.requiresAuth && !user) {
    next('/login');
  }
  // Se o usuário está logado e tenta acessar login
  else if (to.name === 'login' && user) {
    next('/dashboard');
  }
  // Caso contrário, prosseguir
  else {
    next();
  }
});

export default router;
