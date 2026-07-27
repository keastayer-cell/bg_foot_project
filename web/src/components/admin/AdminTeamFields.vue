<template>
  <div :class="{ 'admin-team-identity-grid': !compact }">
    <label>
      Название команды
      <input v-model.trim="form.name" type="text" required />
    </label>
    <label>
      Короткое название
      <input v-model.trim="form.shortName" type="text" required />
    </label>
    <label>
      Город
      <input v-model.trim="form.city" type="text" required />
    </label>
    <label :class="{ 'admin-team-logo-field': !compact }">
      Лого команды
      <input type="file" accept="image/*" @change="$emit('logo-selected', $event)" />
    </label>
    <div v-if="form.logoDataUrl && !compact" class="admin-team-logo-preview-wrap">
      <img :src="form.logoDataUrl" alt="Превью лого команды" class="admin-team-logo-preview" />
    </div>
    <img
      v-else-if="form.logoDataUrl"
      :src="form.logoDataUrl"
      alt="Превью лого команды"
      class="admin-team-logo-preview"
    />
  </div>
</template>

<script setup>
defineProps({
  compact: { type: Boolean, default: false },
  form: { type: Object, required: true },
})

defineEmits(['logo-selected'])
</script>
