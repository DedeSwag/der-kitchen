<template>
  <view class="confirm-page">
    <!-- 用餐时段 -->
    <view class="section">
      <text class="section-title">用餐时段</text>
      <view class="meal-options">
        <view
          v-for="m in mealOptions"
          :key="m.value"
          class="meal-option"
          :class="{ active: selectedMeal === m.value }"
          @tap="selectedMeal = m.value"
        >
          <text>{{ m.label }}</text>
        </view>
      </view>
    </view>

    <!-- 已选菜品 -->
    <view class="section">
      <text class="section-title">已选菜品</text>
      <view class="dish-list">
        <view v-for="item in cartStore.items" :key="item.dish.id" class="dish-item">
          <image class="dish-thumb" :src="item.dish.imageUrl || '/static/default-dish.png'" mode="aspectFill" />
          <text class="dish-name ellipsis">{{ item.dish.name }}</text>
          <text class="dish-qty">×{{ item.quantity }}</text>
          <view class="dish-del" @tap="cartStore.removeDish(item.dish.id)">
            <text>✕</text>
          </view>
        </view>
      </view>
      <view v-if="cartStore.items.length === 0" class="empty-hint">
        <text>请至少选择一道菜</text>
      </view>
    </view>

    <!-- 口味偏好 -->
    <view class="section">
      <text class="section-title">口味偏好（可多选）</text>
      <view class="tag-group">
        <view
          v-for="tag in flavorOptions"
          :key="tag"
          class="flavor-tag"
          :class="{ active: selectedFlavors.includes(tag) }"
          @tap="toggleFlavor(tag)"
        >
          <text>{{ tag }}</text>
        </view>
      </view>
    </view>

    <!-- 忌口说明 -->
    <view class="section">
      <text class="section-title">忌口说明</text>
      <textarea
        v-model="avoidNote"
        class="note-input"
        placeholder="输入忌口，如：不要香菜..."
        :maxlength="200"
      />
    </view>

    <!-- 特殊要求 -->
    <view class="section">
      <text class="section-title">特殊要求</text>
      <textarea
        v-model="specialNote"
        class="note-input"
        placeholder="输入特殊要求，如：软烂一点..."
        :maxlength="200"
      />
    </view>

    <!-- 提交按钮 -->
    <view class="submit-wrap">
      <view class="submit-btn" :class="{ disabled: submitting }" @tap="handleSubmit">
        <text>{{ submitting ? '提交中...' : '提交订单 🍽️' }}</text>
      </view>
    </view>

    <!-- 撒花动效 -->
    <Confetti ref="confettiRef" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useCartStore } from '@/stores/cart'
import { useAppStore } from '@/stores/app'
import { createOrder, addOrderItems } from '@/api/order'
import Confetti from '@/components/Confetti.vue'

const cartStore = useCartStore()
const appStore = useAppStore()
const confettiRef = ref<InstanceType<typeof Confetti>>()

const selectedMeal = ref(appStore.selectedMealType)
const selectedFlavors = ref<string[]>([])
const avoidNote = ref('')
const specialNote = ref('')
const submitting = ref(false)
let extraOrderId: number | null = null

const mealOptions = [
  { label: '今日午餐', value: 'today_lunch' },
  { label: '今日晚餐', value: 'today_dinner' },
  { label: '明日午餐', value: 'tomorrow_lunch' },
  { label: '明日晚餐', value: 'tomorrow_dinner' },
]

const flavorOptions = ['微辣', '不辣', '少油', '少盐', '清淡']

onLoad((query) => {
  if (query?.extraOrderId) {
    extraOrderId = Number(query.extraOrderId)
  }
})

function toggleFlavor(tag: string) {
  const idx = selectedFlavors.value.indexOf(tag)
  if (idx >= 0) {
    selectedFlavors.value.splice(idx, 1)
  } else {
    selectedFlavors.value.push(tag)
  }
}

async function handleSubmit() {
  // 校验
  if (cartStore.items.length === 0) {
    uni.showToast({ title: '请至少选择一道菜', icon: 'none' })
    return
  }
  if (!selectedMeal.value && !extraOrderId) {
    uni.showToast({ title: '请选择用餐时段', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const items = cartStore.items.map(i => ({ dishId: i.dish.id, quantity: i.quantity }))

    if (extraOrderId) {
      // 加菜模式
      await addOrderItems(extraOrderId, items)
      uni.showToast({ title: '加菜成功！', icon: 'success' })
      cartStore.clear()
      setTimeout(() => {
        uni.navigateBack()
      }, 1200)
    } else {
      // 新建订单
      const order = await createOrder({
        mealType: selectedMeal.value,
        items,
        flavorTags: selectedFlavors.value.join(','),
        avoidNote: avoidNote.value.trim(),
        specialNote: specialNote.value.trim(),
      })
      cartStore.clear()
      confettiRef.value?.play()
      uni.showToast({ title: '下单成功！等陈哥接单~', icon: 'none', duration: 2000 })
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/order/detail?id=${order.id}` })
      }, 1500)
    }
  } catch (e: any) {
    const msg = e?.message || '提交失败'
    // 13.12 菜品下架/缺货提交时自动提示移除
    if (msg.includes('下架') || msg.includes('缺货') || msg.includes('不可用')) {
      uni.showModal({
        title: '部分菜品不可用',
        content: msg + '\n请返回移除后重新提交',
        showCancel: false,
      })
    } else {
      uni.showToast({ title: msg, icon: 'none' })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.confirm-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 180rpx;
}

.section {
  background: var(--bg-card);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-sm);
}

.section-title {
  font-size: var(--font-md);
  font-weight: bold;
  margin-bottom: var(--spacing-md);
  display: block;
}

/* 用餐时段 */
.meal-options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.meal-option {
  padding: 16rpx 28rpx;
  border-radius: 32rpx;
  font-size: var(--font-sm);
  background: #f5f5f5;
  color: var(--text-regular);
  border: 2rpx solid transparent;

  &.active {
    background: #FFF3ED;
    color: var(--color-primary);
    border-color: var(--color-primary);
  }
}

/* 菜品列表 */
.dish-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.dish-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: #fafafa;
  border-radius: var(--radius-md);
}

.dish-thumb {
  width: 80rpx;
  height: 80rpx;
  border-radius: var(--radius-sm);
}

.dish-name {
  flex: 1;
  font-size: var(--font-md);
}

.dish-qty {
  font-size: var(--font-md);
  color: var(--text-secondary);
  margin-right: var(--spacing-sm);
}

.dish-del {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: var(--text-secondary);
}

.empty-hint {
  text-align: center;
  color: var(--text-secondary);
  padding: var(--spacing-lg) 0;
  font-size: var(--font-sm);
}

/* 口味标签 */
.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.flavor-tag {
  padding: 12rpx 24rpx;
  border-radius: 24rpx;
  font-size: var(--font-sm);
  background: #f5f5f5;
  color: var(--text-regular);
  border: 2rpx solid transparent;

  &.active {
    background: #FFF3ED;
    color: var(--color-primary);
    border-color: var(--color-primary);
  }
}

/* 备注输入 */
.note-input {
  width: 100%;
  height: 140rpx;
  background: #f9f9f9;
  border-radius: var(--radius-md);
  padding: var(--spacing-sm);
  font-size: var(--font-md);
  line-height: 1.5;
  box-sizing: border-box;
}

/* 提交按钮 */
.submit-wrap {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--spacing-lg);
  padding-bottom: calc(var(--spacing-lg) + env(safe-area-inset-bottom));
  background: var(--bg-card);
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.05);
}

.submit-btn {
  height: 96rpx;
  background: var(--color-primary);
  color: #fff;
  font-size: var(--font-lg);
  font-weight: bold;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  &.disabled { opacity: 0.6; }
  &:active:not(.disabled) { opacity: 0.85; }
}
</style>

