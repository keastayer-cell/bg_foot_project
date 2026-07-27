# football-stats-app

Backend модуля Football Stats внутри monorepo.

## Что здесь лежит

- Spring Boot backend
- `pom.xml`
- `src/`
- backend resources и Flyway migration

## Локальный запуск

```bash
mvn -f app/pom.xml spring-boot:run
```

## Сборка

```bash
mvn -f app/pom.xml clean package
```

## Где лежат migration

Актуальные runtime-migration:

- `src/main/resources/db/migration`

## Где читать актуальную документацию

Актуальная документация хранится в корневом `README.md` и каталоге `docs/` этого репозитория.

## Что важно помнить

- backend и mailer читают единый корневой `.env`
- секреты и server layout описаны в `docs/Рефакторинг 28.07.2026/secrets-management.md`
- если меняется API, нужно синхронизировать API Explorer во frontend
