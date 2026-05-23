<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <el-button type="primary" @click="$router.push('/dashboard')">回到工作台</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  gap: 12px;
}
.not-found h1 {
  font-size: 72px;
  color: #ddd;
}
.not-found p {
  color: #999;
  font-size: 16px;
}
</style>
