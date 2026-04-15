# football-stats-web

Frontend модуля Football Stats внутри monorepo.

## Что здесь лежит

- Vue frontend
- `src/`
- `public/`
- `package.json`
- Vite config

## Локальный запуск

```bash
cd /Users/korytov/projects/bg_foot_project/web
npm install
npm run dev
```

## Где читать актуальную документацию

Единый комплект документов вынесен в:

- `/Users/korytov/projects/football-stat-readme`

Ключевые файлы:

- `FOOTBALL_STATS_WORKFLOW_RU.md`
- `FOOTBALL_STATS_SESSION_CONTEXT.md`

## Что важно помнить

- основной рабочий репозиторий: `/Users/korytov/projects/bg_foot_project`
- frontend-specific session docs больше не поддерживаются отдельными markdown-файлами внутри `web`
- при изменении backend API нужно синхронизировать API Explorer