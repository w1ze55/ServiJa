export interface UserLocationAddressLike {
  id?: string | number
  city: string
  state: string
  isDefault?: boolean
}

interface UserLocationCachePayload {
  location: string
  address: {
    id?: string
    city: string
    state: string
    isDefault: boolean
  } | null
}

const USER_LOCATION_EVENT = 'user-location-updated'

const buildUserLocationCacheKey = (userId: string) => `user-location:${userId}`

export const getUserLocationCacheKey = buildUserLocationCacheKey

export const getCachedUserLocation = (userId: string): UserLocationCachePayload | null => {
  const cacheKey = buildUserLocationCacheKey(userId)
  const rawValue = localStorage.getItem(cacheKey)

  if (!rawValue) {
    return null
  }

  try {
    return JSON.parse(rawValue) as UserLocationCachePayload
  } catch (error) {
    console.error('Erro ao ler cache de localização do usuário:', error)
    localStorage.removeItem(cacheKey)
    return null
  }
}

export const setCachedUserLocation = (
  userId: string,
  address: UserLocationAddressLike | null,
) => {
  const cacheKey = buildUserLocationCacheKey(userId)

  if (!address?.city?.trim() || !address?.state?.trim()) {
    localStorage.removeItem(cacheKey)
    return
  }

  const normalizedState = address.state.trim().toUpperCase()
  const normalizedCity = address.city.trim()
  const payload: UserLocationCachePayload = {
    location: `${normalizedCity}, ${normalizedState}`,
    address: {
      id: address.id !== undefined ? String(address.id) : undefined,
      city: normalizedCity,
      state: normalizedState,
      isDefault: Boolean(address.isDefault),
    },
  }

  localStorage.setItem(cacheKey, JSON.stringify(payload))
}

export const clearCachedUserLocation = (userId: string) => {
  localStorage.removeItem(buildUserLocationCacheKey(userId))
}

export const dispatchUserLocationUpdated = (
  userId: string,
  address: UserLocationAddressLike | null,
) => {
  setCachedUserLocation(userId, address)

  window.dispatchEvent(
    new CustomEvent(USER_LOCATION_EVENT, {
      detail: {
        userId,
        address: address
          ? {
              id: address.id !== undefined ? String(address.id) : undefined,
              city: address.city.trim(),
              state: address.state.trim().toUpperCase(),
              isDefault: Boolean(address.isDefault),
            }
          : null,
      },
    }),
  )
}

export const userLocationUpdatedEventName = USER_LOCATION_EVENT
