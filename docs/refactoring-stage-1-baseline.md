# Refactoring Stage 1 Baseline

Дата проверки: 2026-07-27

## Цель этапа

Зафиксировать чистое состояние репозитория `keastayer-cell/bg_foot_project`, проверить базовую собираемость backend, mailer и frontend, а также записать найденные риски перед дальнейшим рефакторингом.

## Проверенный источник

- Репозиторий: `git@github.com:keastayer-cell/bg_foot_project.git`
- Ветка: `dev`
- Локальный чистый checkout для проверки: `/private/tmp/bg_foot_project_stage1`

Отдельное наблюдение: старая локальная папка `/Users/anton/projects (5)/bg_foot_project` была в грязном/неактуальном состоянии: много файлов отображались как `AD`, а backend/mailer `src` отсутствовали на диске. Для дальнейшей работы source of truth должен быть чистый checkout из GitHub.

## Окружение проверки

- Java: OpenJDK 21.0.11
- Maven: 3.9.16
- Node.js: 24.16.0
- npm: 11.13.0

CI `deploy-test.yml` использует Java 21 и Node 20. Backend и mailer в текущем GitHub checkout также настроены на Java 21.

## Выполненные проверки

```bash
mvn -f app/pom.xml -DskipTests package
mvn -f mailer/pom.xml -DskipTests package
npm --prefix web ci
npm --prefix web run build
```

Результат:

- backend `app` собирается успешно;
- mailer собирается успешно;
- frontend собирается успешно;
- тестовых source-файлов пока не найдено, поэтому Maven сообщает `No sources to compile` для testCompile.

## Изменения этапа

- В `web/package.json` добавлено `"type": "module"`, чтобы Vite работал в ESM-режиме и не показывал предупреждение про deprecated CJS Node API.
- Добавлен этот baseline-документ для фиксации выполненных проверок и дальнейшей поэтапной работы.

## Риски для следующих этапов

- В проекте не найдено тестов для backend, mailer и frontend. Это главный риск перед рефакторингом бизнес-логики.
- Крупные frontend-компоненты требуют декомпозиции: `Admin.vue`, `Tours.vue`, `Match.vue`, `TeamRepDashboard.vue`.
- API debug logging в `web/src/main.js` сейчас включен глобально через monkey-patch `window.fetch`; перед production-полировкой его нужно спрятать за env-флаг.
- Нужно решить, какая локальная папка будет основной рабочей копией: старая грязная копия или новый чистый checkout.

## Статус исходного этапа

Этап выполнен частично. Чистый checkout, ветки, Java 21 и сборка проверены. Пункт о самодостаточном README не завершён: текущие README ссылаются на внешний локальный каталог `/Users/korytov/projects/football-stat-readme` и не описывают полный запуск/deploy внутри репозитория.
