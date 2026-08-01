import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'

export const useContestStore = defineStore('contest', () => {
  const status = ref('NOT_STARTED')
  const remainingTime = ref(0)
  const canStart = ref(false)
  const isActive = ref(false)
  const currentQuestion = reactive({})
  const selectedQuestions = ref([])
  const rankings = ref([])

  const setContestStatus = (newStatus) => {
    status.value = newStatus.status
    remainingTime.value = newStatus.remainingTime
    canStart.value = newStatus.canStartContest
    isActive.value = newStatus.contestActive
  }

  const setCurrentQuestion = (question) => {
    Object.assign(currentQuestion, question)
  }

  const setSelectedQuestions = (questions) => {
    selectedQuestions.value = questions
  }

  const setRankings = (newRankings) => {
    rankings.value = newRankings
  }

  return {
    status,
    remainingTime,
    canStart,
    isActive,
    currentQuestion,
    selectedQuestions,
    rankings,
    setContestStatus,
    setCurrentQuestion,
    setSelectedQuestions,
    setRankings,
  }
})
