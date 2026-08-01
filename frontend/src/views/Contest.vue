<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-500 to-purple-500">
    <!-- 公告滚动横幅 -->
    <div 
      v-if="latestAnnouncement" 
      class="bg-yellow-400 text-yellow-900 py-3 shadow-lg announcement-enter"
    >
      <div class="announcement-marquee-wrapper">
        <div :key="announcementKey" class="announcement-marquee-content">
          <span class="font-bold mr-3">📢 公告：</span>
          {{ latestAnnouncement.content }}
          <span class="mx-10">◆</span>
          <span class="font-bold mr-3">📢 公告：</span>
          {{ latestAnnouncement.content }}
          <span class="mx-10">◆</span>
          <span class="font-bold mr-3">📢 公告：</span>
          {{ latestAnnouncement.content }}
          <span class="mx-10">◆</span>
        </div>
      </div>
    </div>

    <div class="max-w-7xl mx-auto p-6">
      <!-- 头部 -->
      <div class="flex justify-between items-center mb-8">
        <div class="text-white">
          <h1 class="text-3xl font-bold">校园 CTF 竞赛</h1>
          <p class="text-blue-100">Flag 提交 · 计分与实时榜单</p>
        </div>
        <button
          @click="handleLogout"
          class="px-6 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
        >
          退出登录
        </button>
      </div>

      <!-- 比赛状态卡 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="bg-white rounded-lg shadow-lg p-6">
          <h3 class="text-gray-700 font-semibold mb-2">比赛状态</h3>
          <p class="text-2xl font-bold text-purple-600">{{ getStatusLabel(contestStatus) }}</p>
          <p class="text-gray-500 text-sm mt-2">{{ statusMessage }}</p>
        </div>

        <div class="bg-white rounded-lg shadow-lg p-6">
          <h3 class="text-gray-700 font-semibold mb-2">倒计时</h3>
          <p class="text-2xl font-bold text-blue-600">{{ formatTime(remainingTime) }}</p>
          <p class="text-gray-500 text-sm mt-2">距离{{ nextEvent }}</p>
        </div>

        <div class="bg-white rounded-lg shadow-lg p-6">
          <h3 class="text-gray-700 font-semibold mb-2">个人排名</h3>
          <p class="text-2xl font-bold text-green-600">{{ myRank || '-' }}</p>
          <p class="text-gray-500 text-sm mt-2">当前成绩: {{ myScore }} 分</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 答题区 -->
        <div class="lg:col-span-2 bg-white rounded-lg shadow-lg p-8">
          <div v-if="!contestStarted && canStartContest && contestStatus === 'RUNNING'">
            <div class="text-center">
              <h2 class="text-2xl font-bold text-gray-800 mb-4">准备开始比赛？</h2>
              <p class="text-gray-600 mb-6">点击下方按钮开始答题</p>
              <button
                @click="startContest"
                :disabled="loading"
                class="px-8 py-3 bg-gradient-to-r from-green-500 to-blue-500 text-white font-bold rounded-lg hover:shadow-lg transition disabled:opacity-50"
              >
                {{ loading ? '加载中...' : '开始比赛' }}
              </button>
            </div>
          </div>

          <div v-else-if="!contestStarted && contestStatus === 'READY'">
            <div class="text-center py-12">
              <div class="text-6xl mb-4">⏰</div>
              <h2 class="text-2xl font-bold text-gray-800 mb-2">比赛准备中</h2>
              <p class="text-gray-600">比赛尚未正式开始，请等待开始时间到来</p>
              <p class="text-blue-500 mt-3 font-semibold">距离比赛开始：{{ formatTime(remainingTime) }}</p>
            </div>
          </div>

          <div v-else-if="!contestStarted && !canStartContest">
            <div class="text-center py-12">
              <div class="text-6xl mb-4">⏳</div>
              <h2 class="text-2xl font-bold text-gray-800 mb-2">{{ getStatusLabel(contestStatus) }}</h2>
              <p class="text-gray-600">{{ statusMessage }}</p>
            </div>
          </div>

          <div v-else-if="contestStarted && currentQuestion">
            <div class="mb-6">
              <div class="flex justify-between items-center mb-4">
                <h2 class="text-2xl font-bold text-gray-800">{{ currentQuestion.title }}</h2>
                <span class="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-semibold">
                  {{ currentQuestion.points }} 分
                </span>
              </div>
              <p class="text-gray-600">难度: {{ getDifficultyLabel(currentQuestion.difficulty) }}</p>
            </div>

            <div class="bg-gray-50 rounded-lg p-6 mb-6">
              <p class="text-gray-700 whitespace-pre-wrap">{{ currentQuestion.description }}</p>
              <div v-if="currentQuestion.fileUrl" class="mt-4">
                <a
                  :href="currentQuestion.fileUrl"
                  target="_blank"
                  class="inline-block px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition"
                >
                  📥 下载文件
                </a>
              </div>
            </div>

            <div v-if="currentQuestion.hints && currentQuestion.hints.length > 0" class="mb-6">
              <h3 class="text-lg font-bold text-gray-800 mb-3">💡 提示</h3>
              <div class="space-y-3">
                <div
                  v-for="(hint, index) in currentQuestion.hints"
                  :key="hint.id"
                  :class="[
                    'border rounded-lg p-4 transition',
                    hint.unlocked ? 'bg-yellow-50 border-yellow-300' : 'bg-gray-50 border-gray-200'
                  ]"
                >
                  <div class="flex justify-between items-start mb-2">
                    <span class="font-semibold text-gray-700">
                      提示 {{ index + 1 }}
                      <span v-if="hint.penalty > 0" class="text-sm text-red-500 ml-2">
                        (扣除 {{ hint.penalty }} 分)
                      </span>
                    </span>
                    <button
                  v-if="!hint.unlocked"
                  @click="unlockHint(hint)"
                  :disabled="loading || !scoringAllowed"
                      :class="[
                        'px-3 py-1 text-sm rounded transition',
                        canUnlockHint(index)
                          ? 'bg-orange-500 text-white hover:bg-orange-600 disabled:opacity-50'
                          : 'bg-gray-300 text-gray-500 cursor-not-allowed'
                      ]"
                    >
                      {{ canUnlockHint(index) ? '🔓 解锁' : '🔒 请先解锁前面的提示' }}
                    </button>
                  </div>
                  <div v-if="hint.unlocked" class="text-gray-700 bg-white p-3 rounded border border-yellow-200">
                    {{ hint.content }}
                  </div>
                  <div v-else class="text-gray-400 text-center py-3">
                    🔒 提示已锁定，请点击解锁按钮查看
                  </div>
                </div>
              </div>
            </div>

            <div class="mb-6">
              <label class="block text-gray-700 font-semibold mb-2">输入Flag答案</label>
              <div class="flex gap-2">
                <input
                  v-model="answer"
                  type="text"
                  placeholder="请输入答案"
                  class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                  @keyup.enter="submitAnswer"
                />
                <button
                  @click="submitAnswer"
                  :disabled="loading || !scoringAllowed"
                  class="px-6 py-2 bg-green-500 text-white font-semibold rounded-lg hover:bg-green-600 transition disabled:opacity-50"
                >
                  提交
                </button>
              </div>
            </div>

            <div v-if="answerFeedback" :class="[
              'p-4 rounded-lg mb-6',
              answerFeedback.correct ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
            ]">
              {{ answerFeedback.message }}
            </div>

            <div class="flex justify-between gap-4">
              <button
                @click="prevQuestion"
                :disabled="questionIndex === 0 || loading"
                class="flex-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition disabled:opacity-50"
              >
                ← 上一题
              </button>
              <div class="flex-1 text-center py-2 bg-gray-100 rounded-lg">
                <span class="text-gray-700 font-semibold">{{ questionIndex + 1 }}/{{ totalQuestions }}</span>
              </div>
              <button
                @click="nextQuestion"
                :disabled="questionIndex >= totalQuestions - 1 || loading"
                class="flex-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition disabled:opacity-50"
              >
                下一题 →
              </button>
            </div>
            <!-- 提交比赛按钮（答完所有题后显示） -->
            <div v-if="allAnswered" class="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg text-center">
              <p class="text-green-700 font-semibold mb-3">🎉 已完成所有题目！</p>
              <button
                @click="handleFinalize"
                :disabled="loading || finalized"
                class="px-8 py-3 bg-gradient-to-r from-green-500 to-blue-500 text-white font-bold rounded-lg hover:shadow-lg transition disabled:opacity-50"
              >
                {{ finalized ? '✅ 已提交比赛' : '🚀 提交比赛' }}
              </button>
              <p v-if="finalized" class="text-green-600 text-sm mt-2">成绩已记录，请查看右侧排名</p>
            </div>          </div>
        </div>

        <!-- 侧边栏 - 题目导航和排名 -->
        <div class="space-y-6">
          <!-- 题目导航 -->
          <div class="bg-white rounded-lg shadow-lg p-6">
            <h3 class="text-lg font-bold text-gray-800 mb-4">题目导航</h3>
            <div class="grid grid-cols-3 gap-2">
              <button
                v-for="(q, idx) in selectedQuestions"
                :key="q"
                @click="jumpToQuestion(q)"
                :class="[
                  'py-2 px-3 rounded font-semibold transition text-sm',
                  currentQuestion?.id === q
                    ? 'bg-purple-500 text-white'
                    : answeredQuestions.includes(q)
                    ? 'bg-green-100 text-green-700 hover:bg-green-200'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                ]"
              >
                {{ idx + 1 }}
              </button>
            </div>
          </div>

          <!-- 排名榜 -->
          <div class="bg-white rounded-lg shadow-lg p-6">
            <h3 class="text-lg font-bold text-gray-800 mb-4">🏆 排名榜</h3>
            <div class="space-y-2 max-h-96 overflow-y-auto">
              <div
                v-for="(rank, idx) in rankings.slice(0, 10)"
                :key="idx"
                :class="[
                  'flex justify-between items-center p-2 rounded',
                  rank.studentId === authStore.user?.studentId ? 'bg-purple-100' : 'bg-gray-50'
                ]"
              >
                <div class="flex-1">
                  <p class="font-semibold text-gray-800">
                    {{ idx + 1 }}. {{ rank.fullName || rank.studentId }}
                  </p>
                </div>
                <div class="text-right">
                  <p class="font-bold text-purple-600">{{ rank.score }}分</p>
                  <p class="text-xs text-gray-500">{{ formatSeconds(rank.useTime) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { contestAPI } from '../api/client'

const router = useRouter()
const authStore = useAuthStore()

const contestStatus = ref('NOT_STARTED')
const remainingTime = ref(0)
const canStartContest = ref(false)
const isContestActive = ref(false)
const scoringAllowed = ref(false)
const loading = ref(false)
const contestStarted = ref(false)
const currentQuestion = ref(null)
const selectedQuestions = ref([])
const answer = ref('')
const answerFeedback = ref(null)
const rankings = ref([])
const answeredQuestions = ref([])
const questionIndex = ref(0)
const finalized = ref(false)
const latestAnnouncement = ref(null)
const announcementKey = ref(0)

const myScore = computed(() => {
  const myRank = rankings.value.find(r => r.studentId === authStore.user?.studentId)
  return myRank?.score || 0
})

const myRank = computed(() => {
  const idx = rankings.value.findIndex(r => r.studentId === authStore.user?.studentId)
  return idx >= 0 ? idx + 1 : null
})

const totalQuestions = computed(() => selectedQuestions.value.length)

const allAnswered = computed(() =>
  totalQuestions.value > 0 && answeredQuestions.value.length >= totalQuestions.value
)

const statusMessage = computed(() => {
  switch (contestStatus.value) {
    case 'NOT_STARTED': return '比赛将在指定时间开始'
    case 'READY': return '比赛即将开始，请做好准备'
    case 'RUNNING': return '比赛进行中，加油！'
    case 'FINISHED': return '比赛已结束'
    default: return '成绩已公布'
  }
})

const nextEvent = computed(() => {
  if (contestStatus.value === 'NOT_STARTED') return '准备阶段'
  if (contestStatus.value === 'READY') return '比赛开始'
  if (contestStatus.value === 'RUNNING') return '比赛结束'
  return '成绩公布'
})

const getStatusLabel = (status) => {
  const labels = {
    'NOT_STARTED': '未开始',
    'READY': '准备中',
    'RUNNING': '进行中',
    'FINISHED': '已结束',
    'RESULTS_PUBLISHED': '成绩已公布'
  }
  return labels[status] || status
}



const getDifficultyLabel = (difficulty) => {
  const labels = { 'easy': '简单', 'medium': '中等', 'hard': '困难' }
  return labels[difficulty] || difficulty
}

const formatTime = (ms) => {
  if (ms <= 0) return '00:00:00'
  const seconds = Math.floor(ms / 1000)
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const formatSeconds = (seconds) => {
  if (!seconds) return '-'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分`
  return `${minutes}分钟`
}

const loadContestStatus = async () => {
  try {
    const response = await contestAPI.getStatus()
    const status = response.data.data
    contestStatus.value = status.status
    remainingTime.value = status.remainingTime
    canStartContest.value = status.canStartContest
    isContestActive.value = status.contestActive
    scoringAllowed.value = status.scoringAllowed ?? status.contestActive
  } catch (err) {
    console.error('Failed to load contest status:', err)
  }
}

const loadRankings = async () => {
  try {
    const response = await contestAPI.getRankings()
    rankings.value = response.data.data || []
  } catch (err) {
    console.error('Failed to load rankings:', err)
  }
}

const startContest = async () => {
  loading.value = true
  try {
    await contestAPI.startContest()
    contestStarted.value = true
    await loadSelectedQuestions()
    await loadCurrentQuestion()
  } catch (err) {
    console.error('Failed to start contest:', err)
  } finally {
    loading.value = false
  }
}

const loadSelectedQuestions = async () => {
  try {
    const response = await contestAPI.getQuestions()
    selectedQuestions.value = response.data.data.map(q => q.id)
  } catch (err) {
    console.error('Failed to load selected questions:', err)
  }
}

const loadCurrentQuestion = async () => {
  try {
    const response = await contestAPI.getCurrentQuestion()
    currentQuestion.value = response.data.data
  } catch (err) {
    console.error('Failed to load current question:', err)
  }
}

const canUnlockHint = (index) => {
  if (!currentQuestion.value || !currentQuestion.value.hints) {
    return false
  }
  
  if (index === 0) {
    return true
  }
  
  for (let i = 0; i < index; i++) {
    if (!currentQuestion.value.hints[i].unlocked) {
      return false
    }
  }
  return true
}

const unlockHint = async (hint) => {
  if (!hint || !currentQuestion.value) return
  
  loading.value = true
  try {
    const response = await contestAPI.unlockHint(hint.id)
    
    if (response.data.data) {
      const hints = currentQuestion.value.hints
      const idx = hints.findIndex(h => h.id === hint.id)
      if (idx >= 0) {
        hints[idx].unlocked = true
        hints[idx].content = response.data.data.content
      }
      
      await loadRankings()
    }
  } catch (err) {
    console.error('Failed to unlock hint:', err)
    alert('解锁提示失败: ' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  if (!answer.value.trim()) return

  loading.value = true
  answerFeedback.value = null

  try {
    const response = await contestAPI.submitAnswer(currentQuestion.value.id, answer.value)
    const isCorrect = response.data.data

    if (isCorrect) {
      answerFeedback.value = { correct: true, message: '✅ 答案正确！' }
      if (!answeredQuestions.value.includes(currentQuestion.value.id)) {
        answeredQuestions.value.push(currentQuestion.value.id)
      }
      answer.value = ''
      setTimeout(() => nextQuestion(), 1500)
    } else {
      answerFeedback.value = { correct: false, message: '❌ 答案错误，请重试' }
    }

    await loadRankings()
  } catch (err) {
    const backendMsg = err.response?.data?.message
    answerFeedback.value = { correct: false, message: backendMsg ? '❌ ' + backendMsg : '提交失败，请重试' }
    console.error('Failed to submit answer:', err)
  } finally {
    loading.value = false
  }
}

const nextQuestion = async () => {
  if (questionIndex.value < totalQuestions.value - 1) {
    loading.value = true
    try {
      await contestAPI.nextQuestion()
      await loadCurrentQuestion()
      questionIndex.value += 1
      answer.value = ''
      answerFeedback.value = null
    } catch (err) {
      console.error('Failed to move to next question:', err)
    } finally {
      loading.value = false
    }
  }
}

const prevQuestion = async () => {
  if (questionIndex.value > 0) {
    loading.value = true
    try {
      await contestAPI.prevQuestion()
      await loadCurrentQuestion()
      questionIndex.value -= 1
      answer.value = ''
      answerFeedback.value = null
    } catch (err) {
      console.error('Failed to move to previous question:', err)
    } finally {
      loading.value = false
    }
  }
}

const jumpToQuestion = async (questionId) => {
  loading.value = true
  answer.value = ''
  answerFeedback.value = null
  try {
    await contestAPI.jumpToQuestion(questionId)
    await loadCurrentQuestion()
    questionIndex.value = selectedQuestions.value.indexOf(questionId)
  } catch (err) {
    console.error('Failed to jump to question:', err)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const handleFinalize = async () => {
  if (finalized.value) return
  loading.value = true
  try {
    await contestAPI.finalize()
    finalized.value = true
    await loadRankings()
  } catch (err) {
    console.error('Failed to finalize:', err)
  } finally {
    loading.value = false
  }
}

const loadLatestAnnouncement = async () => {
  try {
    const response = await contestAPI.getLatestAnnouncement()
    const newAnnouncement = response.data.data
    
    const oldId = latestAnnouncement.value?.id
    const oldContent = latestAnnouncement.value?.content
    
    if (newAnnouncement && newAnnouncement.id) {
      latestAnnouncement.value = newAnnouncement
      
      if (oldId !== newAnnouncement.id || oldContent !== newAnnouncement.content) {
        announcementKey.value++
        console.log('公告已更新:', newAnnouncement.content)
      }
    } else {
      if (latestAnnouncement.value !== null) {
        latestAnnouncement.value = null
        announcementKey.value++
        console.log('公告已撤回')
      }
    }
  } catch (err) {
    console.error('Failed to load announcement:', err)
  }
}

let timer = null
let statusTimer = null
let announcementTimer = null

onMounted(async () => {
  await loadContestStatus()
  await loadRankings()
  await loadLatestAnnouncement()

  // 本地倒计时每秒递减
  timer = setInterval(() => {
    remainingTime.value = Math.max(0, remainingTime.value - 1000)
  }, 1000)

  // 每10秒从服务器刷新一次状态（确保状态切换及时反映）
  statusTimer = setInterval(() => {
    loadContestStatus()
  }, 10000)

  // 每30秒刷新一次排名
  setInterval(() => {
    loadRankings()
  }, 30000)

  // 每10秒轮询一次最新公告
  announcementTimer = setInterval(() => {
    loadLatestAnnouncement()
  }, 10000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (statusTimer) clearInterval(statusTimer)
  if (announcementTimer) clearInterval(announcementTimer)
})
</script>
