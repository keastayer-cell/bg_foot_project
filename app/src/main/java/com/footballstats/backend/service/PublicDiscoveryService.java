package com.footballstats.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PublicDiscoveryService {
    private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final JdbcTemplate jdbcTemplate;
    private final String publicWebUrl;

    public PublicDiscoveryService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.public-web-url:http://127.0.0.1:5173}") String publicWebUrl
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.publicWebUrl = publicWebUrl.replaceAll("/+$", "");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String rawQuery, int requestedLimit) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        int limit = Math.max(1, Math.min(requestedLimit, 10));
        if (query.length() < 2) {
            return Map.of("query", query, "players", List.of(), "teams", List.of(), "matches", List.of(), "competitions", List.of());
        }
        String pattern = "%" + query.toLowerCase() + "%";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("players", jdbcTemplate.queryForList("""
            SELECT player.id, player.full_name AS title,
                   COALESCE(team.name, 'Без команды') AS subtitle,
                   '/players/' || player.id AS url
            FROM work.w_player player
            LEFT JOIN work.w_player_team link ON link.player_id = player.id AND link.active = TRUE AND link.valid_to IS NULL
            LEFT JOIN work.w_team team ON team.id = link.team_id
            WHERE player.active = TRUE AND LOWER(player.full_name) LIKE ?
            ORDER BY player.full_name LIMIT ?
            """, pattern, limit));
        result.put("teams", jdbcTemplate.queryForList("""
            SELECT team.id, team.name AS title,
                   COALESCE(team.city, 'Команда лиги') AS subtitle,
                   '/teams/' || team.id AS url
            FROM work.w_team team
            WHERE team.active = TRUE AND LOWER(team.name) LIKE ?
            ORDER BY team.name LIMIT ?
            """, pattern, limit));
        result.put("matches", jdbcTemplate.queryForList("""
            SELECT match.id, home.name || ' — ' || away.name AS title,
                   TO_CHAR(match.kickoff_at AT TIME ZONE 'Europe/Moscow', 'DD.MM.YYYY HH24:MI') AS subtitle,
                   '/matches/' || match.id AS url
            FROM work.w_tour_match match
            JOIN work.w_tour tour ON tour.id = match.tour_id AND tour.active = TRUE AND tour.published = TRUE
            JOIN work.w_team home ON home.id = match.home_team_id
            JOIN work.w_team away ON away.id = match.away_team_id
            WHERE match.active = TRUE AND LOWER(home.name || ' ' || away.name) LIKE ?
            ORDER BY match.kickoff_at DESC LIMIT ?
            """, pattern, limit));
        result.put("competitions", jdbcTemplate.queryForList("""
            SELECT competition.id, competition.name AS title, season.name AS subtitle,
                   '/seasons/' || season.id || '/competitions/' || competition.id AS url
            FROM work.w_competition competition
            JOIN work.w_season season ON season.id = competition.season_id
            WHERE competition.active = TRUE
              AND LOWER(competition.name || ' ' || season.name) LIKE ?
            ORDER BY season.id DESC, competition.name LIMIT ?
            """, pattern, limit));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calendar(Long seasonId, Long competitionId, Long teamId, Long venueId, LocalDate from, LocalDate to) {
        StringBuilder sql = new StringBuilder("""
            SELECT match.id, match.kickoff_at, match.original_kickoff_at, match.schedule_status,
                   match.schedule_change_reason, home.id AS home_team_id, home.name AS home_team_name,
                   away.id AS away_team_id, away.name AS away_team_name,
                   season.id AS season_id, season.name AS season_name,
                   competition.id AS competition_id, competition.name AS competition_name,
                   venue.id AS venue_id, venue.name AS venue_name, venue.address AS venue_address,
                   protocol.status AS protocol_status, protocol.home_score, protocol.away_score
            FROM work.w_tour_match match
            JOIN work.w_tour tour ON tour.id = match.tour_id AND tour.active = TRUE AND tour.published = TRUE
            JOIN work.w_season season ON season.id = tour.season_id
            LEFT JOIN work.w_competition competition ON competition.id = tour.competition_id
            JOIN work.w_team home ON home.id = match.home_team_id
            JOIN work.w_team away ON away.id = match.away_team_id
            LEFT JOIN work.w_league_venue venue ON venue.id = match.venue_id
            LEFT JOIN work.w_match_protocol protocol ON protocol.match_id = match.id
            WHERE match.active = TRUE
            """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (seasonId != null) { sql.append(" AND season.id = ?"); args.add(seasonId); }
        if (competitionId != null) { sql.append(" AND competition.id = ?"); args.add(competitionId); }
        if (teamId != null) { sql.append(" AND (home.id = ? OR away.id = ?)"); args.add(teamId); args.add(teamId); }
        if (venueId != null) { sql.append(" AND venue.id = ?"); args.add(venueId); }
        if (from != null) { sql.append(" AND match.kickoff_at >= ?"); args.add(from.atStartOfDay().atOffset(ZoneOffset.UTC)); }
        if (to != null) { sql.append(" AND match.kickoff_at < ?"); args.add(to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)); }
        sql.append(" ORDER BY match.kickoff_at, match.id");
        List<Map<String, Object>> matches = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        return Map.of(
            "matches", matches,
            "venues", jdbcTemplate.queryForList("SELECT id, name, address FROM work.w_league_venue WHERE active = TRUE ORDER BY sort_order, name")
        );
    }

    @Transactional(readOnly = true)
    public String exportIcs(Long matchId, Long teamId, Long competitionId) {
        if (matchId == null && teamId == null && competitionId == null) {
            throw new IllegalArgumentException("Укажите матч, команду или соревнование для экспорта.");
        }
        StringBuilder sql = new StringBuilder("""
            SELECT match.id, match.kickoff_at, home.name AS home_name, away.name AS away_name,
                   venue.name AS venue_name, venue.address AS venue_address
            FROM work.w_tour_match match
            JOIN work.w_tour tour ON tour.id = match.tour_id AND tour.active = TRUE AND tour.published = TRUE
            JOIN work.w_team home ON home.id = match.home_team_id
            JOIN work.w_team away ON away.id = match.away_team_id
            LEFT JOIN work.w_league_venue venue ON venue.id = match.venue_id
            WHERE match.active = TRUE AND match.schedule_status <> 'CANCELLED'
            """);
        java.util.ArrayList<Object> args = new java.util.ArrayList<>();
        if (matchId != null) { sql.append(" AND match.id = ?"); args.add(matchId); }
        if (teamId != null) { sql.append(" AND (home.id = ? OR away.id = ?)"); args.add(teamId); args.add(teamId); }
        if (competitionId != null) { sql.append(" AND tour.competition_id = ?"); args.add(competitionId); }
        sql.append(" ORDER BY match.kickoff_at");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        StringBuilder result = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Football Bogorodsk//Calendar//RU\r\nCALSCALE:GREGORIAN\r\n");
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            OffsetDateTime kickoff = (OffsetDateTime) row.get("kickoff_at");
            String title = row.get("home_name") + " — " + row.get("away_name");
            String location = joinLocation(row.get("venue_name"), row.get("venue_address"));
            result.append("BEGIN:VEVENT\r\n")
                .append("UID:match-").append(id).append("@bgfoot.ru\r\n")
                .append("DTSTAMP:").append(formatIcs(OffsetDateTime.now())).append("\r\n")
                .append("DTSTART:").append(formatIcs(kickoff)).append("\r\n")
                .append("DTEND:").append(formatIcs(kickoff.plusHours(2))).append("\r\n")
                .append("SUMMARY:").append(escapeIcs(title)).append("\r\n")
                .append("LOCATION:").append(escapeIcs(location)).append("\r\n")
                .append("URL:").append(publicWebUrl).append("/matches/").append(id).append("\r\n")
                .append("END:VEVENT\r\n");
        }
        return result.append("END:VCALENDAR\r\n").toString();
    }

    private String formatIcs(OffsetDateTime value) {
        return value.withOffsetSameInstant(ZoneOffset.UTC).format(ICS_DATE_TIME);
    }

    private String joinLocation(Object venue, Object address) {
        if (venue == null) return "Площадка уточняется";
        return address == null ? venue.toString() : venue + ", " + address;
    }

    private String escapeIcs(String value) {
        return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}

