<template>
  <div class="admin-season-grid">
    <label class="admin-season-field admin-season-field-wide">
      Название сезона
      <input v-model.trim="form.name" type="text" placeholder="Например: 2026/27" :required="isCreate" />
    </label>
    <label class="admin-season-field">
      Количество кругов
      <select v-model="form.roundsCount">
        <option v-for="count in 4" :key="count" :value="String(count)">{{ count }} круг{{ count === 1 ? '' : count < 5 ? 'а' : 'ов' }}</option>
      </select>
    </label>
    <label v-if="!isCreate" class="admin-season-field">
      Статус сезона
      <select v-model="form.status">
        <option value="ACTIVE">Активный</option>
        <option value="DRAFT">Черновик</option>
        <option value="CLOSED">Закрыт</option>
      </select>
    </label>
    <label class="admin-season-field">
      Дедлайн заявки
      <input v-model="form.applicationDeadline" type="date" />
    </label>
    <label class="admin-season-field">
      Лимит игроков в заявке
      <input v-model="form.maxRosterSize" type="number" min="1" placeholder="Пусто = без лимита" />
    </label>
    <label class="admin-season-field">
      Старт окна трансферов
      <input v-model="form.transferWindowStartDate" type="date" />
    </label>
    <label class="admin-season-field">
      Конец окна трансферов
      <input v-model="form.transferWindowEndDate" type="date" />
    </label>
    <label class="admin-season-field">
      Пропуск за ЖК
      <input v-model="form.yellowCardsForSuspension" type="number" min="0" placeholder="0 = выключено" />
    </label>
    <label class="admin-season-field">
      Пропуск за КК
      <input v-model="form.redCardsForSuspension" type="number" min="0" placeholder="0 = выключено" />
    </label>
    <label :class="['admin-season-field', 'admin-season-toggle-field', { 'admin-season-field-wide': !form.playoffEnabled }]">
      Плей-офф
      <span class="admin-season-toggle-control">
        <input v-model="form.playoffEnabled" type="checkbox" />
        <span>Включить плей-офф</span>
      </span>
    </label>
    <label v-if="form.playoffEnabled" class="admin-season-field">
      Команд в плей-офф
      <select v-model="form.playoffTeamCount">
        <option value="">— выберите —</option>
        <option v-for="count in playoffTeamOptions" :key="count" :value="String(count)">{{ count }}</option>
      </select>
    </label>
    <label v-if="form.playoffEnabled && Number(form.playoffTeamCount || 0) >= 4" class="admin-season-field admin-season-toggle-field">
      Матч за 3 место
      <span class="admin-season-toggle-control">
        <input v-model="form.thirdPlaceEnabled" type="checkbox" />
        <span>Добавить бронзовый матч</span>
      </span>
    </label>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  isCreate: { type: Boolean, default: false },
  playoffTeamOptions: { type: Array, required: true },
})
</script>
