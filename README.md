# bg_foot_project

Monorepo проекта Football Stats.

## Что здесь лежит

- `app` — backend на Spring Boot
- `web` — frontend на Vue
- `.github/workflows` — CI/CD

## Где читать актуальную документацию

Полный комплект project docs вынесен отдельно в:

- `/Users/korytov/projects/football-stat-readme`

Ключевые файлы там:

- `FOOTBALL_STATS_WORKFLOW_RU.md`
- `FOOTBALL_STATS_SESSION_CONTEXT.md`
- `FOOTBALL_STATS_RELEASES.md`
- `SYSTEM_DESCRIPTION_DETAILED_RU.md`

## Что важно помнить

- основной рабочий код находится в этом monorepo
- актуальные runtime-миграции лежат в `app/src/main/resources/db/migration`
- legacy root `db/migration` больше не используется как источник Flyway migration