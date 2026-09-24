<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { createNote, deleteNote, fetchNotes, fetchStatus } from './api'

const status = ref({ mysql: 'UNKNOWN', redis: 'UNKNOWN', pod: '…' })
const notes = ref([])
const cached = ref(false)
const ttlSeconds = ref(30)
const author = ref('')
const content = ref('')
const error = ref('')
const submitting = ref(false)
let timer

function formatTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function loadStatus() {
  try {
    status.value = await fetchStatus()
  } catch {
    status.value = { mysql: 'DOWN', redis: 'DOWN', pod: '不可达' }
  }
}

async function loadNotes() {
  try {
    const data = await fetchNotes()
    notes.value = data.notes
    cached.value = data.cached
    ttlSeconds.value = data.ttlSeconds
    error.value = ''
  } catch (err) {
    error.value = err.message
  }
}

async function refresh() {
  await Promise.all([loadStatus(), loadNotes()])
}

async function submit() {
  submitting.value = true
  error.value = ''
  try {
    await createNote({ author: author.value, content: content.value })
    content.value = ''
    await loadNotes()
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}

async function remove(id) {
  error.value = ''
  try {
    await deleteNote(id)
    await loadNotes()
  } catch (err) {
    error.value = err.message
  }
}

onMounted(() => {
  refresh()
  timer = setInterval(refresh, 5000)
})

onUnmounted(() => clearInterval(timer))
</script>

<template>
  <main class="page">
    <header class="hero">
      <div>
        <p class="eyebrow">Kubernetes Practice</p>
        <h1>留言板</h1>
        <p class="lede">留言写入 MySQL，列表在 Redis 中缓存 {{ ttlSeconds }} 秒。刷新页面会打到不同的后端 Pod。</p>
      </div>
      <div class="pod">Pod {{ status.pod }}</div>
    </header>

    <section class="panel">
      <div class="status-row">
        <span class="pill"><i class="dot" :class="status.mysql === 'UP' ? 'up' : 'down'"></i>MySQL {{ status.mysql }}</span>
        <span class="pill"><i class="dot" :class="status.redis === 'UP' ? 'up' : 'down'"></i>Redis {{ status.redis }}</span>
        <span class="badge" :class="{ cache: cached }">{{ cached ? 'Redis 缓存' : 'MySQL' }}</span>
      </div>
    </section>

    <section class="panel">
      <div class="composer">
        <input v-model="author" class="author" maxlength="40" placeholder="昵称，可空" />
        <textarea v-model="content" maxlength="200" placeholder="写一条留言"></textarea>
      </div>
      <div class="actions">
        <button class="ghost" type="button" @click="refresh">刷新</button>
        <button class="primary" type="button" :disabled="submitting || !content.trim()" @click="submit">
          {{ submitting ? '发布中…' : '发布' }}
        </button>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </section>

    <section class="panel">
      <p v-if="notes.length === 0" class="empty">还没有留言。发布第一条后，下一次刷新应显示“Redis 缓存”。</p>
      <article v-for="note in notes" :key="note.id" class="note">
        <div class="note-head">
          <strong>{{ note.author }}</strong>
          <span class="meta">{{ formatTime(note.createdAt) }}</span>
          <button class="danger" type="button" @click="remove(note.id)">删除</button>
        </div>
        <p>{{ note.content }}</p>
      </article>
    </section>
  </main>
</template>
