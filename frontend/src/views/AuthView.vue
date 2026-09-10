<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({ username: '', password: '', contact: '' })
const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度应为 3 到 30 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度应为 6 到 64 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  if (!(await formRef.value?.validate())) return
  loading.value = true
  try {
    if (mode.value === 'login') await auth.login(form.username, form.password)
    else await auth.register(form.username, form.password, form.contact)
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    router.push(String(route.query.redirect || (auth.isAdmin ? '/admin' : '/')))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-layout">
    <section class="auth-intro">
      <span class="eyebrow">CAMPUS LOST & FOUND</span>
      <h1>把线索汇聚起来，<br />让遗失有回应。</h1>
      <p>公开信息可直接浏览。登录后可以发布信息、提交认领申请并跟踪处理结果。</p>
      <div class="demo-account">管理员演示账号：<b>admin</b> / <b>admin123</b></div>
    </section>
    <el-card class="auth-card" shadow="never">
      <div class="auth-tabs">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" size="large" placeholder="3–30 位字母、数字或下划线" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" show-password placeholder="至少 6 位" @keyup.enter="submit" /></el-form-item>
        <el-form-item v-if="mode === 'register'" label="联系方式"><el-input v-model="form.contact" size="large" placeholder="邮箱、手机号或其他联系方式" /></el-form-item>
        <el-button type="primary" size="large" native-type="submit" :loading="loading" class="full-button">
          {{ mode === 'login' ? '登录' : '创建账号并登录' }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>
