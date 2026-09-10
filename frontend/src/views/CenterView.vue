<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { claimApi, itemApi, notificationApi } from '@/api'
import type { Claim, Item, Notification } from '@/types'

const active = ref('items')
const items = ref<Item[]>([])
const applications = ref<Claim[]>([])
const received = ref<Claim[]>([])
const notifications = ref<Notification[]>([])
const loading = ref(false)

const statusText: Record<string, string> = {
  PENDING: '待处理', PUBLISHED: '已公开', REJECTED: '未通过', CLAIMED: '已认领', CLOSED: '已关闭', APPROVED: '已通过',
}
const statusType = (status: string) => status === 'APPROVED' || status === 'PUBLISHED' || status === 'CLAIMED' ? 'success' : status === 'REJECTED' ? 'danger' : 'warning'

async function loadAll() {
  loading.value = true
  try {
    ;[items.value, applications.value, received.value, notifications.value] = await Promise.all([
      itemApi.mine(), claimApi.mine(), claimApi.received(), notificationApi.list(),
    ])
  } finally { loading.value = false }
}

async function removeItem(row: unknown) {
  const item = row as Item
  await ElMessageBox.confirm(`确认删除“${item.name}”？`, '删除发布', { type: 'warning' })
  await itemApi.remove(item.id)
  ElMessage.success('已删除')
  loadAll()
}

async function closeItem(row: unknown) {
  const item = row as Item
  await itemApi.close(item.id)
  ElMessage.success('信息已关闭')
  loadAll()
}

async function decide(row: unknown, result: 'APPROVED' | 'REJECTED') {
  const claim = row as Claim
  await ElMessageBox.confirm(result === 'APPROVED' ? '通过后物品将标记为已认领，是否继续？' : '确认拒绝该认领申请？', '处理申请', { type: 'warning' })
  await claimApi.decide(claim.id, result)
  ElMessage.success('处理完成')
  loadAll()
}

async function markAllRead() {
  await notificationApi.readAll()
  notifications.value.forEach((item) => { item.isRead = true })
}

onMounted(loadAll)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="page-title"><span>MY ACCOUNT</span><h1>个人中心</h1><p>管理发布记录、认领申请和系统通知。</p></div>
    <el-tabs v-model="active" class="center-tabs">
      <el-tab-pane label="我的发布" name="items">
        <el-table :data="items" empty-text="还没有发布记录">
          <el-table-column prop="name" label="物品" min-width="180"><template #default="{ row }"><router-link :to="`/items/${row.id}`" class="table-link">{{ row.name }}</router-link></template></el-table-column>
          <el-table-column prop="type" label="类型" width="100"><template #default="{ row }">{{ row.type === 'LOST' ? '失物' : '招领' }}</template></el-table-column>
          <el-table-column prop="categoryName" label="分类" width="120" />
          <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText[row.status] }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="220" fixed="right"><template #default="{ row }"><el-button text type="primary" :disabled="['CLAIMED','CLOSED'].includes(row.status)" @click="$router.push(`/publish/${row.id}`)">编辑</el-button><el-button text :disabled="['CLAIMED','CLOSED'].includes(row.status)" @click="closeItem(row)">关闭</el-button><el-button text type="danger" :disabled="row.status === 'CLAIMED'" @click="removeItem(row)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="我的认领" name="applications">
        <el-table :data="applications" empty-text="还没有提交认领申请">
          <el-table-column prop="itemName" label="物品" min-width="180" />
          <el-table-column prop="description" label="认领说明" min-width="240" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="申请时间" width="180"><template #default="{ row }">{{ row.createdAt.replace('T', ' ') }}</template></el-table-column>
          <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText[row.status] }}</el-tag></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="收到的认领" name="received">
        <el-table :data="received" empty-text="还没有收到认领申请">
          <el-table-column prop="itemName" label="物品" min-width="150" />
          <el-table-column prop="applicantName" label="申请人" width="120" />
          <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column prop="proof" label="验证信息" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusText[row.status] }}</el-tag></template></el-table-column>
          <el-table-column label="处理" width="150"><template #default="{ row }"><template v-if="row.status === 'PENDING'"><el-button text type="success" @click="decide(row, 'APPROVED')">通过</el-button><el-button text type="danger" @click="decide(row, 'REJECTED')">拒绝</el-button></template><span v-else>已处理</span></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane name="notifications"><template #label>通知 <el-badge v-if="notifications.some(n => !n.isRead)" is-dot /></template>
        <div class="tab-toolbar"><el-button text type="primary" @click="markAllRead">全部标为已读</el-button></div>
        <div class="notification-list">
          <div v-for="notice in notifications" :key="notice.id" :class="['notification-item', { unread: !notice.isRead }]">
            <i></i><div><p>{{ notice.content }}</p><time>{{ notice.createdAt.replace('T', ' ') }}</time></div>
          </div>
          <el-empty v-if="!notifications.length" description="暂无通知" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
