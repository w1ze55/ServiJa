import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/userStore';

// Mapa de rotas para freelancers
export const freelancerRoutes: Record<string, string> = {
  'home': '/freelancer-dashboard',
  'freelancer-dashboard': '/freelancer-dashboard',
  'profile': '/freelancer-profile-settings',
  'freelancer-profile': '/freelancer-profile-settings',
  'freelancer-profile-settings': '/freelancer-profile-settings',
  'freelancer-professional-profile': '/freelancer-professional-profile',
  'freelancer-services': '/freelancer-services',
  'freelancer-proposals': '/freelancer-proposals',
  'freelancer-proposal-detail': '/freelancer-proposal-detail',
  'freelancer-deliveries': '/freelancer-deliveries',
  'freelancer-wallet': '/freelancer-wallet',
  'freelancer-invoices': '/freelancer-invoices',
  'freelancer-portfolio': '/freelancer-portfolio',
  'freelancer-schedule': '/freelancer-schedule',
  'freelancer-kyc': '/freelancer-kyc',
  'freelancer-account-settings': '/freelancer-account-settings',
  'my-account': '/freelancer-profile-settings',
  'messages': '/messages',
  'notifications': '/notifications',
  'settings': '/settings',
};

// Mapa de rotas para clientes
export const clientRoutes: Record<string, string> = {
  'home': '/cliente-dashboard',
  'cliente-dashboard': '/cliente-dashboard',
  'contractor-dashboard': '/cliente-dashboard',
  'profile': '/cliente-profile-settings',
  'cliente-profile-settings': '/cliente-profile-settings',
  'my-account': '/cliente-profile-settings',
  'explore': '/explore',
  'search': '/search',
  'requests': '/my-requests',
  'favorites': '/favorites',
  'contracts': '/contracts',
  'messages': '/messages',
  'notifications': '/notifications',
  'settings': '/settings',
};

// Função para obter a rota baseada no tipo de usuário
export function getRoute(page: string, userType?: 'provider' | 'contractor'): string {
  if (userType === 'provider') {
    return freelancerRoutes[page] || `/${page}`;
  } else if (userType === 'contractor') {
    return clientRoutes[page] || `/${page}`;
  }

  // Fallback: tenta encontrar em qualquer um dos mapas
  return freelancerRoutes[page] || clientRoutes[page] || `/${page}`;
}

// Composable para navegação
export function useNavigation() {
  const router = useRouter();

  const navigate = (page: string, userType?: 'provider' | 'contractor', params?: Record<string, any>) => {
    const route = getRoute(page, userType);

    if (params) {
      router.push({ path: route, query: params });
    } else {
      router.push(route);
    }
  };

  const logout = () => {
    const userStore = useUserStore();
    userStore.clearUser();
    router.push('/login');
  };

  return {
    navigate,
    logout,
    getRoute,
  };
}
