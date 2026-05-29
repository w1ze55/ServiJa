export type UserRole = 'contractor' | 'freelancer' | 'admin';
export type LegacyUserType = 'contractor' | 'provider' | 'admin';

export interface User {
  id: string;
  name: string;
  email: string;
  type: LegacyUserType;
  activeRole: UserRole;
  roles: UserRole[];
  avatar?: string;
  token?: string;
}

export interface RouterMeta {
  requiresAuth?: boolean;
  userType?: LegacyUserType | 'both';
}

declare module 'vue-router' {
  interface RouteMeta extends RouterMeta {}
}
