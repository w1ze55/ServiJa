import type { LegacyUserType, User, UserRole } from '@/types';

export type BackendUserRole = 'CONTRACTOR' | 'FREELANCER' | 'ADMIN';

const dedupeRoles = (roles: UserRole[]) => Array.from(new Set(roles));

export const mapBackendRoleToFrontend = (role?: string | null): UserRole => {
  if (role === 'ADMIN') return 'admin';
  if (role === 'FREELANCER') return 'freelancer';
  return 'contractor';
};

export const mapFrontendRoleToBackend = (role: UserRole): BackendUserRole => {
  if (role === 'admin') return 'ADMIN';
  if (role === 'freelancer') return 'FREELANCER';
  return 'CONTRACTOR';
};

export const mapFrontendRoleToLegacyType = (role: UserRole): LegacyUserType => {
  if (role === 'admin') return 'admin';
  if (role === 'freelancer') return 'provider';
  return 'contractor';
};

export const mapLegacyTypeToFrontendRole = (type?: string | null): UserRole => {
  if (type === 'admin') return 'admin';
  if (type === 'provider') return 'freelancer';
  return 'contractor';
};

export const normalizeStoredUser = (rawUser: Partial<User> | null | undefined): User | null => {
  if (!rawUser) {
    return null;
  }

  const fallbackRole = mapLegacyTypeToFrontendRole(rawUser.type);
  const activeRole = rawUser.activeRole || fallbackRole;
  const roles = dedupeRoles(
    (rawUser.roles?.length ? rawUser.roles : [activeRole]).map((role) =>
      role === 'admin' || role === 'freelancer' || role === 'contractor'
        ? role
        : mapLegacyTypeToFrontendRole(String(role)),
    ),
  );

  if (!roles.includes(activeRole)) {
    roles.push(activeRole);
  }

  return {
    id: rawUser.id?.toString() || '',
    name: rawUser.name || '',
    email: rawUser.email || '',
    avatar: rawUser.avatar,
    token: rawUser.token,
    activeRole,
    roles,
    type: mapFrontendRoleToLegacyType(activeRole),
  };
};

export const buildUserFromApi = (
  payload: any,
  token?: string,
  fallbackEmail = '',
): User => {
  const rawUser = payload?.user || payload || {};
  const activeRole = mapBackendRoleToFrontend(rawUser.activeRole || rawUser.userType);
  const roles = dedupeRoles(
    ((rawUser.roles as string[] | undefined) || [rawUser.activeRole || rawUser.userType]).map(
      (role) => mapBackendRoleToFrontend(role),
    ),
  );

  return normalizeStoredUser({
    id: rawUser.id?.toString() || '',
    name: rawUser.name || '',
    email: rawUser.email || fallbackEmail,
    avatar: rawUser.avatarUrl || rawUser.avatar || undefined,
    token,
    activeRole,
    roles,
  }) as User;
};

export const mergeUserRoleState = (user: User, payload: any): User => {
  const nextActiveRole = payload?.activeRole
    ? mapBackendRoleToFrontend(payload.activeRole)
    : user.activeRole;
  const nextRoles = dedupeRoles(
    ((payload?.roles as string[] | undefined) || user.roles).map((role) =>
      role === 'admin' || role === 'freelancer' || role === 'contractor'
        ? role
        : mapBackendRoleToFrontend(role),
    ),
  );

  return normalizeStoredUser({
    ...user,
    activeRole: nextActiveRole,
    roles: nextRoles,
  }) as User;
};

export const getLandingRouteForRole = (role: UserRole): string => {
  if (role === 'admin') return '/admin/categories';
  if (role === 'freelancer') return '/freelancer-dashboard';
  return '/cliente-home';
};

export const isOwnFreelancerProfile = (
  user: User | null | undefined,
  freelancerUserId?: number | string | null,
): boolean => {
  if (!user || !freelancerUserId) {
    return false;
  }

  return user.activeRole === 'contractor' && String(user.id) === String(freelancerUserId);
};
