<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, categoryApi } from '@/api'
import type { Category, Item, Statistics } from '@/types'

const active = ref('review')
const loading = ref(false)
const pending = ref<Item[]>([])
const categories = ref<Category[]>([])
const stats = ref<Statistics>()
const reviewDialog = ref(false)
const selected = ref<Item>()
const reviewForm = reactive<{ result: 'APPROVED' | 'REJECTED'; comment: string }>({ result: 'APPROVED', comment: '' })
const categoryDialog = ref(false)
const categoryForm = reactive({ id: 0, name: '', sortOrder: 0 })

async function loadAll() {
  loading.value = true
  try {
    ;[pending.value, categories.value, stats.value] = await Promise.all([
      adminApi.pendingItems(), categoryApi.list(), adminApi.statistics(),
    ])
  } finally { loading.value = false }
}

function openReview(row: unknown, result: 'APPROVED' | 'REJECTED') {
  const item = row as Item
  selected.value = item
  reviewForm.result = result
  reviewForm.comment = ''
  reviewDialog.value = true
}

async function submitReview() {
  if (!selected.value) return
  await adminApi.review(selected.value.id, reviewForm)
  ElMessage.success('审核完成')
  reviewDialog.value = false
  loadAll()
}

function openCategory(row?: unknown) {
  const category = row as Category | undefined
  Object.assign(categoryForm, category ? { id: category.id, name: category.name, sortOrder: category.sortOrder } : { id: 0, name: '', sortOrder: 0 })
  categoryDialog.value = true
}

async function saveCategory() {
  if (!categoryForm.name.trim()) return ElMessage.warning('请输入分类名称')
  const payload = { name: categoryForm.name, sortOrder: categoryForm.sortOrder }
  if (categoryForm.id) await categoryApi.update(categoryForm.id, payload)
  else await categoryApi.create(payload)
  ElMessage.success('分类已保存')
  categoryDialog.value = false
  loadAll()
}

async function removeCategory(row: unknown) {
  const category = row as Category
  await ElMessageBox.confirm(`确认删除分类“${category.name}”？`, '删除分类', { type: 'warning' })
  await categoryApi.remove(category.id)
  ElMessage.success('分类已删除')
  loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div class="page-container admin-page" v-loading="loading">
    <div class="page-title"><span>ADMINISTRATION</span><h1>管理后台</h1><p>审核信息、维护分类并查看核心业务指标。</p></div>
    <div v-if="stats" class="stat-grid">
      <div><span>注册用户</span><strong>{{ stats.userCount }}</strong></div>
      <div><span>物品信息</span><strong>{{ stats.itemCount }}</strong></div>
      <div><span>待审核</span><strong>{{ stats.pendingCount }}</strong></div>
      <div><span>已认领</span><strong>{{ stats.claimedCount }}</strong></div>
      <div><span>认领成功率</span><strong>{{ stats.claimRate }}%</strong></div>
    </div>
    <el-tabs v-model="active" class="center-tabs">
      <el-tab-pane :label="`信息审核 (${pending.length})`" name="review">
        <el-table :data="pending" empty-text="当前没有待审核信息">
          <el-table-column prop="name" label="物品" min-width="150" />
          <el-table-column prop="type" label="类型" width="90"><template #default="{ row }">{{ row.type === 'LOST' ? '失物' : '招领' }}</template></el-table-column>
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column prop="location" label="地点" min-width="140" />
          <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
          <el-table-column prop="publisherName" label="发布者" width="110" />
          <el-table-column label="操作" width="140" fixed="right"><template #default="{ row }"><el-button text type="success" @click="openReview(row, 'APPROVED')">通过</el-button><el-button text type="danger" @click="openReview(row, 'REJECTED')">拒绝</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="分类管理" name="categories">
        <div class="tab-toolbar"><el-button type="primary" @click="openCategory()">新增分类</el-button></div>
        <el-table :data="categories">
          <el-table-column prop="name" label="分类名称" min-width="180" />
          <el-table-column prop="sortOrder" label="排序" width="120" />
          <el-table-column label="操作" width="150"><template #default="{ row }"><el-button text type="primary" @click="openCategory(row)">编辑</el-button><el-button text type="danger" @click="removeCategory(row)">删除</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="数据概览" name="statistics">
        <el-descriptions v-if="stats" :column="2" border>
          <el-descriptions-item label="已公开信息">{{ stats.publishedCount }}</el-descriptions-item>
          <el-descriptions-item label="认领申请">{{ stats.claimCount }}</el-descriptions-item>
          <el-descriptions-item label="失物信息">{{ stats.lostCount }}</el-descriptions-item>
          <el-descriptions-item label="招领信息">{{ stats.foundCount }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="reviewDialog" :title="reviewForm.result === 'APPROVED' ? '通过信息' : '拒绝信息'" width="500px">
      <p v-if="selected">正在审核：<b>{{ selected.name }}</b></p>
      <el-input v-model="reviewForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写审核意见；拒绝时建议说明原因" />
      <template #footer><el-button @click="reviewDialog = false">取消</el-button><el-button :type="reviewForm.result === 'APPROVED' ? 'success' : 'danger'" @click="submitReview">确认{{ reviewForm.result === 'APPROVED' ? '通过' : '拒绝' }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="categoryDialog" :title="categoryForm.id ? '编辑分类' : '新增分类'" width="460px">
      <el-form label-position="top"><el-form-item label="分类名称"><el-input v-model="categoryForm.name" maxlength="50" /></el-form-item><el-form-item label="排序值"><el-input-number v-model="categoryForm.sortOrder" :min="0" :max="999" /></el-form-item></el-form>
      <template #footer><el-button @click="categoryDialog = false">取消</el-button><el-button type="primary" @click="saveCategory">保存</el-button></template>
    </el-dialog>
  </div>
</template>
