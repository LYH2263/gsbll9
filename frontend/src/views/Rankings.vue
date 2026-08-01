<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-500 to-purple-500 p-6">
    <div class="max-w-6xl mx-auto">
      <div class="flex justify-between items-center mb-8">
        <div class="text-white">
          <h1 class="text-3xl font-bold">🏆 排名榜</h1>
          <p class="text-blue-100">CTF竞赛实时排名</p>
        </div>
        <router-link to="/contest" class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition">
          返回答题
        </router-link>
      </div>

      <div class="bg-white rounded-lg shadow-2xl p-8">
        <div class="mb-6">
          <h2 class="text-2xl font-bold text-gray-800 mb-4">参赛者排名</h2>
          <div class="text-sm text-gray-600">
            共 {{ rankings.length }} 名参赛者 | 已提交: {{ submittedCount }} | 未提交: {{ unsubmittedCount }}
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b-2 border-gray-300">
                <th class="px-6 py-3 text-left font-semibold text-gray-700">排名</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700">学号</th>
                <th class="px-6 py-3 text-left font-semibold text-gray-700">姓名</th>
                <th class="px-6 py-3 text-center font-semibold text-gray-700">分数</th>
                <th class="px-6 py-3 text-center font-semibold text-gray-700">用时</th>
                <th class="px-6 py-3 text-center font-semibold text-gray-700">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(rank, idx) in rankings"
                :key="idx"
                :class="[
                  'border-b hover:bg-gray-50 transition',
                  idx < 3 ? (idx === 0 ? 'bg-yellow-50' : idx === 1 ? 'bg-gray-50' : 'bg-orange-50') : ''
                ]"
              >
                <td class="px-6 py-4">
                  <span v-if="idx < 3" class="text-2xl">{{ ['🥇', '🥈', '🥉'][idx] }}</span>
                  <span v-else class="font-semibold text-gray-700">{{ rank.rank }}</span>
                </td>
                <td class="px-6 py-4 font-mono text-gray-800">{{ rank.studentId }}</td>
                <td class="px-6 py-4 text-gray-700">{{ rank.fullName || '-' }}</td>
                <td class="px-6 py-4 text-center">
                  <span class="inline-block px-3 py-1 bg-blue-100 text-blue-700 font-bold rounded">
                    {{ rank.score }}分
                  </span>
                </td>
                <td class="px-6 py-4 text-center text-gray-600">{{ formatSeconds(rank.useTime) }}</td>
                <td class="px-6 py-4 text-center">
                  <span v-if="rank.submitted" class="inline-block px-3 py-1 bg-green-100 text-green-700 rounded text-sm">
                    ✓ 已提交
                  </span>
                  <span v-else class="inline-block px-3 py-1 bg-yellow-100 text-yellow-700 rounded text-sm">
                    进行中
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { contestAPI } from '../api/client'

const rankings = ref([])

const submittedCount = computed(() => rankings.value.filter(r => r.submitted).length)
const unsubmittedCount = computed(() => rankings.value.filter(r => !r.submitted).length)

const formatSeconds = (seconds) => {
  if (!seconds) return '-'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分钟`
}

const loadRankings = async () => {
  try {
    const response = await contestAPI.getRankings()
    rankings.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load rankings:', err)
  }
}

let timer = null

onMounted(() => {
  loadRankings()
  // 每10秒刷新一次
  timer = setInterval(() => {
    loadRankings()
  }, 10000)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
  }
})
</script>
