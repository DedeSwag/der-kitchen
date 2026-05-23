<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Odometer, Dish, Menu as IconMenu, List, DataLine, Setting } from '@element-plus/icons-vue'

defineProps<{ collapsed: boolean }>()
const route = useRoute()

const menuItems = [
  { path: '/dashboard', title: '工作台', icon: Odometer },
  { path: '/dishes', title: '菜品管理', icon: Dish },
  { path: '/categories', title: '分类管理', icon: IconMenu },
  { path: '/orders', title: '订单管理', icon: List },
  { path: '/stats', title: '数据统计', icon: DataLine },
  { path: '/settings', title: '设置', icon: Setting },
]
</script>

<template>
  <el-menu
    :default-active="route.path"
    :collapse="collapsed"
    background-color="#ffffff"
    text-color="#303133"
    active-text-color="#007cc4"
    router
    :collapse-transition="false"
  >
    <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
      <el-icon><component :is="item.icon" /></el-icon>
      <template #title>{{ item.title }}</template>
    </el-menu-item>
  </el-menu>
</template>

<style scoped>
.el-menu {
  border-right: none;
  flex: 1;
}
</style>

<style>
.admin-sider .el-menu-item.is-active {
  background: #E6F7FF !important;
  border-left: 3px solid #2697ff;
  color: #2697ff !important;
}
.admin-sider .el-menu-item.is-active .el-icon {
  color: #2697ff !important;
}
.admin-sider .el-menu-item {
  border-left: 3px solid transparent;
}
</style>
