<template>
  <div class="admin-fields-grid">
    <label class="admin-field-wide">ФИО<input v-model.trim="form.fullName" type="text" :required="requiredFields" /></label>
    <label>Дата рождения<input v-model="form.birthDate" type="date" class="admin-temporal-input" :required="requiredFields" /></label>
    <label>Прописка<input v-model.trim="form.residence" type="text" placeholder="Город или населённый пункт" :required="requiredFields" /></label>
    <label>
      Позиция <span class="player-field-optional">необязательно</span>
      <select v-model="form.position">
        <option value="">— не указана —</option>
        <option value="GOALKEEPER">Вратарь</option>
        <option value="DEFENDER">Защитник</option>
        <option value="MIDFIELDER">Полузащитник</option>
        <option value="FORWARD">Нападающий</option>
      </select>
    </label>
    <label>Фото игрока<input type="file" accept="image/*" @change="$emit('photo-selected', $event)" /></label>
    <img v-if="form.photoDataUrl" :src="form.photoDataUrl" alt="Превью фото игрока" class="team-rep-player-photo-preview" />
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  requiredFields: { type: Boolean, default: false },
})
defineEmits(['photo-selected'])
</script>

<style scoped>
.player-field-optional {
  margin-left: 5px;
  color: var(--muted);
  font-size: 0.7rem;
  font-weight: 500;
}
</style>
