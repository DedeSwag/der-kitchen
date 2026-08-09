<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getTopDishes, getDailySummary } from '@/api/stats'
import { useOrderRealtimeRefresh } from '@/composables/useOrderRealtimeRefresh'

// 日期快捷
const shortcuts = [
  { text: '本周', value: () => { const now = new Date(); const d = now.getDay() || 7; const start = new Date(now); start.setDate(now.getDate() - d + 1); return [start, now] } },
  { text: '本月', value: () => { const now = new Date(); return [new Date(now.getFullYear(), now.getMonth(), 1), now] } },
  { text: '近30天', value: () => { const end = new Date(); const start = new Date(); start.setDate(end.getDate() - 29); return [start, end] } },
]

const dateRange = ref<[Date, Date]>([(() => { const d = new Date(); d.setDate(d.getDate() - 6); return d })(), new Date()])

const barChartRef = ref<HTMLDivElement>()
const lineChartRef = ref<HTMLDivElement>()
let barChart: echarts.ECharts | null = null
let lineChart: echarts.ECharts | null = null

function formatDate(d: Date) {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function fetchData() {
  const [start, end] = dateRange.value
  const startDate = formatDate(start)
  const endDate = formatDate(end)

  const [topData, dailyData] = await Promise.all([
    getTopDishes({ startDate, endDate, limit: 10 }),
    getDailySummary({ startDate, endDate }),
  ])

  // 条形图
  if (barChart) {
    barChart.setOption({
      title: { text: '高频菜品 Top10', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 100, right: 30, top: 40, bottom: 20 },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: topData.map(d => d.dishName).reverse() },
      series: [{ type: 'bar', data: topData.map(d => d.totalCount).reverse(), itemStyle: { color: '#FF6B35' } }],
    })
  }

  // 折线图
  if (lineChart) {
    lineChart.setOption({
      title: { text: '每日点餐趋势', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 30, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: dailyData.map(d => d.mealDate) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '订单数', type: 'line', data: dailyData.map(d => d.orderCount), smooth: true, itemStyle: { color: '#FF6B35' } },
        { name: '菜品数', type: 'line', data: dailyData.map(d => d.dishCount), smooth: true, itemStyle: { color: '#409EFF' } },
      ],
      legend: { bottom: 0 },
    })
  }
}

useOrderRealtimeRefresh(fetchData, () => true, 500)

function handleResize() {
  barChart?.resize()
  lineChart?.resize()
}

onMounted(async () => {
  await nextTick()
  if (barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (lineChartRef.value) lineChart = echarts.init(lineChartRef.value)
  window.addEventListener('resize', handleResize)
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  lineChart?.dispose()
})
</script>

<template>
  <div class="stats-page">
    <div class="filter-bar">
      <el-date-picker v-model="dateRange" type="daterange" range-separator="~"
        start-placeholder="开始" end-placeholder="结束" :shortcuts="shortcuts"
        @change="fetchData" style="width:300px" />
    </div>
    <div class="charts-grid">
      <div class="chart-card">
        <div ref="barChartRef" style="width:100%;height:360px"></div>
      </div>
      <div class="chart-card">
        <div ref="lineChartRef" style="width:100%;height:360px"></div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.stats-page { }
.filter-bar { margin-bottom: 20px; }
.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
}
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
</style>
