alter table work.w_match_protocol_export_snapshot
    add column if not exists home_team_short_name varchar(60),
    add column if not exists away_team_short_name varchar(60);

update work.w_match_protocol_export_snapshot snapshot
   set home_team_short_name = coalesce(nullif(trim(home_team.short_name), ''), home_team.name),
       away_team_short_name = coalesce(nullif(trim(away_team.short_name), ''), away_team.name),
       updated_at = now()
  from work.w_tour_match tour_match
  join work.w_team home_team on home_team.id = tour_match.home_team_id
  join work.w_team away_team on away_team.id = tour_match.away_team_id
 where snapshot.match_id = tour_match.id;

comment on column work.w_match_protocol_export_snapshot.home_team_short_name is 'Короткое имя команды хозяев для имени PDF-файла';
comment on column work.w_match_protocol_export_snapshot.away_team_short_name is 'Короткое имя команды гостей для имени PDF-файла';
