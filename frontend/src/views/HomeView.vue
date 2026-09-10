<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { categoryApi, itemApi } from '@/api'
import ItemCard from '@/components/ItemCard.vue'
import type { Category, Item } from '@/types'

const loading = ref(false)
const categories = ref<Category[]>([])
const items = ref<Item[]>([])
const total = ref(0)
const filters = reactive({ keyword: '', type: '', categoryId: undefined as number | undefined, location: '', page: 1, size: 12 })

async function loadItems() {
  loading.value = true
  try {
    const result = await itemApi.list(filters)
    items.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadItems()
}

function reset() {
  Object.assign(filters, { keyword: '', type: '', categoryId: undefined, location: '', page: 1 })
  loadItems()
}

onMounted(async () => {
  categories.value = await categoryApi.list()
  await loadItems()
})
</script>

<template>
  <div>
    <section class="hero">
      <div>
        <span class="eyebrow">让每一次遗失，都有被找回的可能</span>
        <h1>在校园里，<br /><strong>找回重要的那一件。</strong></h1>
        <p>集中发布、快速筛选、安全认领，让失物信息不再沉没在聊天记录里。</p>
      </div>
      <div class="hero-stats">
        <div><strong>{{ total }}</strong><span>条公开信息</span></div>
        <div><strong>6</strong><span>类常见物品</span></div>
      </div>
    </section>

    <section class="search-panel">
      <el-input v-model="filters.keyword" clearable placeholder="搜索物品名称或特征" @keyup.enter="search" />
      <el-select v-model="filters.type" clearable placeholder="信息类型">
        <el-option label="寻找失物" value="LOST" />
        <el-option label="招领启事" value="FOUND" />
      </el-select>
      <el-select v-model="filters.categoryId" clearable placeholder="物品分类">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-input v-model="filters.location" clearable placeholder="地点" @keyup.enter="search" />
      <el-button type="primary" @click="search">查找</el-button>
      <el-button @click="reset">重置</el-button>
    </section>

    <div class="section-heading">
      <div><span>INFORMATION</span><h2>最新信息</h2></div>
      <el-button v-if="$route.path === '/'" text type="primary" @click="$router.push('/publish')">发布一条信息 →</el-button>
    </div>

    <div v-loading="loading" class="item-grid">
      <ItemCard v-for="item in items" :key="item.id" :item="item" />
      <el-empty v-if="!loading && !items.length" description="暂时没有符合条件的信息" />
    </div>
    <el-pagination
      v-if="total > filters.size"
      v-model:current-page="filters.page"
      :page-size="filters.size"
      :total="total"
      layout="prev, pager, next"
      class="pagination"
      @current-change="loadItems"
    />
  </div>
</template>
