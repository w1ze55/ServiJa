const STORAGE_KEY = 'pendingEmailVerification';

export interface PendingEmailVerification {
  email: string;
  accountType?: 'contractor' | 'provider';
  name?: string;
}

export const getPendingEmailVerification = (): PendingEmailVerification | null => {
  const rawValue = localStorage.getItem(STORAGE_KEY);
  if (!rawValue) {
    return null;
  }

  try {
    const parsedValue = JSON.parse(rawValue) as PendingEmailVerification;
    if (!parsedValue?.email?.trim()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }

    return {
      email: parsedValue.email.trim(),
      accountType: parsedValue.accountType,
      name: parsedValue.name?.trim() || undefined,
    };
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
};

export const setPendingEmailVerification = (value: PendingEmailVerification) => {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      email: value.email.trim(),
      accountType: value.accountType,
      name: value.name?.trim() || undefined,
    }),
  );
};

export const clearPendingEmailVerification = () => {
  localStorage.removeItem(STORAGE_KEY);
};
