<script setup lang="ts">
import type { Item } from '@/types'

defineProps<{ item: Item }>()

const typeText = { LOST: '寻找失物', FOUND: '招领启事' }
</script>

<template>
  <article class="item-card" @click="$router.push(`/items/${item.id}`)">
    <div class="item-cover" :class="item.type.toLowerCase()">
      <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" />
      <span v-else>{{ item.categoryName.slice(0, 1) }}</span>
      <em>{{ typeText[item.type] }}</em>
    </div>
    <div class="item-card-body">
      <h3>{{ item.name }}</h3>
      <p class="location">{{ item.location }}</p>
      <p class="description">{{ item.description }}</p>
      <div class="card-meta">
        <span>{{ item.categoryName }}</span>
        <time>{{ item.eventTime.slice(0, 10) }}</time>
      </div>
    </div>
  </article>
</template>
