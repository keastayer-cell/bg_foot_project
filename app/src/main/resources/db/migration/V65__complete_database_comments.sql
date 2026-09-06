COMMENT ON TABLE work.w_cup_tie_match IS 'Связь кубковой пары с матчами отдельных игр противостояния';

DO $$
DECLARE
    missing_column RECORD;
    column_comment TEXT;
BEGIN
    FOR missing_column IN
        SELECT
            columns.table_schema,
            columns.table_name,
            columns.column_name
        FROM information_schema.columns columns
        WHERE columns.table_schema IN ('work', 'mailer')
          AND NULLIF(
              btrim(
                  col_description(
                      to_regclass(format('%I.%I', columns.table_schema, columns.table_name)),
                      columns.ordinal_position
                  )
              ),
              ''
          ) IS NULL
        ORDER BY columns.table_schema, columns.table_name, columns.ordinal_position
    LOOP
        column_comment := CASE missing_column.column_name
            WHEN 'id' THEN 'Технический идентификатор записи'
            WHEN 'active' THEN 'Признак активности записи'
            WHEN 'address' THEN 'Адрес места проведения'
            WHEN 'aggregate_away_score' THEN 'Суммарный счёт гостевой команды по всем играм противостояния'
            WHEN 'aggregate_home_score' THEN 'Суммарный счёт домашней команды по всем играм противостояния'
            WHEN 'application_id' THEN 'Идентификатор заявки команды на сезон'
            WHEN 'away_penalty_score' THEN 'Количество голов гостевой команды в серии пенальти'
            WHEN 'away_score' THEN 'Итоговый счёт гостевой команды'
            WHEN 'away_source_result' THEN 'Результат исходной пары, определяющий гостевого участника: победитель или проигравший'
            WHEN 'away_source_tie_id' THEN 'Идентификатор исходной пары для определения гостевого участника'
            WHEN 'away_team_id' THEN 'Идентификатор гостевой команды'
            WHEN 'away_team_name' THEN 'Название гостевой команды на момент создания снимка'
            WHEN 'away_technical_defeat' THEN 'Признак технического поражения гостевой команды'
            WHEN 'bio' THEN 'Краткая биографическая информация'
            WHEN 'bracket_id' THEN 'Идентификатор сетки плей-офф'
            WHEN 'code' THEN 'Уникальный символьный код записи'
            WHEN 'competition_id' THEN 'Идентификатор соревнования'
            WHEN 'competition_type' THEN 'Тип соревнования: чемпионат или кубок'
            WHEN 'created_at' THEN 'Дата и время создания записи'
            WHEN 'created_by_user_id' THEN 'Идентификатор пользователя, создавшего запись'
            WHEN 'dataset_id' THEN 'Идентификатор набора демонстрационных данных'
            WHEN 'decision_at' THEN 'Дата и время принятия решения по заявке'
            WHEN 'decision_by_user_id' THEN 'Идентификатор пользователя, принявшего решение по заявке'
            WHEN 'decision_comment' THEN 'Комментарий к решению по заявке'
            WHEN 'description' THEN 'Описание записи для отображения пользователям'
            WHEN 'draw_status' THEN 'Статус проведения жеребьёвки'
            WHEN 'draws' THEN 'Количество ничьих команды'
            WHEN 'enabled' THEN 'Признак включения настройки'
            WHEN 'expires_at' THEN 'Дата и время окончания срока действия'
            WHEN 'extra_time_enabled' THEN 'Признак использования дополнительного времени'
            WHEN 'extra_time_minutes' THEN 'Продолжительность дополнительного времени в минутах'
            WHEN 'file_name' THEN 'Имя файла выгрузки протокола'
            WHEN 'final_legs' THEN 'Количество матчей в финальном противостоянии'
            WHEN 'finished_at' THEN 'Дата и время завершения'
            WHEN 'full_name' THEN 'Полное имя человека'
            WHEN 'generated_at' THEN 'Дата и время формирования сетки'
            WHEN 'home_penalty_score' THEN 'Количество голов домашней команды в серии пенальти'
            WHEN 'home_score' THEN 'Итоговый счёт домашней команды'
            WHEN 'home_source_result' THEN 'Результат исходной пары, определяющий домашнего участника: победитель или проигравший'
            WHEN 'home_source_tie_id' THEN 'Идентификатор исходной пары для определения домашнего участника'
            WHEN 'home_team_id' THEN 'Идентификатор домашней команды'
            WHEN 'home_team_name' THEN 'Название домашней команды на момент создания снимка'
            WHEN 'home_technical_defeat' THEN 'Признак технического поражения домашней команды'
            WHEN 'ip_address' THEN 'IP-адрес клиентского устройства'
            WHEN 'kickoff_at' THEN 'Дата и время начала матча'
            WHEN 'last_used_at' THEN 'Дата и время последнего использования'
            WHEN 'leg_count' THEN 'Количество матчей в противостоянии'
            WHEN 'leg_number' THEN 'Порядковый номер матча в противостоянии'
            WHEN 'losses' THEN 'Количество поражений команды'
            WHEN 'match_id' THEN 'Идентификатор матча'
            WHEN 'match_roster_size' THEN 'Максимальное количество игроков в заявке на отдельный матч'
            WHEN 'max_roster_size' THEN 'Максимальное количество игроков в заявке'
            WHEN 'name' THEN 'Название записи'
            WHEN 'note' THEN 'Текст примечания'
            WHEN 'notes' THEN 'Дополнительные примечания'
            WHEN 'object_id' THEN 'Идентификатор объекта демонстрационного набора'
            WHEN 'object_type' THEN 'Тип объекта демонстрационного набора'
            WHEN 'penalties_enabled' THEN 'Признак использования серии пенальти'
            WHEN 'photo_media_id' THEN 'Идентификатор медиафайла с фотографией'
            WHEN 'player_id' THEN 'Идентификатор игрока'
            WHEN 'players_on_field' THEN 'Количество игроков одной команды на поле'
            WHEN 'position_title' THEN 'Название должности'
            WHEN 'quarterfinal_legs' THEN 'Количество матчей в четвертьфинальном противостоянии'
            WHEN 'red_suspension_matches' THEN 'Количество матчей дисквалификации за красную карточку'
            WHEN 'referee_id' THEN 'Идентификатор судьи'
            WHEN 'regular_tie_legs' THEN 'Количество матчей в обычном кубковом противостоянии'
            WHEN 'representative_user_id' THEN 'Идентификатор пользователя-представителя команды'
            WHEN 'revoked_at' THEN 'Дата и время отзыва'
            WHEN 'roster_mode' THEN 'Режим формирования заявки соревнования'
            WHEN 'round_code' THEN 'Символьный код раунда кубка'
            WHEN 'round_of_16_legs' THEN 'Количество матчей в противостоянии одной восьмой финала'
            WHEN 'round_order' THEN 'Порядковый номер раунда в сетке'
            WHEN 'season_id' THEN 'Идентификатор сезона'
            WHEN 'seed_number' THEN 'Номер посева команды'
            WHEN 'semifinal_legs' THEN 'Количество матчей в полуфинальном противостоянии'
            WHEN 'short_label' THEN 'Короткое название для компактного отображения'
            WHEN 'slot_order' THEN 'Порядковый номер пары внутри раунда'
            WHEN 'sort_order' THEN 'Порядок отображения записи'
            WHEN 'stage' THEN 'Этап жизненного цикла набора данных'
            WHEN 'started_at' THEN 'Дата и время начала'
            WHEN 'status' THEN 'Текущий статус записи'
            WHEN 'submitted_at' THEN 'Дата и время отправки заявки на рассмотрение'
            WHEN 'team_id' THEN 'Идентификатор команды'
            WHEN 'third_place_enabled' THEN 'Признак проведения матча за третье место'
            WHEN 'third_place_legs' THEN 'Количество матчей в противостоянии за третье место'
            WHEN 'tie_id' THEN 'Идентификатор пары турнирной сетки'
            WHEN 'title' THEN 'Отображаемое название пары или этапа'
            WHEN 'tour_name' THEN 'Название тура на момент создания снимка'
            WHEN 'tour_sort_order' THEN 'Порядок тура на момент создания снимка'
            WHEN 'updated_at' THEN 'Дата и время последнего изменения записи'
            WHEN 'updated_by_user_id' THEN 'Идентификатор пользователя, последним изменившего запись'
            WHEN 'user_agent' THEN 'Строка User-Agent клиентского устройства'
            WHEN 'user_id' THEN 'Идентификатор пользователя'
            WHEN 'winner_team_id' THEN 'Идентификатор команды-победителя противостояния'
            WHEN 'wins' THEN 'Количество побед команды'
            WHEN 'yellow_cards_for_suspension' THEN 'Количество жёлтых карточек до автоматической дисквалификации'
            WHEN 'yellow_suspension_matches' THEN 'Количество матчей дисквалификации за перебор жёлтых карточек'
            ELSE NULL
        END;

        IF column_comment IS NULL THEN
            RAISE EXCEPTION 'Не задан комментарий для столбца %.%.%',
                missing_column.table_schema,
                missing_column.table_name,
                missing_column.column_name;
        END IF;

        EXECUTE format(
            'COMMENT ON COLUMN %I.%I.%I IS %L',
            missing_column.table_schema,
            missing_column.table_name,
            missing_column.column_name,
            column_comment
        );
    END LOOP;
END
$$;

DO $$
DECLARE
    missing_tables TEXT;
    missing_columns TEXT;
BEGIN
    SELECT string_agg(format('%I.%I', tables.table_schema, tables.table_name), ', ' ORDER BY tables.table_schema, tables.table_name)
    INTO missing_tables
    FROM information_schema.tables tables
    WHERE tables.table_schema IN ('work', 'mailer')
      AND tables.table_type = 'BASE TABLE'
      AND NULLIF(
          btrim(obj_description(to_regclass(format('%I.%I', tables.table_schema, tables.table_name)), 'pg_class')),
          ''
      ) IS NULL;

    SELECT string_agg(format('%I.%I.%I', columns.table_schema, columns.table_name, columns.column_name), ', ' ORDER BY columns.table_schema, columns.table_name, columns.ordinal_position)
    INTO missing_columns
    FROM information_schema.columns columns
    WHERE columns.table_schema IN ('work', 'mailer')
      AND NULLIF(
          btrim(
              col_description(
                  to_regclass(format('%I.%I', columns.table_schema, columns.table_name)),
                  columns.ordinal_position
              )
          ),
          ''
      ) IS NULL;

    IF missing_tables IS NOT NULL OR missing_columns IS NOT NULL THEN
        RAISE EXCEPTION 'После миграции остались объекты без комментариев. Таблицы: %. Столбцы: %',
            COALESCE(missing_tables, 'нет'),
            COALESCE(missing_columns, 'нет');
    END IF;
END
$$;
