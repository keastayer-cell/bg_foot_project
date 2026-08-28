COMPOSE := docker compose

.DEFAULT_GOAL := help

.PHONY: help up db down logs ps smoke psql test reset-db web-deps clean-build run-backend run-web config config-show

help: ## Показать доступные команды
	@awk 'BEGIN {FS = ":.*## "; printf "Команды BG Foot:\n"} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-14s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

up: config ## Собрать и запустить весь локальный стек
	$(COMPOSE) --profile app up -d --build --wait --wait-timeout 180

db: config ## Запустить только PostgreSQL
	$(COMPOSE) up -d postgres

down: ## Остановить стек без удаления данных
	$(COMPOSE) --profile app down

logs: ## Следить за логами; фильтр: make logs s=backend
	$(COMPOSE) --profile app logs -f $(s)

ps: ## Показать состояние контейнеров
	$(COMPOSE) --profile app ps

smoke: ## Проверить здоровье всех сервисов по фактическим портам
	@set -eu; \
	B=$$($(COMPOSE) --profile app port backend 8080 | head -n 1 | awk -F: '{print $$NF}'); \
	M=$$($(COMPOSE) --profile app port mailer 8090 | head -n 1 | awk -F: '{print $$NF}'); \
	W=$$($(COMPOSE) --profile app port web 5173 | head -n 1 | awk -F: '{print $$NF}'); \
	test -n "$$B" && test -n "$$M" && test -n "$$W"; \
	curl -fsS "http://127.0.0.1:$$B/api/health" && echo "" || { echo "FAIL backend"; exit 1; }; \
	curl -fsS "http://127.0.0.1:$$M/actuator/health/readiness" && echo "" || { echo "FAIL mailer"; exit 1; }; \
	curl -fsS -o /dev/null -w "web: HTTP %{http_code}\n" "http://127.0.0.1:$$W" || { echo "FAIL web"; exit 1; }

psql: db ## Открыть psql внутри контейнера
	$(COMPOSE) exec postgres sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'

test: ## Запустить backend, mailer и frontend проверки
	mvn -f app/pom.xml test
	mvn -f mailer/pom.xml test
	npm --prefix web run lint
	npm --prefix web test
	npm --prefix web run build

reset-db: ## Удалить данные БД и frontend-зависимости; требуется CONFIRM=yes
	@if [ "$(CONFIRM)" != "yes" ]; then \
		echo "Отказ: команда удалит тома pgdata и web_node_modules."; \
		echo "Для подтверждения выполните: make reset-db CONFIRM=yes"; \
		exit 1; \
	fi
	$(COMPOSE) --profile app down -v
	$(MAKE) up

web-deps: ## Обновить node_modules после изменения package-lock.json
	$(COMPOSE) --profile app run --rm --no-deps web npm ci

clean-build: ## Полностью пересобрать образы и обновить node_modules
	$(COMPOSE) --profile app down
	$(COMPOSE) --profile app build --no-cache
	$(MAKE) web-deps

run-backend: db ## Запустить backend на хосте против контейнерной БД
	@DB_HOST=127.0.0.1 \
	DB_PORT=$$($(COMPOSE) port postgres 5432 | head -n 1 | awk -F: '{print $$NF}') \
	mvn -f app/pom.xml spring-boot:run

run-web: ## Запустить frontend на хосте
	npm --prefix web run dev -- --host 127.0.0.1

config: ## Проверить конфигурацию Compose без вывода секретов
	$(COMPOSE) --profile app config --quiet

config-show: ## Показать конфигурацию Compose; вывод содержит секреты
	$(COMPOSE) --profile app config
