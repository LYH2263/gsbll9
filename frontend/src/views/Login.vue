<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-500 via-purple-500 to-pink-500 flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-white rounded-lg shadow-2xl p-8">
      <div class="text-center mb-8">
        <h1 class="text-4xl font-bold text-gray-800 mb-2">校园 CTF 竞赛</h1>
        <p class="text-gray-600">学号登录 · Flag 提交与实时榜单</p>
      </div>

      <form @submit.prevent="handleLogin" class="space-y-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">学号</label>
          <input
            v-model="studentId"
            type="text"
            placeholder="请输入学号"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
            required
          />
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-gradient-to-r from-purple-500 to-pink-500 text-white font-semibold py-2 rounded-lg hover:shadow-lg transition disabled:opacity-50"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </form>

      <div v-if="error" class="mt-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { authAPI } from '../api/client'

const router = useRouter()
const authStore = useAuthStore()

const studentId = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  if (!studentId.value || !password.value) {
    error.value = '请输入学号和密码'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await authAPI.login(studentId.value, password.value)

    // 后端始终返回 HTTP 200，通过 code 字段判断成功与否
    if (!response.data || response.data.code !== 200) {
      error.value = response.data?.message || '登录失败，请检查学号和密码'
      return
    }

    const loginData = response.data.data

    if (!loginData || !loginData.token) {
      error.value = '登录响应异常，请重试'
      return
    }

    authStore.setAuth(loginData, loginData.token)

    if (loginData.role === 'admin') {
      router.push('/admin')
    } else {
      router.push('/contest')
    }
  } catch (err) {
    // HTTP 网络错误时尝试取后端消息，否则显示网络提示
    const backendMsg = err.response?.data?.message
    if (backendMsg) {
      error.value = backendMsg
    } else if (err.response) {
      error.value = `请求失败 (${err.response.status})，请重试`
    } else {
      error.value = '无法连接到服务器，请检查网络'
    }
    console.error('Login error:', err)
  } finally {
    loading.value = false
  }
}
</script>
