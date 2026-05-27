<template>
  <div class="home-page">
    <div class="welcome-card">
      <h1>学生首页</h1>
      <p>欢迎使用在线考试系统，祝你考试顺利！</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.home-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 60px);
  padding: 20px;
}

.welcome-card {
  background: #fff;
  padding: 60px 80px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  text-align: center;
}

.welcome-card h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 12px;
}

.welcome-card p {
  font-size: 16px;
  color: #888;
}
</style>
