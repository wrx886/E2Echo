<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Delete, DocumentCopy, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addGroupMember,
  deleteGroupMember,
  getConversation,
  listGroupMembers,
  resendGroupKey,
  updateGroupKey,
} from '@/api'
import type { ConversationDto, GroupMemberDto } from '@/api'
import { useConversationStore } from '@/stores/conversation'
import { useUserStore } from '@/stores/user'
import { copyText } from '@/utils/clipboard'
import { formatTime } from '@/utils/display'
import { showError } from '@/utils/feedback'

/**
 * 群聊管理页。
 *
 * <p>只有群主能进：群标识以群主公钥开头，据此判断。页面可以分页查看成员、按公钥加人、删除成员，
 * 以及轮换或重发群密钥。</p>
 *
 * <p>删除成员只是把他从分发名单里去掉，对方手里还有当前群密钥、仍能收发消息，所以删除后要提示
 * 群主更新一次密钥；没更新就离开页面时会再拦一次。</p>
 */

/** 成员列表每页条数。 */
const PAGE_SIZE = 20

const route = useRoute()
const router = useRouter()
const conversationStore = useConversationStore()
const userStore = useUserStore()

/** 群标识。 */
const group = computed(() => String(route.params.peer ?? ''))

/** 当前会话，用于判断会话是否存在。 */
const conversation = ref<ConversationDto | null>(null)

/** 是否正在初始化。 */
const loading = ref(true)

/** 会话不存在。 */
const missing = ref(false)

/** 自己不是群主。 */
const denied = ref(false)

/** 是否是自己管理的群。 */
const isOwner = computed(() =>
  userStore.publicKey.length > 0 && group.value.startsWith(userStore.publicKey))

/** 成员列表。 */
const members = ref<GroupMemberDto[]>([])

/** 成员总数。 */
const total = ref(0)

/** 当前页码，从 1 开始。 */
const pageNum = ref(1)

/** 是否正在加载成员。 */
const loadingMembers = ref(false)

/** 正在删除的成员 ID。 */
const deletingId = ref('')

/** 新成员公钥。 */
const newMember = ref('')

/** 是否正在添加成员。 */
const adding = ref(false)

/** 是否正在更新群密钥。 */
const updating = ref(false)

/** 是否正在重发群密钥。 */
const resending = ref(false)

/** 删了成员但还没更新密钥：此时旧成员仍持有密钥，离开前必须再提示一次。 */
const pendingKeyUpdate = ref(false)

/**
 * 初始化：确认会话存在且自己是群主，然后加载第一页成员。
 */
onMounted(async () => {
  try {
    await userStore.ensureLoaded()
  } catch {
    // 取不到自己的公钥就无法判断群主身份，下面按“不是群主”处理
  }

  try {
    conversation.value = await getConversation(group.value)
    if (conversation.value === null) {
      missing.value = true
      return
    }
    denied.value = !isOwner.value
    if (!denied.value) {
      await reloadMembers()
    }
  } catch (error) {
    showError(error, '加载群聊失败')
  } finally {
    loading.value = false
  }
})

/**
 * 重新加载当前页的成员。
 */
async function reloadMembers(): Promise<void> {
  loadingMembers.value = true
  try {
    const page = await listGroupMembers(group.value, pageNum.value, PAGE_SIZE)
    members.value = page.content
    total.value = page.totalElements
    await resolveAliases(page.content)
  } catch (error) {
    showError(error, '加载成员失败')
  } finally {
    loadingMembers.value = false
  }
}

/**
 * 补齐一批成员的别名。
 *
 * <p>别名由会话 store 统一解析并缓存（先看已加载的会话列表，再按公钥单独查一次），这里只是并发
 * 触发一遍；翻页来回切不会重复请求。</p>
 *
 * @param list 当前页的成员
 */
async function resolveAliases(list: GroupMemberDto[]): Promise<void> {
  await Promise.all(list.map((item) => conversationStore.resolveAlias(item.member)))
}

/**
 * 取成员展示名称。
 *
 * @param member 成员公钥
 * @returns 别名，没有对应会话或没有别名时返回“未命名”
 */
function aliasOf(member: string): string {
  return conversationStore.aliasOf(member) ?? '未命名'
}

/**
 * 切换页码。
 *
 * @param page 目标页码，从 1 开始
 */
async function onPageChange(page: number): Promise<void> {
  pageNum.value = page
  await reloadMembers()
}

/**
 * 添加成员。
 */
async function onAdd(): Promise<void> {
  const member = newMember.value.trim()
  if (member.length === 0) {
    ElMessage.warning('请填写成员公钥')
    return
  }
  if (member === userStore.publicKey) {
    ElMessage.warning('不能把自己加为群成员')
    return
  }
  if (members.value.some((item) => item.member === member)) {
    ElMessage.warning('该成员已经在群聊中')
    return
  }

  adding.value = true
  try {
    await addGroupMember(group.value, member)
    newMember.value = ''
    ElMessage.success('已添加，并把当前群密钥私发给了该成员')
    await reloadMembers()
  } catch (error) {
    // 添加与密钥分发在同一个事务里，失败时成员不会写入，界面不用刷新
    showError(error, '添加成员失败')
  } finally {
    adding.value = false
  }
}

/**
 * 删除成员：确认后删除，成功即进入“待更新密钥”状态并提示更新。
 *
 * @param item 待删除的成员
 */
async function onDelete(item: GroupMemberDto): Promise<void> {
  if (!item.id) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定把该成员移出群聊吗？\n${aliasOf(item.member)}\n${item.member}`,
      '删除成员',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        customClass: 'members__message',
      },
    )
  } catch {
    return
  }

  deletingId.value = item.id
  try {
    await deleteGroupMember(item.id)
    ElMessage.success('已删除成员')
    pendingKeyUpdate.value = true
    await reloadMembers()
    await askUpdateKey()
  } catch (error) {
    showError(error, '删除成员失败')
  } finally {
    deletingId.value = ''
  }
}

/**
 * 删除成员后提示更新群密钥。
 */
async function askUpdateKey(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '被删除的成员仍持有当前群密钥，仍然能收发该群的消息。请立即更新一次群密钥。',
      '需要更新群密钥',
      {
        type: 'warning',
        confirmButtonText: '立即更新',
        cancelButtonText: '稍后',
        closeOnClickModal: false,
        customClass: 'members__message',
      },
    )
  } catch {
    // 用户选择“稍后”，页面上会保留待更新的提示
    return
  }
  await doUpdateKey()
}

/**
 * 更新群密钥：生成新密钥并分发给全部成员，成功后清除待更新状态。
 *
 * @returns 更新成功返回 true
 */
async function doUpdateKey(): Promise<boolean> {
  updating.value = true
  try {
    await updateGroupKey(group.value)
    pendingKeyUpdate.value = false
    ElMessage.success('已更新群密钥，并分发给全部成员')
    return true
  } catch (error) {
    showError(error, '更新群密钥失败')
    return false
  } finally {
    updating.value = false
  }
}

/**
 * 点击“更新群密钥”：没有处于待更新状态时先确认一次。
 */
async function onUpdateKey(): Promise<void> {
  if (!pendingKeyUpdate.value) {
    try {
      await ElMessageBox.confirm(
        '将生成新的群密钥并分发给全部成员，之后的消息用新密钥加密。确定更新吗？',
        '更新群密钥',
        { type: 'warning', confirmButtonText: '更新', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
  }
  await doUpdateKey()
}

/**
 * 重发群密钥：把当前最新的群密钥再分发给全部成员，用于成员漏收。
 */
async function onResendKey(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '把当前最新的群密钥重新私发给全部成员，用于成员漏收密钥。确定重发吗？',
      '重发群密钥',
      { type: 'warning', confirmButtonText: '重发', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  resending.value = true
  try {
    await resendGroupKey(group.value)
    ElMessage.success('已重发群密钥')
  } catch (error) {
    showError(error, '重发群密钥失败')
  } finally {
    resending.value = false
  }
}

/**
 * 复制文本到剪贴板。
 *
 * @param text  待复制内容
 * @param label 成功提示里的名称
 */
async function onCopy(text: string, label: string): Promise<void> {
  const ok = await copyText(text)
  if (ok) {
    ElMessage.success(`已复制${label}`)
  } else {
    ElMessage.error('复制失败，请手动选中复制')
  }
}

/**
 * 返回会话设置页。
 */
function onBack(): void {
  void router.push({ name: 'chat', params: { peer: group.value } })
}

/**
 * 离开页面前提示更新群密钥。
 *
 * <p>三个选择：立即更新并离开（更新失败则留在本页）、仍然离开、关闭弹窗留在本页。</p>
 */
onBeforeRouteLeave(async () => {
  if (!pendingKeyUpdate.value) {
    return true
  }
  try {
    await ElMessageBox.confirm(
      '还没有更新群密钥：被删除的成员仍然能收发该群的消息。建议先更新一次密钥。',
      '尚未更新群密钥',
      {
        type: 'warning',
        confirmButtonText: '立即更新并离开',
        cancelButtonText: '仍然离开',
        distinguishCancelAndClose: true,
        closeOnClickModal: false,
        customClass: 'members__message',
      },
    )
  } catch (action) {
    // 取消 = 仍然离开；关闭弹窗 = 留在本页
    return typeof action === 'string' && action === 'cancel'
  }
  return await doUpdateKey()
})

/**
 * 刷新或关闭标签页时，若还没更新密钥，让浏览器给出原生确认。
 *
 * @param event 卸载事件
 */
function onBeforeUnload(event: BeforeUnloadEvent): void {
  if (!pendingKeyUpdate.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

onMounted(() => {
  window.addEventListener('beforeunload', onBeforeUnload)
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<template>
  <section class="members">
    <header class="members__header">
      <el-button :icon="ArrowLeft" circle size="small" title="返回" @click="onBack" />
      <span class="members__title">群聊管理</span>
    </header>

    <div class="members__body scroll-y">
      <el-empty v-if="missing" description="会话不存在">
        <el-button @click="onBack">返回</el-button>
      </el-empty>

      <el-empty v-else-if="!loading && denied" description="只有群主可以管理该群聊">
        <el-button @click="onBack">返回</el-button>
      </el-empty>

      <el-skeleton v-else-if="loading" :rows="6" animated />

      <template v-else>
        <el-alert v-if="pendingKeyUpdate" class="members__alert" type="warning" :closable="false" show-icon>
          <template #title>尚未更新群密钥</template>
          <div class="members__alert-body">
            <span>被删除的成员仍持有当前群密钥，仍然能收发该群消息。</span>
            <el-button size="small" type="warning" :loading="updating" @click="doUpdateKey">
              立即更新
            </el-button>
          </div>
        </el-alert>

        <div class="members__block">
          <div class="members__label">群标识</div>
          <div class="members__group">
            <span class="members__key">{{ group }}</span>
            <el-button :icon="DocumentCopy" text size="small" title="复制群标识" @click="onCopy(group, '群标识')" />
          </div>
        </div>

        <div class="members__block">
          <div class="members__label">群密钥</div>
          <div class="members__actions">
            <el-button :icon="Refresh" :loading="updating" @click="onUpdateKey">更新群密钥</el-button>
            <el-button :loading="resending" @click="onResendKey">重发群密钥</el-button>
          </div>
          <div class="members__hint">
            更新会生成新的群密钥并分发给全部成员；重发是把当前密钥再发一遍，用于成员漏收。
            新群聊还没有密钥，添加成员前请先更新一次。
          </div>
        </div>

        <div class="members__block">
          <div class="members__label">添加成员</div>
          <div class="members__add">
            <el-input
              v-model="newMember"
              placeholder="成员公钥（RAW HEX）"
              clearable
              @keydown.enter.exact="onAdd"
            />
            <el-button type="primary" :icon="Plus" :loading="adding" @click="onAdd">添加</el-button>
          </div>
          <div class="members__hint">
            添加后会立刻把当前群密钥私聊发给该成员；群还没有密钥时会添加失败，请先在「群密钥」里
            生成一次密钥再加人。
          </div>
        </div>

        <div class="members__block">
          <div class="members__label">群成员（{{ total }}）</div>
          <el-table v-loading="loadingMembers" :data="members" size="small" class="members__table">
            <el-table-column label="名称" width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ aliasOf(row.member) }}</template>
            </el-table-column>
            <el-table-column label="成员公钥" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="members__member">{{ row.member }}</span>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" width="160">
              <template #default="{ row }">{{ row.createTime ? formatTime(row.createTime) : '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="right">
              <template #default="{ row }">
                <el-button
                  :icon="DocumentCopy"
                  text
                  size="small"
                  title="复制公钥"
                  @click="onCopy(row.member, '成员公钥')"
                />
                <el-button
                  :icon="Delete"
                  text
                  size="small"
                  type="danger"
                  :loading="deletingId === row.id"
                  @click="onDelete(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>还没有成员</template>
          </el-table>

          <el-pagination
            v-if="total > PAGE_SIZE"
            class="members__pagination"
            layout="prev, pager, next"
            :current-page="pageNum"
            :page-size="PAGE_SIZE"
            :total="total"
            @current-change="onPageChange"
          />
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.members {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  background: #f5f5f5;
}

.members__header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.members__title {
  font-size: 15px;
  font-weight: 600;
}

.members__body {
  flex: 1;
  min-height: 0;
  padding: 16px;
}

.members__alert {
  margin-bottom: 16px;
}

.members__alert-body {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.members__block {
  max-width: 760px;
  margin-bottom: 20px;
  padding: 14px 16px;
  border-radius: 6px;
  background: #fff;
}

.members__label {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.members__group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.members__key,
.members__member {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.6;
}

/* 群标识完整展示（会换行），表格里的成员公钥单行省略、悬浮看全文 */
.members__key {
  word-break: break-all;
}

.members__member {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.members__actions {
  display: flex;
  gap: 8px;
}

.members__add {
  display: flex;
  gap: 8px;
}

.members__hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.members__table {
  margin-top: 4px;
}

.members__pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
