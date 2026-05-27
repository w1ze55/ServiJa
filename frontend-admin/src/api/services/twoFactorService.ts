import api from '@/api/config/axios'

export interface TwoFactorStatus {
  enabled: boolean
  enabledAt: string | null
  unusedBackupCodes: number
}

export interface TwoFactorSetup {
  secret: string
  qrCodeDataUri: string
}

export interface BackupCodesPayload {
  backupCodes: string[]
}

export const twoFactorService = {
  async status(): Promise<TwoFactorStatus> {
    const { data } = await api.get('/users/me/2fa/status')
    return data
  },
  async setup(): Promise<TwoFactorSetup> {
    const { data } = await api.post('/users/me/2fa/setup')
    return data
  },
  async enable(code: string): Promise<BackupCodesPayload> {
    const { data } = await api.post('/users/me/2fa/enable', { code })
    return data
  },
  async disable(password: string, code: string): Promise<void> {
    await api.post('/users/me/2fa/disable', { password, code })
  },
  async regenerateBackupCodes(password: string, code: string): Promise<BackupCodesPayload> {
    const { data } = await api.post('/users/me/2fa/backup-codes/regenerate', { password, code })
    return data
  },
}
