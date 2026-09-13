package com.footballstats.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class AdminDashboardService {
    private final JdbcTemplate jdbcTemplate;
    private final DisciplineSnapshotService snapshots;
    public AdminDashboardService(JdbcTemplate jdbcTemplate,DisciplineSnapshotService snapshots) {
        this.jdbcTemplate=jdbcTemplate;this.snapshots=snapshots;
    }
    @Transactional(readOnly=true)
    public List<DashboardTask> getTasks(boolean superAdmin,Long seasonId,Long competitionId) {
        if(seasonId==null || competitionId==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Выберите сезон и соревнование.");
        Map<String,Object> competition;
        try {
            competition=jdbcTemplate.queryForMap("SELECT roster_mode,players_on_field FROM work.w_competition WHERE id=? AND season_id=? AND active=TRUE",competitionId,seasonId);
        } catch(EmptyResultDataAccessException e) {throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Соревнование не относится к выбранному сезону или недоступно.");}
        String query="?season="+seasonId+"&competition="+competitionId;
        List<DashboardTask> tasks=new ArrayList<>();
        tasks.add(task("applications","Заявки на проверке",count("SELECT COUNT(*) FROM work.w_season_application WHERE status='SUBMITTED' AND season_id=?",seasonId),null,"/season-applications-review"+query,"SEASON"));
        tasks.add(task("transfers","Ожидающие трансферы",count("SELECT COUNT(*) FROM work.w_season_transfer_request WHERE status='PENDING' AND season_id=?",seasonId),null,"/team-rep-transfers"+query,"SEASON"));
        tasks.add(task("protocols","Матчи без подтверждённого протокола",count("""
            SELECT COUNT(*) FROM work.w_tour_match match
            JOIN work.w_tour tour ON tour.id=match.tour_id AND tour.active=TRUE AND tour.published=TRUE
            LEFT JOIN work.w_match_protocol protocol ON protocol.match_id=match.id
            WHERE tour.season_id=? AND tour.competition_id=? AND match.active=TRUE AND match.kickoff_at<NOW()
              AND match.schedule_status<>'CANCELLED' AND (protocol.status IS NULL OR protocol.status<>'VERIFIED')
            """,seasonId,competitionId),"tours",null,"COMPETITION"));
        tasks.add(task("lineups","Ближайшие матчи без двух составов",count("""
            SELECT COUNT(*) FROM (
              SELECT match.id FROM work.w_tour_match match
              JOIN work.w_tour tour ON tour.id=match.tour_id AND tour.active=TRUE AND tour.published=TRUE
              LEFT JOIN work.w_match_lineup lineup ON lineup.match_id=match.id AND lineup.submitted_at IS NOT NULL
              WHERE tour.season_id=? AND tour.competition_id=? AND match.active=TRUE
                AND match.kickoff_at BETWEEN NOW() AND NOW()+INTERVAL '7 days'
                AND match.schedule_status NOT IN ('CANCELLED','COMPLETED','TECHNICAL_RESULT')
              GROUP BY match.id HAVING COUNT(DISTINCT lineup.team_id)<2
            ) dashboard_rows
            """,seasonId,competitionId),"tours",null,"COMPETITION"));
        tasks.add(task("unpublished","Неопубликованные туры с матчами",count("""
            SELECT COUNT(DISTINCT tour.id) FROM work.w_tour tour
            JOIN work.w_tour_match match ON match.tour_id=tour.id AND match.active=TRUE
            WHERE tour.season_id=? AND tour.competition_id=? AND tour.active=TRUE AND tour.published=FALSE
            """,seasonId,competitionId),"tours",null,"COMPETITION"));
        if(superAdmin) {
            boolean own="OWN".equals(competition.get("roster_mode"));
            String table=own?"w_competition_roster_player":"w_season_player";
            String key=own?"competition_id":"season_id";
            long rosterCount=count("SELECT COUNT(*) FROM (SELECT team.team_id FROM work.w_competition_team team LEFT JOIN work."+table+" player ON player.team_id=team.team_id AND player."+key+"=? AND player.active=TRUE WHERE team.competition_id=? GROUP BY team.team_id HAVING COUNT(player.id)<?) dashboard_rows",own?competitionId:seasonId,competitionId,competition.get("players_on_field"));
            tasks.add(task("rosters","Команды с неполной заявкой",rosterCount,own?"competitions":"teams",null,"COMPETITION"));
        }
        long suspended=snapshots.players(competitionId).stream().filter(row -> DisciplineSnapshotService.number(row,"remaining_matches")>0).count();
        tasks.add(task("suspensions","Действующие дисквалификации",suspended,null,"/discipline"+query,"COMPETITION"));
        if(superAdmin) tasks.add(task("acknowledgements","Обязательные оповещения без ознакомления",count("""
            SELECT COUNT(*) FROM work.w_site_notification_recipient recipient
            JOIN work.w_site_notification notification ON notification.id=recipient.notification_id
            WHERE notification.requires_acknowledgement=TRUE AND recipient.acknowledged_at IS NULL
              AND (notification.expires_at IS NULL OR notification.expires_at>NOW())
            """),"notifications",null,"LEAGUE"));
        return tasks;
    }
    private DashboardTask task(String key,String label,long count,String tab,String path,String scope){return new DashboardTask(key,label,count,tab,path,scope);}
    private long count(String sql,Object...args){Long value=jdbcTemplate.queryForObject(sql,Long.class,args);return value==null?0:value;}
    public record DashboardTask(String key,String label,long count,String adminTab,String path,String scope) {}
}
