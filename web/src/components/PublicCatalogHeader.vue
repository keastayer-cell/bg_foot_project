<template>
  <header class="catalog-header">
    <div>
      <p class="catalog-kicker">Футбольная лига · Богородск</p>
      <h1 class="section-title">{{ title }}</h1>
      <p class="catalog-description">{{ description }}</p>
    </div>
    <div v-if="count !== null" class="catalog-total"><strong>{{ count }}</strong><span>{{ countLabel }}</span></div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  count: { type: Number, default: null },
  countLabel: { type: String, default: '' },
})
</script>
