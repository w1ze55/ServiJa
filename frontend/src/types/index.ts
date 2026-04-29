// Tipos globais da aplicação

export interface User {
  id: string;
  name: string;
  email: string;
  type: 'contractor' | 'provider' | 'admin';
  avatar?: string;
  token?: string;
}

export interface RouterMeta {
  requiresAuth?: boolean;
  userType?: 'contractor' | 'provider' | 'admin' | 'both';
}

declare module 'vue-router' {
  interface RouteMeta extends RouterMeta {}
}
