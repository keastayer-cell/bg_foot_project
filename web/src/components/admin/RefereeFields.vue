<template>
  <div class="admin-fields-grid">
    <label class="admin-field-wide">ФИО<input v-model.trim="form.fullName" type="text" :required="requiredName" /></label>
    <label>Город<input v-model.trim="form.city" type="text" placeholder="Например: Богородск" /></label>
    <label>Дата рождения<input v-model="form.birthDate" type="date" class="admin-temporal-input" /></label>
    <label class="admin-field-wide">Фото судьи<input type="file" accept="image/*" @change="$emit('photo-selected', $event)" /></label>
    <img v-if="form.photoDataUrl" :src="form.photoDataUrl" alt="Превью фото судьи" class="team-rep-player-photo-preview" />
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  requiredName: { type: Boolean, default: false },
})
defineEmits(['photo-selected'])
</script>
