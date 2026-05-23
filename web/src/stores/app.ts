import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const pendingCount = ref(0)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setPendingCount(count: number) {
    pendingCount.value = count
  }

  return { sidebarCollapsed, pendingCount, toggleSidebar, setPendingCount }
})
