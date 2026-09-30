<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { copyText } from '@/utils/clipboard'

/**
 * 左侧边栏底部的个人信息框。
 *
 * <p>平时只占一个头像的位置，鼠标放上去弹出当前用户的名称与公钥，公钥可以一键复制。</p>
 */

const userStore = useUserStore()

/** 刚刚复制过，用于把按钮文字临时改成“已复制”。 */
const copied = ref(false)

onMounted(() => {
  // 侧边栏可能在聊天页之前挂载完，这里补一次；已经取到或正在取时该方法会直接返回
  void userStore.ensureLoaded().catch(() => {})
})

/**
 * 复制当前用户公钥。
 */
async function onCopy(): Promise<void> {
  if (!userStore.publicKey) {
    return
  }
  const ok = await copyText(userStore.publicKey)
  if (!ok) {
    ElMessage.error('复制失败，请手动选中公钥复制')
    return
  }
  copied.value = true
  ElMessage.success('已复制公钥')
  window.setTimeout(() => {
    copied.value = false
  }, 1500)
}
</script>

<template>
  <el-popover
    placement="right-end"
    :width="320"
    trigger="hover"
    :show-after="80"
    popper-class="user-profile__popover"
  >
    <template #reference>
      <button type="button" class="user-profile" title="个人信息">
        <el-avatar :size="36" class="user-profile__avatar">
          <el-icon v-if="userStore.avatarText.length === 0"><User /></el-icon>
          <template v-else>{{ userStore.avatarText }}</template>
        </el-avatar>
      </button>
    </template>

    <div class="profile">
      <div class="profile__head">
        <el-avatar :size="34" class="profile__avatar">
          <el-icon v-if="userStore.avatarText.length === 0"><User /></el-icon>
          <template v-else>{{ userStore.avatarText }}</template>
        </el-avatar>
        <div class="profile__name">{{ userStore.displayName || '未获取到用户信息' }}</div>
      </div>

      <div class="profile__label">公钥</div>
      <div class="profile__key">{{ userStore.publicKey || '加载中…' }}</div>

      <el-button
        class="profile__copy"
        size="small"
        :icon="DocumentCopy"
        :disabled="userStore.publicKey.length === 0"
        @click="onCopy"
      >
        {{ copied ? '已复制' : '复制公钥' }}
      </el-button>
    </div>
  </el-popover>
</template>

<style scoped>
.user-profile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  padding: 6px 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-profile:hover {
  background: #3a3a3a;
}

.user-profile__avatar {
  background: #07c160;
  color: #fff;
  font-size: 13px;
}

.profile__head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile__avatar {
  flex: none;
  background: #07c160;
  color: #fff;
  font-size: 13px;
}

.profile__name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.profile__label {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.profile__key {
  max-height: 96px;
  margin-top: 4px;
  overflow-y: auto;
  padding: 6px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.profile__copy {
  width: 100%;
  margin-top: 10px;
}
</style>
