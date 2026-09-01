<template>
  <div class="admin-season-grid">
    <label class="admin-season-field admin-season-field-wide">
      Название сезона
      <input v-model.trim="form.name" type="text" placeholder="Например: 2026/27" :required="isCreate" />
    </label>
    <label class="admin-season-field">
      Статус сезона
      <select v-model="form.status">
        <option value="ACTIVE">Активный</option>
        <option value="DRAFT">Черновик</option>
        <option value="CLOSED">Закрыт</option>
      </select>
    </label>
    <p class="muted-text admin-season-field-wide">Формат, заявки, трансферы и дисциплина задаются отдельно внутри чемпионата или Кубка.</p>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  isCreate: { type: Boolean, default: false },
  playoffTeamOptions: { type: Array, required: true },
})
</script>
