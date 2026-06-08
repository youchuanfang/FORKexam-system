<template>
  <div class="teacher-page">
    <header class="page-header">
      <div>
        <h1>教师控制台</h1>
        <p>管理题库、试卷，批改学生答卷。</p>
      </div>
    </header>

    <section class="panel">
      <div class="dashboard-cards">
        <router-link to="/teacher/questions" class="dash-card">
          <div class="dash-icon">📋</div>
          <h3>题库管理</h3>
          <p>新增、编辑、删除题目，支持批量导入，支持五种题型。</p>
        </router-link>

        <router-link to="/teacher/papers" class="dash-card">
          <div class="dash-icon">📝</div>
          <h3>试卷管理</h3>
          <p>创建试卷、设置考试参数、从题库选题组卷、发布与取消发布。</p>
        </router-link>

        <router-link to="/teacher/classes" class="dash-card">
          <div class="dash-icon">🏫</div>
          <h3>班级管理</h3>
          <p>创建班级、查看邀请码、管理班级学生。</p>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
</script>

<style scoped>
.teacher-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 32px;
}

.page-header {
  max-width: 1080px;
  margin: 0 auto 24px;
}

h1, h3, p { margin: 0; }

h1 {
  color: #1f2937;
  font-size: 28px;
  margin-bottom: 8px;
}

.page-header p { color: #6b7280; }

.panel {
  max-width: 1080px;
  margin: 0 auto;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}

.dashboard-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.dash-card {
  display: block;
  text-decoration: none;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 28px 24px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.dash-card:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.12);
}

.dash-icon {
  font-size: 32px;
  margin-bottom: 14px;
}

.dash-card h3 {
  color: #1f2937;
  font-size: 18px;
  margin-bottom: 8px;
}

.dash-card p {
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .teacher-page { padding: 20px; }
  .dashboard-cards { grid-template-columns: 1fr; }
}
</style>
