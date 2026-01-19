<template>
  <div class="profile-page">
    <h1>个人主页</h1>
    <p>用户ID: {{ route.params.id }}</p>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped lang="scss">
.profile-page {
  padding: 24px;
}
</style>
