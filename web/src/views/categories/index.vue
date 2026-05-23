<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory, batchSortCategories } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Category } from '@/types'
import { Rank } from '@element-plus/icons-vue'

const categories = ref<Category[]>([])
const loading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const editingId = ref<number | null>(null)
const formName = ref('')

async function fetchCategories() {
  loading.value = true
  try {
    categories.value = await getCategories()
  } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  formName.value = ''
  dialogTitle.value = '新增分类'
  dialogVisible.value = true
}

function openEdit(cat: Category) {
  editingId.value = cat.id
  formName.value = cat.name
  dialogTitle.value = '编辑分类'
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formName.value.trim()) { ElMessage.warning('请输入分类名'); return }
  if (editingId.value) {
    await updateCategory(editingId.value, { name: formName.value.trim() })
    ElMessage.success('修改成功')
  } else {
    await createCategory({ name: formName.value.trim(), sortOrder: categories.value.length })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  fetchCategories()
}

async function handleDelete(cat: Category) {
  try {
    await ElMessageBox.confirm(`确定删除「${cat.name}」吗？`, '提示', { type: 'warning' })
    await deleteCategory(cat.id)
    ElMessage.success('已删除')
    fetchCategories()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message) ElMessage.error(e.message)
  }
}

async function handleToggleStatus(cat: Category) {
  const newStatus = cat.status === 'active' ? 'hidden' : 'active'
  await updateCategory(cat.id, { status: newStatus })
  cat.status = newStatus
  ElMessage.success(newStatus === 'active' ? '已显示' : '已隐藏')
}

// 简易拖拽排序（使用上移/下移按钮代替拖拽库）
async function moveUp(index: number) {
  if (index === 0) return
  const arr = [...categories.value]
  ;[arr[index - 1], arr[index]] = [arr[index], arr[index - 1]]
  categories.value = arr
  await saveSortOrder()
}

async function moveDown(index: number) {
  if (index === categories.value.length - 1) return
  const arr = [...categories.value]
  ;[arr[index], arr[index + 1]] = [arr[index + 1], arr[index]]
  categories.value = arr
  await saveSortOrder()
}

async function saveSortOrder() {
  const sortList = categories.value.map((c, i) => ({ id: c.id, sortOrder: i }))
  await batchSortCategories(sortList)
}

onMounted(fetchCategories)
</script>

<template>
  <div class="categories-page">
    <div class="page-header">
      <h3>分类管理</h3>
      <el-button type="primary" @click="openCreate">+ 新增分类</el-button>
    </div>

    <el-table :data="categories" v-loading="loading" stripe>
      <el-table-column label="排序" width="100">
        <template #default="{ $index }">
          <el-button-group size="small">
            <el-button :disabled="$index === 0" @click="moveUp($index)" :icon="Rank">↑</el-button>
            <el-button :disabled="$index === categories.length - 1" @click="moveDown($index)">↓</el-button>
          </el-button-group>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
            {{ row.status === 'active' ? '显示' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button text size="small" @click="handleToggleStatus(row)">
            {{ row.status === 'active' ? '隐藏' : '显示' }}
          </el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-input v-model="formName" placeholder="请输入分类名称" maxlength="20" show-word-limit @keyup.enter="handleSubmit" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.categories-page { background:#fff; border-radius:8px; padding:20px; }
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; h3 { font-size:16px; } }
</style>
