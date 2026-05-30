import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem, Dish } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  // 加菜模式：关联的订单 ID
  const extraOrderId = ref<number | null>(null)

  const totalCount = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))

  const isExtraMode = computed(() => extraOrderId.value !== null)

  /** 添加菜品（或增加数量） */
  function addDish(dish: Dish, qty = 1) {
    const exist = items.value.find(i => i.dish.id === dish.id)
    if (exist) {
      exist.quantity += qty
    } else {
      items.value.push({ dish, quantity: qty })
    }
  }

  /** 减少数量 */
  function minusDish(dishId: number) {
    const idx = items.value.findIndex(i => i.dish.id === dishId)
    if (idx === -1) return
    items.value[idx].quantity--
    if (items.value[idx].quantity <= 0) {
      items.value.splice(idx, 1)
    }
  }

  /** 获取某菜品数量 */
  function getQuantity(dishId: number): number {
    return items.value.find(i => i.dish.id === dishId)?.quantity || 0
  }

  /** 移除某菜品 */
  function removeDish(dishId: number) {
    items.value = items.value.filter(i => i.dish.id !== dishId)
  }

  /** 清空购物车 */
  function clear() {
    items.value = []
    extraOrderId.value = null
  }

  /** 进入加菜模式 */
  function enterExtraMode(orderId: number) {
    items.value = []
    extraOrderId.value = orderId
  }

  /** 批量添加（再来一单） */
  function batchAdd(dishes: { dish: Dish; quantity: number }[]) {
    items.value = []
    extraOrderId.value = null
    dishes.forEach(d => items.value.push({ dish: d.dish, quantity: d.quantity }))
  }

  return {
    items,
    extraOrderId,
    totalCount,
    isExtraMode,
    addDish,
    minusDish,
    getQuantity,
    removeDish,
    clear,
    enterExtraMode,
    batchAdd,
  }
})
