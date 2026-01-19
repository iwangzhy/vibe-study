<template>
  <div class="search-page">
    <el-card>
      <h2>搜索结果</h2>
      <p>搜索关键词: {{ searchQuery }}</p>
      <el-empty description="暂无搜索结果" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const searchQuery = ref('')

onMounted(() => {
  searchQuery.value = route.query.q as string || ''
})
</script>

<style scoped lang="scss">
.search-page {
  h2 {
    margin: 0 0 8px;
  }

  p {
    margin: 0 0 16px;
    color: #666;
  }
}
</style>
