<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory, batchSortCategories } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Category } from '@/types'
import draggable from 'vuedraggable'

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

// 拖拽结束保存排序
async function onDragEnd() {
  const sortList = categories.value.map((c, i) => ({ id: c.id, sortOrder: i }))
  await batchSortCategories(sortList)
  ElMessage.success('排序已更新')
}

onMounted(fetchCategories)
</script>

<template>
  <div class="categories-page">
    <div class="page-header">
      <h3>分类管理</h3>
      <el-button type="primary" @click="openCreate">+ 新增分类</el-button>
    </div>

    <p class="drag-tip">💡 拖拽卡片即可调整排序</p>

    <draggable
      v-model="categories"
      item-key="id"
      handle=".drag-handle"
      ghost-class="ghost-card"
      animation="250"
      @end="onDragEnd"
    >
      <template #item="{ element, index }">
        <div class="category-card" :class="{ hidden: element.status !== 'active' }">
          <div class="drag-handle">
            <el-icon :size="18"><Rank /></el-icon>
          </div>
          <div class="card-index">{{ index + 1 }}</div>
          <div class="card-name">{{ element.name }}</div>
          <div class="card-status">
            <el-tag :type="element.status === 'active' ? 'success' : 'info'" size="small" effect="light">
              {{ element.status === 'active' ? '显示' : '隐藏' }}
            </el-tag>
          </div>
          <div class="card-actions">
            <el-button text type="primary" size="small" @click="openEdit(element)">编辑</el-button>
            <el-button text size="small" @click="handleToggleStatus(element)">
              {{ element.status === 'active' ? '隐藏' : '显示' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(element)">删除</el-button>
          </div>
        </div>
      </template>
    </draggable>

    <el-empty v-if="!loading && categories.length === 0" description="还没有分类，快去添加吧~" />

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

<script lang="ts">
import { Rank } from '@element-plus/icons-vue'
export default { components: { Rank } }
</script>

<style scoped lang="scss">
.categories-page {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  min-height: 400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  h3 { font-size: 16px; margin: 0; }
}

.drag-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

/* ===== 卡片列表 ===== */
.category-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fafbfc;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  margin-bottom: 10px;
  transition: box-shadow 0.2s, transform 0.15s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0,0,0,0.06);
    transform: translateY(-1px);
  }

  &.hidden {
    opacity: 0.6;
  }
}

.drag-handle {
  cursor: grab;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  padding: 4px;
  border-radius: 4px;

  &:hover { background: #e8e8e8; color: var(--color-primary); }
  &:active { cursor: grabbing; }
}

.card-index {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.card-status {
  flex-shrink: 0;
}

.card-actions {
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

/* 拖拽占位样式 */
.ghost-card {
  opacity: 0.4;
  background: #e6f7ff !important;
  border-color: var(--color-primary) !important;
}
</style>
