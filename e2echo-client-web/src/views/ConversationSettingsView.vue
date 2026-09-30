<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getConversation } from '@/api'
import { useConversationStore } from '@/stores/conversation'
import { showError } from '@/utils/feedback'

/**
 * 会话设置页：新增与修改共用。
 *
 * <p>新增时手工填写会话对方；修改时载入已有会话，会话对方与是否群聊不可改（它们决定会话的身份）。</p>
 */

const route = useRoute()
const router = useRouter()
const conversationStore = useConversationStore()

/** 是否新增模式。 */
const isCreate = computed(() => route.name === 'conversation-create')

/** 修改模式下的会话对方。 */
const peer = computed(() => String(route.params.peer ?? ''))

/** 表单实例。 */
const formRef = ref<FormInstance>()

/** 是否正在载入。 */
const loading = ref(false)

/** 是否正在保存。 */
const saving = ref(false)

/** 会话不存在。 */
const missing = ref(false)

/** 会话名称是否被手工改过：没改过时跟随会话对方自动填充。 */
const aliasEdited = ref(false)

/** 表单内容。 */
const form = ref({
  id: '',
  peer: '',
  alias: '',
  group: false,
  enabled: true,
})

/** 校验规则。 */
const rules: FormRules = {
  peer: [{ required: true, message: '请填写会话对方', trigger: 'blur' }],
  alias: [{ required: true, message: '请填写会话名称', trigger: 'blur' }],
}

// 新增时，会话名称默认取会话对方的末 5 位（与后端自动建会话的规则一致），手工改过之后不再覆盖
watch(() => form.value.peer, (value) => {
  if (isCreate.value && !aliasEdited.value) {
    form.value.alias = value.length > 5 ? value.slice(value.length - 5) : value
  }
})

// 私聊的启用没有意义：客户端始终订阅自己的私聊消息，启用只决定要不要订阅群聊
watch(() => form.value.group, (value) => {
  if (!value) {
    form.value.enabled = true
  }
}, { immediate: true })

/**
 * 新增页与设置页用的是同一个组件，路由在这两者之间切换时组件实例会被复用、不会重新挂载，
 * 所以这里监听路由把表单重置干净，否则新增页会带着上一个会话的数据。
 */
watch(() => `${String(route.name ?? '')}|${peer.value}`, async () => {
  form.value = { id: '', peer: '', alias: '', group: false, enabled: true }
  aliasEdited.value = false
  missing.value = false

  if (isCreate.value) {
    return
  }

  loading.value = true
  try {
    const conversation = await getConversation(peer.value)
    if (conversation === null) {
      missing.value = true
      return
    }
    form.value = {
      id: conversation.id ?? '',
      peer: conversation.peer,
      alias: conversation.alias,
      group: conversation.group,
      enabled: conversation.enabled,
    }
    // 已有会话的别名不再跟随会话对方变化
    aliasEdited.value = true
  } catch (error) {
    showError(error, '加载会话失败')
  } finally {
    loading.value = false
  }
}, { immediate: true })

/**
 * 返回：新增回空状态，修改回聊天页。
 */
function onBack(): void {
  if (isCreate.value) {
    void router.push({ name: 'chat-empty' })
    return
  }
  void router.push({ name: 'chat', params: { peer: peer.value } })
}

/**
 * 保存会话。
 */
async function onSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid !== true) {
    return
  }

  const payload = {
    peer: form.value.peer.trim(),
    alias: form.value.alias.trim(),
    group: form.value.group,
    enabled: form.value.enabled,
  }

  saving.value = true
  try {
    if (isCreate.value) {
      // 后端对同一用户的同一会话对方有唯一约束，先查一次给出可读提示，而不是等保存失败
      const exists = await getConversation(payload.peer)
      if (exists !== null) {
        ElMessage.warning('该会话已存在，请直接打开它')
        return
      }
      await conversationStore.save(payload)
    } else {
      await conversationStore.save({ id: form.value.id, ...payload })
    }
    ElMessage.success('已保存')
    void router.push({ name: 'chat', params: { peer: payload.peer } })
  } catch (error) {
    showError(error, '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="settings">
    <header class="settings__header">
      <el-button :icon="ArrowLeft" circle size="small" title="返回" @click="onBack" />
      <span class="settings__title">{{ isCreate ? '新增会话' : '会话设置' }}</span>
    </header>

    <div class="settings__body scroll-y">
      <el-empty v-if="missing" description="会话不存在">
        <el-button @click="onBack">返回</el-button>
      </el-empty>

      <el-skeleton v-else-if="loading" :rows="5" animated />

      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        class="settings__form"
      >
        <el-form-item label="会话对方" prop="peer">
          <el-input
            v-model="form.peer"
            :disabled="!isCreate"
            placeholder="私聊填对方公钥，群聊填群聊标识"
            clearable
          />
          <div v-if="!isCreate" class="settings__hint">会话对方是会话的身份，不可修改。</div>
        </el-form-item>

        <el-form-item label="会话名称" prop="alias">
          <el-input
            v-model="form.alias"
            placeholder="用于展示的名称"
            clearable
            @update:model-value="aliasEdited = true"
          />
        </el-form-item>

        <el-form-item label="会话类型">
          <el-radio-group v-model="form.group" :disabled="!isCreate">
            <el-radio :value="false">私聊</el-radio>
            <el-radio :value="true">群聊</el-radio>
          </el-radio-group>
          <div v-if="!isCreate" class="settings__hint">会话类型决定消息的加解密方式，不可修改。</div>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :disabled="!form.group" />
          <div class="settings__hint">
            只有群聊可以停用：停用后不再订阅该群的消息，收不到新消息。私聊始终启用。
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
          <el-button @click="onBack">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.settings {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.settings__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.settings__title {
  font-size: 15px;
  font-weight: 600;
}

.settings__body {
  flex: 1;
  min-height: 0;
}

.settings__form {
  max-width: 560px;
  padding: 20px 16px;
}

.settings__hint {
  width: 100%;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}
</style>
