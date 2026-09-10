<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="site-header">
      <div class="nav-wrap">
        <router-link to="/" class="brand">
          <span class="brand-mark">拾</span>
          <span><strong>拾光</strong><small>校园失物招领</small></span>
        </router-link>
        <nav>
          <router-link to="/">信息广场</router-link>
          <router-link v-if="auth.isLoggedIn" to="/publish">发布信息</router-link>
          <router-link v-if="auth.isLoggedIn" to="/center">个人中心</router-link>
          <router-link v-if="auth.isAdmin" to="/admin">管理后台</router-link>
        </nav>
        <div class="account">
          <template v-if="auth.isLoggedIn">
            <span class="username">{{ auth.user?.username }}</span>
            <el-button text @click="logout">退出</el-button>
          </template>
          <el-button v-else type="primary" round @click="router.push('/auth')">登录 / 注册</el-button>
        </div>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view />
    </el-main>
    <el-footer class="site-footer">校园失物招领系统 · 软件度量课程项目</el-footer>
  </el-container>
</template>
