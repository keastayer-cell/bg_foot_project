package com.footballstats.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

/** Replays confirmed matches and dated organiser decisions; fixtures never serve a ban. */
@Service
public class DisciplineSnapshotService {
    private final JdbcTemplate jdbc;
    public DisciplineSnapshotService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> players(Long competitionId) {
        return players(competitionId, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> players(Long competitionId, Long beforeMatchId) {
        var matches = jdbc.queryForList("""
            SELECT m.id, t.competition_id, m.home_team_id, m.away_team_id, m.kickoff_at,
                   c.competition_type,
                   CASE WHEN c.competition_type='CHAMPIONSHIP' THEN cfg.yellow_cards_for_suspension ELSE c.yellow_cards_for_suspension END AS threshold,
                   CASE WHEN c.competition_type='CHAMPIONSHIP' THEN cfg.yellow_suspension_matches ELSE c.yellow_suspension_matches END AS yellow_length,
                   CASE WHEN c.competition_type='CHAMPIONSHIP' THEN cfg.red_cards_for_suspension ELSE c.red_suspension_matches END AS red_length
            FROM work.w_tour_match m JOIN work.w_tour t ON t.id=m.tour_id
            JOIN work.w_competition c ON c.id=t.competition_id
            LEFT JOIN work.w_season_standings_config cfg ON cfg.season_id=c.season_id
            JOIN work.w_match_protocol p ON p.match_id=m.id AND p.status='VERIFIED'
            WHERE m.active=TRUE AND t.active=TRUE AND m.schedule_status<>'CANCELLED'
              AND (?::bigint IS NULL OR c.id=?)
            ORDER BY m.kickoff_at,m.id
            """, competitionId, competitionId);
        var cards = jdbc.queryForList("""
            SELECT e.match_id,e.player_id,e.team_id,
                   COUNT(*) FILTER (WHERE e.event_type='YELLOW_CARD') AS yellow,
                   COUNT(*) FILTER (WHERE e.event_type IN ('RED_CARD','SECOND_YELLOW_RED')) AS red
            FROM work.w_match_event e JOIN work.w_tour_match m ON m.id=e.match_id
            JOIN work.w_tour t ON t.id=m.tour_id
            WHERE (?::bigint IS NULL OR t.competition_id=?) AND e.player_id IS NOT NULL AND e.team_id IS NOT NULL
              AND e.event_type IN ('YELLOW_CARD','RED_CARD','SECOND_YELLOW_RED')
            GROUP BY e.match_id,e.player_id,e.team_id
            """, competitionId, competitionId);
        var decisions = jdbc.queryForList("""
            SELECT competition_id,player_id,team_id,new_remaining_matches,reason,created_at,id
            FROM work.w_discipline_adjustment WHERE (?::bigint IS NULL OR competition_id=?)
            ORDER BY created_at,id
            """, competitionId, competitionId);
        if (beforeMatchId != null) {
            var target = jdbc.queryForMap("SELECT kickoff_at,id FROM work.w_tour_match WHERE id=?", beforeMatchId);
            var cutoff = time(target.get("kickoff_at"));
            matches.removeIf(m -> time(m.get("kickoff_at")).isAfter(cutoff)
                || (time(m.get("kickoff_at")).isEqual(cutoff) && number(m,"id") >= beforeMatchId));
            decisions.removeIf(d -> time(d.get("created_at")).isAfter(cutoff));
        }
        var names = jdbc.queryForList("""
            SELECT p.id AS player_id,p.full_name AS player_name,t.id AS team_id,t.name AS team_name,t.short_name AS team_short_name
            FROM work.w_player p CROSS JOIN work.w_team t
            WHERE (p.id,t.id) IN (
              SELECT e.player_id,e.team_id FROM work.w_match_event e JOIN work.w_tour_match m ON m.id=e.match_id
              JOIN work.w_tour tour ON tour.id=m.tour_id WHERE (?::bigint IS NULL OR tour.competition_id=?)
              UNION SELECT player_id,team_id FROM work.w_discipline_adjustment WHERE (?::bigint IS NULL OR competition_id=?)
            )
            """, competitionId, competitionId, competitionId, competitionId);
        var states = replay(matches, cards, decisions);
        List<Map<String,Object>> result = new ArrayList<>();
        for (var state : states.values()) {
            Map<String,Object> row = new LinkedHashMap<>();
            names.stream().filter(n -> number(n,"player_id")==state.player && number(n,"team_id")==state.team)
                .findFirst().ifPresent(row::putAll);
            row.put("player_id",state.player); row.put("team_id",state.team); row.put("competition_id",state.competition);
            row.put("yellow_cards",state.totalYellow); row.put("red_cards",state.totalRed);
            row.put("remaining_matches",state.remaining); row.put("near_threshold",state.threshold>0 && state.cycle==state.threshold-1);
            row.put("source_match_id",state.source); row.put("adjustment_reason",state.reason);
            result.add(row);
        }
        result.sort(Comparator.<Map<String,Object>>comparingLong(r -> number(r,"remaining_matches")).reversed()
            .thenComparing(r -> String.valueOf(r.get("player_name"))));
        return result;
    }

    static Map<String,State> replay(List<Map<String,Object>> matches,List<Map<String,Object>> cards,List<Map<String,Object>> decisions) {
        Map<String,State> states = new LinkedHashMap<>();
        Map<Long,List<Map<String,Object>>> byMatch = new HashMap<>();
        cards.forEach(c -> byMatch.computeIfAbsent(number(c,"match_id"), k -> new ArrayList<>()).add(c));
        int next = 0;
        for (var match : matches) {
            var kickoff = time(match.get("kickoff_at"));
            while (next<decisions.size() && !time(decisions.get(next).get("created_at")).isAfter(kickoff)) applyDecision(states,decisions.get(next++));
            long competition = number(match,"competition_id");
            for (var state : states.values()) {
                if (state.competition==competition && (state.team==number(match,"home_team_id") || state.team==number(match,"away_team_id")) && state.remaining>0) state.remaining--;
            }
            for (var card : byMatch.getOrDefault(number(match,"id"),List.of())) {
                long player = number(card,"player_id");
                State state = states.computeIfAbsent(competition+":"+player,k -> new State(competition,player));
                state.team=number(card,"team_id"); state.threshold=(int)number(match,"threshold");
                int before=state.remaining;
                int yellow=(int)number(card,"yellow"), red=(int)number(card,"red");
                state.totalYellow+=yellow; state.totalRed+=red;
                if (red>0) {
                    state.remaining+=red*(int)number(match,"red_length");
                    if ("CHAMPIONSHIP".equals(match.get("competition_type"))) state.cycle=0;
                }
                if (red==0 || !"CHAMPIONSHIP".equals(match.get("competition_type"))) {
                    state.cycle+=yellow;
                    if (state.threshold>0) {
                        state.remaining+=(state.cycle/state.threshold)*(int)number(match,"yellow_length");
                        state.cycle%=state.threshold;
                    }
                }
                if (state.remaining>before) state.source=number(match,"id");
            }
        }
        while (next<decisions.size()) applyDecision(states,decisions.get(next++));
        return states;
    }
    private static void applyDecision(Map<String,State> states,Map<String,Object> decision) {
        long competition=number(decision,"competition_id"),player=number(decision,"player_id");
        State state=states.computeIfAbsent(competition+":"+player,k -> new State(competition,player));
        state.team=number(decision,"team_id"); state.remaining=(int)number(decision,"new_remaining_matches"); state.reason=String.valueOf(decision.get("reason"));
    }
    static long number(Map<String,Object> row,String key) { Object n=row.get(key); return n instanceof Number value ? value.longValue() : 0; }
    static OffsetDateTime time(Object value) { return value instanceof OffsetDateTime t ? t : ((java.sql.Timestamp)value).toInstant().atOffset(java.time.ZoneOffset.UTC); }
    static class State {
        final long competition,player; long team; int remaining,cycle,totalYellow,totalRed,threshold; Long source; String reason;
        State(long competition,long player) { this.competition=competition; this.player=player; }
    }
}
