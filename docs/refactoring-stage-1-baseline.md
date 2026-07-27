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

## Финальные проверки

```bash
mvn -f app/pom.xml clean test
mvn -f mailer/pom.xml clean test package
npm --prefix web test
npm --prefix web run build
npm --prefix web run test:e2e
```

Результат:

- backend `app` собирается и проходит 18 тестов;
- mailer собирается и проходит 2 теста;
- frontend проходит 55 unit/route-тестов, production build и 4 e2e smoke.

## Изменения этапа

- В `web/package.json` добавлено `"type": "module"`, чтобы Vite работал в ESM-режиме и не показывал предупреждение про deprecated CJS Node API.
- Корневой README больше не зависит от внешней локальной документации и описывает source of truth, окружение, запуск, проверки и test deploy.
- Локальный режим mailer по умолчанию изменён на безопасный `MAILER_TRANSPORT_TYPE=log`.
- Добавлен этот baseline-документ для фиксации выполненных проверок и дальнейшей поэтапной работы.

## Риски для следующих этапов

- Крупные frontend-компоненты требуют декомпозиции: `Admin.vue`, `Tours.vue`, `Match.vue`, `TeamRepDashboard.vue`.
- Production workflow, backup и rollback пока не реализованы; это задачи этапа 6.

## Статус исходного этапа

Этап завершён. Чистый checkout, ветки, Java 21, сборки всех модулей и самодостаточная документация проверены.
