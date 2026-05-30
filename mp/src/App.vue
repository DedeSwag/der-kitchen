<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

onLaunch(() => {
  // 启动时尝试静默登录
  const userStore = useUserStore()
  userStore.silentLogin()
})
</script>

<style>
@import './styles/global.scss';
</style>
