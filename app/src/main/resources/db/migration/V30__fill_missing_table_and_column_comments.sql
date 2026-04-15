COMMENT ON TABLE work.w_match_lineup IS 'Заявка команды на конкретный матч';
COMMENT ON TABLE work.w_match_lineup_player IS 'Игрок, включенный в заявку команды на матч';
COMMENT ON TABLE work.w_season_player IS 'Связь игрока с командой в рамках конкретного сезона';

COMMENT ON COLUMN work.w_match_event.id IS 'Идентификатор события матча';
COMMENT ON COLUMN work.w_match_event.value_text IS 'Дополнительное текстовое значение события, если оно требуется';
COMMENT ON COLUMN work.w_match_event.created_by_user_id IS 'Пользователь, который создал запись события';
COMMENT ON COLUMN work.w_match_event.updated_by_user_id IS 'Пользователь, который последним изменил запись события';
COMMENT ON COLUMN work.w_match_event.created_at IS 'Когда запись события была создана';
COMMENT ON COLUMN work.w_match_event.updated_at IS 'Когда запись события была обновлена';

COMMENT ON COLUMN work.w_match_lineup.id IS 'Идентификатор заявки на матч';
COMMENT ON COLUMN work.w_match_lineup.match_id IS 'Матч, для которого подана заявка';
COMMENT ON COLUMN work.w_match_lineup.team_id IS 'Команда, которая подала заявку';
COMMENT ON COLUMN work.w_match_lineup.submitted_by_user_id IS 'Пользователь, который отправил заявку';
COMMENT ON COLUMN work.w_match_lineup.updated_by_user_id IS 'Пользователь, который последним изменил заявку';
COMMENT ON COLUMN work.w_match_lineup.submitted_at IS 'Когда заявка была отправлена';
COMMENT ON COLUMN work.w_match_lineup.created_at IS 'Когда запись заявки была создана';
COMMENT ON COLUMN work.w_match_lineup.updated_at IS 'Когда запись заявки была обновлена';

COMMENT ON COLUMN work.w_match_lineup_player.id IS 'Идентификатор игрока в заявке';
COMMENT ON COLUMN work.w_match_lineup_player.lineup_id IS 'Заявка, к которой относится игрок';
COMMENT ON COLUMN work.w_match_lineup_player.player_id IS 'Игрок, включенный в заявку';
COMMENT ON COLUMN work.w_match_lineup_player.sort_order IS 'Порядок игрока внутри заявки';
COMMENT ON COLUMN work.w_match_lineup_player.created_by_user_id IS 'Пользователь, который добавил игрока в заявку';
COMMENT ON COLUMN work.w_match_lineup_player.updated_by_user_id IS 'Пользователь, который последним изменил запись игрока в заявке';
COMMENT ON COLUMN work.w_match_lineup_player.created_at IS 'Когда запись игрока в заявке была создана';
COMMENT ON COLUMN work.w_match_lineup_player.updated_at IS 'Когда запись игрока в заявке была обновлена';

COMMENT ON COLUMN work.w_season_player.id IS 'Идентификатор связки игрока с сезоном и командой';
COMMENT ON COLUMN work.w_season_player.season_id IS 'Сезон, в рамках которого заявлен игрок';
COMMENT ON COLUMN work.w_season_player.team_id IS 'Команда игрока в этом сезоне';
COMMENT ON COLUMN work.w_season_player.player_id IS 'Игрок, заявленный за команду в сезоне';
COMMENT ON COLUMN work.w_season_player.created_by_user_id IS 'Пользователь, который создал запись сезонной заявки игрока';
COMMENT ON COLUMN work.w_season_player.updated_by_user_id IS 'Пользователь, который последним изменил запись сезонной заявки игрока';
COMMENT ON COLUMN work.w_season_player.active IS 'Признак, что игрок сейчас активен в заявке сезона';
COMMENT ON COLUMN work.w_season_player.created_at IS 'Когда запись сезонной заявки игрока была создана';
COMMENT ON COLUMN work.w_season_player.updated_at IS 'Когда запись сезонной заявки игрока была обновлена';