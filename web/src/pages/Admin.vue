<template>
  <section class="section-wrap admin-hub">
    <article class="card admin-hub-header">
      <h2 class="section-title">Админ-панель</h2>
      <p class="muted-text">Управление турниром, участниками и правами доступа из одного экрана.</p>
    </article>

    <article class="card admin-tabs-wrap">
      <div class="admin-tab-groups">
        <section v-for="group in visibleTabGroups" :key="group.id" class="admin-tab-group">
          <div class="admin-tab-group-head">
            <span class="admin-tab-group-kicker">{{ group.kicker }}</span>
            <h3 class="admin-tab-group-title">{{ group.title }}</h3>
          </div>
          <div class="admin-tabs admin-tabs-grid">
            <button
              v-for="tab in group.items"
              :key="tab.id"
              class="btn-ghost admin-tab-btn"
              :class="{ 'admin-tab-active': activeTab === tab.id }"
              type="button"
              @click="activeTab = tab.id"
            >
              <span class="admin-tab-label">{{ tab.label }}</span>
            </button>
          </div>
        </section>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'seasons'">
      <div class="admin-panel-head">
        <h3 class="section-title">Сезоны и регламент</h3>
        <p class="muted-text">Создание сезона, настройка регламента и состава участников.</p>
      </div>
      <div class="admin-inline-message" v-if="messageError || messageOk">
        <p class="error-text" v-if="messageError">{{ messageError }}</p>
        <p class="success-text" v-if="messageOk">{{ messageOk }}</p>
      </div>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': seasonSubMode === 'create' }"
          type="button"
          @click="seasonSubMode = 'create'; cancelEditSeason(); seasonEditSelectId = ''"
        >Создать сезон</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': seasonSubMode === 'edit' }"
          type="button"
          @click="seasonSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <div class="admin-form admin-surface">
        <form v-if="seasonSubMode === 'create'" class="admin-form admin-season-form" @submit.prevent="createSeason">
          <div class="admin-season-grid">
            <label class="admin-season-field admin-season-field-wide">
              Название сезона
              <input v-model.trim="seasonForm.name" type="text" placeholder="Например: 2026/27" required />
            </label>
            <label class="admin-season-field">
              Количество кругов
              <select v-model="seasonForm.roundsCount">
                <option value="1">1 круг</option>
                <option value="2">2 круга</option>
                <option value="3">3 круга</option>
                <option value="4">4 круга</option>
              </select>
            </label>
            <label class="admin-season-field">
              Дедлайн заявки
              <input v-model="seasonForm.applicationDeadline" type="date" />
            </label>
            <label class="admin-season-field">
              Пропуск за ЖК
              <input v-model="seasonForm.yellowCardsForSuspension" type="number" min="0" placeholder="0 = выключено" />
            </label>
            <label class="admin-season-field">
              Пропуск за КК
              <input v-model="seasonForm.redCardsForSuspension" type="number" min="0" placeholder="0 = выключено" />
            </label>
            <label :class="['admin-season-field', 'admin-season-toggle-field', { 'admin-season-field-wide': !seasonForm.playoffEnabled }]">
              Плей-офф
              <span class="admin-season-toggle-control">
                <input v-model="seasonForm.playoffEnabled" type="checkbox" />
                <span>Включить плей-офф</span>
              </span>
            </label>
            <label v-if="seasonForm.playoffEnabled" class="admin-season-field">
              Команд в плей-офф
              <select v-model="seasonForm.playoffTeamCount">
                <option value="">— выберите —</option>
                <option v-for="count in playoffTeamOptions" :key="`create-playoff-${count}`" :value="String(count)">{{ count }}</option>
              </select>
            </label>
          </div>

          <section class="admin-season-section">
            <div class="admin-season-section-head">
              <div class="admin-season-section-copy">
                <h4 class="admin-list-title">Команды сезона</h4>
                <p class="muted-text">Сначала собери состав сезона, затем при необходимости настрой регламент таблицы.</p>
              </div>
            </div>
            <div class="actions-row admin-season-team-row">
              <select v-model="seasonTeamToAddId">
                <option value="">— выберите команду —</option>
                <option v-for="team in seasonAvailableTeams" :key="`season-create-team-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
              </select>
              <button class="btn-ghost" type="button" @click="addSeasonTeamToForm">Добавить команду</button>
            </div>
            <p class="muted-text admin-season-selected-note">Выбрано: {{ seasonSelectedTeams.length }}</p>
            <p v-if="!seasonSelectedTeams.length" class="muted-text admin-season-empty-state">Пока не выбрано ни одной команды.</p>
            <div v-else class="admin-list-items admin-season-team-list">
              <article v-for="team in seasonSelectedTeams" :key="`season-create-selected-${team.id}`" class="admin-list-item admin-season-picked-item">
                <div class="admin-season-picked-copy">
                  <strong>{{ team.name }}</strong>
                  <p class="muted-text">Команда включена в состав сезона.</p>
                </div>
                <button class="btn-danger btn-sm" type="button" @click="removeSeasonTeamFromForm(team.id)">Убрать</button>
              </article>
            </div>
            <div class="admin-season-meta-grid">
              <article class="admin-season-meta-card">
                <span class="admin-season-meta-label">Регулярный этап</span>
                <strong>{{ seasonRegularToursCount }} туров</strong>
                <p class="muted-text">При {{ seasonSelectedTeams.length }} командах.</p>
              </article>
              <article class="admin-season-meta-card">
                <span class="admin-season-meta-label">Дисциплина</span>
                <strong>{{ Number(seasonForm.yellowCardsForSuspension || 0) || 0 }} ЖК / {{ Number(seasonForm.redCardsForSuspension || 0) || 0 }} КК</strong>
                <p class="muted-text">Порог автоматического пропуска.</p>
              </article>
              <article v-if="seasonForm.playoffEnabled && seasonForm.playoffTeamCount" class="admin-season-meta-card admin-season-meta-card-accent">
                <span class="admin-season-meta-label">Плей-офф</span>
                <strong>{{ seasonForm.playoffTeamCount }} команд</strong>
                <p class="muted-text">Финальный этап включен.</p>
              </article>
            </div>
          </section>

          <section class="admin-season-section admin-season-section-compact">
            <div class="admin-season-section-head admin-season-section-head-compact">
              <div class="admin-season-section-copy">
                <h4 class="admin-list-title">Судьи сезона</h4>
                <p class="muted-text">Эти судьи будут доступны при заполнении протоколов матчей сезона.</p>
              </div>
            </div>
            <div class="actions-row admin-season-team-row">
              <select v-model="seasonRefereeToAddId">
                <option value="">— выберите судью —</option>
                <option v-for="referee in seasonAvailableReferees" :key="`season-create-referee-${referee.id}`" :value="String(referee.id)">{{ referee.fullName }}</option>
              </select>
              <button class="btn-ghost" type="button" @click="addSeasonRefereeToForm">Добавить судью</button>
            </div>
            <p class="muted-text admin-season-selected-note">Привязано: {{ seasonSelectedReferees.length }}</p>
            <p v-if="!seasonSelectedReferees.length" class="muted-text admin-season-empty-state">Судьи к сезону пока не привязаны.</p>
            <div v-else class="admin-list-items admin-season-team-list">
              <article v-for="referee in seasonSelectedReferees" :key="`season-create-referee-selected-${referee.id}`" class="admin-list-item admin-referee-list-item admin-season-picked-item">
                <div class="admin-season-picked-copy">
                  <strong>{{ referee.fullName }}</strong>
                  <p class="muted-text">{{ referee.city || 'Город не указан' }}<span v-if="referee.birthDate"> · {{ formatDateOnly(referee.birthDate) }}</span></p>
                </div>
                <button class="btn-danger btn-sm" type="button" @click="removeSeasonRefereeFromForm(referee.id)">Убрать</button>
              </article>
            </div>
          </section>

          <section class="admin-season-section admin-season-section-compact admin-season-rules-panel">
            <div class="admin-season-section-head admin-season-section-head-compact">
              <div>
                <h4 class="admin-list-title">Тай-брейки таблицы</h4>
                <p class="muted-text">Блок опущен вниз и работает как компактный список приоритетов.</p>
              </div>
              <button
                class="btn-ghost btn-sm"
                type="button"
                @click="addSeasonRankingRule"
                :disabled="seasonForm.rankingRules.length >= tieBreakerRuleOptions.length"
              >Добавить критерий</button>
            </div>
            <p v-if="!seasonForm.rankingRules.length" class="muted-text">Дополнительные тай-брейки не выбраны.</p>
            <div v-else class="admin-ranking-rule-list">
              <div v-for="(rule, index) in seasonForm.rankingRules" :key="`create-rule-${index}`" class="admin-ranking-rule-card">
                <span class="admin-ranking-rule-badge">{{ index + 1 }}</span>
                <label class="admin-ranking-rule-field">
                  <span class="admin-ranking-rule-label">Приоритет {{ index + 1 }}</span>
                  <select v-model="seasonForm.rankingRules[index]">
                    <option value="">— выберите критерий —</option>
                    <option
                      v-for="option in availableTieBreakerRuleOptions(index)"
                      :key="`create-rule-${index}-${option.value}`"
                      :value="option.value"
                    >{{ option.label }}</option>
                  </select>
                </label>
                <button class="btn-danger btn-sm" type="button" @click="removeSeasonRankingRule(index)">Убрать</button>
              </div>
            </div>
            <div class="admin-season-rules-footer">
              <p class="muted-text">Сначала всегда считаются очки. Если после выбранных правил равенство остается, последним fallback используется алфавит.</p>
              <p class="muted-text admin-season-rules-summary">Текущий порядок: {{ seasonRankingRulesSummary() }}</p>
            </div>
          </section>

          <div class="actions-row admin-season-actions">
            <button class="btn-primary" type="submit" :disabled="isSeasonCreateDisabled">Создать сезон</button>
          </div>
        </form>

        <div v-else class="admin-form admin-season-form">
          <div class="admin-season-edit-toolbar">
            <label class="admin-season-edit-picker">
              Выберите сезон
              <select v-model="seasonEditSelectId" @change="onSeasonSelectChange">
                <option value="">— выберите —</option>
                <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
              </select>
            </label>
            <div v-if="editingSeasonId" class="admin-season-export-wrap">
              <button class="btn-ghost" type="button" @click="toggleSeasonProtocolMenu" :disabled="downloadingSeasonProtocols">
                {{ downloadingSeasonProtocols ? seasonProtocolProgressText || 'Подготовка архива...' : 'Скачать протоколы' }}
              </button>
              <div v-if="seasonProtocolMenuOpen" class="admin-season-export-menu">
                <button class="btn-ghost admin-season-export-action" type="button" @click="downloadSeasonProtocolsArchive" :disabled="downloadingSeasonProtocols">
                  Скачать все подтвержденные (.zip)
                </button>
              </div>
            </div>
          </div>
          <template v-if="editingSeasonId">
            <div class="admin-season-grid">
              <label class="admin-season-field admin-season-field-wide">
                Название сезона
                <input v-model.trim="seasonForm.name" type="text" />
              </label>
              <label class="admin-season-field">
                Количество кругов
                <select v-model="seasonForm.roundsCount">
                  <option value="1">1 круг</option>
                  <option value="2">2 круга</option>
                  <option value="3">3 круга</option>
                  <option value="4">4 круга</option>
                </select>
              </label>
              <label class="admin-season-field">
                Дедлайн заявки
                <input v-model="seasonForm.applicationDeadline" type="date" />
              </label>
              <label class="admin-season-field">
                Пропуск за ЖК
                <input v-model="seasonForm.yellowCardsForSuspension" type="number" min="0" placeholder="0 = выключено" />
              </label>
              <label class="admin-season-field">
                Пропуск за КК
                <input v-model="seasonForm.redCardsForSuspension" type="number" min="0" placeholder="0 = выключено" />
              </label>
              <label :class="['admin-season-field', 'admin-season-toggle-field', { 'admin-season-field-wide': !seasonForm.playoffEnabled }]">
                Плей-офф
                <span class="admin-season-toggle-control">
                  <input v-model="seasonForm.playoffEnabled" type="checkbox" />
                  <span>Включить плей-офф</span>
                </span>
              </label>
              <label v-if="seasonForm.playoffEnabled" class="admin-season-field">
                Команд в плей-офф
                <select v-model="seasonForm.playoffTeamCount">
                  <option value="">— выберите —</option>
                  <option v-for="count in playoffTeamOptions" :key="`edit-playoff-${count}`" :value="String(count)">{{ count }}</option>
                </select>
              </label>
            </div>

            <section class="admin-season-section">
              <div class="admin-season-section-head">
                <div class="admin-season-section-copy">
                  <h4 class="admin-list-title">Команды сезона</h4>
                  <p class="muted-text">Состав сезона и краткая сводка по формату собраны в одном месте.</p>
                </div>
              </div>
              <div class="actions-row admin-season-team-row">
                <select v-model="seasonTeamToAddId">
                  <option value="">— выберите команду —</option>
                  <option v-for="team in seasonAvailableTeams" :key="`season-edit-team-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
                </select>
                <button class="btn-ghost" type="button" @click="addSeasonTeamToForm">Добавить команду</button>
              </div>
              <p class="muted-text admin-season-selected-note">Выбрано: {{ seasonSelectedTeams.length }}</p>
              <p v-if="!seasonSelectedTeams.length" class="muted-text admin-season-empty-state">Пока не выбрано ни одной команды.</p>
              <div v-else class="admin-list-items admin-season-team-list">
                <article v-for="team in seasonSelectedTeams" :key="`season-edit-selected-${team.id}`" class="admin-list-item admin-season-picked-item">
                  <div class="admin-season-picked-copy">
                    <strong>{{ team.name }}</strong>
                    <p class="muted-text">Команда включена в состав сезона.</p>
                  </div>
                  <button class="btn-danger btn-sm" type="button" @click="removeSeasonTeamFromForm(team.id)">Убрать</button>
                </article>
              </div>
              <div class="admin-season-meta-grid">
                <article class="admin-season-meta-card">
                  <span class="admin-season-meta-label">Регулярный этап</span>
                  <strong>{{ seasonRegularToursCount }} туров</strong>
                  <p class="muted-text">При {{ seasonSelectedTeams.length }} командах.</p>
                </article>
                <article class="admin-season-meta-card">
                  <span class="admin-season-meta-label">Дисциплина</span>
                  <strong>{{ Number(seasonForm.yellowCardsForSuspension || 0) || 0 }} ЖК / {{ Number(seasonForm.redCardsForSuspension || 0) || 0 }} КК</strong>
                  <p class="muted-text">Порог автоматического пропуска.</p>
                </article>
                <article v-if="seasonForm.playoffEnabled && seasonForm.playoffTeamCount" class="admin-season-meta-card admin-season-meta-card-accent">
                  <span class="admin-season-meta-label">Плей-офф</span>
                  <strong>{{ seasonForm.playoffTeamCount }} команд</strong>
                  <p class="muted-text">Финальный этап включен.</p>
                </article>
              </div>
            </section>

            <section class="admin-season-section admin-season-section-compact">
              <div class="admin-season-section-head admin-season-section-head-compact">
                <div class="admin-season-section-copy">
                  <h4 class="admin-list-title">Судьи сезона</h4>
                  <p class="muted-text">Привязанные судьи будут доступны на карточках матчей этого сезона.</p>
                </div>
              </div>
              <div class="actions-row admin-season-team-row">
                <select v-model="seasonRefereeToAddId">
                  <option value="">— выберите судью —</option>
                  <option v-for="referee in seasonAvailableReferees" :key="`season-edit-referee-${referee.id}`" :value="String(referee.id)">{{ referee.fullName }}</option>
                </select>
                <button class="btn-ghost" type="button" @click="addSeasonRefereeToForm">Добавить судью</button>
              </div>
              <p class="muted-text admin-season-selected-note">Привязано: {{ seasonSelectedReferees.length }}</p>
              <p v-if="!seasonSelectedReferees.length" class="muted-text admin-season-empty-state">Судьи к сезону пока не привязаны.</p>
              <div v-else class="admin-list-items admin-season-team-list">
                <article v-for="referee in seasonSelectedReferees" :key="`season-edit-referee-selected-${referee.id}`" class="admin-list-item admin-referee-list-item admin-season-picked-item">
                  <div class="admin-season-picked-copy">
                    <strong>{{ referee.fullName }}</strong>
                    <p class="muted-text">{{ referee.city || 'Город не указан' }}<span v-if="referee.birthDate"> · {{ formatDateOnly(referee.birthDate) }}</span></p>
                  </div>
                  <button class="btn-danger btn-sm" type="button" @click="removeSeasonRefereeFromForm(referee.id)">Убрать</button>
                </article>
              </div>
            </section>

            <section class="admin-season-section admin-season-section-compact admin-season-rules-panel">
              <div class="admin-season-section-head admin-season-section-head-compact">
                <div>
                  <h4 class="admin-list-title">Тай-брейки таблицы</h4>
                  <p class="muted-text">Компактный блок внизу формы, чтобы не мешал базовым параметрам сезона.</p>
                </div>
                <button
                  class="btn-ghost btn-sm"
                  type="button"
                  @click="addSeasonRankingRule"
                  :disabled="seasonForm.rankingRules.length >= tieBreakerRuleOptions.length"
                >Добавить критерий</button>
              </div>
              <p v-if="!seasonForm.rankingRules.length" class="muted-text">Дополнительные тай-брейки не выбраны.</p>
              <div v-else class="admin-ranking-rule-list">
                <div v-for="(rule, index) in seasonForm.rankingRules" :key="`edit-rule-${index}`" class="admin-ranking-rule-card">
                  <span class="admin-ranking-rule-badge">{{ index + 1 }}</span>
                  <label class="admin-ranking-rule-field">
                    <span class="admin-ranking-rule-label">Приоритет {{ index + 1 }}</span>
                    <select v-model="seasonForm.rankingRules[index]">
                      <option value="">— выберите критерий —</option>
                      <option
                        v-for="option in availableTieBreakerRuleOptions(index)"
                        :key="`edit-rule-${index}-${option.value}`"
                        :value="option.value"
                      >{{ option.label }}</option>
                    </select>
                  </label>
                  <button class="btn-danger btn-sm" type="button" @click="removeSeasonRankingRule(index)">Убрать</button>
                </div>
              </div>
              <div class="admin-season-rules-footer">
                <p class="muted-text">Сначала всегда считаются очки. Если после выбранных правил равенство остается, последним fallback используется алфавит.</p>
                <p class="muted-text admin-season-rules-summary">Текущий порядок: {{ seasonRankingRulesSummary() }}</p>
              </div>
            </section>

            <div class="actions-row admin-season-actions">
              <button class="btn-primary" type="button" @click="saveEditSeason">Сохранить изменения</button>
              <button class="btn-danger" type="button" @click="deactivateSeason(editingSeasonId)">Удалить сезон</button>
              <button class="btn-ghost" type="button" @click="cancelEditSeason(); seasonEditSelectId = ''">Отмена</button>
            </div>
          </template>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'teams'">
      <div class="admin-panel-head">
        <h3 class="section-title">Команды и составы</h3>
        <p class="muted-text">Карточка команды, текущий состав и сезонная заявка игроков.</p>
      </div>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': teamSubMode === 'create' }"
          type="button"
          @click="teamSubMode = 'create'; cancelEditTeam(); teamEditSelectId = ''"
        >Создать команду</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': teamSubMode === 'edit' }"
          type="button"
          @click="teamSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <form v-if="teamSubMode === 'create'" class="admin-form admin-surface" @submit.prevent="createTeam">
        <label>
          Название команды
          <input v-model.trim="teamForm.name" type="text" required />
        </label>
        <label>
          Короткое название
          <input v-model.trim="teamForm.shortName" type="text" required />
        </label>
        <label>
          Город
          <input v-model.trim="teamForm.city" type="text" required />
        </label>
        <label>
          Лого команды
          <input type="file" accept="image/*" @change="onTeamLogoSelected" />
        </label>
        <img v-if="teamForm.logoDataUrl" :src="teamForm.logoDataUrl" alt="Превью лого команды" class="admin-team-logo-preview" />
        <div class="actions-row">
          <button class="btn-primary" type="submit">Создать команду</button>
        </div>
      </form>

      <div v-else class="admin-form admin-surface">
        <label>
          Выберите команду
          <select v-model="teamEditSelectId" @change="onTeamSelectChange">
            <option value="">— выберите —</option>
            <option v-for="t in teamsList" :key="t.id" :value="String(t.id)">{{ t.name }}</option>
          </select>
        </label>
        <template v-if="editingTeamId">
          <div class="admin-sticky-actions-spacer"></div>
          <div class="admin-team-editor-shell">
            <section class="admin-surface admin-team-identity-card">
              <div class="admin-team-identity-head">
                <div>
                  <h4 class="admin-list-title">Карточка команды</h4>
                  <p class="muted-text">Базовые поля и быстрые показатели в одном компактном блоке.</p>
                </div>
                <div class="admin-team-summary-grid">
                  <article class="admin-team-summary-card">
                    <span class="admin-team-summary-label">В составе</span>
                    <strong>{{ teamRoster.length }}</strong>
                  </article>
                  <article class="admin-team-summary-card">
                    <span class="admin-team-summary-label">Сезонов</span>
                    <strong>{{ teamSeasonOptions.length }}</strong>
                  </article>
                  <article class="admin-team-summary-card" :class="{ 'is-accent': selectedTeamSeasonId }">
                    <span class="admin-team-summary-label">В заявке</span>
                    <strong>{{ selectedTeamSeasonId ? teamSeasonSelectedPlayers.length : '—' }}</strong>
                  </article>
                </div>
              </div>

              <div class="admin-team-identity-grid">
                <label>
                  Название команды
                  <input v-model.trim="teamForm.name" type="text" />
                </label>
                <label>
                  Короткое название
                  <input v-model.trim="teamForm.shortName" type="text" />
                </label>
                <label>
                  Город
                  <input v-model.trim="teamForm.city" type="text" />
                </label>
                <label class="admin-team-logo-field">
                  Лого команды
                  <input type="file" accept="image/*" @change="onTeamLogoSelected" />
                </label>
                <div v-if="teamForm.logoDataUrl" class="admin-team-logo-preview-wrap">
                  <img :src="teamForm.logoDataUrl" alt="Превью лого команды" class="admin-team-logo-preview" />
                </div>
              </div>
            </section>

            <div class="admin-team-management-grid">
            <section class="admin-list admin-team-management-card">
              <div class="toolbar admin-team-management-head">
                <div>
                  <h4 class="admin-list-title">Состав команды</h4>
                  <p class="muted-text">Массовое добавление в команду и быстрый контроль текущего состава.</p>
                </div>
                <div class="actions-row admin-team-head-actions">
                  <button class="btn-ghost btn-sm" type="button" @click="toggleTeamRosterVisibility">
                    {{ isTeamRosterVisible ? 'Скрыть состав' : 'Показать весь состав' }}
                  </button>
                  <button class="btn-ghost btn-sm" type="button" @click="refreshAdminTeamContext">Обновить</button>
                </div>
              </div>

              <div class="actions-row admin-team-picker-row admin-team-management-toolbar">
                <SearchableSelect
                  :key="`team-roster-multi-${editingTeamId}-${teamRosterAddOptions.length}`"
                  v-model="teamRosterToAddIds"
                  :options="teamRosterAddOptions"
                  multiple
                  multiple-summary-text="Выбрано игроков"
                  placeholder="Выберите игроков в состав"
                  search-placeholder="Начните вводить ФИО игрока"
                  empty-text="Игрок по такому ФИО не найден"
                />
                <div class="admin-team-picker-side">
                  <span class="admin-team-picker-count">Выбрано: {{ teamRosterToAddIds.length }}</span>
                  <button class="btn-primary btn-sm" type="button" @click="addPlayerToEditingTeam" :disabled="teamRosterBusy || !teamRosterToAddIds.length">
                    Добавить выбранных
                  </button>
                </div>
              </div>

              <p v-if="!isTeamRosterVisible" class="muted-text">Состав скрыт. Нажмите «Показать весь состав», если нужен полный список игроков.</p>
              <p v-else-if="!teamRoster.length" class="muted-text">В текущем составе команды пока нет игроков.</p>
              <div v-else class="admin-list-items">
                <article v-for="player in teamRoster" :key="`team-roster-${player.id}`" class="admin-list-item admin-player-manage-item">
                  <div class="admin-player-manage-copy">
                    <strong>{{ player.fullName }}<span v-if="player.isGoalkeeper" class="goalkeeper-icon" aria-label="Вратарь" title="Вратарь">🧤</span></strong>
                    <span class="muted-text">В команде с {{ formatDateOnly(player.inTeamSince) }}</span>
                  </div>
                  <button class="btn-danger btn-sm" type="button" @click="removePlayerFromEditingTeam(player.id)" :disabled="teamRosterBusy">
                    Удалить из состава
                  </button>
                </article>
              </div>
            </section>

            <section class="admin-list admin-team-management-card">
              <div class="toolbar admin-team-management-head">
                <div>
                  <h4 class="admin-list-title">Заявка на сезон</h4>
                  <p class="muted-text">Выбери сезон, затем массово добавь или сними игроков без длинного списка.</p>
                </div>
              </div>

              <div class="admin-team-management-toolbar admin-team-management-toolbar-spacer" aria-hidden="true"></div>

              <label class="admin-team-season-select-field">
                Выберите сезон
                <select v-model="selectedTeamSeasonId" @change="onAdminTeamSeasonChange">
                  <option value="">— выберите —</option>
                  <option v-for="season in teamSeasonOptions" :key="`team-season-${season.id}`" :value="String(season.id)">
                    {{ season.name }}
                  </option>
                </select>
              </label>

              <p v-if="!teamSeasonOptions.length" class="muted-text">Эта команда пока не включена ни в один активный сезон.</p>
              <p v-else-if="!selectedTeamSeasonId" class="muted-text">Выберите сезон, чтобы управлять заявкой команды.</p>
              <p v-else-if="!teamSeasonPlayers.length" class="muted-text">В составе команды пока нет игроков для управления заявкой сезона.</p>

              <div v-else class="admin-team-season-tools">
                <div class="admin-team-season-summary">
                  <span class="admin-season-player-badge is-selected">В заявке: {{ teamSeasonSelectedPlayers.length }}</span>
                  <span class="admin-season-player-badge is-not-selected">Доступно: {{ teamSeasonAvailablePlayers.length }}</span>
                </div>

                <div class="admin-team-season-action-block">
                  <label class="admin-team-season-control">
                    Добавить игроков в сезон
                    <SearchableSelect
                      :key="`team-season-add-${selectedTeamSeasonId}-${teamSeasonAddOptions.length}`"
                      v-model="teamSeasonToAddIds"
                      :options="teamSeasonAddOptions"
                      multiple
                      multiple-summary-text="Выбрано игроков"
                      placeholder="Выберите игроков для заявки"
                      search-placeholder="Начните вводить ФИО игрока"
                      empty-text="Нет доступных игроков для добавления"
                      :disabled="teamSeasonBusy || !teamSeasonAddOptions.length"
                    />
                  </label>
                  <div class="admin-team-picker-side admin-team-picker-side-inline">
                    <span class="admin-team-picker-count">Выбрано: {{ teamSeasonToAddIds.length }}</span>
                    <button class="btn-primary btn-sm" type="button" @click="addSelectedPlayersToSeason" :disabled="teamSeasonBusy || !teamSeasonToAddIds.length">
                      Добавить выбранных
                    </button>
                  </div>
                </div>

                <div class="admin-team-season-action-block" v-if="teamSeasonRemoveOptions.length">
                  <label class="admin-team-season-control">
                    Убрать игроков из сезона
                    <SearchableSelect
                      :key="`team-season-remove-${selectedTeamSeasonId}-${teamSeasonRemoveOptions.length}`"
                      v-model="teamSeasonToRemoveIds"
                      :options="teamSeasonRemoveOptions"
                      multiple
                      multiple-summary-text="Выбрано игроков"
                      placeholder="Выберите игроков для удаления"
                      search-placeholder="Начните вводить ФИО игрока"
                      empty-text="В заявке пока нет игроков"
                      :disabled="teamSeasonBusy"
                    />
                  </label>
                  <div class="admin-team-picker-side admin-team-picker-side-inline">
                    <span class="admin-team-picker-count">Выбрано: {{ teamSeasonToRemoveIds.length }}</span>
                    <button class="btn-danger btn-sm" type="button" @click="removeSelectedPlayersFromSeason" :disabled="teamSeasonBusy || !teamSeasonToRemoveIds.length">
                      Убрать выбранных
                    </button>
                  </div>
                </div>
              </div>
            </section>
            </div>
          </div>

          <div class="actions-row admin-sticky-actions">
            <button class="btn-primary" type="button" @click="saveEditTeam">Сохранить изменения</button>
            <button class="btn-danger" type="button" @click="deactivateTeam(editingTeamId)">Удалить команду</button>
            <button class="btn-ghost" type="button" @click="cancelEditTeam(); teamEditSelectId = ''">Отмена</button>
          </div>
        </template>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'tours'">
      <div class="admin-panel-head">
        <h3 class="section-title">Туры и матчи</h3>
        <p class="muted-text">Управление календарём сезона, публикацией туров и матчами внутри тура.</p>
      </div>

      <div class="admin-form admin-surface">
        <label>
          Сезон
          <select v-model="tourSeasonId" @change="onTourSeasonChange">
            <option value="">— выберите —</option>
            <option v-for="item in seasonsList" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
          </select>
        </label>
        <p v-if="selectedTourSeason" class="muted-text">
          Регулярный этап рассчитан автоматически: {{ selectedTourSeason.regularToursCount }} туров, {{ selectedTourSeason.roundsCount }} круг(а), {{ tourTeamsList.length }} команд.
        </p>
        <p v-if="selectedTourSeason?.playoffEnabled" class="muted-text">
          Плей-офф включен на {{ selectedTourSeason.playoffTeamCount }} команд. Наполнение сетки будет добавлено следующим этапом.
        </p>
      </div>

      <div class="admin-form admin-surface" v-if="tourSeasonId">
        <label>
          Выберите тур
          <select v-model="selectedTourId" @change="onTourSelectChange">
            <option value="">— выберите —</option>
            <option v-for="tour in toursList" :key="tour.id" :value="String(tour.id)">{{ tour.name }}</option>
          </select>
        </label>
        <p v-if="!toursList.length" class="muted-text">Для выбранного сезона туры еще не сформированы. Проверьте состав команд и количество кругов.</p>
      </div>

      <div v-if="selectedTour" class="admin-grid">
        <form class="admin-form admin-surface" @submit.prevent="createTourMatch">
          <h4 class="admin-list-title">Добавить матч в тур {{ selectedTour.name }}</h4>
          <label>
            Команда 1
            <select v-model="matchForm.homeTeamId">
              <option value="">— выберите —</option>
              <option v-for="team in tourTeamsList" :key="`home-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>
          <label>
            Команда 2
            <select v-model="matchForm.awayTeamId">
              <option value="">— выберите —</option>
              <option v-for="team in tourTeamsList" :key="`away-${team.id}`" :value="String(team.id)">{{ team.name }}</option>
            </select>
          </label>
          <label>
            Время матча
            <input v-model="matchForm.kickoffAt" type="datetime-local" class="admin-temporal-input admin-temporal-input-wide" step="60" />
          </label>
          <div class="actions-row">
            <button class="btn-primary" type="submit">Добавить матч</button>
          </div>
          <p class="muted-text">Публично на сайт попадут только опубликованные туры.</p>
        </form>

        <div class="admin-list admin-surface">
          <div class="tour-matches-header">
            <h4 class="admin-list-title">Матчи тура</h4>
            <button class="btn-ghost tour-publish-button" type="button" :disabled="!canPublishSelectedTour" @click="publishSelectedTour">
              {{ selectedTour.published ? 'Опубликован' : 'Опубликовать тур' }}
            </button>
          </div>
          <p v-if="!tourMatchesList.length" class="muted-text">В этом туре пока нет матчей.</p>
          <div class="admin-list-items" v-else>
            <article class="admin-list-item tour-match-item" v-for="match in tourMatchesList" :key="match.id">
              <div class="tour-match-copy">
                <strong>{{ match.homeTeamName }} - {{ match.awayTeamName }}</strong>
                <span class="muted-text">{{ formatDateTime(match.kickoffAt) }}</span>
                <span class="tour-match-status-badge" :class="protocolStatusBadgeClass(match.protocolStatus)">
                  {{ matchProtocolStatusLabel(match.protocolStatus) }}
                </span>
              </div>
              <button
                class="btn-danger btn-sm"
                type="button"
                @click="deleteTourMatch(match.id)"
                :disabled="!canDeleteTourMatch(match)"
                :title="tourMatchDeleteTitle(match)"
              >
                Удалить
              </button>
            </article>
          </div>
          <p class="muted-text tour-publish-note">Публично на сайт попадут только опубликованные туры.</p>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'players'">
      <div class="admin-panel-head">
        <h3 class="section-title">Игроки</h3>
        <p class="muted-text">Единый реестр игроков с быстрым созданием и редактированием карточек.</p>
      </div>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': playerSubMode === 'create' }"
          type="button"
          @click="playerSubMode = 'create'; cancelEditPlayer(); playerEditSelectId = ''"
        >Создать игрока</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': playerSubMode === 'edit' }"
          type="button"
          @click="playerSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <div class="admin-grid">
        <form v-if="playerSubMode === 'create'" class="admin-form admin-surface" @submit.prevent="createPlayer">
          <label>
            ФИО
            <input v-model.trim="playerForm.fullName" type="text" required />
          </label>
          <label>
            Дата рождения
            <input v-model="playerForm.birthDate" type="date" class="admin-temporal-input" required />
          </label>
          <label>
            Прописка
            <input v-model.trim="playerForm.residence" type="text" placeholder="Город/деревня" required />
          </label>
          <label class="admin-checkbox-row">
            <input v-model="playerForm.isGoalkeeper" type="checkbox" />
            <span>Вратарь</span>
          </label>
          <label>
            Фото игрока
            <input type="file" accept="image/*" @change="onPlayerPhotoSelected" />
          </label>
          <img v-if="playerForm.photoDataUrl" :src="playerForm.photoDataUrl" alt="Превью фото игрока" class="team-rep-player-photo-preview" />
          <div class="actions-row">
            <button class="btn-primary" type="submit">Создать игрока</button>
          </div>
        </form>

        <div v-else class="admin-form admin-surface">
          <label>
            Выберите игрока
            <SearchableSelect
              v-model="playerEditSelectId"
              :options="playerEditOptions"
              placeholder="— выберите —"
              search-placeholder="Начните вводить ФИО игрока"
              empty-text="Игрок по такому ФИО не найден"
            />
          </label>
          <template v-if="editingPlayerId">
            <label>
              ФИО
              <input v-model.trim="playerForm.fullName" type="text" />
            </label>
            <label>
              Дата рождения
              <input v-model="playerForm.birthDate" type="date" class="admin-temporal-input" />
            </label>
            <label>
              Прописка
              <input v-model.trim="playerForm.residence" type="text" placeholder="Город/деревня" />
            </label>
            <label class="admin-checkbox-row">
              <input v-model="playerForm.isGoalkeeper" type="checkbox" />
              <span>Вратарь</span>
            </label>
            <label>
              Фото игрока
              <input type="file" accept="image/*" @change="onPlayerPhotoSelected" />
            </label>
            <img v-if="playerForm.photoDataUrl" :src="playerForm.photoDataUrl" alt="Превью фото игрока" class="team-rep-player-photo-preview" />
            <div class="actions-row">
              <button class="btn-primary" type="button" @click="saveEditPlayer">Сохранить изменения</button>
              <button class="btn-danger" type="button" @click="deactivatePlayer(editingPlayerId)">Удалить игрока</button>
              <button class="btn-ghost" type="button" @click="cancelEditPlayer(); playerEditSelectId = ''">Отмена</button>
            </div>
          </template>
        </div>

      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'referees'">
      <div class="admin-panel-head">
        <h3 class="section-title">Судьи</h3>
        <p class="muted-text">Реестр арбитров с быстрым созданием и редактированием карточек.</p>
      </div>
      <div class="admin-subnav">
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': refereeSubMode === 'create' }"
          type="button"
          @click="refereeSubMode = 'create'; cancelEditReferee(); refereeEditSelectId = ''"
        >Создать судью</button>
        <button
          class="btn-ghost admin-subnav-btn"
          :class="{ 'admin-subnav-active': refereeSubMode === 'edit' }"
          type="button"
          @click="refereeSubMode = 'edit'"
        >Редактировать</button>
      </div>

      <div class="admin-grid">
        <form v-if="refereeSubMode === 'create'" class="admin-form admin-surface" @submit.prevent="createReferee">
          <label>
            ФИО
            <input v-model.trim="refereeForm.fullName" type="text" required />
          </label>
          <label>
            Город
            <input v-model.trim="refereeForm.city" type="text" placeholder="Например: Богородск" />
          </label>
          <label>
            Дата рождения
            <input v-model="refereeForm.birthDate" type="date" class="admin-temporal-input" />
          </label>
          <label>
            Фото судьи
            <input type="file" accept="image/*" @change="onRefereePhotoSelected" />
          </label>
          <img v-if="refereeForm.photoDataUrl" :src="refereeForm.photoDataUrl" alt="Превью фото судьи" class="team-rep-player-photo-preview" />
          <div class="actions-row">
            <button class="btn-primary" type="submit">Создать судью</button>
          </div>
        </form>

        <div v-else class="admin-form admin-surface">
          <label>
            Выберите судью
            <select v-model="refereeEditSelectId" @change="onRefereeSelectChange">
              <option value="">— выберите —</option>
              <option v-for="referee in refereesList" :key="referee.id" :value="String(referee.id)">{{ referee.fullName }}</option>
            </select>
          </label>
          <template v-if="editingRefereeId">
            <label>
              ФИО
              <input v-model.trim="refereeForm.fullName" type="text" />
            </label>
            <label>
              Город
              <input v-model.trim="refereeForm.city" type="text" placeholder="Например: Богородск" />
            </label>
            <label>
              Дата рождения
              <input v-model="refereeForm.birthDate" type="date" class="admin-temporal-input" />
            </label>
            <label>
              Фото судьи
              <input type="file" accept="image/*" @change="onRefereePhotoSelected" />
            </label>
            <img v-if="refereeForm.photoDataUrl" :src="refereeForm.photoDataUrl" alt="Превью фото судьи" class="team-rep-player-photo-preview" />
            <div class="actions-row">
              <button class="btn-primary" type="button" @click="saveEditReferee">Сохранить изменения</button>
              <button class="btn-danger" type="button" @click="deactivateReferee(editingRefereeId)">Удалить судью</button>
              <button class="btn-ghost" type="button" @click="cancelEditReferee(); refereeEditSelectId = ''">Отмена</button>
            </div>
          </template>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'roles'">
      <div class="admin-panel-head">
        <h3 class="section-title">Роли и доступ</h3>
        <p class="muted-text">Поиск пользователя, управление ролями и сброс временного пароля.</p>
      </div>

      <div class="admin-form admin-surface">
        <label>
          Поиск по email
          <input v-model.trim="rolesSearch" type="text" placeholder="Начните вводить email..." />
        </label>
        <label>
          Выберите пользователя
          <select v-model="rolesSelectedEmail">
            <option value="">— выберите —</option>
            <option v-for="u in filteredUsersForSelect" :key="u.email" :value="u.email">{{ u.email }}</option>
          </select>
        </label>
        <p v-if="!roleUsersList.length" class="muted-text">Пользователи не найдены.</p>
        <div class="actions-row">
          <button class="btn-primary" type="button" @click="findUserForRoles">Найти</button>
        </div>
      </div>

      <div v-if="rolesFoundUser" class="admin-found-user">
        <p class="admin-found-email">{{ rolesFoundUser.email }}</p>
        <p class="muted-text" v-if="rolesFoundUser.name">{{ rolesFoundUser.name }}</p>
        <p class="muted-text">
          Требуется смена пароля: {{ rolesFoundUser.mustChangePassword ? 'да' : 'нет' }}
        </p>
        <p v-if="!rolesFoundUser.roles.length" class="muted-text">Ролей нет.</p>

        <div class="actions-row">
          <button class="btn-danger btn-sm" type="button" @click="resetPasswordForFoundUser">Сбросить пароль</button>
        </div>

        <article v-if="passwordResetResult" class="card admin-reset-password-card">
          <p><strong>Одноразовая ссылка:</strong></p>
          <p class="admin-reset-password-link">{{ absolutePasswordResetLink }}</p>
          <div class="actions-row">
            <button class="btn-ghost btn-sm" type="button" @click="copyPasswordResetLink">Скопировать ссылку</button>
          </div>
          <p class="muted-text">
            Ссылка действует до {{ formatDateTime(passwordResetResult.expiresAt) }}. Передайте ее пользователю по безопасному каналу. После установки нового пароля ссылка станет недействительной.
          </p>
        </article>

        <div v-for="role in rolesFoundUser.roles" :key="role" class="admin-role-manage-row">
          <span class="admin-role-badge">{{ role }}</span>
          <template v-if="replaceRoleTarget === role">
            <select v-model="replaceRoleNewCode" class="admin-role-select-inline">
              <option value="USER">USER</option>
              <option value="REFEREE">REFEREE</option>
              <option value="TEAM_REP">TEAM_REP</option>
              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
            </select>
            <button class="btn-primary btn-sm" type="button" @click="confirmReplaceRole">Подтвердить</button>
            <button class="btn-ghost btn-sm" type="button" @click="replaceRoleTarget = ''">Отмена</button>
          </template>
          <template v-else>
            <button class="btn-ghost btn-sm" type="button" @click="startReplaceRole(role)">Заменить</button>
            <button class="btn-danger btn-sm" type="button" @click="removeRoleFromFound(role)">Снять</button>
          </template>
        </div>

        <div class="admin-add-role-row">
          <select v-model="assignRoleCode" class="admin-role-select-inline">
            <option value="USER">USER</option>
            <option value="REFEREE">REFEREE</option>
            <option value="TEAM_REP">TEAM_REP</option>
            <option value="SUPER_ADMIN">SUPER_ADMIN</option>
          </select>
          <button class="btn-ghost btn-sm" type="button" @click="assignRoleToFound">+ Добавить роль</button>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'representatives'">
      <div class="admin-panel-head">
        <h3 class="section-title">Представители команд</h3>
        <p class="muted-text">Привязка пользователей к командам и управление доступом представителя.</p>
      </div>

      <div class="admin-form admin-surface">
        <label>
          Поиск по email
          <input v-model.trim="repSearch" type="text" placeholder="Начните вводить email представителя..." />
        </label>
        <label>
          Выберите представителя
          <select v-model="repSelectedEmail">
            <option value="">— выберите —</option>
            <option v-for="u in filteredRepresentativeUsersForSelect" :key="u.email" :value="String(u.email || '').toLowerCase()">{{ u.email }}</option>
          </select>
        </label>
        <p v-if="!repUsersList.length" class="muted-text">Представители команды не найдены.</p>
        <div class="actions-row">
          <button class="btn-primary" type="button" @click="findRepresentative">Найти</button>
        </div>
      </div>

      <div v-if="repFoundUser" class="admin-found-user">
        <p class="admin-found-email">{{ repFoundUser.email }}</p>
        <p class="muted-text" v-if="repFoundUser.name">{{ repFoundUser.name }}</p>
        <p class="muted-text">Текущая команда: {{ repCurrentTeamScope?.teamName || 'не назначена' }}</p>
        <p class="muted-text" v-if="repHasMultipleTeamScopes">Найдено несколько активных привязок. При сохранении старые привязки будут сняты.</p>

        <label>
          Назначить команду
          <select v-model="repSelectedTeamId">
            <option value="">— выберите —</option>
            <option v-for="team in teamsList" :key="team.id" :value="String(team.id)">{{ team.name }}</option>
          </select>
        </label>

        <div class="actions-row">
          <button class="btn-primary" type="button" @click="saveRepresentativeTeam">{{ repPrimaryActionLabel }}</button>
          <button class="btn-danger" type="button" :disabled="!repCurrentTeamScope" @click="unassignRepresentativeTeam">Открепить команду</button>
        </div>
      </div>
    </article>

    <article class="card admin-panel" v-if="activeTab === 'ban'">
      <div class="admin-panel-head">
        <h3 class="section-title">Блокировки пользователей</h3>
        <p class="muted-text">Локальный реестр блокировок и ручное управление статусом пользователя.</p>
      </div>
      <div class="admin-grid">
        <form class="admin-form admin-surface" @submit.prevent="banUser">
          <label>
            Email пользователя
            <input v-model.trim="banForm.email" type="email" required />
          </label>
          <label>
            Причина
            <input v-model.trim="banForm.reason" type="text" placeholder="Нарушение правил" required />
          </label>
          <div class="actions-row">
            <button class="btn-danger" type="submit">Забанить</button>
          </div>
        </form>

        <div class="admin-list admin-surface">
          <h4 class="admin-list-title">Статус пользователей</h4>
          <p v-if="!usersRegistry.length" class="muted-text">Пока пусто.</p>
          <div class="admin-list-items" v-else>
            <article class="admin-list-item" v-for="item in usersRegistry" :key="`ban-${item.email}`">
              <strong>{{ item.email }}</strong>
              <span class="muted-text" v-if="item.banned">Заблокирован: {{ item.banReason || '-' }}</span>
              <span class="success-text" v-else>Активен</span>
              <div class="actions-row" v-if="item.banned">
                <button class="btn-ghost" type="button" @click="unbanUser(item.email)">Разбанить</button>
              </div>
            </article>
          </div>
        </div>
      </div>
    </article>

    <article class="card" v-if="messageError || messageOk">
      <p class="error-text" v-if="messageError">{{ messageError }}</p>
      <p class="success-text" v-if="messageOk">{{ messageOk }}</p>
    </article>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useAuth } from '../store/auth'
import { useStore } from '../store/store'
import SearchableSelect from '../components/SearchableSelect.vue'

const USERS_KEY = 'football_stats_admin_users_registry'

const tabGroups = [
  {
    id: 'competition',
    kicker: 'Турнир',
    title: 'Соревнование и участники',
    items: [
      {
        id: 'seasons',
        label: 'Сезоны',
      },
      {
        id: 'teams',
        label: 'Команды',
      },
      {
        id: 'players',
        label: 'Игроки',
      },
      {
        id: 'referees',
        label: 'Судьи',
      },
      {
        id: 'tours',
        label: 'Туры и матчи',
      },
    ],
  },
  {
    id: 'access',
    kicker: 'Доступ',
    title: 'Права и модерация',
    items: [
      {
        id: 'roles',
        label: 'Роли и доступ',
      },
      {
        id: 'representatives',
        label: 'Представители',
      },
      {
        id: 'ban',
        label: 'Блокировки',
      },
    ],
  },
]

const tabs = tabGroups.flatMap((group) => group.items)

const visibleTabGroups = computed(() => {
  if (hasRole('SUPER_ADMIN')) {
    return tabGroups
  }
  if (hasRole('REFEREE')) {
    return tabGroups.filter((group) => group.id !== 'access')
  }
  return []
})

const activeTab = ref('seasons')

const { authorizedApiRequest, authorizedApiRequestRaw, hasRole } = useAuth()
const { loadSeasons } = useStore()

const seasonsList = ref([])
const teamsList = ref([])
const tourTeamsList = ref([])
const toursList = ref([])
const tourMatchesList = ref([])
const playersList = ref([])
const refereesList = ref([])
const usersRegistry = ref(loadFromStorage(USERS_KEY))
const roleUsersList = ref([])
const repUsersList = ref([])

const messageError = ref('')
const messageOk = ref('')

const playoffTeamOptions = [4, 8, 16]
const tieBreakerRuleOptions = [
  { value: 'GOAL_DIFFERENCE', label: 'Разница мячей' },
  { value: 'GOALS_FOR', label: 'Забитые мячи' },
  { value: 'WINS', label: 'Количество побед' },
  { value: 'HEAD_TO_HEAD', label: 'Личные встречи' },
]

const seasonForm = reactive({
  name: '',
  roundsCount: '1',
  playoffEnabled: false,
  playoffTeamCount: '',
  applicationDeadline: '',
  rankingRules: ['GOAL_DIFFERENCE', 'GOALS_FOR'],
  yellowCardsForSuspension: '0',
  redCardsForSuspension: '0',
})

const matchForm = reactive({
  homeTeamId: '',
  awayTeamId: '',
  kickoffAt: '',
})

const teamForm = reactive({
  name: '',
  shortName: '',
  city: '',
  logoDataUrl: '',
})

const playerForm = reactive({
  fullName: '',
  birthDate: '',
  residence: '',
  isGoalkeeper: false,
  photoDataUrl: '',
})

const refereeForm = reactive({
  fullName: '',
  city: '',
  birthDate: '',
  photoDataUrl: '',
})

const editingSeasonId = ref(null)
const seasonSubMode = ref('create')
const seasonEditSelectId = ref('')
const seasonProtocolMenuOpen = ref(false)
const downloadingSeasonProtocols = ref(false)
const seasonProtocolProgressText = ref('')
const seasonTeamIds = ref([])
const originalSeasonTeamIds = ref([])
const seasonTeamToAddId = ref('')
const seasonRefereeIds = ref([])
const originalSeasonRefereeIds = ref([])
const seasonRefereeToAddId = ref('')
const editingTeamId = ref(null)
const teamSubMode = ref('create')
const teamEditSelectId = ref('')
const teamRoster = ref([])
const teamSeasonOptions = ref([])
const teamSeasonPlayers = ref([])
const teamRosterToAddIds = ref([])
const selectedTeamSeasonId = ref('')
const teamSeasonToAddIds = ref([])
const teamSeasonToRemoveIds = ref([])
const teamRosterBusy = ref(false)
const teamSeasonBusy = ref(false)
const isTeamRosterVisible = ref(false)
const tourSeasonId = ref('')
const selectedTourId = ref('')
const editingPlayerId = ref(null)
const playerSubMode = ref('create')
const playerEditSelectId = ref('')
const editingRefereeId = ref(null)
const refereeSubMode = ref('create')
const refereeEditSelectId = ref('')

const rolesSearch = ref('')
const rolesSelectedEmail = ref('')
const rolesFoundEmail = ref('')
const replaceRoleTarget = ref('')
const replaceRoleNewCode = ref('USER')
const assignRoleCode = ref('USER')
const passwordResetResult = ref(null)
const rolesSearchDebounceMs = 5000
let rolesSearchDebounceTimer = null

const repSearch = ref('')
const repSelectedEmail = ref('')
const repFoundEmail = ref('')
const repSelectedTeamId = ref('')
const repUserAccess = ref(null)
const repSearchDebounceMs = 5000
let repSearchDebounceTimer = null

const roleForm = reactive({
  email: '',
  roleCode: 'USER',
})

const banForm = reactive({
  email: '',
  reason: '',
})

const userByEmail = computed(() => {
  const map = new Map()
  for (const item of usersRegistry.value) {
    map.set(String(item.email || '').toLowerCase(), item)
  }
  return map
})

const roleUserByEmail = computed(() => {
  const map = new Map()
  for (const item of roleUsersList.value) {
    map.set(String(item.email || '').toLowerCase(), item)
  }
  return map
})

const repUserByEmail = computed(() => {
  const map = new Map()
  for (const item of repUsersList.value) {
    map.set(String(item.email || '').toLowerCase(), item)
  }
  return map
})

const filteredUsersForSelect = computed(() => {
  const q = rolesSearch.value.toLowerCase()
  if (!q) return roleUsersList.value
  return roleUsersList.value.filter((u) => String(u.email || '').toLowerCase().includes(q))
})

const rolesFoundUser = computed(() => {
  if (!rolesFoundEmail.value) return null
  return roleUserByEmail.value.get(rolesFoundEmail.value) || null
})

const filteredRepresentativeUsersForSelect = computed(() => {
  const query = String(repSearch.value || '').toLowerCase()
  if (!query) return repUsersList.value
  return repUsersList.value.filter((user) => String(user.email || '').toLowerCase().includes(query))
})

const repFoundUser = computed(() => {
  if (!repFoundEmail.value) return null
  return repUserByEmail.value.get(repFoundEmail.value) || null
})

const repTeamScopes = computed(() => {
  return Array.isArray(repUserAccess.value?.teamScopes) ? repUserAccess.value.teamScopes : []
})

const repCurrentTeamScope = computed(() => {
  return repTeamScopes.value[0] || null
})

const repHasMultipleTeamScopes = computed(() => repTeamScopes.value.length > 1)

const repPrimaryActionLabel = computed(() => {
  return repCurrentTeamScope.value ? 'Изменить команду' : 'Назначить команду'
})

const absolutePasswordResetLink = computed(() => {
  const resetPath = String(passwordResetResult.value?.resetPath || '').trim()
  if (!resetPath) {
    return ''
  }
  if (typeof window === 'undefined' || !window.location?.origin) {
    return resetPath
  }
  return `${window.location.origin}${resetPath}`
})

const seasonSelectedTeams = computed(() => {
  const selectedIds = new Set(seasonTeamIds.value.map((id) => Number(id)))
  const teamsById = new Map(teamsList.value.map((team) => [Number(team.id), team]))

  return seasonTeamIds.value
    .map((id) => teamsById.get(Number(id)))
    .filter(Boolean)
    .filter((team, index, array) => array.findIndex((item) => Number(item.id) === Number(team.id)) === index)
    .filter((team) => selectedIds.has(Number(team.id)))
})

const seasonAvailableTeams = computed(() => {
  const selectedIds = new Set(seasonTeamIds.value.map((id) => Number(id)))
  return teamsList.value.filter((team) => !selectedIds.has(Number(team.id)))
})

const seasonSelectedReferees = computed(() => {
  const selectedIds = new Set(seasonRefereeIds.value.map((id) => Number(id)))
  const refereesById = new Map(refereesList.value.map((referee) => [Number(referee.id), referee]))

  return seasonRefereeIds.value
    .map((id) => refereesById.get(Number(id)))
    .filter(Boolean)
    .filter((referee, index, array) => array.findIndex((item) => Number(item.id) === Number(referee.id)) === index)
    .filter((referee) => selectedIds.has(Number(referee.id)))
})

const seasonAvailableReferees = computed(() => {
  const selectedIds = new Set(seasonRefereeIds.value.map((id) => Number(id)))
  return refereesList.value.filter((referee) => !selectedIds.has(Number(referee.id)))
})

const teamPlayersAvailableForRoster = computed(() => {
  const rosterIds = new Set(teamRoster.value.map((player) => Number(player.id)))
  return playersList.value.filter((player) => !rosterIds.has(Number(player.id)))
})

const teamRosterAddOptions = computed(() => {
  return teamPlayersAvailableForRoster.value.map((player) => ({
    value: String(player.id),
    label: formatAdminRosterPlayerOption(player),
    caption: formatAdminPlayerOptionCaption(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const teamSeasonSelectedPlayers = computed(() => {
  return teamSeasonPlayers.value.filter((player) => Boolean(player?.selectedForSeason))
})

const teamSeasonAvailablePlayers = computed(() => {
  return teamSeasonPlayers.value.filter((player) => !player?.selectedForSeason)
})

const teamSeasonAddOptions = computed(() => {
  return teamSeasonAvailablePlayers.value.map((player) => ({
    value: String(player.id),
    label: formatAdminRosterPlayerOption(player),
    caption: formatAdminPlayerOptionCaption(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const teamSeasonRemoveOptions = computed(() => {
  return teamSeasonSelectedPlayers.value.map((player) => ({
    value: String(player.id),
    label: formatAdminRosterPlayerOption(player),
    caption: formatAdminPlayerOptionCaption(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const playerEditOptions = computed(() => {
  return playersList.value.map((player) => ({
    value: String(player.id),
    label: formatPlayerOptionLabel(player),
    keywords: `${player.fullName || ''}`,
  }))
})

const seasonRegularToursCount = computed(() => {
  return calculateRegularToursCount(seasonSelectedTeams.value.length, Number(seasonForm.roundsCount || 1))
})

const isSeasonCreateDisabled = computed(() => {
  return !String(seasonForm.name || '').trim() || seasonSelectedTeams.value.length < 1
})

const selectedTourSeason = computed(() => {
  return seasonsList.value.find((season) => String(season.id) === String(tourSeasonId.value)) || null
})

const selectedSeasonEditItem = computed(() => {
  return seasonsList.value.find((season) => String(season.id) === String(editingSeasonId.value)) || null
})

const selectedTour = computed(() => {
  return toursList.value.find((tour) => String(tour.id) === String(selectedTourId.value)) || null
})

const canPublishSelectedTour = computed(() => {
  return Boolean(selectedTour.value) && !selectedTour.value.published && tourMatchesList.value.length > 0
})

watch(rolesSearch, (rawValue) => {
  if (rolesSearchDebounceTimer) {
    clearTimeout(rolesSearchDebounceTimer)
    rolesSearchDebounceTimer = null
  }

  if (activeTab.value !== 'roles') {
    return
  }

  const emailFilter = String(rawValue || '').trim()
  if (!emailFilter) {
    void loadRoleUsers({ pagenum: 0, pagesize: 20 })
    rolesSelectedEmail.value = ''
    rolesFoundEmail.value = ''
    return
  }

  rolesSearchDebounceTimer = setTimeout(async () => {
    await loadRoleUsers({ email: emailFilter, pagenum: 0, pagesize: 50 })
    if (roleUsersList.value.length === 1) {
      const email = String(roleUsersList.value[0].email || '').toLowerCase()
      rolesSelectedEmail.value = email
      rolesFoundEmail.value = email
      replaceRoleTarget.value = ''
    }
  }, rolesSearchDebounceMs)
})

watch(repSearch, (rawValue) => {
  if (repSearchDebounceTimer) {
    clearTimeout(repSearchDebounceTimer)
    repSearchDebounceTimer = null
  }

  if (activeTab.value !== 'representatives') {
    return
  }

  const emailFilter = String(rawValue || '').trim()
  if (!emailFilter) {
    void loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
    repSelectedEmail.value = ''
    repFoundEmail.value = ''
    repUserAccess.value = null
    repSelectedTeamId.value = ''
    return
  }

  repSearchDebounceTimer = setTimeout(async () => {
    await loadRepresentativeUsers({ email: emailFilter, pagenum: 0, pagesize: 50 })
    if (repUsersList.value.length === 1) {
      const email = String(repUsersList.value[0].email || '').toLowerCase()
      repSelectedEmail.value = email
      await refreshRepresentativeAccessByEmail(email)
    }
  }, repSearchDebounceMs)
})

watch(repSelectedEmail, (value) => {
  const normalized = String(value || '').trim().toLowerCase()
  if (!normalized) {
    repFoundEmail.value = ''
    repUserAccess.value = null
    repSelectedTeamId.value = ''
    return
  }

  void refreshRepresentativeAccessByEmail(normalized)
})

watch(playerEditSelectId, () => {
  if (playerSubMode.value === 'edit') {
    onPlayerSelectChange()
  }
})

watch(activeTab, (tabId) => {
  if (tabId === 'representatives' && !repUsersList.value.length) {
    void loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
  }
  if (tabId === 'tours' && !tourSeasonId.value && seasonsList.value.length) {
    tourSeasonId.value = String(seasonsList.value[0].id)
    void onTourSeasonChange()
  }
})

watch(visibleTabGroups, (groups) => {
  const allowedTabIds = new Set(groups.flatMap((group) => group.items.map((item) => item.id)))
  if (!allowedTabIds.has(activeTab.value)) {
    activeTab.value = groups[0]?.items[0]?.id || 'seasons'
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (rolesSearchDebounceTimer) {
    clearTimeout(rolesSearchDebounceTimer)
    rolesSearchDebounceTimer = null
  }
  if (repSearchDebounceTimer) {
    clearTimeout(repSearchDebounceTimer)
    repSearchDebounceTimer = null
  }
})

function resetMessages() {
  messageError.value = ''
  messageOk.value = ''
  passwordResetResult.value = null
}

function loadFromStorage(key) {
  const raw = localStorage.getItem(key)
  if (!raw) return []

  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function saveToStorage(key, value) {
  localStorage.setItem(key, JSON.stringify(value))
}

function ensureUser(email) {
  const normalized = String(email || '').trim().toLowerCase()
  if (!normalized) return null

  let record = userByEmail.value.get(normalized)
  if (record) return record

  record = {
    email: normalized,
    roles: [],
    banned: false,
    banReason: '',
    updatedAt: new Date().toISOString(),
  }
  usersRegistry.value.push(record)
  return record
}

async function createSeason() {
  resetMessages()

  const seasonValidationError = validateSeasonForm()
  if (seasonValidationError) {
    messageError.value = seasonValidationError
    return
  }

  if (!seasonForm.name) {
    messageError.value = 'Заполните название сезона.'
    return
  }

  if (!seasonTeamIds.value.length) {
    messageError.value = 'Выберите хотя бы одну команду для сезона.'
    return
  }

  try {
    const createdSeason = await authorizedApiRequest('/api/seasons', {
      method: 'POST',
      body: JSON.stringify(buildSeasonPayload()),
    })
    await authorizedApiRequest(`/api/seasons/${createdSeason.id}/teams`, {
      method: 'PUT',
      body: JSON.stringify({ teamIds: seasonTeamIds.value }),
    })
    await loadSeasonRegistry()
    await loadSeasons()
    if (String(tourSeasonId.value || '') === String(createdSeason.id)) {
      await onTourSeasonChange()
    }
    resetSeasonForm()
    messageOk.value = 'Сезон создан.'
  } catch (error) {
    showSeasonOperationError(error.message || 'Не удалось создать сезон.')
  }
}

async function startEditSeason(item) {
  editingSeasonId.value = item.id
  seasonForm.name = item.name
  seasonForm.roundsCount = String(item.roundsCount || 1)
  seasonForm.playoffEnabled = Boolean(item.playoffEnabled)
  seasonForm.playoffTeamCount = item.playoffTeamCount ? String(item.playoffTeamCount) : ''
  seasonForm.applicationDeadline = item.applicationDeadline || ''
  seasonForm.rankingRules = normalizeSeasonRankingRulesForForm(item.rankingRules)
  seasonForm.yellowCardsForSuspension = String(item.yellowCardsForSuspension || 0)
  seasonForm.redCardsForSuspension = String(item.redCardsForSuspension || 0)
  seasonRefereeIds.value = Array.isArray(item.referees) ? item.referees.map((referee) => Number(referee.id)).filter(Boolean) : []
  originalSeasonRefereeIds.value = [...seasonRefereeIds.value]
  seasonTeamIds.value = await loadSeasonTeams(item.id)
  originalSeasonTeamIds.value = [...seasonTeamIds.value]
  resetMessages()
}

function cancelEditSeason() {
  editingSeasonId.value = null
  seasonProtocolMenuOpen.value = false
  seasonProtocolProgressText.value = ''
  resetSeasonForm()
  resetMessages()
}

function addSeasonTeamToForm() {
  resetMessages()

  const teamId = Number(seasonTeamToAddId.value)
  if (!Number.isFinite(teamId) || teamId <= 0) {
    messageError.value = 'Сначала выберите команду из списка.'
    return
  }

  if (seasonTeamIds.value.some((id) => Number(id) === teamId)) {
    messageError.value = 'Эта команда уже добавлена в сезон.'
    return
  }

  seasonTeamIds.value = [...seasonTeamIds.value, teamId]
  seasonTeamToAddId.value = ''
}

function addSeasonRefereeToForm() {
  resetMessages()

  const refereeId = Number(seasonRefereeToAddId.value)
  if (!Number.isFinite(refereeId) || refereeId <= 0) {
    messageError.value = 'Сначала выберите судью из списка.'
    return
  }

  if (seasonRefereeIds.value.some((id) => Number(id) === refereeId)) {
    messageError.value = 'Этот судья уже привязан к сезону.'
    return
  }

  seasonRefereeIds.value = [...seasonRefereeIds.value, refereeId]
  seasonRefereeToAddId.value = ''
}

function removeSeasonTeamFromForm(teamId) {
  seasonTeamIds.value = seasonTeamIds.value.filter((id) => Number(id) !== Number(teamId))
  if (!seasonAvailableTeams.value.length) {
    seasonTeamToAddId.value = ''
  }
}

function removeSeasonRefereeFromForm(refereeId) {
  seasonRefereeIds.value = seasonRefereeIds.value.filter((id) => Number(id) !== Number(refereeId))
  if (!seasonAvailableReferees.value.length) {
    seasonRefereeToAddId.value = ''
  }
}

async function saveEditSeason() {
  resetMessages()

  const seasonValidationError = validateSeasonForm()
  if (seasonValidationError) {
    messageError.value = seasonValidationError
    return
  }

  if (!seasonForm.name) {
    messageError.value = 'Заполните название сезона.'
    return
  }

  if (!seasonTeamIds.value.length) {
    messageError.value = 'Выберите хотя бы одну команду для сезона.'
    return
  }

  try {
    await authorizedApiRequest(`/api/seasons/${editingSeasonId.value}`, {
      method: 'PUT',
      body: JSON.stringify(buildSeasonPayload()),
    })

    if (seasonTeamsChanged()) {
      await authorizedApiRequest(`/api/seasons/${editingSeasonId.value}/teams`, {
        method: 'PUT',
        body: JSON.stringify({ teamIds: seasonTeamIds.value }),
      })
    }

    await loadSeasonRegistry()
    await loadSeasons()
    if (String(tourSeasonId.value || '') === String(editingSeasonId.value)) {
      await onTourSeasonChange()
    }
    cancelEditSeason()
    messageOk.value = 'Сезон обновлен.'
  } catch (error) {
    showSeasonOperationError(error.message || 'Не удалось обновить сезон.')
  }
}

async function onSeasonSelectChange() {
  seasonProtocolMenuOpen.value = false
  if (!seasonEditSelectId.value) {
    cancelEditSeason()
    return
  }
  const item = seasonsList.value.find((seasonItem) => String(seasonItem.id) === seasonEditSelectId.value)
  if (item) {
    await startEditSeason(item)
  }
}

async function deactivateSeason(seasonId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/seasons/${seasonId}`, {
      method: 'DELETE',
    })
    if (String(editingSeasonId.value || '') === String(seasonId)) {
      cancelEditSeason()
      seasonEditSelectId.value = ''
    }
    await loadSeasonRegistry()
    await loadSeasons()
    messageOk.value = 'Сезон деактивирован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить сезон.'
  }
}

async function loadSeasonRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/seasons?active_flag=1', {
      method: 'GET',
    })
    seasonsList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    seasonsList.value = []
    messageError.value = error.message || 'Не удалось загрузить сезоны.'
  }
}

function toggleSeasonProtocolMenu() {
  if (!editingSeasonId.value || downloadingSeasonProtocols.value) return
  seasonProtocolMenuOpen.value = !seasonProtocolMenuOpen.value
}

async function downloadSeasonProtocolsArchive() {
  if (!editingSeasonId.value || downloadingSeasonProtocols.value) return

  resetMessages()
  seasonProtocolMenuOpen.value = false
  downloadingSeasonProtocols.value = true
  seasonProtocolProgressText.value = 'Подготовка архива на сервере...'

  try {
    const response = await authorizedApiRequestRaw(`/api/seasons/${editingSeasonId.value}/protocols/export`, {
      method: 'GET',
    })
    const archiveBlob = await response.blob()
    const disposition = response.headers.get('content-disposition') || ''
    const fileNameMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    const archiveName = fileNameMatch
      ? decodeURIComponent(fileNameMatch[1])
      : buildSeasonProtocolsArchiveName(selectedSeasonEditItem.value?.name || seasonForm.name || 'season')

    downloadBlobFile(archiveBlob, archiveName)
    messageOk.value = 'Архив протоколов сезона скачан.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось скачать архив протоколов сезона.'
  } finally {
    downloadingSeasonProtocols.value = false
    seasonProtocolProgressText.value = ''
  }
}

async function loadSeasonTeams(seasonId) {
  try {
    const payload = await authorizedApiRequest(`/api/seasons/${seasonId}/teams`, {
      method: 'GET',
    })
    return Array.isArray(payload) ? payload.map((team) => team.id) : []
  } catch (error) {
    messageError.value = error.message || 'Не удалось загрузить команды сезона.'
    return []
  }
}

function resetSeasonForm() {
  seasonForm.name = ''
  seasonForm.roundsCount = '1'
  seasonForm.playoffEnabled = false
  seasonForm.playoffTeamCount = ''
  seasonForm.applicationDeadline = ''
  seasonForm.rankingRules = ['GOAL_DIFFERENCE', 'GOALS_FOR']
  seasonForm.yellowCardsForSuspension = '0'
  seasonForm.redCardsForSuspension = '0'
  seasonTeamIds.value = []
  originalSeasonTeamIds.value = []
  seasonTeamToAddId.value = ''
  seasonRefereeIds.value = []
  originalSeasonRefereeIds.value = []
  seasonRefereeToAddId.value = ''
}

function seasonTeamsChanged() {
  return !haveSameTeamIds(originalSeasonTeamIds.value, seasonTeamIds.value)
}

function haveSameTeamIds(left, right) {
  const normalizedLeft = [...new Set((left || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))].sort((a, b) => a - b)
  const normalizedRight = [...new Set((right || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))].sort((a, b) => a - b)

  if (normalizedLeft.length !== normalizedRight.length) {
    return false
  }

  return normalizedLeft.every((teamId, index) => teamId === normalizedRight[index])
}

function showSeasonOperationError(message) {
  messageError.value = message
  if (typeof window !== 'undefined' && typeof window.alert === 'function') {
    window.alert(message)
  }
}

async function createTeam() {
  resetMessages()

  if (!teamForm.name || !teamForm.shortName || !teamForm.city) {
    messageError.value = 'Заполните все поля команды.'
    return
  }

  try {
    await authorizedApiRequest('/api/teams', {
      method: 'POST',
      body: JSON.stringify({
        name: teamForm.name,
        shortName: teamForm.shortName,
        city: teamForm.city,
        logoDataUrl: teamForm.logoDataUrl,
      }),
    })
    await loadTeamRegistry()
    resetTeamForm()
    messageOk.value = 'Команда создана.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать команду.'
  }
}

function startEditTeam(item) {
  editingTeamId.value = item.id
  teamForm.name = item.name
  teamForm.shortName = item.shortName
  teamForm.city = item.city
  teamForm.logoDataUrl = item.logoDataUrl || ''
  isTeamRosterVisible.value = false
  resetMessages()
}

function cancelEditTeam() {
  editingTeamId.value = null
  resetTeamForm()
  resetAdminTeamContext()
  resetMessages()
}

async function saveEditTeam() {
  resetMessages()

  if (!teamForm.name || !teamForm.shortName || !teamForm.city) {
    messageError.value = 'Заполните все поля команды.'
    return
  }

  try {
    await authorizedApiRequest(`/api/teams/${editingTeamId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: teamForm.name,
        shortName: teamForm.shortName,
        city: teamForm.city,
        logoDataUrl: teamForm.logoDataUrl,
      }),
    })
    await loadTeamRegistry()
    cancelEditTeam()
    messageOk.value = 'Команда обновлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить команду.'
  }
}

async function onTeamSelectChange() {
  if (!teamEditSelectId.value) {
    cancelEditTeam()
    return
  }
  const item = teamsList.value.find((t) => String(t.id) === teamEditSelectId.value)
  if (item) {
    startEditTeam(item)
    await refreshAdminTeamContext(item.id)
  }
}

async function deactivateTeam(teamId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/teams/${teamId}`, {
      method: 'DELETE',
    })
    if (String(editingTeamId.value || '') === String(teamId)) {
      cancelEditTeam()
      teamEditSelectId.value = ''
    }
    await loadTeamRegistry()
    messageOk.value = 'Команда деактивирована.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить команду.'
  }
}

function onTeamLogoSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    teamForm.logoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function resetTeamForm() {
  teamForm.name = ''
  teamForm.shortName = ''
  teamForm.city = ''
  teamForm.logoDataUrl = ''
}

function resetAdminTeamContext() {
  teamRoster.value = []
  teamSeasonOptions.value = []
  teamSeasonPlayers.value = []
  teamRosterToAddIds.value = []
  selectedTeamSeasonId.value = ''
  teamSeasonToAddIds.value = []
  teamSeasonToRemoveIds.value = []
  teamRosterBusy.value = false
  teamSeasonBusy.value = false
  isTeamRosterVisible.value = false
}

function toggleTeamRosterVisibility() {
  isTeamRosterVisible.value = !isTeamRosterVisible.value
}

async function refreshAdminTeamContext(teamId = editingTeamId.value) {
  const normalizedTeamId = Number(teamId)
  if (!Number.isFinite(normalizedTeamId) || normalizedTeamId <= 0) {
    resetAdminTeamContext()
    return
  }

  await loadEditingTeamRoster(normalizedTeamId)
  await loadEditingTeamSeasonOptions(normalizedTeamId)

  if (selectedTeamSeasonId.value && !teamSeasonOptions.value.some((season) => String(season.id) === String(selectedTeamSeasonId.value))) {
    selectedTeamSeasonId.value = ''
    teamSeasonPlayers.value = []
    teamSeasonToAddIds.value = []
    teamSeasonToRemoveIds.value = []
  }

  if (selectedTeamSeasonId.value) {
    await loadEditingTeamSeasonPlayers(normalizedTeamId, selectedTeamSeasonId.value)
  }
}

async function loadEditingTeamRoster(teamId) {
  teamRosterBusy.value = true
  try {
    const payload = await authorizedApiRequest(`/api/teams/${encodeURIComponent(teamId)}/players`, {
      method: 'GET',
    })
    teamRoster.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    teamRoster.value = []
    messageError.value = error.message || 'Не удалось загрузить состав команды.'
  } finally {
    teamRosterBusy.value = false
  }
}

async function loadEditingTeamSeasonOptions(teamId) {
  teamSeasonBusy.value = true
  try {
    const payload = await authorizedApiRequest(`/api/teams/${encodeURIComponent(teamId)}/seasons`, {
      method: 'GET',
    })
    teamSeasonOptions.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    teamSeasonOptions.value = []
    messageError.value = error.message || 'Не удалось определить сезоны команды.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function onAdminTeamSeasonChange() {
  resetMessages()
  teamSeasonToAddIds.value = []
  teamSeasonToRemoveIds.value = []

  if (!editingTeamId.value || !selectedTeamSeasonId.value) {
    teamSeasonPlayers.value = []
    return
  }

  await loadEditingTeamSeasonPlayers(editingTeamId.value, selectedTeamSeasonId.value)
}

async function loadEditingTeamSeasonPlayers(teamId, seasonId) {
  teamSeasonBusy.value = true
  try {
    const payload = await authorizedApiRequest(
      `/api/seasons/${encodeURIComponent(seasonId)}/teams/${encodeURIComponent(teamId)}/players`,
      { method: 'GET' }
    )
    teamSeasonPlayers.value = Array.isArray(payload) ? payload : []
    teamSeasonToAddIds.value = []
    teamSeasonToRemoveIds.value = []
  } catch (error) {
    teamSeasonPlayers.value = []
    teamSeasonToAddIds.value = []
    teamSeasonToRemoveIds.value = []
    messageError.value = error.message || 'Не удалось загрузить заявку команды на сезон.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function addPlayerToEditingTeam() {
  resetMessages()

  if (!editingTeamId.value) {
    messageError.value = 'Сначала выберите команду.'
    return
  }

  const playerIds = normalizePositiveIdList(teamRosterToAddIds.value)

  if (!playerIds.length) {
    messageError.value = 'Выберите хотя бы одного игрока для добавления в состав.'
    return
  }

  teamRosterBusy.value = true
  try {
    for (const playerId of playerIds) {
      await authorizedApiRequest(
        `/api/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'POST' }
      )
    }
    teamRosterToAddIds.value = []
    await loadPlayerRegistry()
    await refreshAdminTeamContext()
    messageOk.value = playerIds.length === 1 ? 'Игрок добавлен в состав команды.' : `В состав команды добавлено игроков: ${playerIds.length}.`
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить игрока в состав команды.'
  } finally {
    teamRosterBusy.value = false
  }
}

async function removePlayerFromEditingTeam(playerId) {
  resetMessages()

  if (!editingTeamId.value) {
    messageError.value = 'Сначала выберите команду.'
    return
  }

  if (!window.confirm('Убрать игрока из состава команды?')) {
    return
  }

  teamRosterBusy.value = true
  try {
    await authorizedApiRequest(
      `/api/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
      { method: 'DELETE' }
    )
    await loadPlayerRegistry()
    await refreshAdminTeamContext()
    messageOk.value = 'Игрок убран из состава команды.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось убрать игрока из состава команды.'
  } finally {
    teamRosterBusy.value = false
  }
}

async function addSelectedPlayersToSeason() {
  resetMessages()

  if (!editingTeamId.value || !selectedTeamSeasonId.value) {
    messageError.value = 'Сначала выберите команду и сезон.'
    return
  }

  const playerIds = normalizePositiveIdList(teamSeasonToAddIds.value)
  if (!playerIds.length) {
    messageError.value = 'Выберите хотя бы одного игрока для добавления в заявку сезона.'
    return
  }

  teamSeasonBusy.value = true

  try {
    for (const playerId of playerIds) {
      await authorizedApiRequest(
        `/api/seasons/${encodeURIComponent(selectedTeamSeasonId.value)}/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'POST' }
      )
    }
    await loadEditingTeamSeasonPlayers(editingTeamId.value, selectedTeamSeasonId.value)
    messageOk.value = playerIds.length === 1 ? 'Игрок добавлен в заявку сезона.' : `В заявку сезона добавлено игроков: ${playerIds.length}.`
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить игроков в заявку сезона.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function removeSelectedPlayersFromSeason() {
  resetMessages()

  if (!editingTeamId.value || !selectedTeamSeasonId.value) {
    messageError.value = 'Сначала выберите команду и сезон.'
    return
  }

  const playerIds = normalizePositiveIdList(teamSeasonToRemoveIds.value)
  if (!playerIds.length) {
    messageError.value = 'Выберите хотя бы одного игрока для удаления из заявки сезона.'
    return
  }

  teamSeasonBusy.value = true

  try {
    for (const playerId of playerIds) {
      await authorizedApiRequest(
        `/api/seasons/${encodeURIComponent(selectedTeamSeasonId.value)}/teams/${encodeURIComponent(editingTeamId.value)}/players/${encodeURIComponent(playerId)}`,
        { method: 'DELETE' }
      )
    }
    await loadEditingTeamSeasonPlayers(editingTeamId.value, selectedTeamSeasonId.value)
    messageOk.value = playerIds.length === 1 ? 'Игрок убран из заявки сезона.' : `Из заявки сезона убрано игроков: ${playerIds.length}.`
  } catch (error) {
    messageError.value = error.message || 'Не удалось изменить заявку сезона.'
  } finally {
    teamSeasonBusy.value = false
  }
}

async function loadTeamRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/teams?active_flag=1', {
      method: 'GET',
    })
    teamsList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    teamsList.value = []
    messageError.value = error.message || 'Не удалось загрузить команды.'
  }
}

async function onTourSeasonChange() {
  resetMessages()
  selectedTourId.value = ''
  tourMatchesList.value = []
  resetMatchForm()

  if (!tourSeasonId.value) {
    toursList.value = []
    tourTeamsList.value = []
    return
  }

  await Promise.all([loadTours(), loadTeamsForTourSeason()])
}

async function loadTours() {
  if (!tourSeasonId.value) {
    toursList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/tours?season_id=${encodeURIComponent(tourSeasonId.value)}&active_flag=1`, {
      method: 'GET',
    })
    toursList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    toursList.value = []
    messageError.value = error.message || 'Не удалось загрузить туры.'
  }
}

async function loadTeamsForTourSeason() {
  if (!tourSeasonId.value) {
    tourTeamsList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/teams?active_flag=1&season_id=${encodeURIComponent(tourSeasonId.value)}`, {
      method: 'GET',
    })
    tourTeamsList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    tourTeamsList.value = []
    messageError.value = error.message || 'Не удалось загрузить команды сезона для туров.'
  }
}

async function onTourSelectChange() {
  resetMessages()
  resetMatchForm()

  if (!selectedTourId.value) {
    tourMatchesList.value = []
    return
  }

  try {
    const payload = await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches?active_flag=1`, {
      method: 'GET',
    })
    tourMatchesList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    tourMatchesList.value = []
    messageError.value = error.message || 'Не удалось загрузить матчи тура.'
  }
}

async function publishSelectedTour() {
  resetMessages()

  if (!selectedTourId.value) {
    messageError.value = 'Сначала выберите тур.'
    return
  }
  if (!tourMatchesList.value.length) {
    messageError.value = 'Нельзя публиковать пустой тур без матчей.'
    return
  }
  if (selectedTour.value?.published) {
    messageError.value = 'Этот тур уже опубликован.'
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/publish`, {
      method: 'PUT',
    })
    await Promise.all([loadTours(), onTourSelectChange()])
    messageOk.value = 'Тур опубликован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось опубликовать тур.'
  }
}

async function createTourMatch() {
  resetMessages()

  if (!selectedTourId.value) {
    messageError.value = 'Сначала выберите тур.'
    return
  }
  if (!matchForm.homeTeamId || !matchForm.awayTeamId) {
    messageError.value = 'Выберите обе команды матча.'
    return
  }
  if (String(matchForm.homeTeamId) === String(matchForm.awayTeamId)) {
    messageError.value = 'Команды матча должны отличаться.'
    return
  }
  if (!matchForm.kickoffAt) {
    messageError.value = 'Укажите дату и время матча.'
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches`, {
      method: 'POST',
      body: JSON.stringify({
        homeTeamId: Number(matchForm.homeTeamId),
        awayTeamId: Number(matchForm.awayTeamId),
        kickoffAt: new Date(matchForm.kickoffAt).toISOString(),
      }),
    })
    await onTourSelectChange()
    resetMatchForm()
    messageOk.value = 'Матч добавлен в тур.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить матч.'
  }
}

async function deleteTourMatch(matchId) {
  resetMessages()

  if (!selectedTourId.value) {
    messageError.value = 'Сначала выберите тур.'
    return
  }
  const match = tourMatchesList.value.find((item) => Number(item.id) === Number(matchId))
  if (match && !canDeleteTourMatch(match)) {
    messageError.value = tourMatchDeleteTitle(match)
    return
  }
  if (!window.confirm('Удалить матч из тура без возможности восстановления?')) {
    return
  }

  try {
    await authorizedApiRequest(`/api/tours/${selectedTourId.value}/matches/${matchId}`, {
      method: 'DELETE',
    })
    await Promise.all([loadTours(), onTourSelectChange()])
    messageOk.value = 'Матч удален из тура.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить матч из тура.'
  }
}

function resetMatchForm() {
  matchForm.homeTeamId = ''
  matchForm.awayTeamId = ''
  matchForm.kickoffAt = ''
}

function canDeleteTourMatch(match) {
  return String(match?.protocolStatus || 'SCHEDULED') === 'SCHEDULED'
}

function tourMatchDeleteTitle(match) {
  if (canDeleteTourMatch(match)) {
    return 'Удалить матч из тура'
  }
  return 'Нельзя удалить матч, если по нему уже поданы составы или подтвержден протокол.'
}

function matchProtocolStatusLabel(status) {
  switch (String(status || 'SCHEDULED')) {
    case 'LINEUPS_SUBMITTED':
      return 'Составы поданы'
    case 'LIVE':
      return 'Идет матч'
    case 'FINISHED':
      return 'Матч сыгран'
    case 'VERIFIED':
      return 'Протокол подтвержден'
    default:
      return 'Не сыгран'
  }
}

function buildSeasonProtocolsArchiveName(seasonName) {
  const normalizedSeasonName = String(seasonName || 'season').replace(/[\\/:*?"<>|]/g, '_').trim() || 'season'
  return `Протоколы_${normalizedSeasonName}.zip`
}

function downloadBlobFile(blob, fileName) {
  const objectUrl = URL.createObjectURL(blob)

  try {
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName
    link.style.display = 'none'
    document.body.appendChild(link)
    link.click()
    link.remove()
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

function protocolStatusBadgeClass(status) {
  return {
    'is-scheduled': String(status || 'SCHEDULED') === 'SCHEDULED',
    'is-lineups': String(status || '') === 'LINEUPS_SUBMITTED',
    'is-live': String(status || '') === 'LIVE',
    'is-finished': String(status || '') === 'FINISHED',
    'is-verified': String(status || '') === 'VERIFIED',
  }
}

function buildSeasonPayload() {
  const roundsCount = Number(seasonForm.roundsCount || 1)
  const playoffEnabled = Boolean(seasonForm.playoffEnabled)
  const playoffTeamCount = playoffEnabled && seasonForm.playoffTeamCount
    ? Number(seasonForm.playoffTeamCount)
    : null

  return {
    name: seasonForm.name,
    roundsCount,
    playoffEnabled,
    playoffTeamCount,
    applicationDeadline: seasonForm.applicationDeadline || null,
    rankingRules: buildSeasonRankingRulesPayload(),
    refereeIds: seasonRefereeIds.value,
    yellowCardsForSuspension: Number(seasonForm.yellowCardsForSuspension || 0),
    redCardsForSuspension: Number(seasonForm.redCardsForSuspension || 0),
  }
}

function validateSeasonForm() {
  const rankingRules = normalizedSeasonTieBreakers()
  if (rankingRules.length !== new Set(rankingRules).size) {
    return 'Правила таблицы не должны повторяться.'
  }
  return ''
}

function formatSeasonApplicationDeadline(value) {
  return value ? formatDateOnly(value) : 'Без ограничения'
}

function normalizedSeasonTieBreakers() {
  return (seasonForm.rankingRules || [])
    .map((rule) => String(rule || '').trim())
    .filter(Boolean)
}

function buildSeasonRankingRulesPayload() {
  return ['POINTS', ...normalizedSeasonTieBreakers(), 'ALPHABETICAL']
}

function normalizeSeasonRankingRulesForForm(rawRules) {
  const tieBreakers = Array.isArray(rawRules)
    ? rawRules.filter((rule) => rule !== 'POINTS' && rule !== 'ALPHABETICAL')
    : []

  if (!tieBreakers.length) {
    return ['GOAL_DIFFERENCE', 'GOALS_FOR']
  }

  return tieBreakers.map((rule) => String(rule || '')).filter(Boolean)
}

function availableTieBreakerRuleOptions(index) {
  const usedRules = new Set(
    (seasonForm.rankingRules || [])
      .filter((_, ruleIndex) => ruleIndex !== index)
      .map((rule) => String(rule || '').trim())
      .filter(Boolean)
  )

  return tieBreakerRuleOptions.filter((option) => !usedRules.has(option.value) || option.value === seasonForm.rankingRules[index])
}

function addSeasonRankingRule() {
  const usedRules = new Set(normalizedSeasonTieBreakers())
  const nextRule = tieBreakerRuleOptions.find((option) => !usedRules.has(option.value))
  seasonForm.rankingRules = [...seasonForm.rankingRules, nextRule?.value || '']
}

function removeSeasonRankingRule(index) {
  seasonForm.rankingRules = seasonForm.rankingRules.filter((_, ruleIndex) => ruleIndex !== index)
}

function standingsRuleLabel(rule) {
  return tieBreakerRuleOptions.find((option) => option.value === rule)?.label || rule
}

function seasonRankingRulesSummary() {
  const labels = normalizedSeasonTieBreakers().map((rule) => standingsRuleLabel(rule))
  if (!labels.length) {
    return 'только очки, затем алфавит'
  }
  return `очки, затем ${labels.join(' -> ')}, затем алфавит`
}

function calculateRegularToursCount(teamCount, roundsCount) {
  const normalizedTeamCount = Number(teamCount || 0)
  const normalizedRoundsCount = Number(roundsCount || 0)
  if (normalizedTeamCount < 2 || normalizedRoundsCount < 1) {
    return 0
  }
  const toursPerRound = normalizedTeamCount % 2 === 0 ? normalizedTeamCount - 1 : normalizedTeamCount
  return toursPerRound * normalizedRoundsCount
}

async function createPlayer() {
  resetMessages()

  if (!playerForm.fullName || !playerForm.birthDate || !playerForm.residence) {
    messageError.value = 'Заполните все поля игрока.'
    return
  }

  try {
    await authorizedApiRequest('/api/players', {
      method: 'POST',
      body: JSON.stringify({
        fullName: playerForm.fullName,
        birthDate: playerForm.birthDate,
        residence: playerForm.residence,
        isGoalkeeper: Boolean(playerForm.isGoalkeeper),
        photoDataUrl: playerForm.photoDataUrl,
      }),
    })
    await loadPlayerRegistry()
    resetPlayerForm()
    messageOk.value = 'Игрок создан.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать игрока.'
  }
}

function startEditPlayer(item) {
  editingPlayerId.value = item.id
  playerForm.fullName = item.fullName
  playerForm.birthDate = item.birthDate
  playerForm.residence = item.residence
  playerForm.isGoalkeeper = Boolean(item.isGoalkeeper)
  playerForm.photoDataUrl = item.photoDataUrl || ''
  resetMessages()
}

function cancelEditPlayer() {
  editingPlayerId.value = null
  resetPlayerForm()
  resetMessages()
}

async function saveEditPlayer() {
  resetMessages()

  if (!playerForm.fullName) {
    messageError.value = 'Укажите ФИО игрока.'
    return
  }

  try {
    await authorizedApiRequest(`/api/players/${editingPlayerId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        fullName: playerForm.fullName,
        birthDate: playerForm.birthDate,
        residence: playerForm.residence,
        isGoalkeeper: Boolean(playerForm.isGoalkeeper),
        photoDataUrl: playerForm.photoDataUrl,
      }),
    })
    await loadPlayerRegistry()
    cancelEditPlayer()
    messageOk.value = 'Игрок обновлен.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить игрока.'
  }
}

function onPlayerSelectChange() {
  if (!playerEditSelectId.value) {
    cancelEditPlayer()
    return
  }
  const item = playersList.value.find((playerItem) => String(playerItem.id) === playerEditSelectId.value)
  if (item) startEditPlayer(item)
}

async function deactivatePlayer(playerId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/players/${playerId}`, {
      method: 'DELETE',
    })
    if (String(editingPlayerId.value || '') === String(playerId)) {
      cancelEditPlayer()
      playerEditSelectId.value = ''
    }
    await loadPlayerRegistry()
    messageOk.value = 'Игрок деактивирован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить игрока.'
  }
}

async function loadPlayerRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/players?active_flag=1&pagenum=0&pagesize=500', {
      method: 'GET',
    })
    playersList.value = Array.isArray(payload?.content) ? payload.content : []
  } catch (error) {
    playersList.value = []
    messageError.value = error.message || 'Не удалось загрузить игроков.'
  }
}

function onPlayerPhotoSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    playerForm.photoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function resetPlayerForm() {
  playerForm.fullName = ''
  playerForm.birthDate = ''
  playerForm.residence = ''
  playerForm.isGoalkeeper = false
  playerForm.photoDataUrl = ''
}

async function createReferee() {
  resetMessages()

  if (!refereeForm.fullName) {
    messageError.value = 'Укажите ФИО судьи.'
    return
  }

  try {
    await authorizedApiRequest('/api/referees', {
      method: 'POST',
      body: JSON.stringify({
        fullName: refereeForm.fullName,
        city: refereeForm.city,
        birthDate: refereeForm.birthDate || null,
        photoDataUrl: refereeForm.photoDataUrl,
      }),
    })
    await loadRefereeRegistry()
    resetRefereeForm()
    messageOk.value = 'Судья создан.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось создать судью.'
  }
}

function startEditReferee(item) {
  editingRefereeId.value = item.id
  refereeForm.fullName = item.fullName
  refereeForm.city = item.city || ''
  refereeForm.birthDate = item.birthDate || ''
  refereeForm.photoDataUrl = item.photoDataUrl || ''
  resetMessages()
}

function cancelEditReferee() {
  editingRefereeId.value = null
  resetRefereeForm()
  resetMessages()
}

async function saveEditReferee() {
  resetMessages()

  if (!refereeForm.fullName) {
    messageError.value = 'Укажите ФИО судьи.'
    return
  }

  try {
    await authorizedApiRequest(`/api/referees/${editingRefereeId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        fullName: refereeForm.fullName,
        city: refereeForm.city,
        birthDate: refereeForm.birthDate || null,
        photoDataUrl: refereeForm.photoDataUrl,
      }),
    })
    await loadRefereeRegistry()
    cancelEditReferee()
    messageOk.value = 'Судья обновлен.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось обновить судью.'
  }
}

function onRefereeSelectChange() {
  if (!refereeEditSelectId.value) {
    cancelEditReferee()
    return
  }
  const item = refereesList.value.find((referee) => String(referee.id) === refereeEditSelectId.value)
  if (item) {
    startEditReferee(item)
  }
}

async function deactivateReferee(refereeId) {
  resetMessages()

  try {
    await authorizedApiRequest(`/api/referees/${refereeId}`, {
      method: 'DELETE',
    })
    if (String(editingRefereeId.value || '') === String(refereeId)) {
      cancelEditReferee()
      refereeEditSelectId.value = ''
    }
    await loadRefereeRegistry()
    messageOk.value = 'Судья деактивирован.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось удалить судью.'
  }
}

async function loadRefereeRegistry() {
  try {
    const payload = await authorizedApiRequest('/api/referees?active_flag=1', {
      method: 'GET',
    })
    refereesList.value = Array.isArray(payload) ? payload : []
  } catch (error) {
    refereesList.value = []
    messageError.value = error.message || 'Не удалось загрузить судей.'
  }
}

function onRefereePhotoSelected(event) {
  const file = event.target?.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = () => {
    refereeForm.photoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
}

function resetRefereeForm() {
  refereeForm.fullName = ''
  refereeForm.city = ''
  refereeForm.birthDate = ''
  refereeForm.photoDataUrl = ''
}

function formatPlayerOptionLabel(player) {
  if (!player) return ''
  return `${player.fullName || ''}`
}

function formatAdminRosterPlayerOption(player) {
  if (!player) return ''
  return formatPlayerOptionLabel(player)
}

function formatAdminPlayerOptionCaption(player) {
  if (!player) return ''

  const parts = []
  if (player.birthDate) {
    parts.push(`ДР: ${formatDateOnly(player.birthDate)}`)
  }
  if (player.residence) {
    parts.push(player.residence)
  }
  if (player.isGoalkeeper) {
    parts.push('Вратарь')
  }

  return parts.join(' · ')
}

function normalizePositiveIdList(values) {
  return [...new Set((Array.isArray(values) ? values : [])
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value) && value > 0))]
}

function assignRole() {
  resetMessages()

  const email = String(roleForm.email || '').trim().toLowerCase()
  if (!email) {
    messageError.value = 'Укажите email пользователя.'
    return
  }

  const user = ensureUser(email)
  if (!user) {
    messageError.value = 'Не удалось подготовить профиль пользователя.'
    return
  }

  if (!user.roles.includes(roleForm.roleCode)) {
    user.roles.push(roleForm.roleCode)
  }
  user.updatedAt = new Date().toISOString()

  saveToStorage(USERS_KEY, usersRegistry.value)

  roleForm.email = ''
  roleForm.roleCode = 'USER'
  messageOk.value = 'Роль назначена.'
}

function banUser() {
  resetMessages()

  const email = String(banForm.email || '').trim().toLowerCase()
  const reason = String(banForm.reason || '').trim()

  if (!email || !reason) {
    messageError.value = 'Укажите email и причину блокировки.'
    return
  }

  const user = ensureUser(email)
  if (!user) {
    messageError.value = 'Не удалось подготовить профиль пользователя.'
    return
  }

  user.banned = true
  user.banReason = reason
  user.updatedAt = new Date().toISOString()

  saveToStorage(USERS_KEY, usersRegistry.value)

  banForm.email = ''
  banForm.reason = ''
  messageOk.value = 'Пользователь заблокирован.'
}

function removeRole(email, roleCode) {
  resetMessages()
  const user = userByEmail.value.get(String(email || '').trim().toLowerCase())
  if (!user) return

  user.roles = user.roles.filter((r) => r !== roleCode)
  user.updatedAt = new Date().toISOString()
  saveToStorage(USERS_KEY, usersRegistry.value)
  messageOk.value = `Роль ${roleCode} снята.`
}

function findUserForRoles() {
  resetMessages()
  const emailFilter = String(rolesSearch.value || rolesSelectedEmail.value || '').trim()
  if (!emailFilter) {
    messageError.value = 'Введите email или выберите пользователя.'
    return
  }

  void loadRoleUsers({ email: emailFilter, pagenum: 0, pagesize: 50 }).then(() => {
    const selected = String(rolesSelectedEmail.value || '').trim().toLowerCase()
    if (selected) {
      rolesFoundEmail.value = selected
      replaceRoleTarget.value = ''
      return
    }

    if (roleUsersList.value.length === 1) {
      const onlyEmail = String(roleUsersList.value[0].email || '').toLowerCase()
      rolesSelectedEmail.value = onlyEmail
      rolesFoundEmail.value = onlyEmail
      replaceRoleTarget.value = ''
      return
    }

    if (!roleUsersList.value.length) {
      messageError.value = 'Пользователь не найден.'
      rolesFoundEmail.value = ''
      return
    }

    messageError.value = 'Выберите пользователя из найденного списка.'
  })
}

function startReplaceRole(role) {
  replaceRoleTarget.value = role
  replaceRoleNewCode.value = 'USER'
}

async function confirmReplaceRole() {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return

  try {
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${replaceRoleTarget.value}`, {
      method: 'DELETE',
    })
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${replaceRoleNewCode.value}`, {
      method: 'POST',
    })
    await refreshFoundUserAccess()
    replaceRoleTarget.value = ''
    messageOk.value = 'Роль заменена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось заменить роль.'
  }
}

async function removeRoleFromFound(role) {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return

  try {
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${role}`, {
      method: 'DELETE',
    })
    await refreshFoundUserAccess()
    messageOk.value = 'Роль снята.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось снять роль.'
  }
}

async function assignRoleToFound() {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return
  if (user.roles.includes(assignRoleCode.value)) {
    messageError.value = 'Такая роль уже назначена.'
    return
  }

  try {
    await authorizedApiRequest(`/api/admin/access/users/${user.id}/roles/${assignRoleCode.value}`, {
      method: 'POST',
    })
    await refreshFoundUserAccess()
    messageOk.value = 'Роль добавлена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось добавить роль.'
  }
}

async function resetPasswordForFoundUser() {
  resetMessages()
  const user = rolesFoundUser.value
  if (!user) return

  try {
    const payload = await authorizedApiRequest(`/api/admin/access/users/${user.id}/reset-password`, {
      method: 'POST',
    })
    passwordResetResult.value = payload
    await refreshFoundUserAccess()
    messageOk.value = 'Одноразовая ссылка для установки нового пароля создана.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось сбросить пароль пользователя.'
  }
}

async function copyPasswordResetLink() {
  resetMessages()
  const link = absolutePasswordResetLink.value
  if (!link) {
    messageError.value = 'Ссылка для сброса пароля недоступна.'
    return
  }

  try {
    await navigator.clipboard.writeText(link)
    messageOk.value = 'Ссылка скопирована в буфер обмена.'
  } catch {
    messageError.value = 'Не удалось скопировать ссылку. Скопируйте ее вручную.'
  }
}

async function loadRoleUsers({ name = '', email = '', pagenum = 0, pagesize = 20 } = {}) {
  try {
    const search = new URLSearchParams()
    search.set('pagenum', String(Math.max(0, pagenum)))
    search.set('pagesize', String(Math.min(Math.max(1, pagesize), 100)))
    if (String(name || '').trim()) search.set('name', String(name).trim())
    if (String(email || '').trim()) search.set('email', String(email).trim())

    const payload = await authorizedApiRequest(`/api/admin/access/users?${search.toString()}`, {
      method: 'GET',
    })

    roleUsersList.value = Array.isArray(payload?.content) ? payload.content : []
    if (rolesSelectedEmail.value && !roleUserByEmail.value.has(String(rolesSelectedEmail.value).toLowerCase())) {
      rolesSelectedEmail.value = ''
    }
    if (rolesFoundEmail.value && !roleUserByEmail.value.has(String(rolesFoundEmail.value).toLowerCase())) {
      rolesFoundEmail.value = ''
    }
  } catch (error) {
    roleUsersList.value = []
    messageError.value = error.message || 'Не удалось загрузить список пользователей.'
  }
}

async function loadRepresentativeUsers({ name = '', email = '', pagenum = 0, pagesize = 20 } = {}) {
  try {
    const search = new URLSearchParams()
    search.set('pagenum', String(Math.max(0, pagenum)))
    search.set('pagesize', String(Math.min(Math.max(1, pagesize), 100)))
    search.set('role', 'TEAM_REP')
    if (String(name || '').trim()) search.set('name', String(name).trim())
    if (String(email || '').trim()) search.set('email', String(email).trim())

    const payload = await authorizedApiRequest(`/api/admin/access/users?${search.toString()}`, {
      method: 'GET',
    })

    repUsersList.value = Array.isArray(payload?.content) ? payload.content : []

    const selectedEmail = String(repSelectedEmail.value || '').toLowerCase()
    if (selectedEmail && !repUserByEmail.value.has(selectedEmail)) {
      repSelectedEmail.value = ''
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
    }
  } catch (error) {
    repUsersList.value = []
    messageError.value = error.message || 'Не удалось загрузить представителей команд.'
  }
}

async function refreshRepresentativeAccessByEmail(email) {
  const user = repUserByEmail.value.get(String(email || '').toLowerCase())
  if (!user) {
    repFoundEmail.value = ''
    repUserAccess.value = null
    repSelectedTeamId.value = ''
    return
  }

  await refreshRepresentativeAccessById(user.id)
}

async function refreshRepresentativeAccessById(userId) {
  const payload = await authorizedApiRequest(`/api/admin/access/users/${userId}`, {
    method: 'GET',
  })

  repUserAccess.value = payload
  const normalizedEmail = String(payload?.email || '').toLowerCase()
  repFoundEmail.value = normalizedEmail
  repSelectedEmail.value = normalizedEmail
  repSelectedTeamId.value = payload?.teamScopes?.[0]?.teamId ? String(payload.teamScopes[0].teamId) : ''

  const index = repUsersList.value.findIndex((item) => String(item.email || '').toLowerCase() === normalizedEmail)
  if (index >= 0) {
    repUsersList.value[index] = {
      ...repUsersList.value[index],
      id: payload.userId,
      email: payload.email,
      name: payload.name,
      roles: Array.isArray(payload.roles) ? payload.roles : [],
    }
  }
}

function findRepresentative() {
  resetMessages()
  const emailFilter = String(repSearch.value || repSelectedEmail.value || '').trim()
  if (!emailFilter) {
    messageError.value = 'Введите email или выберите представителя.'
    return
  }

  void loadRepresentativeUsers({ email: emailFilter, pagenum: 0, pagesize: 50 }).then(async () => {
    const selected = String(repSelectedEmail.value || '').trim().toLowerCase()
    if (selected) {
      await refreshRepresentativeAccessByEmail(selected)
      return
    }

    if (repUsersList.value.length === 1) {
      const onlyEmail = String(repUsersList.value[0].email || '').toLowerCase()
      await refreshRepresentativeAccessByEmail(onlyEmail)
      return
    }

    if (!repUsersList.value.length) {
      messageError.value = 'Представитель не найден.'
      repFoundEmail.value = ''
      repUserAccess.value = null
      repSelectedTeamId.value = ''
      return
    }

    messageError.value = 'Выберите представителя из найденного списка.'
  })
}

async function saveRepresentativeTeam() {
  resetMessages()
  const user = repFoundUser.value
  if (!user) {
    messageError.value = 'Сначала выберите представителя.'
    return
  }

  const nextTeamId = Number(repSelectedTeamId.value)
  if (!Number.isFinite(nextTeamId) || nextTeamId <= 0) {
    messageError.value = 'Выберите команду.'
    return
  }

  const currentScopes = repTeamScopes.value
  if (currentScopes.length === 1 && Number(currentScopes[0].teamId) === nextTeamId) {
    messageError.value = 'Эта команда уже назначена представителю.'
    return
  }

  try {
    for (const scope of currentScopes) {
      await authorizedApiRequest(`/api/admin/access/users/${user.id}/team-scopes/${scope.teamId}`, {
        method: 'DELETE',
      })
    }

    await authorizedApiRequest(`/api/admin/access/users/${user.id}/team-scopes`, {
      method: 'POST',
      body: JSON.stringify({
        teamId: nextTeamId,
        canEditRoster: true,
        canEditApplication: true,
      }),
    })

    await refreshRepresentativeAccessById(user.id)
    messageOk.value = currentScopes.length ? 'Команда представителя обновлена.' : 'Команда представителю назначена.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось назначить команду представителю.'
  }
}

async function unassignRepresentativeTeam() {
  resetMessages()
  const user = repFoundUser.value
  if (!user) {
    messageError.value = 'Сначала выберите представителя.'
    return
  }

  if (!repTeamScopes.value.length) {
    messageError.value = 'У представителя нет активной привязки к команде.'
    return
  }

  try {
    for (const scope of repTeamScopes.value) {
      await authorizedApiRequest(`/api/admin/access/users/${user.id}/team-scopes/${scope.teamId}`, {
        method: 'DELETE',
      })
    }

    await refreshRepresentativeAccessById(user.id)
    repSelectedTeamId.value = ''
    messageOk.value = 'Команда откреплена от представителя.'
  } catch (error) {
    messageError.value = error.message || 'Не удалось открепить команду.'
  }
}

async function refreshFoundUserAccess() {
  const user = rolesFoundUser.value
  if (!user) return

  const payload = await authorizedApiRequest(`/api/admin/access/users/${user.id}`, {
    method: 'GET',
  })

  const normalizedEmail = String(payload?.email || '').toLowerCase()
  const index = roleUsersList.value.findIndex((item) => String(item.email || '').toLowerCase() === normalizedEmail)
  if (index >= 0) {
    roleUsersList.value[index] = {
      ...roleUsersList.value[index],
      id: payload.userId,
      email: payload.email,
      name: payload.name,
      roles: Array.isArray(payload.roles) ? payload.roles : [],
      mustChangePassword: Boolean(payload.mustChangePassword),
    }
  }
}

function unbanUser(email) {
  resetMessages()
  const user = userByEmail.value.get(String(email || '').trim().toLowerCase())
  if (!user) return

  user.banned = false
  user.banReason = ''
  user.updatedAt = new Date().toISOString()
  saveToStorage(USERS_KEY, usersRegistry.value)
  messageOk.value = 'Пользователь разблокирован.'
}

function formatDateOnly(value) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function formatDateTime(value) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

onMounted(async () => {
  await loadSeasonRegistry()
  await loadTeamRegistry()
  await loadPlayerRegistry()
  await loadRefereeRegistry()
  await loadRoleUsers({ pagenum: 0, pagesize: 20 })
  await loadRepresentativeUsers({ pagenum: 0, pagesize: 20 })
  await loadSeasons()
  if (seasonsList.value.length) {
    tourSeasonId.value = String(seasonsList.value[0].id)
    await onTourSeasonChange()
  }
})
</script>

<style scoped>
.admin-temporal-input {
  min-height: 44px;
  border-radius: 12px;
  border-color: rgba(124, 163, 255, 0.34);
  background:
    linear-gradient(180deg, rgba(31, 43, 86, 0.96), rgba(16, 24, 53, 0.98)),
    rgba(19, 26, 52, 0.98);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 8px 22px rgba(3, 8, 24, 0.24);
  color: var(--text);
  letter-spacing: 0.02em;
}

.admin-temporal-input:hover {
  border-color: rgba(97, 232, 162, 0.52);
}

.admin-temporal-input:focus-visible {
  outline: none;
  border-color: rgba(97, 232, 162, 0.78);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 3px rgba(97, 232, 162, 0.16),
    0 10px 26px rgba(3, 8, 24, 0.28);
}

.admin-tab-groups {
  align-items: start;
}

.admin-tab-group {
  align-content: start;
}

.admin-tabs-grid {
  align-content: start;
}

.admin-temporal-input-wide {
  font-weight: 600;
}

.admin-season-form {
  display: grid;
  gap: 18px;
}

.admin-season-edit-toolbar {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-season-edit-picker {
  flex: 1 1 320px;
}

.admin-season-export-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.admin-season-export-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 280px;
  padding: 8px;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: rgba(10, 16, 37, 0.98);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.28);
  z-index: 4;
}

.admin-season-export-action {
  width: 100%;
  justify-content: flex-start;
}

.admin-season-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
  align-items: stretch;
}

.admin-season-field {
  min-width: 0;
}

.admin-season-field-wide {
  grid-column: 1 / -1;
}

.admin-season-toggle-field {
  display: grid;
  gap: 6px;
}

.admin-season-toggle-control {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 56px;
  padding: 0 16px;
  border: 1px solid rgba(124, 163, 255, 0.18);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(24, 35, 72, 0.88), rgba(14, 22, 48, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.admin-season-toggle-control input {
  margin: 0;
}

.admin-season-toggle-control span {
  line-height: 1.25;
}

.admin-season-section {
  display: grid;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  background: linear-gradient(180deg, rgba(18, 27, 57, 0.82), rgba(11, 18, 41, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.admin-season-section-compact {
  gap: 12px;
  padding: 14px 16px;
}

.admin-season-section-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.admin-season-section-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.admin-season-section-head-compact {
  align-items: center;
}

.admin-season-team-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: stretch;
  width: min(100%, 720px);
}

.admin-season-team-row select {
  min-width: 0;
}

.admin-season-team-row .btn-ghost {
  white-space: nowrap;
}

.admin-season-selected-note {
  margin: -2px 0 0;
  font-size: 0.84rem;
  letter-spacing: 0.02em;
}

.admin-season-empty-state {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px dashed rgba(124, 163, 255, 0.18);
  background: rgba(255, 255, 255, 0.025);
}

.admin-season-team-list {
  gap: 10px;
}

.admin-season-picked-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}

.admin-season-picked-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.admin-season-picked-copy p {
  margin: 0;
}

.admin-referee-list-item {
  align-items: center;
}

.admin-season-meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.admin-season-meta-card {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(255, 255, 255, 0.03);
}

.admin-season-meta-card p {
  margin: 0;
}

.admin-season-meta-card strong {
  font-size: 1rem;
  color: #f2f5ff;
}

.admin-season-meta-card-accent {
  border-color: rgba(97, 232, 162, 0.22);
  background: linear-gradient(180deg, rgba(97, 232, 162, 0.08), rgba(255, 255, 255, 0.03));
}

.admin-season-meta-label {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #9eb4ff;
}

.admin-season-rules-panel {
  margin-top: 2px;
}

.admin-ranking-rule-list {
  display: grid;
  gap: 10px;
}

.admin-ranking-rule-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(124, 163, 255, 0.18);
}

.admin-ranking-rule-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: rgba(97, 232, 162, 0.14);
  border: 1px solid rgba(97, 232, 162, 0.28);
  color: #aef3ca;
  font-weight: 700;
}

.admin-ranking-rule-field {
  min-width: 0;
}

.admin-ranking-rule-label {
  display: block;
  margin-bottom: 6px;
  font-size: 0.84rem;
  color: var(--muted);
}

.admin-season-rules-footer {
  display: grid;
  gap: 6px;
}

.admin-season-rules-summary {
  color: #dfe8ff;
}

.admin-season-actions {
  justify-content: flex-start;
  padding-top: 4px;
}

.admin-temporal-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  filter: invert(88%) sepia(17%) saturate(1186%) hue-rotate(88deg) brightness(103%) contrast(88%);
  opacity: 0.9;
}

.admin-checkbox-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-inline-check {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.admin-team-logo-preview {
  width: 84px;
  height: 84px;
  padding: 6px;
  object-fit: contain;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.admin-team-editor-shell {
  display: grid;
  gap: 16px;
}

.admin-team-identity-card {
  display: grid;
  gap: 14px;
}

.admin-team-identity-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.admin-team-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 10px;
}

.admin-team-summary-card {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(124, 163, 255, 0.16);
  background: rgba(15, 22, 50, 0.72);
}

.admin-team-summary-card.is-accent {
  border-color: rgba(97, 232, 162, 0.32);
  background: rgba(17, 43, 39, 0.52);
}

.admin-team-summary-label {
  font-size: 0.76rem;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--muted);
}

.admin-team-identity-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.admin-team-logo-field {
  grid-column: span 2;
}

.admin-team-logo-preview-wrap {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.admin-team-management-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.admin-team-management-card {
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(11, 17, 39, 0.82);
  border-radius: 20px;
}

.admin-team-management-head {
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.admin-team-head-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.admin-team-management-toolbar {
  min-height: 44px;
}

.admin-team-management-toolbar-spacer {
  display: block;
}

.admin-team-season-select-field {
  display: grid;
  gap: 8px;
}

.admin-team-picker-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: stretch;
  gap: 10px;
}

.admin-team-picker-side {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  flex-wrap: wrap;
}

.admin-team-picker-side-inline {
  min-width: 0;
}

.admin-team-picker-count {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 12px;
  background: rgba(124, 163, 255, 0.1);
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 600;
}

.admin-team-picker-side .btn-primary,
.admin-team-picker-side .btn-danger {
  min-width: 220px;
}

.admin-player-manage-item {
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
}

.admin-player-manage-copy {
  display: grid;
  gap: 4px;
}

.admin-team-season-tools {
  display: grid;
  gap: 12px;
}

.admin-team-season-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-team-season-action-block {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid rgba(124, 163, 255, 0.14);
  background: rgba(17, 24, 52, 0.52);
}

.admin-team-season-control {
  display: grid;
  gap: 8px;
}

.admin-team-management-card :deep(.searchable-select.is-multiple.is-open .searchable-select-dropdown) {
  max-height: none;
}

.admin-season-player-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.admin-season-player-badge.is-selected {
  background: rgba(97, 232, 162, 0.18);
  color: #8ff0bb;
}

.admin-season-player-badge.is-not-selected {
  background: rgba(124, 163, 255, 0.16);
  color: #b5c7ff;
}

.admin-sticky-actions-spacer {
  height: 0;
}

.admin-sticky-actions {
  position: sticky;
  bottom: 0;
  z-index: 12;
  margin-top: 8px;
  padding: 14px 16px calc(14px + env(safe-area-inset-bottom, 0px));
  border-radius: 16px;
  border: 1px solid rgba(124, 163, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(16, 24, 53, 0.96), rgba(10, 16, 38, 0.98)),
    rgba(10, 16, 38, 0.98);
  box-shadow: 0 -12px 30px rgba(3, 8, 24, 0.28);
  backdrop-filter: blur(10px);
}

@media (max-width: 960px) {
  .admin-team-identity-head,
  .admin-team-picker-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .admin-team-management-toolbar-spacer {
    display: none;
  }

  .admin-team-identity-grid,
  .admin-team-summary-grid,
  .admin-season-grid,
  .admin-season-meta-grid {
    grid-template-columns: 1fr;
  }

  .admin-season-section-head,
  .admin-season-section-head-compact,
  .admin-ranking-rule-card {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .admin-season-toggle-control {
    min-height: 52px;
  }

  .admin-season-team-row,
  .admin-season-picked-item,
  .admin-referee-list-item {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .admin-season-picked-item,
  .admin-referee-list-item {
    align-items: stretch;
  }

  .admin-team-management-grid {
    grid-template-columns: 1fr;
  }

  .admin-team-logo-field {
    grid-column: auto;
  }

  .admin-team-picker-side {
    min-width: 0;
    align-items: stretch;
  }

  .admin-sticky-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .admin-team-logo-preview {
    width: 72px;
    height: 72px;
  }
}

.admin-inline-check input[type='checkbox'] {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: #d63b57;
  cursor: pointer;
}

.tour-matches-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.tour-match-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.tour-match-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tour-match-status-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.05);
}

.tour-match-status-badge.is-scheduled {
  color: #bfd0ff;
  border-color: rgba(124, 163, 255, 0.34);
  background: rgba(86, 122, 214, 0.16);
}

.tour-match-status-badge.is-lineups {
  color: #ffe2a3;
  border-color: rgba(255, 196, 84, 0.34);
  background: rgba(255, 196, 84, 0.14);
}

.tour-match-status-badge.is-live {
  color: #ffcfbf;
  border-color: rgba(255, 124, 84, 0.34);
  background: rgba(255, 124, 84, 0.14);
}

.tour-match-status-badge.is-finished {
  color: #d9dff8;
  border-color: rgba(188, 196, 230, 0.3);
  background: rgba(188, 196, 230, 0.12);
}

.tour-match-status-badge.is-verified {
  color: #bff8d8;
  border-color: rgba(97, 232, 162, 0.38);
  background: rgba(97, 232, 162, 0.14);
}

.tour-publish-note {
  margin-top: 16px;
}

.tour-publish-button {
  flex: 0 0 auto;
  min-width: 230px;
}

@media (max-width: 900px) {
  .tour-matches-header {
    align-items: stretch;
    flex-direction: column;
  }

  .tour-match-item {
    align-items: stretch;
    flex-direction: column;
  }

  .tour-publish-button {
    width: 100%;
  }
}

.tour-publish-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
 </style>
