# football-stats-app

Backend модуля Football Stats внутри monorepo.

## Что здесь лежит

- Spring Boot backend
- `pom.xml`
- `src/`
- backend resources и Flyway migration

## Локальный запуск

```bash
cd /Users/korytov/projects/bg_foot_project
mvn -f app/pom.xml spring-boot:run
```

## Сборка

```bash
cd /Users/korytov/projects/bg_foot_project
mvn clean package -f app/pom.xml
```

## Где лежат migration

Актуальные runtime-migration:

- `src/main/resources/db/migration`

## Где читать актуальную документацию

Единый комплект документов вынесен в:

- `/Users/korytov/projects/football-stat-readme`

Ключевые файлы:

- `FOOTBALL_STATS_WORKFLOW_RU.md`
- `FOOTBALL_STATS_SESSION_CONTEXT.md`
- `SYSTEM_DESCRIPTION_DETAILED_RU.md`

## Что важно помнить

- основной рабочий репозиторий: `/Users/korytov/projects/bg_foot_project`
- backend документация больше не поддерживается отдельными markdown-файлами внутри `app`
- если меняется API, нужно синхронизировать API Explorer во frontend
