<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import SideMenu from '@/components/SideMenu.vue'
import { Bell, CaretBottom, Fold, Expand } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    userStore.logout()
    router.push('/login')
  } catch { /* 取消 */ }
}
</script>

<template>
  <div class="admin-layout">
    <!-- 顶部通栏 -->
    <header class="admin-header">
      <div class="header-logo">
        <img src="/images/logo.png" class="logo-img" alt="logo" />
        <span class="logo-text">陈哥厨房 · 管理后台</span>
      </div>
      <div class="header-actions">
        <!-- 待处理订单铃铛 -->
        <el-badge :value="appStore.pendingCount" :max="99" :hidden="appStore.pendingCount === 0">
          <el-button :icon="Bell" circle @click="router.push('/orders')" />
        </el-badge>
        <!-- 用户下拉 -->
        <el-dropdown @command="(cmd: string) => cmd === 'logout' && handleLogout()">
          <span class="user-btn">
            <el-avatar :size="28" :src="userStore.avatarUrl || undefined">
              {{ userStore.nickname?.[0] || 'A' }}
            </el-avatar>
            <span>{{ userStore.nickname || '管理员' }}</span>
            <el-icon><CaretBottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="settings" @click="router.push('/settings')">个人设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 主体区域 -->
    <div class="admin-body">
      <!-- 侧边栏 -->
      <aside class="admin-sider" :class="{ collapsed: appStore.sidebarCollapsed }">
        <div class="collapse-trigger" @click="appStore.toggleSidebar">
          <el-icon><Fold v-if="!appStore.sidebarCollapsed" /><Expand v-else /></el-icon>
        </div>
        <SideMenu :collapsed="appStore.sidebarCollapsed" />
      </aside>

      <!-- 主内容 -->
      <main class="admin-main">
        <!-- 面包屑 -->
        <div class="admin-breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ route.meta?.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <!-- 页面内容 -->
        <div class="admin-content">
          <router-view v-slot="{ Component }">
            <keep-alive :max="10">
              <component :is="Component" />
            </keep-alive>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.admin-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ===== 顶部通栏 ===== */
.admin-header {
  height: var(--header-height);
  background: var(--color-primary-dark);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
  z-index: 200;
}

.header-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  .logo-img { height: 32px; width: auto; object-fit: contain; }
  .logo-text { font-size: 16px; font-weight: 600; letter-spacing: 0.5px; }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;

  :deep(.el-button.is-circle) {
    background: transparent;
    border-color: rgba(255,255,255,0.3);
    color: #fff;
    &:hover { border-color: #fff; }
  }
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  &:hover { opacity: 0.85; }
}

/* ===== 主体 ===== */
.admin-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ===== 侧边栏 ===== */
.admin-sider {
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width 0.25s;
  overflow: hidden;
  position: relative;

  &.collapsed {
    width: var(--sidebar-collapsed-width);
  }
}

.collapse-trigger {
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 12px;
  cursor: pointer;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
  font-size: 16px;
  &:hover { color: var(--color-primary); background: #fafafa; }
}

/* ===== 主内容区 ===== */
.admin-main {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
}

.admin-breadcrumb {
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.admin-content {
  flex: 1;
  padding: 20px;
}
</style>
