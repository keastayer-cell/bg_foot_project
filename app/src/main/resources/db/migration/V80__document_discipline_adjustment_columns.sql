COMMENT ON COLUMN work.w_discipline_adjustment.id IS 'Идентификатор записи корректировки';
COMMENT ON COLUMN work.w_discipline_adjustment.competition_id IS 'Соревнование, внутри которого действует решение';
COMMENT ON COLUMN work.w_discipline_adjustment.player_id IS 'Игрок, наказание которого изменено';
COMMENT ON COLUMN work.w_discipline_adjustment.team_id IS 'Команда игрока на момент решения';
COMMENT ON COLUMN work.w_discipline_adjustment.old_remaining_matches IS 'Число оставшихся матчей до корректировки';
COMMENT ON COLUMN work.w_discipline_adjustment.new_remaining_matches IS 'Число оставшихся матчей после корректировки';
COMMENT ON COLUMN work.w_discipline_adjustment.adjustment_type IS 'Тип изменения наказания';
COMMENT ON COLUMN work.w_discipline_adjustment.created_by_user_id IS 'Пользователь, зафиксировавший решение';
COMMENT ON COLUMN work.w_discipline_adjustment.created_at IS 'Дата и время фиксации решения';
