<template><nav class="breadcrumbs" aria-label="Навигационная цепочка"><RouterLink to="/">Главная</RouterLink><template v-for="item in items" :key="item.label"><span>›</span><RouterLink v-if="item.to" :to="item.to">{{ item.label }}</RouterLink><b v-else>{{ item.label }}</b></template></nav></template>
<script setup>defineProps({ items: { type: Array, default: () => [] } })</script>
<style scoped>.breadcrumbs{display:flex;align-items:center;flex-wrap:wrap;gap:8px;color:var(--muted);font-size:.75rem}.breadcrumbs a:hover{color:var(--brand)}.breadcrumbs b{color:var(--text)}</style>
