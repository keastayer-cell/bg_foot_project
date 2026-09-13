<template>
  <button v-if="isAuthenticated" class="btn-ghost favorite-button" :class="{ 'is-favorite': active }" type="button" :disabled="loading" @click="toggle">
    {{ active ? '★ В избранном' : '☆ В избранное' }}
  </button>
</template>
<script setup>
import { onMounted, ref, watch } from 'vue'
import { useAuth } from '../store/auth'
const props = defineProps({ type: { type: String, required: true }, targetId: { type: [String, Number], required: true } })
const { isAuthenticated, loadFavorites, addFavorite, removeFavorite } = useAuth()
const active = ref(false)
const loading = ref(false)
async function refresh() {
  if (!isAuthenticated.value || !props.targetId) return
  const items = await loadFavorites()
  active.value = items.some((item) => item.type === props.type && String(item.targetId) === String(props.targetId))
}
async function toggle() {
  loading.value = true
  try {
    if (active.value) await removeFavorite(props.type, props.targetId)
    else await addFavorite(props.type, props.targetId)
    active.value = !active.value
  } finally { loading.value = false }
}
onMounted(() => void refresh())
watch(() => props.targetId, () => void refresh())
</script>
<style scoped>
.favorite-button.is-favorite{border-color:rgba(255,205,84,.5);background:rgba(255,205,84,.1);color:#ffd76a}
</style>
