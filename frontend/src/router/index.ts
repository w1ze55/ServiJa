import { createRouter, createWebHistory, RouteRecordRaw } from "vue-router";

// Importar as páginas
import LandingPage from "@/pages/LandingPage/index.vue";
import Login from "@/pages/Login/index.vue";
import Perfil from "@/pages/Perfil/index.vue";
import Dashboard from "@/pages/Dashboard/index.vue";
import FreelancerDashboard from "@/pages/FreelancerDashboard/index.vue";
import ClienteHomePage from "@/pages/ClienteDashboard/index.vue";
import FreelancerProfileSettings from "@/pages/FreelancerProfileSettings/index.vue";
import FreelancerProfessionalProfile from "@/pages/FreelancerProfessionalProfile/index.vue";
import FreelancerPortfolio from "@/pages/FreelancerPortfolio/index.vue";
import ClienteProfileSettings from "@/pages/ClienteProfileSettings/index.vue";
import ClienteRequestDetailPage from "@/pages/ClienteRequestDetail/index.vue";
import MapPage from "@/pages/Map/map.vue";
import SettingsPage from "@/pages/Settings/index.vue";
import FreelancerServicesPage from "@/pages/FreelancerServices/index.vue";
import FreelancerProposalsPage from "@/pages/FreelancerProposals/index.vue";
import FreelancerRequestDetailPage from "@/pages/FreelancerRequestDetail/index.vue";
import FreelancerDeliveriesRoutePage from "@/pages/FreelancerDeliveries/index.vue";
import FreelancerProjectDetailPage from "@/pages/FreelancerProjectDetail/index.vue";
import FreelancerWalletPage from "@/pages/FreelancerWallet/index.vue";


import MessagesPage from "@/pages/Messages/index.vue";
import NotificationsPage from "@/pages/Notifications/index.vue";
import ExplorePage from "@/pages/Explore/index.vue";
import SearchPage from "@/pages/Search/index.vue";
import MyRequestsPage from "@/pages/MyRequests/index.vue";
import FavoritesPage from "@/pages/Favorites/index.vue";
import FreelancerKYCPage from "@/pages/FreelancerKYC/index.vue";
import ProviderProfilePage from "@/pages/ProviderProfile/index.vue";
import ServiceDetailPage from "@/pages/ServiceDetail/index.vue";
import NotFoundPage from "@/pages/NotFound/index.vue";
import RegisterPage from "@/pages/Register/index.vue";
import CheckoutPage from "@/pages/Checkout/index.vue";
import PaymentSuccessPage from "@/pages/PaymentSuccess/index.vue";
import ClienteProjectsPage from "@/pages/ClienteProjects/index.vue";
import ClienteDeliveryApprovalPage from "@/pages/ClienteDeliveryApproval/index.vue";
import ClienteProjectDetailPage from "@/pages/ClienteProjectDetail/index.vue";
import RateFreelancerPage from "@/pages/RateFreelancer/index.vue";

const routes: RouteRecordRaw[] = [
  // ============ ROTAS PÚBLICAS ============
  {
    path: "/",
    name: "landing",
    component: LandingPage,
    meta: { requiresAuth: false },
  },
  {
    path: "/login",
    name: "login",
    component: Login,
    meta: { requiresAuth: false },
  },
  {
    path: "/cadastro",
    name: "cadastro",
    component: RegisterPage,
    meta: { requiresAuth: false },
  },

  // ============ ROTAS DE REDIRECIONAMENTO ============
  {
    path: "/dashboard",
    name: "dashboard",
    component: Dashboard,
    meta: { requiresAuth: true },
    // Redireciona automaticamente para dashboard específico
  },
  {
    path: "/perfil",
    name: "perfil",
    component: Perfil,
    meta: { requiresAuth: true },
    // Redireciona automaticamente para perfil específico
  },

  // ============ DASHBOARDS ESPECÍFICOS ============
  {
    path: "/freelancer-dashboard",
    name: "freelancer-dashboard",
    component: FreelancerDashboard,
    meta: { requiresAuth: true, userType: "provider" },
  },
  {
    path: "/cliente-home",
    name: "cliente-home",
    component: ClienteHomePage,
    meta: { requiresAuth: true, userType: "contractor" },
  },
  {
    path: "/cliente-dashboard",
    redirect: { name: "cliente-home" },
  },

  // ============ PROFILE SETTINGS ESPECÍFICOS ============
  {
    path: "/freelancer-profile-settings",
    name: "freelancer-profile-settings",
    component: FreelancerProfileSettings,
    meta: { requiresAuth: true, userType: "provider" },
  },
  {
    path: "/freelancer-professional-profile",
    name: "freelancer-professional-profile",
    component: FreelancerProfessionalProfile,
    meta: { requiresAuth: true, userType: "provider" },
  },
  {
    path: "/cliente-profile-settings",
    name: "cliente-profile-settings",
    component: ClienteProfileSettings,
    meta: { requiresAuth: true, userType: "contractor" },
  },
  {
    path: "/cliente-request-detail",
    name: "cliente-request-detail",
    component: ClienteRequestDetailPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },
  {
    path: "/cliente-projects",
    name: "cliente-projects",
    component: ClienteProjectsPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },
  {
    path: "/cliente-delivery-approval",
    name: "cliente-delivery-approval",
    component: ClienteDeliveryApprovalPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },
  {
    path: "/cliente-project-detail",
    name: "cliente-project-detail",
    component: ClienteProjectDetailPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // ============ MAPA ============
  {
    path: "/map",
    name: "map",
    component: MapPage,
    meta: { requiresAuth: true },
  },

  // ============ FREELANCER SERVICES ============
  {
    path: "/freelancer-services",
    name: "freelancer-services",
    component: FreelancerServicesPage,
    meta: { requiresAuth: true, userType: "provider" },
  },

  // ============ FREELANCER PROPOSALS ============
  {
    path: "/freelancer-proposals",
    name: "freelancer-proposals",
    component: FreelancerProposalsPage,
    meta: { requiresAuth: true, userType: "provider" },
  },
  {
    path: "/freelancer-proposal-detail",
    name: "freelancer-proposal-detail",
    component: FreelancerRequestDetailPage,
    meta: { requiresAuth: true, userType: "provider" },
  },

  // ============ FREELANCER DELIVERIES ============
  {
    path: "/freelancer-deliveries",
    name: "freelancer-deliveries",
    component: FreelancerDeliveriesRoutePage,
    meta: { requiresAuth: true, userType: "provider" },
  },
  {
    path: "/freelancer-project-detail",
    name: "freelancer-project-detail",
    component: FreelancerProjectDetailPage,
    meta: { requiresAuth: true, userType: "provider" },
  },

  // ============ FREELANCER WALLET ============
  {
    path: "/freelancer-wallet",
    name: "freelancer-wallet",
    component: FreelancerWalletPage,
    meta: { requiresAuth: true, userType: "provider" },
  },



  // ============ FREELANCER PORTFOLIO/GALLERY ============
  {
    path: "/freelancer-portfolio",
    name: "freelancer-portfolio",
    component: FreelancerPortfolio,
    meta: { requiresAuth: true, userType: "provider" },
  },

  // ============ MESSAGES ============
  {
    path: "/messages",
    name: "messages",
    component: MessagesPage,
    meta: { requiresAuth: true },
  },

  // ============ CONFIGURAÇÕES ============
  {
    path: "/settings",
    name: "settings",
    component: SettingsPage,
    meta: { requiresAuth: true },
  },

  // ============ ROTAS ADICIONAIS ============

  // Notifications
  {
    path: "/notifications",
    name: "notifications",
    component: NotificationsPage,
    meta: { requiresAuth: true },
  },

  // Explore (Cliente)
  {
    path: "/explore",
    name: "explore",
    component: ExplorePage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // Search
  {
    path: "/search",
    name: "search",
    component: SearchPage,
    meta: { requiresAuth: false },
  },

  // My Requests (Cliente)
  {
    path: "/my-requests",
    name: "my-requests",
    component: MyRequestsPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // Favorites (Cliente)
  {
    path: "/favorites",
    name: "favorites",
    component: FavoritesPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // Freelancer KYC
  {
    path: "/freelancer-kyc",
    name: "freelancer-kyc",
    component: FreelancerKYCPage,
    meta: { requiresAuth: true, userType: "provider" },
  },

  // Provider Profile (público)
  {
    path: "/provider-profile/:providerId",
    name: "provider-profile",
    component: ProviderProfilePage,
    meta: { requiresAuth: true },
    props: true,
  },

  // Service Detail Page
  {
    path: "/provider-profile/:providerId/service/:serviceId",
    name: "service-detail",
    component: ServiceDetailPage,
    meta: { requiresAuth: true },
    props: true,
  },

  // Checkout (Cliente)
  {
    path: "/checkout",
    name: "checkout",
    component: CheckoutPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // Payment Success
  {
    path: "/payment-success",
    name: "payment-success",
    component: PaymentSuccessPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // ============ AVALIAÇÕES ============
  {
    path: "/rate-freelancer",
    name: "rate-freelancer",
    component: RateFreelancerPage,
    meta: { requiresAuth: true, userType: "contractor" },
  },

  // ============ PÁGINA 404 - DEVE SER A ÚLTIMA ROTA ============
  {
    path: "/:pathMatch(.*)*",
    name: "not-found",
    component: NotFoundPage,
    meta: { requiresAuth: false },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// Guard de navegação para verificar autenticação
router.beforeEach((to, from, next) => {
  const userString = localStorage.getItem("user");
  const user = userString ? JSON.parse(userString) : null;
  const requiredUserType = to.meta.userType;

  // Se a rota requer autenticação e o usuário não está logado
  if (to.meta.requiresAuth && !user) {
    next("/login");
  } else if (
    user &&
    requiredUserType &&
    requiredUserType !== "both" &&
    user.type !== requiredUserType
  ) {
    next("/dashboard");
  } else {
    next();
  }
});

export default router;
