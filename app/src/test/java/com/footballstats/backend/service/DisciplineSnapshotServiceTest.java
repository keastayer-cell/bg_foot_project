package com.footballstats.backend.service;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
class DisciplineSnapshotServiceTest {
    private Map<String,Object> match(long id,long competition,long home,long away) {
        return Map.of("id",id,"competition_id",competition,"home_team_id",home,"away_team_id",away,
            "kickoff_at",OffsetDateTime.parse("2026-09-"+String.format("%02d",id)+"T12:00:00Z"),
            "competition_type","CHAMPIONSHIP","threshold",2,"yellow_length",1,"red_length",2);
    }
    private Map<String,Object> card(long match,int yellow,int red) {
        return Map.of("match_id",match,"player_id",7L,"team_id",1L,"yellow",yellow,"red",red);
    }
    private Map<String,Object> decision(int remaining) {
        return Map.of("competition_id",1L,"player_id",7L,"team_id",1L,"new_remaining_matches",remaining,
            "reason","Решение КДК","created_at",OffsetDateTime.parse("2026-09-02T18:00:00Z"));
    }
    @Test void thresholdBanIsServedByNextConfirmedMatchOfPlayersTeam() {
        var cards=List.of(card(1,1,0),card(2,1,0));
        var matches=new ArrayList<>(List.of(match(1,1,1,2),match(2,1,1,2),match(3,1,3,4)));
        assertThat(DisciplineSnapshotService.replay(matches,cards,List.of()).get("1:7").remaining).isEqualTo(1);
        matches.add(match(4,1,1,2));
        assertThat(DisciplineSnapshotService.replay(matches,cards,List.of()).get("1:7").remaining).isZero();
    }
    @Test void anotherTournamentDoesNotServeBanAndRedRequiresTwoMatches() {
        var matches=List.of(match(1,1,1,2),match(2,2,1,2),match(3,1,1,2));
        assertThat(DisciplineSnapshotService.replay(matches,List.of(card(1,0,1)),List.of()).get("1:7").remaining).isEqualTo(1);
    }
    @Test void manualDecisionIsServedAndLaterCardsStillCreateNewBan() {
        var matches=List.of(match(1,1,1,2),match(2,1,1,2),match(3,1,1,2),match(4,1,1,2));
        var state=DisciplineSnapshotService.replay(matches,List.of(card(1,0,1),card(4,0,1)),List.of(decision(1))).get("1:7");
        assertThat(state.remaining).isEqualTo(2);
    }
    @Test void cancellationReplacesPriorBanWithoutErasingYellowStatistics() {
        var state=DisciplineSnapshotService.replay(List.of(match(1,1,1,2),match(2,1,1,2)),
            List.of(card(1,1,0),card(2,1,0)),List.of(decision(0))).get("1:7");
        assertThat(state.remaining).isZero(); assertThat(state.totalYellow).isEqualTo(2);
    }
}
