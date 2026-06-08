<template>
  <div v-if="totalPages > 1" class="pagination">
    <span class="pagination-info">共 {{ totalElements }} 条记录</span>
    <div class="pagination-btns">
      <button :disabled="currentPage <= 0" @click="$emit('page-change', currentPage - 1)">&laquo; 上一页</button>
      <button
        v-for="p in visiblePages"
        :key="p"
        :class="{ active: p === currentPage }"
        @click="$emit('page-change', p)"
      >{{ p + 1 }}</button>
      <button :disabled="currentPage >= totalPages - 1" @click="$emit('page-change', currentPage + 1)">下一页 &raquo;</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, default: 0 },
  totalPages: { type: Number, default: 0 },
  totalElements: { type: Number, default: 0 }
})

defineEmits(['page-change'])

const visiblePages = computed(() => {
  const pages = []
  const total = props.totalPages
  const current = props.currentPage
  const start = Math.max(0, Math.min(current - 2, total - 5))
  const end = Math.min(total, start + 5)
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  return pages
})
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
  flex-wrap: wrap;
  gap: 10px;
}
.pagination-info {
  font-size: 13px;
  color: #6b7280;
}
.pagination-btns {
  display: flex;
  gap: 4px;
}
.pagination-btns button {
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  cursor: pointer;
  border-radius: 5px;
  font-size: 13px;
  padding: 6px 12px;
  min-width: 36px;
}
.pagination-btns button.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.pagination-btns button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
