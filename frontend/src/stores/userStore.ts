import { defineStore } from 'pinia'
import { ref } from 'vue'

interface UserClient {
    id: string
    name: string
    email: string
    type: 'contractor' | 'provider' | 'admin'
    avatar?: string
}

export const useUserStore = defineStore('user', () => {
    const user = ref<UserClient | null>(null)

    // Inicializar do localStorage se disponível
    const init = () => {
        const storedUser = localStorage.getItem('user')
        if (storedUser) {
            try {
                user.value = JSON.parse(storedUser)
            } catch (e) {
                console.error('Erro ao parsear usuário do localStorage:', e)
                user.value = null
            }
        }
    }

    const setUser = (userData: UserClient | null) => {
        user.value = userData
        if (userData) {
            localStorage.setItem('user', JSON.stringify(userData))
        } else {
            localStorage.removeItem('user')
        }
    }

    const updateUser = (data: Partial<UserClient>) => {
        if (user.value) {
            user.value = { ...user.value, ...data }
            localStorage.setItem('user', JSON.stringify(user.value))
            console.log('User updated in store:', user.value)
        }
    }

    const clearUser = () => {
        user.value = null
        localStorage.removeItem('user')
        localStorage.removeItem('token')
    }

    // Chamar init imediatamente
    init()

    return {
        user,
        setUser,
        updateUser,
        clearUser,
        init
    }
})
