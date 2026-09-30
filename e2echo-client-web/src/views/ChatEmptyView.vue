<script setup lang="ts">
/**
 * 聊天空状态：没有选中会话时显示。
 */
</script>

<template>
  <section class="chat-empty">
    <el-empty description="选择一个会话开始聊天" />
  </section>
</template>

<style scoped>
.chat-empty {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
</style>
