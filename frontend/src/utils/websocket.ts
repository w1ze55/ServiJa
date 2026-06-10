import SockJS from 'sockjs-client';

const DEFAULT_API_BASE_URL = 'http://localhost:8080';
const XHR_FALLBACK_TRANSPORTS = ['xhr-streaming', 'xhr-polling'] as const;

type SockJsTransport = (typeof XHR_FALLBACK_TRANSPORTS)[number] | 'websocket';

interface SockJsCandidate {
  transports?: SockJsTransport[];
  url: string;
}

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '');
}

function normalizeApiBaseUrl(value?: string) {
  const trimmed = value?.trim();
  if (trimmed) {
    return trimTrailingSlash(trimmed.replace(/\/api\/?$/, ''));
  }

  if (typeof window !== 'undefined' && window.location.origin) {
    return trimTrailingSlash(window.location.origin);
  }

  return DEFAULT_API_BASE_URL;
}

function readStoredToken() {
  if (typeof window === 'undefined') {
    return null;
  }

  const directToken = localStorage.getItem('token')?.trim();
  if (directToken) {
    return directToken;
  }

  const rawUser = localStorage.getItem('user');
  if (!rawUser) {
    return null;
  }

  try {
    const parsedUser = JSON.parse(rawUser);
    return typeof parsedUser?.token === 'string' && parsedUser.token.trim()
      ? parsedUser.token.trim()
      : null;
  } catch {
    return null;
  }
}

function uniqueCandidates(candidates: SockJsCandidate[]) {
  const seen = new Set<string>();
  return candidates.filter((candidate) => {
    const key = `${candidate.url}::${candidate.transports?.join(',') ?? 'default'}`;
    if (seen.has(key)) {
      return false;
    }

    seen.add(key);
    return true;
  });
}

export function getSockJsCandidates() {
  const bases = new Set<string>([normalizeApiBaseUrl(import.meta.env.VITE_API_URL)]);

  if (typeof window !== 'undefined' && window.location.origin) {
    bases.add(trimTrailingSlash(window.location.origin));
  }

  const candidates: SockJsCandidate[] = [];
  for (const base of bases) {
    candidates.push({ url: `${base}/ws` });
    candidates.push({ url: `${base}/ws`, transports: [...XHR_FALLBACK_TRANSPORTS] });
    candidates.push({ url: `${base}/api/ws` });
    candidates.push({ url: `${base}/api/ws`, transports: [...XHR_FALLBACK_TRANSPORTS] });
  }

  return uniqueCandidates(candidates);
}

export function describeSockJsCandidate(candidate: SockJsCandidate) {
  if (!candidate.transports?.length) {
    return candidate.url;
  }

  return `${candidate.url} [${candidate.transports.join(', ')}]`;
}

export function createSockJsFactory(candidates = getSockJsCandidates()) {
  const safeCandidates =
    candidates.length > 0 ? candidates : [{ url: `${DEFAULT_API_BASE_URL}/ws` }];
  let currentIndex = 0;

  const getCurrentCandidate = () => safeCandidates[currentIndex] ?? safeCandidates[0];

  return {
    advanceCandidate() {
      if (currentIndex >= safeCandidates.length - 1) {
        return false;
      }

      currentIndex += 1;
      return true;
    },
    getCurrentCandidate,
    webSocketFactory: () => {
      const candidate = getCurrentCandidate();
      const options = candidate.transports
        ? { transports: [...candidate.transports] }
        : undefined;

      return new SockJS(candidate.url, undefined, options);
    },
  };
}

export function getStompConnectHeaders() {
  const token = readStoredToken();
  if (!token) {
    return {};
  }

  return {
    Authorization: `Bearer ${token}`,
    token,
  };
}
