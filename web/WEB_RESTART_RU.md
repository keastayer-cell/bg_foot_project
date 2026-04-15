# Перезапуск frontend локально

Инструкция для проекта `football-stats-web`.

## Самый удобный способ из VS Code

Я добавил готовые задачи VS Code в файл `.vscode/tasks.json`.

Запускать их можно так:

- `Terminal -> Run Task`
- или через `Cmd+Shift+P -> Tasks: Run Task`

Для frontend доступны задачи:

- `Restart Web Dev (4173)`
- `Build + Preview Web (4173)`

## Что использовать в обычной работе

Для разработки используй именно `dev`.

Это основной режим, в котором:

- фронт поднимается локально
- изменения подхватываются после правок
- не нужно каждый раз делать production-сборку

## Быстрый перезапуск web

Открой терминал и перейди в папку frontend:

```bash
cd /Users/korytov/projects/football-stats-web
```

Если текущий dev-сервер уже запущен в этом терминале, останови его через `Ctrl+C`.

После этого запусти заново:

```bash
npm run dev -- --host 127.0.0.1 --port 4173
```

Если увидишь сообщение `Port 4173 is in use, trying another one...`, это значит, что старый frontend уже висит на `4173`, и Vite автоматически поднял новый сервер на другом порту, например `4174`.

Если порт `4173` уже занят, найди процесс и останови его:

```bash
lsof -i :4173
kill -9 <PID>
```

Потом снова запусти:

```bash
npm run dev -- --host 127.0.0.1 --port 4173
```

## Проверка, что frontend поднялся

Открой в браузере:

```text
http://127.0.0.1:4173/
```

Если `4173` был занят, Vite может подняться, например, на `4174`. В таком случае открывай именно тот адрес, который он напишет в терминале:

```text
http://127.0.0.1:4174/
```

Если страница открывается, frontend запущен.

## Когда использовать build и preview

Эти команды нужны не для обычной разработки, а для проверки уже собранной версии.

Запуск:

```bash
cd /Users/korytov/projects/football-stats-web
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

Смысл такой:

- `npm run dev` = режим разработки
- `npm run build` = production-сборка в `dist/`
- `npm run preview` = просмотр уже собранного `dist/`

Если мы просто вносим правки и хотим быстро проверять UI, нужен именно `dev`.

## Рекомендуемый рабочий сценарий

Для обычной разработки:

```bash
cd /Users/korytov/projects/football-stats-web
Ctrl+C
npm run dev -- --host 127.0.0.1 --port 4173
```

Если тебе нужен именно фиксированный порт `4173` и ты не хочешь, чтобы Vite сам уходил на другой, запускай так:

```bash
cd /Users/korytov/projects/football-stats-web
npm run dev -- --host 127.0.0.1 --port 4173 --strictPort
```

Тогда Vite не переключится на `4174`, а сразу покажет, что порт занят.

Если хочешь запускать это без ручного ввода, используй задачу `Restart Web Dev (4173)`.

Если нужно проверить именно production-вариант:

```bash
cd /Users/korytov/projects/football-stats-web
Ctrl+C
npm run build
npm run preview -- --host 127.0.0.1 --port 4173
```

## Если frontend не стартует

Проверь по порядку:

- установлен ли `node -v`
- установлен ли `npm -v`
- существуют ли `node_modules`
- не занят ли порт `4173`

Если зависимостей нет, установи их:

```bash
cd /Users/korytov/projects/football-stats-web
npm install
```