# APP Session Context (RU)

## 1) Что это за проект

Это backend часть общего проекта статистики футбола.

- Backend: `football-stats-app` (Spring Boot, Java 21, PostgreSQL, Flyway, JWT)
- Frontend: `football-stats-web` (Vue 3 + Vite)

Локальные адреса:

- Frontend preview: `http://127.0.0.1:4173`
- Backend API: `http://127.0.0.1:8080`
- PostgreSQL: `localhost:5432`, db `football_db`, schema `work`

## 2) Главные договоренности по безопасности и доступам

### 2.1 Два уровня прав

1. API-level (кто может вызывать endpoint/метод):
- Хранится в матрице `work.w_api_access_rule`
- Поля: `role_id`, `url_pattern`, `http_method`, `active`
- Загружается и кешируется сервисом `ApiAccessRuleService`
- Проверяется фильтром `ApiAccessRuleFilter`

2. Data-level (какие данные можно менять):
- Для TEAM_REP ограничение идет по команде через `work.w_user_team_scope`
- Даже если API-level разрешен, редактировать можно только свою команду

### 2.2 SUPER_ADMIN

- Для роли `SUPER_ADMIN` сделан code-level bypass в `ApiAccessRuleService`: полный доступ ко всем API
- В матрице `w_api_access_rule` записи для `SUPER_ADMIN` не обязательны
- По просьбе пользователя записи `SUPER_ADMIN` из матрицы удалены

## 3) Что сделано по сезонам

### 3.1 Таблица сезонов

Таблица: `work.w_season`

- `id`
- `name` (уникально)
- `created_by_user_id`
- `updated_by_user_id` (добавлено)
- `active` (soft delete)
- `created_at`
- `updated_at`

### 3.2 Логика сезонов

- Один endpoint списка: `GET /api/seasons`
- Параметр фильтра: `active_flag`
  - `1` -> только активные
  - `0` -> все
- В админке используется `active_flag=1` (возвращать только активные)
- Дубликаты названий запрещены
- Создание/изменение/деактивация пишут user id в соответствующие поля

### 3.3 Привязка команд к сезону

Добавлена отдельная таблица связей: `work.w_season_team`.

- Один сезон может содержать несколько команд
- Привязка команд к сезону редактируется отдельно от карточки сезона
- В админке выбор сделан через выпадающий список доступных команд + добавление в список выбранных ниже
- Сохранение выполняется полной заменой набора команд сезона

API для этого:

- `GET /api/seasons/{seasonId}/teams`
- `PUT /api/seasons/{seasonId}/teams`

Формат сохранения:

- тело: `{ "teamIds": [1, 2, 3] }`
- backend также корректно обрабатывает значения, пришедшие строками из UI, если они маппятся в числа

### 3.4 Туры и матчи тура

Добавлены отдельные сущности и таблицы:

- `work.w_tour` — тур, привязанный к сезону
- `work.w_tour_match` — матч внутри тура

Основные правила:

- Каждый тур создается внутри выбранного сезона
- Матчи создаются внутри выбранного тура
- Для матча выбираются только команды, уже привязанные к сезону этого тура
- Домашняя и гостевая команды не могут совпадать

API для туров:

- `GET /api/tours?season_id=...`
- `POST /api/tours`
- `GET /api/tours/{tourId}/matches`
- `POST /api/tours/{tourId}/matches`

Также `GET /api/teams` расширен фильтром:

- `season_id` — вернуть только команды, привязанные к конкретному сезону

### 3.5 Конфигуратор сезона

Сезон больше хранит не только название, но и настройки формата проведения:

- `rounds_count` — количество кругов регулярного этапа
- `playoff_enabled` — включен ли плей-офф
- `playoff_team_count` — сколько команд выходит в плей-офф

Текущее правило реализации:

- регулярные туры рассчитываются автоматически из количества команд сезона и `rounds_count`
- ручное создание туров для регулярного этапа отключено
- раздел "Управление турами" должен работать как выбор уже рассчитанного тура и наполнение его матчами
- плей-офф пока хранится как конфигурация сезона; автоматическая генерация сетки будет добавлена следующим этапом
- стартовая страница frontend теперь берет список сезонов, команды сезона и туры сезона напрямую через API, без локальных моков
- для публичной стартовой страницы добавлен агрегирующий endpoint `GET /api/seasons/{seasonId}/overview`, который одним вызовом возвращает сезон, команды сезона и только опубликованные туры вместе с матчами

### 3.6 Турнирная таблица сезона

Для сезона реализован отдельный persist-слой турнирной таблицы.

Новые таблицы:

- `work.w_season_standings_config` — правила начисления очков и время последнего пересчета
- `work.w_season_standings_row` — готовые строки таблицы сезона

Текущая логика:

- `GET /api/seasons/{seasonId}/overview` теперь возвращает не только сезон, команды и опубликованные туры, но и:
  - `standingsConfig`
  - `standings`
- таблица не пересчитывается на каждом `GET`
- backend пересчитывает и сохраняет снэпшот таблицы отдельно
- для старых подтвержденных матчей сделан backfill миграцией `V27`
- правила по умолчанию сейчас такие:
  - победа = 3 очка
  - ничья = 1 очко
  - поражение = 0 очков

Порядок сортировки строк:

- очки
- разница мячей
- забитые мячи
- название команды

Формула количества туров регулярки:

- если команд четное число `N`, то туров: `(N - 1) * rounds_count`
- если команд нечетное число `N`, то туров: `N * rounds_count`

Ограничение на изменение структуры:

- если для регулярного этапа уже заведены матчи, менять состав команд сезона нельзя
- если для регулярного этапа уже заведены матчи, менять `rounds_count` нельзя
- это сделано сознательно, чтобы не получать рассинхрон между уже заведенными матчами и пересчитанной структурой сезона

## 4) Что сделано по API-матрице

Таблица: `work.w_api_access_rule`

Актуальная идея:

- Гости/пользователи/представители имеют равные GET-права чтения
- TEAM_REP имеет mutating права только для своих функций, а фактическая проверка "своя команда" делается отдельным data-level check

На текущем этапе добавлены точечные активные правила для:

- GET:
  - `/api/seasons`
  - `/api/teams`
  - `/api/players`
  - `/api/players/*/history`
  - `/api/teams/*/players`
  - `/api/admin/access/me`
- TEAM_REP mutating:
  - `POST /api/teams/*/players/*`
  - `DELETE /api/teams/*/players/*`

## 5) Где назначается команда представителю

Привязка пользователя к команде делается через:

- API: `POST /api/admin/access/users/{userId}/team-scopes`
- Controller: `AdminAccessController`
- Service: `AccessControlService.assignTeamScope(...)`
- Таблица: `work.w_user_team_scope`

Именно эта привязка потом используется в проверке:

- `AccessControlService.hasTeamPermission(userId, teamId, permissionCode)`

### 5.1 Что добавлено для админки представителей

- В `GET /api/admin/access/users` добавлен необязательный параметр `role`
- Теперь админка может запрашивать только пользователей с конкретной ролью, например:
  - `GET /api/admin/access/users?role=TEAM_REP`
- Это используется как защита от ошибок оператора: в разделе управления представителями показываются только пользователи с ролью `TEAM_REP`
- Для назначения команды представителю используется тот же endpoint:
  - `POST /api/admin/access/users/{userId}/team-scopes`
- Для открепления команды используется:
  - `DELETE /api/admin/access/users/{userId}/team-scopes/{teamId}`
- Для чтения текущей привязки и ролей конкретного пользователя используется:
  - `GET /api/admin/access/users/{userId}`

## 6) Важные backend файлы

- Security config:
  - `src/main/java/com/footballstats/backend/config/SecurityConfig.java`
- JWT auth filter:
  - `src/main/java/com/footballstats/backend/security/JwtAuthenticationFilter.java`
- API matrix filter:
  - `src/main/java/com/footballstats/backend/security/ApiAccessRuleFilter.java`
- API matrix cache/service:
  - `src/main/java/com/footballstats/backend/service/ApiAccessRuleService.java`
- Team scope service:
  - `src/main/java/com/footballstats/backend/service/AccessControlService.java`
- Seasons controller/service/entity:
  - `src/main/java/com/footballstats/backend/controller/SeasonController.java`
  - `src/main/java/com/footballstats/backend/service/SeasonService.java`
  - `src/main/java/com/footballstats/backend/domain/Season.java`
- Season-team:
  - `src/main/java/com/footballstats/backend/domain/SeasonTeam.java`
  - `src/main/java/com/footballstats/backend/repository/SeasonTeamRepository.java`
- Tours:
  - `src/main/java/com/footballstats/backend/service/SeasonStructureService.java`
  - `src/main/java/com/footballstats/backend/controller/TourController.java`
  - `src/main/java/com/footballstats/backend/service/TourService.java`
  - `src/main/java/com/footballstats/backend/domain/Tour.java`
  - `src/main/java/com/footballstats/backend/domain/TourMatch.java`
  - `src/main/java/com/footballstats/backend/repository/TourRepository.java`
  - `src/main/java/com/footballstats/backend/repository/TourMatchRepository.java`

## 7) Миграции, относящиеся к текущему контексту

- `V11__season_last_editor.sql`
- `V12__api_access_rules.sql`
- `V13__fix_api_rules_seed.sql`
- `V14__team_rep_precise_api_rules.sql`
- `V15__team_update_audit_and_logo.sql`
- `V16__media_storage_for_team_and_player.sql`
- `V18__players_get_by_id_api_rule.sql`
- `V19__season_teams_and_tours.sql`
- `V20__season_format_and_tour_structure.sql`
- `V21__tour_published_flag.sql`
- `V22__match_protocol_and_events.sql`
- `V23__match_lineups.sql`
- `V24__season_players_and_team_rep_api.sql`
- `V25__match_protocol_technical_defeat.sql`
- `V26__season_standings.sql`
- `V27__backfill_season_standings.sql`

## 7.2) Заявки на матч

Добавлен отдельный слой для матчевых составов:

- `work.w_match_lineup` — одна заявка конкретной команды на конкретный матч
- `work.w_match_lineup_player` — игроки внутри заявки в заданном порядке

Текущие правила:

- заявка подается отдельно по каждой команде матча
- `SUPER_ADMIN` может подать заявку за любую команду матча
- `TEAM_REP` может подать заявку только за свою закрепленную команду и только если у него есть `canEditRoster`
- в заявку можно добавлять не всех игроков команды, а только пересечение двух условий одновременно:
  - игрок должен быть в текущем составе команды (`w_player_team.active = true`)
  - игрок должен быть заявлен именно на сезон матча через `w_season_player` для этой же команды
- если обе команды подали заявки, статус протокола автоматически переводится в `LINEUPS_SUBMITTED`
- если одну из заявок очистить, статус возвращается в `SCHEDULED`, пока администратор не перевел матч дальше по протоколу

API:

- `GET /api/matches/{matchId}` — теперь возвращает не только протокол, но и:
  - `homeLineup`
  - `awayLineup`
  - внутри каждой заявки также есть `availablePlayers`, уже отфильтрованные backend по правилу `состав команды ∩ сезон матча`
- `PUT /api/matches/{matchId}/lineups/{teamId}` — сохранить заявку команды на матч

## 7.3) Реальная сезонная заявка игроков и кабинет представителя

Старое поле `w_player.season_id` больше не считается главным источником истины для допуска в сезон и в состав матча.

Теперь введена отдельная таблица:

- `work.w_season_player`

Она хранит реальную заявку игрока на сезон в разрезе команды:

- `season_id`
- `team_id`
- `player_id`
- `active`
- audit-поля

Что это меняет:

- игрок может быть в текущем составе команды, но не быть заявлен на сезон
- один и тот же игрок может быть привязан к нескольким сезонам одновременно, это нормальная история участия
- один и тот же игрок не может быть заявлен за две разные команды в рамках одного и того же сезона
- принадлежность игрока к команде для сезонной заявки хранится именно в `w_season_player`, без искусственного глобального "трансфера" между командами
- удаление игрока из сезонной заявки не удаляет игрока из БД
- матчевые составы теперь валидируются через пересечение:
  - игрок в текущем составе команды
  - игрок есть в `w_season_player` для сезона матча и этой команды

Для TEAM_REP добавлены отдельные реальные endpoints кабинета представителя:

- `GET /api/team-rep/seasons`
- `GET /api/team-rep/players`
- `POST /api/team-rep/players`
- `PUT /api/team-rep/players/{playerId}`
- `GET /api/team-rep/seasons/{seasonId}/players`
- `PUT /api/team-rep/seasons/{seasonId}/players`
- `POST /api/team-rep/seasons/{seasonId}/players/{playerId}`
- `DELETE /api/team-rep/seasons/{seasonId}/players/{playerId}`

Смысл прав:

- создание/редактирование игрока требует `canEditRoster`
- сезонная заявка требует `canEditApplication`
- игрок, созданный представителем, сразу попадает в состав его команды
- при выборе существующего игрока из dropdown в кабинете представителя backend проверяет только сезонную уникальность: игрок должен быть свободен в выбранном сезоне, но может уже иметь участия в других сезонах и за другие команды

## 7.4) Вкладка игроков и popup карточки игрока

Экран `football-stats-web/src/pages/Players.vue` больше не использует хардкод внутри popup.

Текущее поведение:

- список игроков загружается через `GET /api/players`
- при клике на игрока popup делает реальные запросы:
  - `GET /api/players/{playerId}`
  - `GET /api/players/{playerId}/history`
- popup показывает реальные поля игрока:
  - ФИО
  - текущую команду
  - дату рождения
  - город проживания
  - голы
  - желтые карточки
  - красные карточки
  - историю команд
  - фото игрока, если оно есть в `w_media_asset`
- popup отдельно увеличен по размеру, чтобы карточка читалась как полноценный профиль

## 7.1) Хранение лого и фото

Текущая модель хранения изображений в БД:

- Таблица `work.w_media_asset` хранит медиа с явной привязкой:
  - `owner_type` (`TEAM` / `PLAYER`)
  - `owner_id`
  - `media_kind` (`TEAM_LOGO` / `PLAYER_PHOTO`)
  - `data_url`, `mime_type`, `created_by_user_id`, `created_at`, `active`
- В `work.w_team` добавлен `logo_media_id`
- В `work.w_player` добавлен `photo_media_id`

Таким образом из БД однозначно видно, чье это изображение: по `owner_type + owner_id + media_kind`.

Также в `work.w_team` добавлены audit-поля изменения:

- `updated_by_user_id`
- `updated_at`

## 8) Как проверять после изменений

Backend build:

```bash
mvn -q -DskipTests -f /Users/korytov/projects/football-stats-app/pom.xml package
```

Backend run:

```bash
mvn -f /Users/korytov/projects/football-stats-app/pom.xml spring-boot:run
```

Если после добавления новых `Repository`/`Service`/`Controller` классов `spring-boot:run` начинает падать с `NoClassDefFoundError` или `UnsatisfiedDependencyException` на devtools/restart classloader, сначала сделать чистую пересборку:

```bash
mvn -q -f /Users/korytov/projects/football-stats-app/pom.xml clean package
```

Для проверки спорных случаев можно запускать уже собранный jar, а не dev-mode:

```bash
java -jar /Users/korytov/projects/football-stats-app/target/football-stats-app-0.0.1-SNAPSHOT.jar
```

Frontend build:

```bash
cd /Users/korytov/projects/football-stats-web && npm run build
```

Проверка правил в БД:

```sql
select r.code as role, ar.url_pattern, ar.http_method, ar.active
from work.w_api_access_rule ar
join work.w_role r on r.id = ar.role_id
order by r.code, ar.url_pattern, ar.http_method;
```

## 9) Что помнить в следующих сессиях

- Пользователь просит без самодеятельности: делать только то, что явно попросил
- После важных правок нужно прогонять локальную сборку
- При изменении endpoint-ов нужно обновлять API Explorer на frontend
- Для TEAM_REP разграничение по данным (team scope) и по API (матрица) — это разные вещи
- Управление командами переведено на backend API (create/update/delete/list + `active_flag`)
- Логотипы команд и фото игроков сохраняются через `w_media_asset`, а не только в полях основных таблиц
- Для игроков `season_id/goals/yellow_cards/red_cards` используются как необязательные фильтры в `GET /api/players`; в `POST/PUT /api/players` эти поля не передаются
- Добавлен endpoint `GET /api/players/{playerId}` для получения карточки одного игрока
- Исправлен `PUT /api/players/{playerId}`: добавлена транзакция в update-метод `PlayerController`, чтобы убрать `LazyInitializationException` при формировании ответа
- Для раздела управления ролями добавлен endpoint `GET /api/admin/access/users` с пагинацией (`pagenum`, `pagesize`) и опциональными фильтрами `name`, `email`
- Раздел "Управление ролями" в админке переведен на backend API: поиск пользователей по email, выдача/снятие/замена ролей через `/api/admin/access/users/...`
- В `GET /api/admin/access/users` добавлен опциональный фильтр `role`; для представителей используется `role=TEAM_REP`
- В админке добавлен новый раздел "Управление Представителями"
- Раздел "Управление Представителями" работает только через backend API, без локальных заглушек
- В разделе "Управление Представителями" доступны операции:
  - поиск только пользователей с ролью `TEAM_REP`
  - назначение команды представителю
  - перепривязка представителя к другой команде
  - открепление команды от представителя
- В UI добавлена защита от ошибочной двойной привязки: если у представителя найдено несколько активных team scope, при сохранении старые привязки снимаются и назначается только выбранная команда
- Добавлена отдельная связь сезон -> команды через `work.w_season_team`
- Сезонный формат теперь хранится прямо в `work.w_season`: `rounds_count`, `playoff_enabled`, `playoff_team_count`
- Добавлен новый раздел админки "Управление турами"
- Туры и матчи хранятся в отдельных таблицах: `w_tour`, `w_tour_match`
- Для `w_tour` добавлены структурные поля `stage_type`, `round_number`, `sort_order`
- Для создания матча в туре можно выбирать только команды, привязанные к сезону этого тура
- `GET /api/tours` и `GET /api/tours/{tourId}/matches` открыты на чтение для публичной стартовой страницы
- для `GET /api/tours` добавлен опциональный фильтр `published_flag`
- у `w_tour` есть флаг `published`; на сайт должны попадать только опубликованные туры
- в админке для выбранного тура есть действие "Опубликовать тур"
- В API Explorer обязательно поддерживать в актуальном состоянии новые endpoint-ы по сезонам, турам и матчам тура
- Исправлен `PUT /api/seasons/{seasonId}/teams`: после `deleteAllBySeason_Id(...)` нужен явный `seasonTeamRepository.flush()`, иначе при повторном сохранении того же набора команд возможен `DataIntegrityViolationException` из-за уникального ограничения `(season_id, team_id)` в той же транзакции
- Исправлен `GET /api/tours/{tourId}/matches`: список матчей нужно читать с `JOIN FETCH` для `tour`, `homeTeam`, `awayTeam`, иначе при формировании ответа возможен `LazyInitializationException: could not initialize proxy ... Team ... - no Session`
- Регулярные туры теперь генерируются автоматически сервисом `SeasonStructureService` на основе количества команд сезона и `rounds_count`
- `POST /api/tours` оставлен только как legacy endpoint, но ручное создание регулярных туров запрещено бизнес-правилом
- Реализован первый реальный слой протокола матча без фронтовых моков:
  - таблица `work.w_match_protocol` хранит статус матча, счет, лучшего игрока, технические поражения, заметки и временные метки
  - таблица `work.w_match_event` хранит реальные события матча: голы, карточки, замены и другие отметки протокола
  - для уже существующих матчей протоколы backfill-ятся миграцией `V22`
- Добавлен публичный endpoint `GET /api/matches/{matchId}`:
  - возвращает матч, команды, сезон, тур, счет/статус, составы команд на матч и список событий протокола
- Добавлен endpoint `PUT /api/matches/{matchId}/protocol`:
  - сохраняет реальный протокол матча в БД
  - теперь работает в модели `playerStats`, а не как ручной редактор списка событий
  - backend валидирует, что статистика указывается только для игроков из заявок матча
  - backend валидирует, что сумма голов по игрокам совпадает со счетом, если нет технического поражения
  - backend сам генерирует `MatchEvent` из статистики игроков
  - поддерживаются `homeTechnicalDefeat` и `awayTechnicalDefeat`
  - при техническом поражении счет принудительно становится `3:0` или `0:3`
- `GET /api/seasons/{seasonId}/overview` расширен:
  - у вложенных матчей теперь есть `status`, `homeScore`, `awayScore`
  - у сезона теперь также есть `standingsConfig` и `standings`
- Стартовая страница frontend больше не показывает только голое имя тура:
  - у карточек туров выводится дата тура
  - тур раскрывается в список матчей в той же вкладке
  - каждый матч кликабелен и ведет на страницу `/match/{id}`
- `football-stats-web/src/pages/Match.vue` переведен с локального fake-store на реальный `GET /api/matches/{id}` и уже работает с живыми составами и протоколом
- стартовая страница `football-stats-web/src/pages/Tours.vue` теперь показывает сохраненную турнирную таблицу слева и опубликованные туры справа
