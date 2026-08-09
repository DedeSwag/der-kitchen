<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createDish, updateDish, getDishDetail } from '@/api/dish'
import { getCategories } from '@/api/category'
import { uploadFile } from '@/api/file'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Category } from '@/types'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const dishId = computed(() => route.params.id ? Number(route.params.id) : null)
const isEdit = computed(() => !!dishId.value)

const formRef = ref<FormInstance>()
const loading = ref(false)
const imageLoading = ref(false)
const categories = ref<Category[]>([])

const form = ref({
  name: '',
  categoryId: undefined as number | undefined,
  imageFileId: undefined as number | undefined,
  imageUrl: '',
  description: '',
  cookingTime: undefined as number | undefined,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }, { max: 20, message: '不超过20字', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  imageFileId: [{ required: true, message: '请上传菜品图片', trigger: 'change' }],
}

async function handleUpload(options: any) {
  imageLoading.value = true
  try {
    const res = await uploadFile(options.file)
    const uploaded = res[0]
    form.value.imageFileId = uploaded.fileId
    form.value.imageUrl = uploaded.url
    ElMessage.success('上传成功')
  } finally { imageLoading.value = false }
}

function removeImage() {
  form.value.imageFileId = undefined
  form.value.imageUrl = ''
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (form.value.categoryId == null || form.value.imageFileId == null) return
  loading.value = true
  try {
    const payload = {
      name: form.value.name,
      categoryId: form.value.categoryId,
      imageFileId: form.value.imageFileId,
      description: form.value.description,
      cookingTime: form.value.cookingTime,
    }
    if (isEdit.value) {
      await updateDish(dishId.value!, payload)
      ElMessage.success('修改成功')
    } else {
      await createDish(payload)
      ElMessage.success('新增成功')
    }
    router.push('/dishes')
  } finally { loading.value = false }
}

onMounted(async () => {
  categories.value = await getCategories()
  if (isEdit.value) {
    const dish = await getDishDetail(dishId.value!)
    form.value = { name: dish.name, categoryId: dish.categoryId, imageFileId: dish.imageFileId, imageUrl: dish.imageUrl || '', description: dish.description || '', cookingTime: dish.cookingTime ?? undefined }
  }
})
</script>

<template>
  <div class="dish-form-page">
    <div class="page-header"><h3>{{ isEdit ? '编辑菜品' : '新增菜品' }}</h3></div>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:600px">
      <el-form-item label="菜品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入菜品名称" maxlength="20" show-word-limit />
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="菜品图片" prop="imageFileId">
        <div v-if="form.imageUrl" style="position:relative;display:inline-block">
          <el-image :src="form.imageUrl" fit="cover" style="width:120px;height:120px;border-radius:8px" />
          <el-button text type="danger" size="small" @click="removeImage" style="display:block;margin-top:4px">删除</el-button>
        </div>
        <el-upload v-else :show-file-list="false" :http-request="handleUpload" accept=".jpg,.jpeg,.png,.webp">
          <div v-loading="imageLoading" style="width:120px;height:120px;border:1px dashed #ddd;border-radius:8px;display:flex;flex-direction:column;align-items:center;justify-content:center;cursor:pointer;gap:6px;color:#999">
            <el-icon :size="28"><Plus /></el-icon>
            <span style="font-size:12px">上传图片</span>
          </div>
        </el-upload>
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="简单描述" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="烹饪时长">
        <el-input-number v-model="form.cookingTime" :min="1" :max="300" />
        <span style="margin-left:8px;color:#999">分钟</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">{{ isEdit ? '保存修改' : '确认新增' }}</el-button>
        <el-button @click="router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.dish-form-page { background:#fff; border-radius:8px; padding:24px; }
.page-header { margin-bottom:24px; h3 { font-size:18px; } }
</style>
