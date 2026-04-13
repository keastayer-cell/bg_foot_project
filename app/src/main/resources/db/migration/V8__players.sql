-- Игроки и история привязки к командам.
-- Игрок не привязан жёстко к команде — связь временная через w_player_team.

CREATE TABLE work.w_player (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(150) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_player_full_name_key UNIQUE (full_name)
);

COMMENT ON TABLE  work.w_player            IS 'Игроки';
COMMENT ON COLUMN work.w_player.id         IS 'Идентификатор игрока';
COMMENT ON COLUMN work.w_player.full_name  IS 'Полное имя игрока';
COMMENT ON COLUMN work.w_player.active     IS 'Активен ли игрок в системе';

CREATE TABLE work.w_player_team (
    id         BIGSERIAL PRIMARY KEY,
    player_id  BIGINT NOT NULL REFERENCES work.w_player(id) ON DELETE CASCADE,
    team_id    BIGINT NOT NULL REFERENCES work.w_team(id)   ON DELETE RESTRICT,
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to   DATE,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT w_player_team_unique_period UNIQUE (player_id, team_id, valid_from)
);

COMMENT ON TABLE  work.w_player_team            IS 'История привязки игроков к командам';
COMMENT ON COLUMN work.w_player_team.valid_from IS 'Дата начала привязки';
COMMENT ON COLUMN work.w_player_team.valid_to   IS 'Дата окончания привязки (NULL = текущий состав)';
COMMENT ON COLUMN work.w_player_team.active     IS 'Активна ли привязка';

-- ============================================================
-- Сид: игроки (57 уникальных; трое играли в разных командах)
-- ============================================================
INSERT INTO work.w_player (full_name) VALUES
    -- Север
    ('Иван Петров'), ('Павел Соколов'), ('Андрей Крылов'), ('Сергей Волков'),
    -- Юг
    ('Егор Климов'), ('Олег Жданов'), ('Руслан Котов'), ('Никита Громов'), ('Илья Терехов'),
    -- Восток
    ('Сергей Иванов'), ('Антон Белов'), ('Артем Ларин'),
    -- Центр
    ('Максим Романов'), ('Петр Фадеев'), ('Михаил Козлов'), ('Егор Панов'), ('Алексей Жданов'),
    -- Спартак
    ('Кирилл Гусев'), ('Артем Морозов'), ('Иван Седов'), ('Роман Беляев'), ('Глеб Орлов'),
    -- Динамо
    ('Даниил Брагин'), ('Павел Крылов'), ('Сергей Яковлев'), ('Игорь Титов'), ('Владислав Рябов'),
    -- Локомотив
    ('Антон Панкратов'), ('Виктор Голубев'), ('Николай Матвеев'), ('Алексей Блохин'), ('Семен Шаров'),
    -- Олимп
    ('Юрий Котов'), ('Степан Белов'), ('Петр Мелехин'), ('Артур Панов'), ('Егор Дмитриев'),
    -- Волга
    ('Роман Сычев'), ('Илья Гордеев'), ('Павел Минин'), ('Максим Белый'), ('Егор Шувалов'),
    -- Заря
    ('Станислав Осипов'), ('Михаил Доронин'), ('Дмитрий Кулагин'), ('Олег Назаров'), ('Руслан Фомин'),
    -- Старт
    ('Матвей Чернов'), ('Кирилл Федотов'), ('Григорий Мартынов'), ('Евгений Лавров'), ('Арсений Попов'),
    -- Факел
    ('Александр Трусов'), ('Никита Шестаков'), ('Петр Никифоров'), ('Сергей Дорофеев'), ('Иван Черкасов')
ON CONFLICT (full_name) DO NOTHING;

-- ============================================================
-- Сид: текущие составы (Север, Юг, Восток, Центр и т.д.)
-- Игроки с несколькими командами показывают историю переходов:
--   Максим Романов: Север → Центр
--   Руслан Котов:   Юг    → Восток
--   Илья Терехов:   Юг    → Восток
-- ============================================================

-- Север (текущие, без трёх перешедших)
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Север'
  AND p.full_name IN ('Иван Петров', 'Павел Соколов', 'Андрей Крылов', 'Сергей Волков');

-- Максим Романов: был в Север до 2026-01-01
INSERT INTO work.w_player_team (player_id, team_id, valid_from, valid_to, active)
SELECT p.id, t.id, '2025-09-01', '2025-12-31', FALSE
FROM work.w_player p, work.w_team t
WHERE t.name = 'Север' AND p.full_name = 'Максим Романов';

-- Руслан Котов + Илья Терехов: были в Юг до 2026-01-01
INSERT INTO work.w_player_team (player_id, team_id, valid_from, valid_to, active)
SELECT p.id, t.id, '2025-09-01', '2025-12-31', FALSE
FROM work.w_player p, work.w_team t
WHERE t.name = 'Юг' AND p.full_name IN ('Руслан Котов', 'Илья Терехов');

-- Юг (текущие)
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Юг'
  AND p.full_name IN ('Егор Климов', 'Олег Жданов', 'Никита Громов');

-- Восток (текущие, включая пришедших из Юг)
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2026-01-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Восток'
  AND p.full_name IN ('Сергей Иванов', 'Антон Белов', 'Артем Ларин', 'Руслан Котов', 'Илья Терехов');

-- Центр (текущие, включая Максим Романов пришедший из Север)
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2026-01-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Центр'
  AND p.full_name IN ('Максим Романов', 'Петр Фадеев', 'Михаил Козлов', 'Егор Панов', 'Алексей Жданов');

-- Спартак
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Спартак'
  AND p.full_name IN ('Кирилл Гусев', 'Артем Морозов', 'Иван Седов', 'Роман Беляев', 'Глеб Орлов');

-- Динамо
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Динамо'
  AND p.full_name IN ('Даниил Брагин', 'Павел Крылов', 'Сергей Яковлев', 'Игорь Титов', 'Владислав Рябов');

-- Локомотив
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Локомотив'
  AND p.full_name IN ('Антон Панкратов', 'Виктор Голубев', 'Николай Матвеев', 'Алексей Блохин', 'Семен Шаров');

-- Олимп
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Олимп'
  AND p.full_name IN ('Юрий Котов', 'Степан Белов', 'Петр Мелехин', 'Артур Панов', 'Егор Дмитриев');

-- Волга
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Волга'
  AND p.full_name IN ('Роман Сычев', 'Илья Гордеев', 'Павел Минин', 'Максим Белый', 'Егор Шувалов');

-- Заря
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Заря'
  AND p.full_name IN ('Станислав Осипов', 'Михаил Доронин', 'Дмитрий Кулагин', 'Олег Назаров', 'Руслан Фомин');

-- Старт
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Старт'
  AND p.full_name IN ('Матвей Чернов', 'Кирилл Федотов', 'Григорий Мартынов', 'Евгений Лавров', 'Арсений Попов');

-- Факел
INSERT INTO work.w_player_team (player_id, team_id, valid_from)
SELECT p.id, t.id, '2025-09-01'
FROM work.w_player p, work.w_team t
WHERE t.name = 'Факел'
  AND p.full_name IN ('Александр Трусов', 'Никита Шестаков', 'Петр Никифоров', 'Сергей Дорофеев', 'Иван Черкасов');
