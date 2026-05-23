<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()

// 修改密码
const pwdDialogVisible = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdLoading = ref(false)

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '不少于6位', trigger: 'blur' }],
  confirmPassword: [{
    validator: (_r: any, v: string, cb: any) => {
      if (v !== pwdForm.value.newPassword) cb(new Error('两次密码不一致'))
      else cb()
    }, trigger: 'blur'
  }],
}

async function handleChangePwd() {
  await pwdFormRef.value?.validate()
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.value.oldPassword, newPassword: pwdForm.value.newPassword })
    ElMessage.success('密码修改成功')
    pwdDialogVisible.value = false
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } finally { pwdLoading.value = false }
}
</script>

<template>
  <div class="settings-page">
    <h3 style="margin-bottom:20px">设置</h3>

    <!-- 当前用户卡片 -->
    <el-card style="max-width:500px;margin-bottom:20px">
      <div style="display:flex;align-items:center;gap:16px">
        <el-avatar :size="64" :src="userStore.avatarUrl || undefined">
          {{ userStore.nickname?.[0] || 'A' }}
        </el-avatar>
        <div>
          <h4>{{ userStore.nickname || '管理员' }}</h4>
          <p style="color:#999;margin-top:4px">角色：{{ userStore.role === 'admin' ? '管理员' : '普通用户' }}</p>
        </div>
      </div>
      <div style="margin-top:16px">
        <el-button size="small" @click="pwdDialogVisible = true">修改密码</el-button>
      </div>
    </el-card>

    <!-- 家庭成员（预留） -->
    <el-card style="max-width:500px">
      <template #header>家庭成员管理</template>
      <el-tooltip content="二期开放" placement="top">
        <el-button type="primary" disabled>+ 添加家庭成员</el-button>
      </el-tooltip>
    </el-card>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.settings-page { max-width: 700px; }
</style>
