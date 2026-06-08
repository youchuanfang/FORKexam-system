<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>阅卷评分</h1>
        <p>学生：{{ detail.studentName }} | 试卷：{{ detail.paperTitle }}</p>
      </div>
      <div class="header-actions">
        <button class="secondary-btn" type="button" @click="$router.back()">返回</button>
        <button class="secondary-btn" type="button" @click="loadDetail">刷新</button>
      </div>
    </header>

    <section class="panel">
      <div class="record-summary">
        <div class="summary-item">
          <span class="summary-label">开始时间</span>
          <span>{{ formatTime(detail.startTime) }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">提交时间</span>
          <span>{{ detail.submitted ? formatTime(detail.submitTime) : '未提交' }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">总分</span>
          <span class="total-score">{{ detail.totalScore != null ? detail.totalScore + ' 分' : '-' }}</span>
        </div>
      </div>
    </section>

    <section class="panel" style="margin-top:20px">
      <h3>答题详情</h3>
      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="detail.answers && detail.answers.length === 0" class="state-text">无答题记录。</p>

      <div v-else class="answers-list">
        <div v-for="(item, idx) in detail.answers" :key="item.questionId" class="answer-card">
          <div class="answer-header">
            <span class="answer-num">第 {{ idx + 1 }} 题</span>
            <span class="answer-type">{{ typeLabel(item.type) }}</span>
            <span class="answer-score">满分 {{ item.score }} 分</span>
            <span v-if="item.autoGraded" class="auto-badge">自动判分</span>
            <span v-else class="manual-badge">待批改</span>
          </div>

          <div class="answer-body">
            <div class="answer-section">
              <strong>题目：</strong>
              <p>{{ item.content }}</p>
            </div>
            <div v-if="item.options" class="answer-section">
              <strong>选项：</strong>
              <p>{{ item.options }}</p>
            </div>
            <div class="answer-section">
              <strong>学生答案：</strong>
              <p :class="{ 'no-answer': !item.studentAnswer }">{{ item.studentAnswer || '（未作答）' }}</p>
            </div>
            <div class="answer-section">
              <strong>正确答案：</strong>
              <p>{{ item.correctAnswer || '（无标准答案）' }}</p>
            </div>
            <div class="answer-section">
              <strong>当前得分：</strong>
              <span class="score-val">{{ item.scoreGot != null ? item.scoreGot + ' / ' + item.score : '未评分' }}</span>
            </div>
          </div>

          <!-- 简答题批改 -->
          <div v-if="!item.autoGraded" class="grade-row">
            <label>给分：</label>
            <input
              v-model.number="gradeInputs[item.questionId]"
              type="number"
              :min="0"
              :max="item.score"
              step="0.5"
              placeholder="0"
              style="width:80px"
            />
            <span class="grade-hint">（0 ~ {{ item.score }} 分）</span>
            <button
              class="primary-btn grade-btn"
              type="button"
              :disabled="gradingId === item.questionId"
              @click="handleGrade(item)"
            >{{ gradingId === item.questionId ? '保存中...' : '保存得分' }}</button>
            <span v-if="gradeMsg[item.questionId]" :class="gradeOk[item.questionId] ? 'state-text' : 'error-text'" style="margin-left:8px">
              {{ gradeMsg[item.questionId] }}
            </span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamRecordDetail, gradeQuestion } from '../../api/teacher'

const route = useRoute()
const router = useRouter()
const recordId = Number(route.params.recordId)

const detail = ref({})
const loading = ref(false)
const error = ref('')
const gradingId = ref(null)
const gradeInputs = reactive({})
const gradeMsg = reactive({})
const gradeOk = reactive({})

const typeLabels = {
  single_choice: '单选题',
  multi_choice: '多选题',
  true_false: '判断题',
  fill_blank: '填空题',
  short_answer: '简答题'
}

function typeLabel(type) {
  return typeLabels[type] || type
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const res = await getExamRecordDetail(recordId)
    if (res.code === 200) {
      detail.value = res.data || {}
      if (detail.value.answers) {
        for (const item of detail.value.answers) {
          if (!item.autoGraded && gradeInputs[item.questionId] === undefined) {
            gradeInputs[item.questionId] = item.scoreGot != null ? item.scoreGot : 0
          }
        }
      }
    } else {
      error.value = res.message || '获取详情失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取详情失败'
  } finally {
    loading.value = false
  }
}

async function handleGrade(item) {
  const scoreGot = Number(gradeInputs[item.questionId])
  if (isNaN(scoreGot) || scoreGot < 0 || scoreGot > item.score) {
    gradeMsg[item.questionId] = `得分必须在 0 ~ ${item.score} 之间`
    gradeOk[item.questionId] = false
    return
  }
  gradingId.value = item.questionId
  gradeMsg[item.questionId] = ''
  try {
    const res = await gradeQuestion(recordId, { questionId: item.questionId, scoreGot })
    if (res.code === 200) {
      gradeMsg[item.questionId] = '保存成功'
      gradeOk[item.questionId] = true
      // Reload to get updated total score
      await loadDetail()
    } else {
      gradeMsg[item.questionId] = res.message || '保存失败'
      gradeOk[item.questionId] = false
    }
  } catch (err) {
    gradeMsg[item.questionId] = err.response?.data?.message || err.message || '保存失败'
    gradeOk[item.questionId] = false
  } finally {
    gradingId.value = null
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.teacher-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 32px;
}

.page-header,
.panel {
  max-width: 1080px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 24px;
}

h1, h3, p { margin: 0; }

h1 { color: #1f2937; font-size: 28px; margin-bottom: 8px; }

.page-header p { color: #6b7280; }

.header-actions { display: flex; gap: 10px; }

.panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.panel h3 { color: #1f2937; font-size: 18px; margin-bottom: 18px; }

.record-summary {
  display: flex; gap: 32px; flex-wrap: wrap;
}

.summary-item { display: flex; flex-direction: column; gap: 4px; }

.summary-label { font-size: 12px; color: #6b7280; }

.total-score { color: #2563eb; font-weight: 700; font-size: 20px; }

.answers-list { display: grid; gap: 16px; }

.answer-card {
  border: 1px solid #e5e7eb; border-radius: 8px;
  padding: 18px;
}

.answer-header {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 14px; flex-wrap: wrap;
}

.answer-num { font-weight: 600; color: #1f2937; }

.answer-type {
  background: #f3f4f6; color: #4b5563; font-size: 12px;
  padding: 2px 8px; border-radius: 4px;
}

.answer-score { font-size: 13px; color: #6b7280; }

.auto-badge {
  background: #d1fae5; color: #065f46; font-size: 11px;
  padding: 2px 8px; border-radius: 4px;
}

.manual-badge {
  background: #fef3c7; color: #92400e; font-size: 11px;
  padding: 2px 8px; border-radius: 4px;
}

.answer-body { display: grid; gap: 10px; font-size: 14px; }

.answer-section strong { color: #374151; }

.answer-section p { color: #4b5563; margin-top: 4px; line-height: 1.5; }

.no-answer { color: #9ca3af; font-style: italic; }

.score-val { color: #2563eb; font-weight: 600; }

.grade-row {
  display: flex; align-items: center; gap: 10px;
  margin-top: 14px; padding-top: 14px;
  border-top: 1px dashed #e5e7eb;
}

.grade-row label { font-size: 13px; color: #374151; font-weight: 500; }

.grade-row input {
  border: 1px solid #d1d5db; border-radius: 4px;
  padding: 6px 8px; font-size: 14px; text-align: center;
}

.grade-hint { font-size: 12px; color: #9ca3af; }

.grade-btn { padding: 8px 14px !important; font-size: 13px !important; }

.primary-btn, .secondary-btn {
  border: 0; cursor: pointer; text-decoration: none;
  font-size: 14px; border-radius: 6px; white-space: nowrap;
}

.primary-btn {
  background: #2563eb; color: #fff; padding: 10px 16px;
}

.primary-btn:disabled { cursor: not-allowed; opacity: 0.65; }

.secondary-btn {
  background: #fff; color: #374151;
  border: 1px solid #d1d5db; padding: 9px 14px;
}

.error-text { color: #dc2626; }

.state-text { color: #6b7280; }

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .page-header { flex-direction: column; }
  .record-summary { flex-direction: column; gap: 12px; }
  .grade-row { flex-wrap: wrap; }
}
</style>
