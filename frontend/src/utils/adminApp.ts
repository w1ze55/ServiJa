const DEFAULT_ADMIN_APP_URL = 'http://localhost:5174';

const normalizeAdminAppBaseUrl = () => {
  const configuredUrl = import.meta.env.VITE_ADMIN_APP_URL?.trim() || DEFAULT_ADMIN_APP_URL;
  const normalizedUrl = configuredUrl.replace(/\/+$/, '');

  return normalizedUrl.endsWith('/login')
    ? normalizedUrl.slice(0, -'/login'.length)
    : normalizedUrl;
};

export const getAdminAppUrl = (path = '/dashboard') => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${normalizeAdminAppBaseUrl()}${normalizedPath}`;
};

export const getAdminLoginUrl = () => getAdminAppUrl('/login');

export const redirectToAdminApp = (path = '/dashboard') => {
  if (typeof window !== 'undefined') {
    window.location.href = getAdminAppUrl(path);
  }
};
