import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const storedToken = localStorage.getItem('token')
  const storedUser = localStorage.getItem('user')
  
  let parsedUser = null
  try {
    parsedUser = storedUser ? JSON.parse(storedUser) : null
  } catch (e) {
    console.error('Failed to parse stored user:', e)
    localStorage.removeItem('user')
  }
  
  const user = ref(parsedUser)
  const token = ref(storedToken)
  const isAuthenticated = ref(!!token.value)

  const setAuth = (userData, authToken) => {
    user.value = userData
    token.value = authToken
    isAuthenticated.value = true
    localStorage.setItem('token', authToken)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  const logout = () => {
    user.value = null
    token.value = null
    isAuthenticated.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const isAdmin = () => {
    const result = user.value?.role === 'admin'
    console.log('isAdmin check:', { user: user.value, role: user.value?.role, isAdmin: result })
    return result
  }

  return {
    user,
    token,
    isAuthenticated,
    setAuth,
    logout,
    isAdmin,
  }
})
