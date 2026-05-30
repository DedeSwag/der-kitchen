import { ref } from 'vue'

/**
 * 下拉刷新 + 触底加载组合式函数
 */
export function useLoadMore<T>(
  fetchFn: (page: number) => Promise<{ records: T[]; total: number }>,
  pageSize = 20
) {
  const list = ref<T[]>([]) as any
  const loading = ref(false)
  const refreshing = ref(false)
  const hasMore = ref(true)
  const currentPage = ref(1)

  async function loadFirst() {
    loading.value = true
    currentPage.value = 1
    try {
      const res = await fetchFn(1)
      list.value = res.records
      hasMore.value = res.records.length >= pageSize && list.value.length < res.total
    } catch { /* 忽略 */ }
    loading.value = false
  }

  async function loadMore() {
    if (!hasMore.value || loading.value) return
    loading.value = true
    currentPage.value++
    try {
      const res = await fetchFn(currentPage.value)
      list.value.push(...res.records)
      hasMore.value = res.records.length >= pageSize && list.value.length < res.total
    } catch {
      currentPage.value--
    }
    loading.value = false
  }

  async function onRefresh() {
    refreshing.value = true
    await loadFirst()
    refreshing.value = false
  }

  return { list, loading, refreshing, hasMore, loadFirst, loadMore, onRefresh }
}
