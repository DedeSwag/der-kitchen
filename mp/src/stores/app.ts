import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 当前选中的用餐时段
  const selectedMealType = ref(getDefaultMealType())

  // 管理端 - 待处理订单数
  const pendingCount = ref(0)

  /** 根据当前时间智能推荐默认时段 */
  function getDefaultMealType(): string {
    const hour = new Date().getHours()
    if (hour < 10) return 'today_lunch'
    if (hour < 15) return 'today_dinner'
    return 'tomorrow_lunch'
  }

  function resetMealType() {
    selectedMealType.value = getDefaultMealType()
  }

  return { selectedMealType, pendingCount, resetMealType }
})
