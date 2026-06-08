<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>题库管理</h1>
        <p>管理题目，支持单选、多选、判断、填空、简答五种题型。</p>
      </div>
      <div class="header-actions">
        <button class="primary-btn" type="button" @click="openAddDialog">新增题目</button>
        <button class="secondary-btn" type="button" @click="openBatchDialog">批量导入</button>
        <button class="secondary-btn" type="button" @click="loadQuestions">刷新</button>
      </div>
    </header>

    <section class="panel">
      <div class="filter-bar">
        <button
          v-for="item in types"
          :key="item.key"
          :class="['filter-tag', { active: activeType === item.key }]"
          type="button"
          @click="activeType = item.key; loadQuestions()"
        >{{ item.label }}</button>
      </div>

      <p v-if="loading" class="state-text">加载中...</p>
      <p v-else-if="error" class="error-text">{{ error }}</p>
      <p v-else-if="questions.length === 0" class="state-text">暂无题目。</p>

      <table v-else class="data-table">
        <thead>
          <tr>
            <th style="width:60px">ID</th>
            <th style="width:80px">题型</th>
            <th>题目内容</th>
            <th style="width:140px">答案/选项</th>
            <th style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in questions" :key="q.id">
            <td>{{ q.id }}</td>
            <td>{{ typeLabel(q.type) }}</td>
            <td class="content-cell">{{ q.content }}</td>
            <td class="answer-summary">
              <span v-if="q.options" class="options-hint">有选项</span>
              <span v-if="q.answer" class="answer-hint">{{ truncate(q.answer, 20) }}</span>
            </td>
            <td>
              <div class="row-actions">
                <button class="text-btn" type="button" @click="openEditDialog(q)">编辑</button>
                <button class="text-btn danger" type="button" @click="handleDelete(q)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- 批量导入对话框 -->
    <div v-if="batchDialogOpen" class="dialog-overlay" @click.self="batchDialogOpen = false">
      <div class="dialog-box">
        <h3>批量导入题目</h3>
        <p class="state-text" style="margin-bottom:12px">请粘贴 JSON 数组格式的题目数据，或上传 .json 文件</p>
        <div class="form-group">
          <label>上传 JSON 文件</label>
          <input type="file" accept=".json" @change="handleFileUpload" />
        </div>
        <div class="form-group">
          <label>或手动粘贴 JSON</label>
          <textarea v-model="batchJson" rows="12" placeholder='[{"type":"single_choice","content":"题目内容","options":"[\"A. 选项1\", \"B. 选项2\"]","answer":"A","courseId":1}]'></textarea>
        </div>
        <p v-if="batchError" class="error-text">{{ batchError }}</p>
        <p v-if="batchSuccess" class="state-text">{{ batchSuccess }}</p>
        <div class="dialog-actions">
          <button class="secondary-btn" type="button" @click="batchDialogOpen = false">取消</button>
          <button class="primary-btn" type="button" :disabled="batchSubmitting" @click="handleBatchImport">
            {{ batchSubmitting ? '导入中...' : '导入' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <div v-if="dialogOpen" class="dialog-overlay" @click.self="closeDialog">
      <div class="dialog-box">
        <h3>{{ isEdit ? '编辑题目' : '新增题目' }}</h3>
        <div class="form-group">
          <label>题型</label>
          <select v-model="form.type">
            <option value="">请选择</option>
            <option v-for="t in types" :key="t.key" :value="t.key">{{ t.label }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>题目内容</label>
          <textarea v-model="form.content" rows="3" placeholder="请输入题目内容"></textarea>
        </div>
        <div class="form-group">
          <label>选项（逗号分隔，仅选择题和判断题需要）</label>
          <input v-model="form.options" placeholder="例如: A.选项一,B.选项二,C.选项三,D.选项四" />
        </div>
        <div class="form-group">
          <label>正确答案</label>
          <input v-model="form.answer" placeholder="单选:A / 多选:A,C / 判断:true / 填空:答案 / 简答:可留空" />
        </div>
        <div class="form-group">
          <label>参考答案（简答题可填写评分要点）</label>
          <textarea v-model="form.referenceAnswer" rows="2" placeholder="简答题参考答案或评分要点"></textarea>
        </div>
        <p v-if="submitError" class="error-text">{{ submitError }}</p>
        <div class="dialog-actions">
          <button class="secondary-btn" type="button" @click="closeDialog">取消</button>
          <button class="primary-btn" type="button" :disabled="submitting" @click="handleSubmit">{{ submitting ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getQuestions, createQuestion, updateQuestion, deleteQuestion, createQuestionsBatch } from '../../api/teacher'

const questions = ref([])
const loading = ref(false)
const error = ref('')
const activeType = ref('')

const dialogOpen = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const submitError = ref('')
const form = ref({ type: '', content: '', options: '', answer: '', referenceAnswer: '' })

// 批量导入
const batchDialogOpen = ref(false)
const batchJson = ref('')
const batchSubmitting = ref(false)
const batchError = ref('')
const batchSuccess = ref('')

const types = [
  { key: '', label: '全部' },
  { key: 'single_choice', label: '单选题' },
  { key: 'multi_choice', label: '多选题' },
  { key: 'true_false', label: '判断题' },
  { key: 'fill_blank', label: '填空题' },
  { key: 'short_answer', label: '简答题' }
]

function typeLabel(type) {
  return types.find(t => t.key === type)?.label || type
}

function truncate(str, n) {
  if (!str) return ''
  return str.length > n ? str.slice(0, n) + '...' : str
}

async function loadQuestions() {
  loading.value = true
  error.value = ''
  try {
    const params = activeType.value ? { type: activeType.value } : {}
    const res = await getQuestions(params)
    if (res.code === 200) {
      questions.value = res.data || []
    } else {
      error.value = res.message || '获取题目失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '获取题目失败'
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  editId.value = null
  form.value = { type: '', content: '', options: '', answer: '', referenceAnswer: '' }
  submitError.value = ''
  dialogOpen.value = true
}

function openEditDialog(q) {
  isEdit.value = true
  editId.value = q.id
  form.value = {
    type: q.type || '',
    content: q.content || '',
    options: q.options || '',
    answer: q.answer || '',
    referenceAnswer: q.referenceAnswer || ''
  }
  submitError.value = ''
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
}

async function handleSubmit() {
  if (!form.value.type || !form.value.content) {
    submitError.value = '题型和题目内容不能为空'
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    const data = {
      type: form.value.type,
      content: form.value.content,
      options: form.value.options || null,
      answer: form.value.answer || null,
      referenceAnswer: form.value.referenceAnswer || null
    }
    let res
    if (isEdit.value) {
      res = await updateQuestion(editId.value, data)
    } else {
      res = await createQuestion(data)
    }
    if (res.code === 200) {
      closeDialog()
      await loadQuestions()
    } else {
      submitError.value = res.message || '保存失败'
    }
  } catch (err) {
    submitError.value = err.response?.data?.message || err.message || '保存失败'
  } finally {
    submitting.value = false
  }
}

async function handleDelete(q) {
  if (!confirm(`确定删除题目 "${truncate(q.content, 30)}" 吗？`)) return
  try {
    const res = await deleteQuestion(q.id)
    if (res.code === 200) {
      await loadQuestions()
    } else {
      error.value = res.message || '删除失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || err.message || '删除失败'
  }
}

function openBatchDialog() {
  batchJson.value = ''
  batchError.value = ''
  batchSuccess.value = ''
  batchDialogOpen.value = true
}

function handleFileUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => { batchJson.value = ev.target.result }
  reader.readAsText(file)
}

async function handleBatchImport() {
  if (!batchJson.value.trim()) {
    batchError.value = '请输入题目数据'
    return
  }
  batchSubmitting.value = true
  batchError.value = ''
  batchSuccess.value = ''
  try {
    const data = JSON.parse(batchJson.value)
    if (!Array.isArray(data)) throw new Error('数据格式必须为 JSON 数组')
    const res = await createQuestionsBatch(data)
    if (res.code === 200) {
      batchSuccess.value = `成功导入 ${res.data.length} 道题目`
      batchJson.value = ''
      await loadQuestions()
    } else {
      batchError.value = res.message || '导入失败'
    }
  } catch (err) {
    batchError.value = err.message || 'JSON 格式错误或导入失败'
  } finally {
    batchSubmitting.value = false
  }
}

onMounted(loadQuestions)
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

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.filter-tag {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  cursor: pointer;
  border-radius: 6px;
  font-size: 13px;
  padding: 6px 12px;
}

.filter-tag.active {
  background: #eff6ff;
  border-color: #2563eb;
  color: #1d4ed8;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th {
  text-align: left;
  padding: 10px 12px;
  background: #f9fafb;
  color: #374151;
  font-weight: 600;
  border-bottom: 2px solid #e5e7eb;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: top;
}

.content-cell { max-width: 300px; word-break: break-all; }

.answer-summary { display: flex; flex-wrap: wrap; gap: 4px; }

.options-hint {
  background: #fef3c7; color: #92400e; font-size: 11px;
  padding: 2px 6px; border-radius: 4px;
}

.answer-hint {
  background: #dbeafe; color: #1e40af; font-size: 11px;
  padding: 2px 6px; border-radius: 4px;
}

.row-actions { display: flex; gap: 8px; }

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
}

.text-btn {
  background: transparent; color: #2563eb; padding: 4px 0; font-size: 13px;
}

.text-btn.danger { color: #dc2626; }

.error-text { color: #dc2626; }

.state-text { color: #6b7280; }

/* Dialog */
.dialog-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; justify-content: center; align-items: flex-start;
  padding-top: 80px; z-index: 200;
}

.dialog-box {
  background: #fff; border-radius: 12px; padding: 28px;
  width: 560px; max-width: 95vw; max-height: 85vh; overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
}

.dialog-box h3 { color: #1f2937; font-size: 20px; margin-bottom: 20px; }

.form-group { margin-bottom: 16px; }

.form-group label {
  display: block; font-size: 13px; color: #374151;
  margin-bottom: 6px; font-weight: 500;
}

.form-group input, .form-group textarea, .form-group select {
  width: 100%; border: 1px solid #d1d5db; border-radius: 6px;
  padding: 8px 12px; font-size: 14px; font-family: inherit;
}

.form-group input:focus, .form-group textarea:focus, .form-group select:focus {
  outline: none; border-color: #2563eb; box-shadow: 0 0 0 2px rgba(37,99,235,0.1);
}

.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .page-header { flex-direction: column; }
  .data-table { font-size: 12px; }
  .data-table th, .data-table td { padding: 6px; }
}
</style>
