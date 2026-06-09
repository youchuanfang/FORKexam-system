<template>
  <div class="student-page">
    <section class="panel">
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>

      <template v-else-if="paper">
        <header class="exam-header">
          <div>
            <h1>{{ paper.title || '在线考试' }}</h1>
            <p>考试时长：{{ paper.duration || 0 }} 分钟</p>
          </div>
          <div class="exam-tools">
            <strong :class="['timer', { danger: remainingSeconds <= 60 }]">剩余时间：{{ formattedRemaining }}</strong>
            <button class="secondary-btn" type="button" @click="router.push('/student')">返回首页</button>
          </div>
          <div v-if="showTabWarning" class="tab-warning">⚠️ 你已多次切换离开考试页面（{{ tabSwitchCount }} 次），请专注作答</div>
        </header>

        <form class="question-list" @submit.prevent="handleSubmit(false)">
          <article v-for="(question, index) in paper.questions" :key="question.questionId" class="question-card">
            <div class="question-head">
              <strong>第 {{ index + 1 }} 题</strong>
              <span>{{ typeLabel(question.type) }} · {{ question.score || 0 }} 分</span>
            </div>
            <p class="question-content">{{ question.content }}</p>

            <div v-if="question.type === 'single_choice'" class="answer-area">
              <label v-for="option in parseOptions(question.options)" :key="option" class="choice-line">
                <input v-model="answers[question.questionId]" type="radio" :name="`q-${question.questionId}`" :value="optionValue(option)" />
                <span>{{ option }}</span>
              </label>
              <input v-if="parseOptions(question.options).length === 0" v-model="answers[question.questionId]" class="text-input" placeholder="请输入答案" />
            </div>

            <div v-else-if="question.type === 'multi_choice'" class="answer-area">
              <label v-for="option in parseOptions(question.options)" :key="option" class="choice-line">
                <input
                  :checked="selectedMulti(question.questionId).includes(optionValue(option))"
                  type="checkbox"
                  @change="toggleMulti(question.questionId, optionValue(option))"
                />
                <span>{{ option }}</span>
              </label>
              <input v-if="parseOptions(question.options).length === 0" v-model="answers[question.questionId]" class="text-input" placeholder="多个答案请用逗号分隔" />
            </div>

            <div v-else-if="question.type === 'true_false'" class="answer-area">
              <label class="choice-line">
                <input v-model="answers[question.questionId]" type="radio" :name="`q-${question.questionId}`" value="正确" />
                <span>正确</span>
              </label>
              <label class="choice-line">
                <input v-model="answers[question.questionId]" type="radio" :name="`q-${question.questionId}`" value="错误" />
                <span>错误</span>
              </label>
            </div>

            <input v-else-if="question.type === 'fill_blank'" v-model="answers[question.questionId]" class="text-input" placeholder="请输入答案" />
            <textarea v-else v-model="answers[question.questionId]" class="text-area" rows="5" placeholder="请输入答案"></textarea>
          </article>

          <div class="submit-row">
            <button class="primary-btn" type="submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交答案' }}
            </button>
          </div>
        </form>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { getExamRecordDetail, submitExam } from '../../api/student'

const route = useRoute()
const router = useRouter()
const paper = ref(null)
const loading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const error = ref('')
const answers = reactive({})
const remainingSeconds = ref(0)
let timerId = null
let autoSaveId = null
const tabSwitchCount = ref(0)
const showTabWarning = ref(false)

const labels = {
  single_choice: '单选题',
  multi_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  short_answer: '简答题'
}

const formattedRemaining = computed(() => {
  const seconds = Math.max(0, remainingSeconds.value)
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
})

function typeLabel(type) {
  return labels[type] || type || '题目'
}

function parseOptions(options) {
  if (!options) return []
  if (Array.isArray(options)) {
    return options.map(item => String(item).trim()).filter(Boolean)
  }
  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      if (Array.isArray(parsed)) {
        return parsed.map(item => String(item).trim()).filter(Boolean)
      }
    } catch (e) {
      return splitOptionText(options)
    }
    return splitOptionText(options)
  }
  return []
}

function splitOptionText(options) {
  const text = String(options).trim()
  if (!text) return []
  const lines = text.split(/\r?\n/).map(item => item.trim()).filter(Boolean)
  if (lines.length > 1) return lines
  const compactOptions = text.match(/[A-Za-z][.、)]\s*.*?(?=\s+[A-Za-z][.、)]\s*|$)/g)
  if (compactOptions && compactOptions.length > 1) {
    return compactOptions.map(item => item.trim())
  }
  return text.split(/[,，;；]/).map(item => item.trim()).filter(Boolean)
}

function optionValue(option) {
  const match = String(option).trim().match(/^([A-Za-z])[.、)]/)
  return match ? match[1].toUpperCase() : option
}

function selectedMulti(questionId) {
  const value = answers[questionId]
  return value ? value.split(',').filter(Boolean) : []
}

function toggleMulti(questionId, optionValueText) {
  const values = new Set(selectedMulti(questionId))
  if (values.has(optionValueText)) {
    values.delete(optionValueText)
  } else {
    values.add(optionValueText)
  }
  answers[questionId] = Array.from(values).join(',')
}

function storageKey() {
  return `student_exam_${route.params.recordId}`
}

function applyExamData(data) {
  if (!data) return
  paper.value = {
    paperId: data.paperId,
    title: data.title || data.paperTitle,
    duration: data.duration,
    openStartTime: data.openStartTime,
    openEndTime: data.openEndTime,
    startTime: data.startTime,
    questions: data.questions || []
  }
  setupTimer(data.startTime, data.duration, data.openEndTime)
}

function setupTimer(startTime, duration, openEndTime) {
  clearTimer()
  const durationMinutes = Number(duration || 0)
  const start = startTime ? new Date(startTime).getTime() : Date.now()
  const durationEnd = durationMinutes > 0 ? start + durationMinutes * 60 * 1000 : null
  const paperEnd = openEndTime ? new Date(openEndTime).getTime() : null
  const endCandidates = [durationEnd, paperEnd].filter(value => Number.isFinite(value))
  if (endCandidates.length === 0) {
    remainingSeconds.value = 0
    return
  }
  const end = Math.min(...endCandidates)
  const tick = () => {
    remainingSeconds.value = Math.max(0, Math.ceil((end - Date.now()) / 1000))
    if (remainingSeconds.value <= 0) {
      clearTimer()
      handleSubmit(true)
    }
  }
  tick()
  timerId = window.setInterval(tick, 1000)
}

function clearTimer() {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = null
  }
  if (autoSaveId) {
    window.clearInterval(autoSaveId)
    autoSaveId = null
  }
}

function autoSaveAnswers() {
  const key = storageKey() + '_autosave'
  const data = {}
  for (const qid of Object.keys(answers)) {
    if (answers[qid] !== undefined && answers[qid] !== '') {
      data[qid] = answers[qid]
    }
  }
  if (Object.keys(data).length > 0) {
    sessionStorage.setItem(key, JSON.stringify(data))
  }
}

function restoreAutoSave() {
  const key = storageKey() + '_autosave'
  const saved = sessionStorage.getItem(key)
  if (saved) {
    try {
      const data = JSON.parse(saved)
      for (const [qid, val] of Object.entries(data)) {
        if (!answers[qid] || answers[qid] === '') {
          answers[qid] = val
        }
      }
    } catch { /* ignore */ }
  }
}

function handleVisibilityChange() {
  if (document.hidden && !submitted.value) {
    tabSwitchCount.value++
    if (tabSwitchCount.value > 3) {
      showTabWarning.value = true
    }
  }
}

async function loadExam() {
  loading.value = true
  error.value = ''
  try {
    const cached = sessionStorage.getItem(storageKey())
    if (cached) {
      applyExamData(JSON.parse(cached))
    }

    const res = await getExamRecordDetail(route.params.recordId)
    if (res.code === 200) {
      if (res.data?.submitTime) {
        router.replace(`/student/records/${route.params.recordId}`)
        return
      }
      applyExamData(res.data)
      if (!paper.value?.questions?.length) {
        error.value = '未获取到考试题目，请返回考试列表重新开始'
      }
    } else {
      error.value = res.message || '获取考试信息失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取考试信息失败'
  } finally {
    loading.value = false
  }
}

async function handleSubmit(autoSubmit) {
  if (submitting.value || submitted.value) return
  if (!autoSubmit && !window.confirm('确认提交答案？提交后不能再次修改。')) {
    return
  }

  submitting.value = true
  error.value = ''
  try {
    const payload = (paper.value?.questions || []).map(question => ({
      questionId: question.questionId,
      answer: answers[question.questionId] || ''
    }))
    const res = await submitExam(route.params.recordId, payload)
    if (res.code === 200) {
      submitted.value = true
      clearTimer()
      sessionStorage.removeItem(storageKey())
      if (!autoSubmit) {
        window.alert(`提交成功，当前得分：${res.data?.totalScore ?? 0}`)
      }
      router.push(`/student/records/${route.params.recordId}`)
    } else {
      error.value = res.message || '提交失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '提交失败'
  } finally {
    submitting.value = false
  }
}

function beforeUnload(event) {
  if (!submitted.value && paper.value?.questions?.length) {
    event.preventDefault()
    event.returnValue = ''
  }
}

onBeforeRouteLeave(() => {
  if (!submitted.value && paper.value?.questions?.length && !submitting.value) {
    return window.confirm('考试尚未提交，确认离开答题页面？')
  }
  return true
})

onMounted(() => {
  window.addEventListener('beforeunload', beforeUnload)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  loadExam().then(() => {
    restoreAutoSave()
    autoSaveId = window.setInterval(autoSaveAnswers, 30000)
  })
})

onBeforeUnmount(() => {
  clearTimer()
  window.removeEventListener('beforeunload', beforeUnload)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped>
.student-page { min-height: 100vh; background: #f5f7fb; padding: 32px; }
.panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; margin: 0 auto; max-width: 980px; padding: 24px; }
.exam-header { display: flex; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.exam-tools { align-items: flex-end; display: flex; flex-direction: column; gap: 10px; }
h1, p { margin: 0; }
h1 { color: #111827; font-size: 26px; margin-bottom: 8px; }
.exam-header p, .state-text { color: #6b7280; }
.timer { color: #1f2937; font-size: 18px; }
.timer.danger { color: #dc2626; }
.tab-warning {
  background: #fef3c7; color: #92400e; padding: 10px 16px;
  border-radius: 6px; margin-bottom: 14px; font-size: 14px;
  border: 1px solid #fcd34d;
}
.question-list { display: grid; gap: 14px; }
.question-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
.question-head { display: flex; justify-content: space-between; color: #374151; margin-bottom: 10px; }
.question-content { color: #111827; line-height: 1.6; margin-bottom: 12px; }
.answer-area { display: grid; gap: 8px; }
.choice-line { align-items: center; color: #374151; display: flex; gap: 8px; line-height: 1.5; }
.text-input, .text-area { border: 1px solid #d1d5db; border-radius: 6px; box-sizing: border-box; font-size: 14px; padding: 10px 12px; width: 100%; }
.text-area { resize: vertical; }
.submit-row { display: flex; justify-content: flex-end; }
.primary-btn, .secondary-btn { border-radius: 6px; cursor: pointer; font-size: 14px; text-decoration: none; white-space: nowrap; }
.primary-btn { background: #2563eb; border: 0; color: #fff; padding: 10px 18px; }
.primary-btn:disabled { cursor: not-allowed; opacity: 0.7; }
.secondary-btn { background: #fff; border: 1px solid #d1d5db; color: #374151; padding: 9px 14px; }
.error-text { color: #dc2626; }
@media (max-width: 720px) {
  .student-page { padding: 20px; }
  .exam-header, .question-head { flex-direction: column; }
  .exam-tools { align-items: stretch; }
}
</style>
