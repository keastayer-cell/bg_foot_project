# Локальный Workflow Football Stats

## Главный принцип

С этого момента **единственный рабочий репозиторий** проекта:

- `/Users/korytov/projects/bg_foot_project`

Старые папки:

- `/Users/korytov/projects/football-stats-app`
- `/Users/korytov/projects/football-stats-web`

нужно считать **старыми рабочими копиями** и больше не использовать как основное место разработки.

## Ветки

- `dev` — локальная разработка
- `test` — ветка для test-стенда и автодеплоя
- `prod` — задел под production

## Как работать локально

### 1. Открывать именно monorepo

Рабочая папка:

```bash
cd /Users/korytov/projects/bg_foot_project
```

### 2. Работать в ветке dev

```bash
git checkout dev
git pull --rebase origin dev
```

### 3. Запуск backend локально

```bash
mvn -f app/pom.xml spring-boot:run
```

### 4. Запуск frontend локально

```bash
cd web
npm install
npm run dev
```

Frontend локально использует:

- `web/.env.local`
- `VITE_API_BASE_URL=http://127.0.0.1:8080`

Backend локально использует:

- `app/.env`

## Как выкатывать на test

После того как изменения проверены в `dev`:

```bash
git checkout test
git merge dev
git push origin test
```

Push в `test` запускает GitHub Actions и автодеплой на test VPS.

## Что не делать

- не править проект параллельно в старых папках `football-stats-app` и `football-stats-web`
- не считать старые папки актуальным источником правды
- не пушить рабочие изменения напрямую в `test`, если они не проверены в `dev`

## Практическая схема

1. Разработка локально в `dev`
2. Локальная проверка backend/frontend
3. Commit в `dev`
4. Merge `dev -> test`
5. Push `test`
6. GitHub Actions деплоит на `80.78.242.245`