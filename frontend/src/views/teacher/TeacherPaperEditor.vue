<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>{{ isNew ? '新建试卷' : '编辑试卷' }}</h1>
        <p>设置试卷基本信息、从题库选题组卷。</p>
      </div>
      <div class="header-actions">
        <router-link class="secondary-btn" to="/teacher/papers">返回列表</router-link>
      </div>
    </header>

    <section class="panel">
      <h3>试卷信息</h3>
      <p v-if="loadError" class="error-text">{{ loadError }}</p>

      <div class="form-row">
        <div class="form-group flex-1">
          <label>试卷标题 *</label>
          <input v-model="form.title" placeholder="请输入试卷标题" />
        </div>
        <div class="form-group" style="width:140px">
          <label>考试时长（分钟）</label>
          <input v-model.number="form.duration" type="number" min="1" placeholder="分钟" />
        </div>
        <div class="form-group" style="width:140px">
          <label>最大作答次数</label>
          <input v-model.number="form.maxAttempts" type="number" min="1" placeholder="默认1" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-group flex-1">
          <label>开放开始时间（可选）</label>
          <input v-model="form.openStartTime" type="datetime-local" />
        </div>
        <div class="form-group flex-1">
          <label>开放结束时间（可选）</label>
          <input v-model="form.openEndTime" type="datetime-local" />
        </div>
      </div>

      <div class="form-row">
        <div class="form-group flex-1">
          <label class="checkbox-label">
            <input v-model="form.releaseAnswerFlag" type="checkbox" />
            允许学生查看答案
          </label>
        </div>
        <div class="form-group flex-1" v-if="form.releaseAnswerFlag">
          <label>答案公布时间（可选，留空则提交后可查看）</label>
          <input v-model="form.answerReleaseTime" type="datetime-local" />
        </div>
      </div>

      <div class="form-actions">
        <button class="primary-btn" type="button" :disabled="saving" @click="savePaper">{{ saving ? '保存中...' : '保存试卷信息' }}</button>
        <p v-if="saveMsg" :class="saveError ? 'error-text' : 'state-text'" style="margin-left:12px">{{ saveMsg }}</p>
      </div>
    </section>

    <!-- 组题区域：仅当 paperId 存在时显示 -->
    <section v-if="paperId" class="panel" style="margin-top:20px">
      <div class="panel-title">
        <h3>试卷题目（{{ assignedQuestions.length }} 题）</h3>
        <button class="secondary-btn" type="button" @click="showPicker = !showPicker">
          {{ showPicker ? '收起题库' : '从题库选题' }}
        </button>
      </div>

      <!-- 已选题目列表 -->
      <div v-if="assignedQuestions.length > 0" class="assigned-list">
        <div v-for="(aq, idx) in assignedQuestions" :key="aq.questionId" class="assigned-item">
          <span class="aq-order">{{ idx + 1 }}</span>
          <div class="aq-content">
            <span class="aq-type">{{ typeLabel(aq.question?.type) }}</span>
            <span>{{ truncate(aq.question?.content || '', 50) }}</span>
          </div>
          <div class="aq-score">
            <label>分值</label>
            <input v-model.number="aq.score" type="number" min="0" step="0.5" style="width:70px" />
          </div>
          <button class="text-btn danger" type="button" @click="removeQuestion(idx)">移除</button>
        </div>
      </div>
      <p v-else class="state-text">暂未选题，请点击"从题库选题"添加。</p>

      <!-- 题库挑选面板 -->
      <div v-if="showPicker" class="picker-panel">
        <div class="filter-bar">
          <input v-model="searchKeyword" placeholder="搜索题目..." class="search-input" />
          <button
            v-for="item in types"
            :key="item.key"
            :class="['filter-tag', { active: pickerType === item.key }]"
            type="button"
            @click="pickerType = item.key"
          >{{ item.label }}</button>
        </div>
        <p v-if="pickerLoading" class="state-text">加载中...</p>
        <p v-else-if="filteredPool.length === 0" class="state-text">无匹配题目。</p>
        <div v-else class="picker-list">
          <div
            v-for="q in filteredPool"
            :key="q.id"
            :class="['picker-item', { selected: isSelected(q.id) }]"
            @click="toggleSelect(q)"
          >
            <span class="picker-type">{{ typeLabel(q.type) }}</span>
            <span class="picker-content">{{ truncate(q.content, 60) }}</span>
            <span v-if="isSelected(q.id)" class="picker-check">✓</span>
          </div>
        </div>
        <div class="picker-actions">
          <button class="primary-btn" type="button" :disabled="savingQuestions" @click="saveQuestions">
            {{ savingQuestions ? '保存中...' : `保存选题（${selectedIds.size} 题）` }}
          </button>
          <p v-if="qError" class="error-text" style="margin-left:12px">{{ qError }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPaper, createPaper, updatePaper, assignQuestions, getQuestions } from '../../api/teacher'

const route = useRoute()
const router = useRouter()
const isNew = !route.params.paperId
const paperId = ref(isNew ? null : Number(route.params.paperId))
const loadError = ref('')
const saving = ref(false)
const saveMsg = ref('')
const saveError = ref(false)

const form = ref({
  title: '',
  duration: null,
  maxAttempts: 1,
  openStartTime: '',
  openEndTime: '',
  releaseAnswerFlag: false,
  answerReleaseTime: ''
})

const types = [
  { key: '', label: '全部' },
  { key: 'single_choice', label: '单选' },
  { key: 'multi_choice', label: '多选' },
  { key: 'true_false', label: '判断' },
  { key: 'fill_blank', label: '填空' },
  { key: 'short_answer', label: '简答' }
]

function typeLabel(type) {
  return types.find(t => t.key === type)?.label || type
}

function truncate(str, n) {
  if (!str) return ''
  return str.length > n ? str.slice(0, n) + '...' : str
}

function toLocalDatetime(val) {
  if (!val) return ''
  const s = String(val).replace('T', ' ').slice(0, 16)
  return s ? s.replace(' ', 'T') : ''
}

// 题库
const poolQuestions = ref([])
const pickerLoading = ref(false)
const pickerType = ref('')
const searchKeyword = ref('')
const showPicker = ref(false)
const assignedQuestions = ref([])
const selectedIds = computed(() => new Set(assignedQuestions.value.map(a => a.questionId)))
const savingQuestions = ref(false)
const qError = ref('')

const filteredPool = computed(() => {
  let list = poolQuestions.value
  if (pickerType.value) {
    list = list.filter(q => q.type === pickerType.value)
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(q => q.content && q.content.toLowerCase().includes(kw))
  }
  // 排除已选
  const ids = selectedIds.value
  return list.filter(q => !ids.has(q.id))
})

function isSelected(qId) { return selectedIds.value.has(qId) }

function toggleSelect(q) {
  if (isSelected(q.id)) return
  assignedQuestions.value.push({ questionId: q.id, score: 0, question: q })
}

function removeQuestion(idx) {
  assignedQuestions.value.splice(idx, 1)
}

async function loadPool() {
  pickerLoading.value = true
  try {
    const res = await getQuestions(pickerType.value ? { type: pickerType.value } : {})
    if (res.code === 200) {
      poolQuestions.value = res.data || []
    }
  } catch (err) {
    // ignore
  } finally {
    pickerLoading.value = false
  }
}

watch(pickerType, () => { loadPool() })

async function loadExistingQuestions(pid) {
  try {
    const res = await getPaper(pid)
    if (res.code === 200 && res.data) {
      // We need to fetch full question info. For simplicity, just show existing data.
    }
  } catch {
    // ignore
  }
}

async function loadPaper() {
  if (isNew) return
  loadError.value = ''
  try {
    const res = await getPaper(paperId.value)
    if (res.code === 200 && res.data) {
      const p = res.data
      form.value.title = p.title || ''
      form.value.duration = p.duration
      form.value.maxAttempts = p.maxAttempts ?? 1
      form.value.openStartTime = toLocalDatetime(p.openStartTime)
      form.value.openEndTime = toLocalDatetime(p.openEndTime)
      form.value.releaseAnswerFlag = p.releaseAnswerFlag || false
      form.value.answerReleaseTime = toLocalDatetime(p.answerReleaseTime)
    } else {
      loadError.value = res.message || '加载失败'
    }
  } catch (err) {
    loadError.value = err.response?.data?.message || err.message || '加载失败'
  }
}

async function savePaper() {
  if (!form.value.title.trim()) {
    saveError.value = true
    saveMsg.value = '试卷标题不能为空'
    return
  }
  saving.value = true
  saveError.value = false
  saveMsg.value = ''
  try {
    const data = {
      title: form.value.title.trim(),
      duration: form.value.duration ? Number(form.value.duration) : null,
      maxAttempts: form.value.maxAttempts ? Number(form.value.maxAttempts) : 1,
      openStartTime: form.value.openStartTime || null,
      openEndTime: form.value.openEndTime || null,
      releaseAnswerFlag: form.value.releaseAnswerFlag,
      answerReleaseTime: form.value.answerReleaseTime || null
    }
    let res
    if (isNew) {
      res = await createPaper(data)
    } else {
      res = await updatePaper(paperId.value, data)
    }
    if (res.code === 200) {
      saveMsg.value = '保存成功'
      if (isNew && res.data && res.data.id) {
        paperId.value = res.data.id
        router.replace(`/teacher/papers/${res.data.id}/edit`)
      }
    } else {
      saveError.value = true
      saveMsg.value = res.message || '保存失败'
    }
  } catch (err) {
    saveError.value = true
    saveMsg.value = err.response?.data?.message || err.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function saveQuestions() {
  if (assignedQuestions.value.length === 0) {
    qError.value = '请至少选择一道题目'
    return
  }
  savingQuestions.value = true
  qError.value = ''
  try {
    const data = assignedQuestions.value.map(a => ({ questionId: a.questionId, score: a.score || 0 }))
    const res = await assignQuestions(paperId.value, data)
    if (res.code === 200) {
      qError.value = ''
      alert('选题保存成功')
    } else {
      qError.value = res.message || '保存失败'
    }
  } catch (err) {
    qError.value = err.response?.data?.message || err.message || '保存失败'
  } finally {
    savingQuestions.value = false
  }
}

onMounted(async () => {
  await loadPaper()
  loadPool()
})
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

.panel-title {
  display: flex; justify-content: space-between;
  align-items: center; margin-bottom: 14px;
}

.form-row {
  display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 12px;
}

.form-group { display: flex; flex-direction: column; gap: 4px; }

.form-group.flex-1 { flex: 1; min-width: 180px; }

.form-group label {
  font-size: 13px; color: #374151; font-weight: 500;
}

.form-group input, .form-group select, .form-group textarea {
  border: 1px solid #d1d5db; border-radius: 6px;
  padding: 8px 12px; font-size: 14px; font-family: inherit;
}

.form-group input:focus { outline: none; border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.1); }

.checkbox-label {
  display: flex; align-items: center; gap: 8px; cursor: pointer;
  font-size: 14px; margin-top: 22px;
}

.checkbox-label input[type="checkbox"] { width: 16px; height: 16px; }

.form-actions {
  display: flex; align-items: center; margin-top: 16px;
}

.assigned-list { display: grid; gap: 8px; margin-bottom: 16px; }

.assigned-item {
  display: flex; align-items: center; gap: 10px;
  border: 1px solid #e5e7eb; border-radius: 6px; padding: 10px 14px;
}

.aq-order {
  width: 28px; height: 28px; background: #eff6ff; color: #2563eb;
  border-radius: 50%; display: flex; align-items: center;
  justify-content: center; font-size: 13px; font-weight: 600; flex-shrink: 0;
}

.aq-content { flex: 1; display: flex; gap: 8px; align-items: center; font-size: 13px; }

.aq-type {
  background: #dbeafe; color: #1e40af; font-size: 11px;
  padding: 2px 6px; border-radius: 4px; flex-shrink: 0;
}

.aq-score { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

.aq-score label { font-size: 12px; color: #6b7280; }

.aq-score input {
  border: 1px solid #d1d5db; border-radius: 4px;
  padding: 4px 6px; font-size: 13px; text-align: center;
}

.picker-panel {
  border-top: 1px solid #e5e7eb; padding-top: 16px; margin-top: 16px;
}

.filter-bar {
  display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px;
}

.search-input {
  border: 1px solid #d1d5db; border-radius: 6px;
  padding: 6px 12px; font-size: 13px; width: 200px;
}

.filter-tag {
  border: 1px solid #d1d5db; background: #fff; color: #374151;
  cursor: pointer; border-radius: 6px; font-size: 12px; padding: 4px 10px;
}

.filter-tag.active { background: #eff6ff; border-color: #2563eb; color: #1d4ed8; }

.picker-list { max-height: 300px; overflow-y: auto; border: 1px solid #e5e7eb; border-radius: 6px; }

.picker-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; cursor: pointer; border-bottom: 1px solid #f3f4f6;
  font-size: 13px;
}

.picker-item:hover { background: #f9fafb; }

.picker-item.selected { background: #eff6ff; }

.picker-type {
  background: #f3f4f6; color: #4b5563; font-size: 11px;
  padding: 2px 6px; border-radius: 4px; flex-shrink: 0;
}

.picker-content { flex: 1; }

.picker-check { color: #2563eb; font-weight: bold; flex-shrink: 0; }

.picker-actions {
  display: flex; align-items: center; margin-top: 12px;
}

.primary-btn, .secondary-btn, .text-btn {
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
  display: inline-block;
}

.text-btn {
  background: transparent; color: #2563eb; padding: 4px 0; font-size: 13px;
}

.text-btn.danger { color: #dc2626; }

.error-text { color: #dc2626; }

.state-text { color: #6b7280; }

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .form-row { flex-direction: column; }
  .form-group.flex-1 { min-width: auto; }
}
</style>
