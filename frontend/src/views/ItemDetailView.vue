<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { itemApi, claimApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { Item } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const item = ref<Item>()
const loading = ref(true)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const claim = reactive({ description: '', proof: '' })
const rules: FormRules = {
  description: [{ required: true, message: '请填写认领说明', trigger: 'blur' }],
  proof: [{ required: true, message: '请填写只有失主知道的验证信息', trigger: 'blur' }],
}

async function openClaim() {
  if (!auth.isLoggedIn) return router.push({ path: '/auth', query: { redirect: route.fullPath } })
  dialogVisible.value = true
}

async function submitClaim() {
  if (!(await formRef.value?.validate()) || !item.value) return
  await claimApi.create(item.value.id, claim)
  dialogVisible.value = false
  ElMessage.success('认领申请已提交，请等待发布者处理')
}

onMounted(async () => {
  try { item.value = await itemApi.detail(Number(route.params.id)) } finally { loading.value = false }
})
</script>

<template>
  <div v-loading="loading" class="detail-page">
    <template v-if="item">
      <button class="back-link" @click="router.back()">← 返回信息广场</button>
      <div class="detail-layout">
        <div class="detail-image" :class="item.type.toLowerCase()">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
          <span v-else>{{ item.categoryName.slice(0, 1) }}</span>
        </div>
        <section class="detail-content">
          <div class="detail-tags"><el-tag :type="item.type === 'LOST' ? 'danger' : 'success'" effect="dark">{{ item.type === 'LOST' ? '寻找失物' : '招领启事' }}</el-tag><el-tag effect="plain">{{ item.categoryName }}</el-tag></div>
          <h1>{{ item.name }}</h1>
          <p class="detail-description">{{ item.description }}</p>
          <dl class="detail-list">
            <div><dt>地点</dt><dd>{{ item.location }}</dd></div>
            <div><dt>时间</dt><dd>{{ item.eventTime.replace('T', ' ') }}</dd></div>
            <div><dt>发布者</dt><dd>{{ item.publisherName }}</dd></div>
            <div><dt>联系方式</dt><dd>{{ item.contact || '提交认领申请后与发布者沟通' }}</dd></div>
          </dl>
          <el-button v-if="item.status === 'PUBLISHED' && auth.user?.id !== item.publisherId" type="primary" size="large" @click="openClaim">申请认领</el-button>
          <el-tag v-else-if="item.status !== 'PUBLISHED'" size="large">当前状态：{{ item.status }}</el-tag>
        </section>
      </div>
    </template>

    <el-dialog v-model="dialogVisible" title="提交认领申请" width="520px">
      <el-form ref="formRef" :model="claim" :rules="rules" label-position="top">
        <el-form-item label="认领说明" prop="description"><el-input v-model="claim.description" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明你与该物品的关系" /></el-form-item>
        <el-form-item label="验证信息" prop="proof"><el-input v-model="claim.proof" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="填写未公开的颜色、划痕、内容物等细节" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitClaim">提交申请</el-button></template>
    </el-dialog>
  </div>
</template>
