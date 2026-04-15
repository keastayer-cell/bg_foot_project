# Перезапуск backend локально

Инструкция для проекта `football-stats-app`.

Связанная инструкция по frontend:

- `/Users/korytov/projects/football-stats-web/WEB_RESTART_RU.md`

## Самый удобный способ из VS Code

Я добавил готовые задачи VS Code в файл `.vscode/tasks.json`.

Запускать их можно так:

- `Terminal -> Run Task`
- или через `Cmd+Shift+P -> Tasks: Run Task`

Для backend доступны задачи:

- `Restart Backend (8080)`
- `Rebuild + Restart Backend (8080)`

## Что должно быть запущено заранее

- PostgreSQL
- Java 21
- Maven 3.9+
- заполнен файл `.env`

## Быстрый перезапуск

Открой терминал и перейди в папку backend:

```bash
cd /Users/korytov/projects/football-stats-app
```

Если backend уже запущен в текущем терминале, останови его через `Ctrl+C`.

Если процесс висит в фоне и порт `8080` занят, найди и останови его:

```bash
lsof -i :8080
kill -9 <PID>
```

После этого запусти backend заново:

```bash
mvn spring-boot:run
```

Нормальный признак успешного старта:

- в логах есть строка `Started ... in ... seconds`
- health-check отвечает на `http://127.0.0.1:8080/api/health`

## Проверка, что backend поднялся

В отдельном терминале:

```bash
curl http://127.0.0.1:8080/api/health
```

Ожидаемый ответ:

```json
{"status":"UP","service":"football-stats-app"}
```

## Полный перезапуск через сборку

Если нужно сначала пересобрать проект:

```bash
cd /Users/korytov/projects/football-stats-app
mvn clean package
mvn spring-boot:run
```

## Запуск jar после сборки

Если нужен запуск не через Maven, а напрямую из собранного jar:

```bash
cd /Users/korytov/projects/football-stats-app
mvn clean package
java -jar target/football-stats-app-0.0.1-SNAPSHOT.jar
```

## Если backend не стартует

Проверь по порядку:

- не занят ли порт `8080`
- доступна ли база PostgreSQL
- заполнен ли `.env`
- установлены ли `java -version` и `mvn -version`

## Рекомендуемый рабочий сценарий

Для обычной разработки достаточно такого цикла:

```bash
cd /Users/korytov/projects/football-stats-app
Ctrl+C
mvn spring-boot:run
```

Если хочешь, чтобы backend поднимался через готовую кнопку в VS Code, запускай задачу `Restart Backend (8080)`.

Если менялись зависимости или есть странное поведение после правок, используй:

```bash
cd /Users/korytov/projects/football-stats-app
Ctrl+C
mvn clean package
mvn spring-boot:run
```