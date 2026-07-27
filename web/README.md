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
npm --prefix web install
npm --prefix web run dev
```

## Где читать актуальную документацию

Актуальная документация хранится в корневом `README.md` и каталоге `docs/` этого репозитория.

## Что важно помнить

- `web/.env.local` содержит только публичные `VITE_*` параметры
- DB/JWT/SMTP/SSH secrets во frontend env запрещены
- при изменении backend API нужно синхронизировать API Explorer
