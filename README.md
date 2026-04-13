# bg_foot_project

Единый репозиторий проекта статистики футбола.

## Структура

- `app` — Spring Boot backend
- `web` — Vue 3 frontend
- `db` — Flyway SQL migrations, вынесенные отдельно для удобства просмотра

## Локальный запуск

### Backend

```bash
mvn -q -DskipTests -f app/pom.xml package
mvn -f app/pom.xml spring-boot:run
```

### Frontend

```bash
cd web
npm install
npm run dev
```

## Примечания

- Секреты и локальные `.env` файлы в репозиторий не включены.
- Сборочные артефакты (`target`, `dist`, `node_modules`) исключены из git.