<template>
  <div ref="root" class="global-search" :class="{ 'is-open': open }">
    <label class="global-search-field">
      <span aria-hidden="true">⌕</span>
      <input
        v-model.trim="query"
        type="search"
        autocomplete="off"
        aria-label="Поиск по сайту"
        placeholder="Игрок, команда, матч..."
        @focus="open = query.length >= 2"
        @keydown.esc="open = false"
      />
    </label>
    <div v-if="open" class="global-search-results">
      <p v-if="loading" class="global-search-state">Ищем…</p>
      <p v-else-if="error" class="global-search-state error-text">{{ error }}</p>
      <template v-else-if="groups.length">
        <section v-for="group in groups" :key="group.key">
          <h3>{{ group.label }}</h3>
          <RouterLink v-for="item in group.items" :key="`${group.key}-${item.id}`" :to="item.url" @click="close">
            <strong>{{ item.title }}</strong><small>{{ item.subtitle }}</small>
          </RouterLink>
        </section>
      </template>
      <p v-else class="global-search-state">Ничего не найдено.</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useAuth } from '../store/auth'
import { createCatalogApi } from '../api/catalog'

const { optionalAuthApiRequest } = useAuth()
const api = createCatalogApi(optionalAuthApiRequest)
const root = ref(null)
const query = ref('')
const payload = ref({})
const open = ref(false)
const loading = ref(false)
const error = ref('')
let timer
let requestSequence = 0

const definitions = [
  ['players', 'Игроки'], ['teams', 'Команды'], ['matches', 'Матчи'], ['competitions', 'Турниры'],
]
const groups = computed(() => definitions
  .map(([key, label]) => ({ key, label, items: payload.value?.[key] || [] }))
  .filter((group) => group.items.length))

watch(query, (value) => {
  clearTimeout(timer)
  error.value = ''
  if (value.length < 2) { open.value = false; payload.value = {}; return }
  open.value = true
  timer = setTimeout(() => runSearch(value), 250)
})

async function runSearch(value) {
  const sequence = ++requestSequence
  loading.value = true
  try {
    const result = await api.search(value, 5)
    if (sequence === requestSequence) payload.value = result
  } catch (requestError) {
    if (sequence === requestSequence) error.value = requestError.message || 'Поиск временно недоступен.'
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

function close() { open.value = false }
function onDocumentClick(event) { if (root.value && !root.value.contains(event.target)) close() }
onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => { clearTimeout(timer); document.removeEventListener('click', onDocumentClick) })
</script>

<style scoped>
.global-search{position:relative;width:min(330px,28vw);z-index:30}.global-search-field{display:flex;align-items:center;gap:8px;height:38px;padding:0 12px;border:1px solid rgba(124,163,255,.22);border-radius:12px;background:rgba(7,13,31,.68)}.global-search-field:focus-within{border-color:rgba(97,232,162,.55);box-shadow:0 0 0 3px rgba(97,232,162,.08)}.global-search-field>span{color:var(--brand);font-size:1.1rem}.global-search-field input{width:100%;height:auto;padding:0;border:0;background:transparent;font-size:.74rem;outline:0}.global-search-results{position:absolute;top:calc(100% + 9px);right:0;width:min(420px,calc(100vw - 24px));max-height:min(70vh,560px);overflow:auto;padding:10px;border:1px solid rgba(124,163,255,.28);border-radius:14px;background:rgba(7,13,31,.98);box-shadow:0 20px 50px rgba(0,0,0,.42)}.global-search-results section+section{margin-top:10px;padding-top:10px;border-top:1px solid var(--line)}.global-search-results h3{margin:0 8px 5px;color:var(--brand);font-size:.58rem;letter-spacing:.09em;text-transform:uppercase}.global-search-results a{display:grid;gap:3px;padding:9px;border-radius:9px}.global-search-results a:hover{background:rgba(97,232,162,.08)}.global-search-results strong{font-size:.75rem}.global-search-results small,.global-search-state{color:var(--muted);font-size:.64rem}.global-search-state{margin:0;padding:12px}
@media(max-width:900px){.global-search{order:3;width:100%}.global-search-results{left:0;right:auto;width:100%}}
</style>

