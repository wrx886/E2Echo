<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { isAuthFailed, listConversations } from '@/api'

/**
 * 首页。
 *
 * <p>目前是用于验证接口层是否打通的占位页面：点一下按钮会调用会话列表接口。真正的会话列表与
 * 聊天界面在此基础上继续实现。</p>
 */

/** 是否正在请求。 */
const loading = ref(false)

/** 会话总数，为空表示还没有查询过。 */
const total = ref<number | null>(null)

/**
 * 调用会话列表接口，确认与客户端容器之间的连接是否正常。
 */
async function checkConnection(): Promise<void> {
  loading.value = true
  try {
    const page = await listConversations(1, 20)
    total.value = page.totalElements
    ElMessage.success(`已连接客户端，当前共有 ${page.totalElements} 个会话`)
  } catch (error) {
    // 会话失效已经由接口层统一提示，这里不再重复弹出
    if (!isAuthFailed(error)) {
      ElMessage.error(error instanceof Error ? error.message : '请求失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="home">
    <h1>E2Echo 客户端</h1>
    <p class="tip">网页已通过客户端完成登录，可以开始使用。</p>

    <el-button type="primary" :loading="loading" @click="checkConnection">
      检查后端连接
    </el-button>

    <p v-if="total !== null" class="result">会话数量：{{ total }}</p>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 24px;
}

h1 {
  font-size: 22px;
  font-weight: 600;
}

.tip,
.result {
  color: #909399;
}
</style>
