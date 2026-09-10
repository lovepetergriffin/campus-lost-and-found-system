<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi, itemApi } from '@/api'
import type { Category, ItemPayload } from '@/types'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const categories = ref<Category[]>([])
const loading = ref(false)
const editingId = Number(route.params.id) || undefined
const form = reactive<ItemPayload>({
  name: '', type: 'LOST', categoryId: undefined, location: '', eventTime: '',
  description: '', imageUrl: '', contact: '',
})
const rules: FormRules = {
  name: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择信息类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  location: [{ required: true, message: '请输入地点', trigger: 'blur' }],
  eventTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  description: [
    { required: true, message: '请描述物品特征', trigger: 'blur' },
    { max: 500, message: '描述不能超过 500 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  if (!(await formRef.value?.validate())) return
  loading.value = true
  try {
    if (editingId) await itemApi.update(editingId, form)
    else await itemApi.create(form)
    ElMessage.success(editingId ? '修改成功，已重新进入审核' : '发布成功，请等待管理员审核')
    router.push('/center')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  categories.value = await categoryApi.list()
  if (editingId) {
    const item = await itemApi.detail(editingId)
    Object.assign(form, {
      name: item.name, type: item.type, categoryId: item.categoryId, location: item.location,
      eventTime: item.eventTime, description: item.description, imageUrl: item.imageUrl || '', contact: item.contact || '',
    })
  }
})
</script>

<template>
  <div class="narrow-page">
    <div class="page-title"><span>POST INFORMATION</span><h1>{{ editingId ? '编辑发布' : '发布信息' }}</h1><p>请尽量填写准确的时间、地点和特征，提交后由管理员审核公开。</p></div>
    <el-card shadow="never" class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <div class="form-row">
          <el-form-item label="信息类型" prop="type">
            <el-radio-group v-model="form.type" size="large">
              <el-radio-button value="LOST">我丢了物品</el-radio-button>
              <el-radio-button value="FOUND">我捡到物品</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="物品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择" size="large">
              <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="物品名称" prop="name"><el-input v-model="form.name" size="large" maxlength="100" show-word-limit /></el-form-item>
        <div class="form-row">
          <el-form-item label="丢失 / 拾取地点" prop="location"><el-input v-model="form.location" size="large" maxlength="200" /></el-form-item>
          <el-form-item label="丢失 / 拾取时间" prop="eventTime"><el-date-picker v-model="form.eventTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" size="large" placeholder="选择日期时间" /></el-form-item>
        </div>
        <el-form-item label="物品特征与经过" prop="description"><el-input v-model="form.description" type="textarea" :rows="5" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item label="图片链接"><el-input v-model="form.imageUrl" size="large" placeholder="可选：填写可公开访问的图片 URL" /></el-form-item>
        <el-form-item label="联系方式"><el-input v-model="form.contact" size="large" placeholder="可选：仅在详情页展示" /></el-form-item>
        <div class="form-actions"><el-button size="large" @click="router.back()">取消</el-button><el-button type="primary" size="large" native-type="submit" :loading="loading">提交审核</el-button></div>
      </el-form>
    </el-card>
  </div>
</template>
