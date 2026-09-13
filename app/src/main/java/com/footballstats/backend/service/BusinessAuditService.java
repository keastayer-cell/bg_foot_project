package com.footballstats.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.BusinessAuditEntry;
import com.footballstats.backend.repository.BusinessAuditRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class BusinessAuditService {
    private final JdbcTemplate jdbc;
    private final BusinessAuditRepository repository;
    private final ObjectMapper mapper;
    public BusinessAuditService(JdbcTemplate jdbc,BusinessAuditRepository repository,ObjectMapper mapper) {
        this.jdbc=jdbc;this.repository=repository;this.mapper=mapper;
    }
    private record Spec(String table,String idColumn,String fields) {}
    private static final Map<String,Spec> SPECS=Map.ofEntries(
        Map.entry("PROTOCOL",new Spec("w_match_protocol","match_id","status,home_score,away_score,home_technical_defeat,away_technical_defeat,best_player_id,chief_referee_id,assistant_referee_one_id,assistant_referee_two_id,notes,started_at,finished_at")),
        Map.entry("MATCH",new Spec("w_tour_match","id","tour_id,home_team_id,away_team_id,kickoff_at,venue_id,schedule_status,original_kickoff_at,schedule_change_reason,active")),
        Map.entry("TRANSFER",new Spec("w_season_transfer_request","id","season_id,player_id,from_team_id,to_team_id,status,decision_comment,processed_at")),
        Map.entry("APPLICATION",new Spec("w_season_application","id","season_id,team_id,status,decision_comment,decision_at")),
        Map.entry("ROLE",new Spec("w_user_role","user_id","role_id,active")),
        Map.entry("TEAM_ACCESS",new Spec("w_user_team_scope","user_id","team_id,active,can_edit_roster,can_edit_application,valid_from,valid_to")),
        Map.entry("SEASON",new Spec("w_season","id","name,active,status,rounds_count,playoff_enabled,playoff_team_count,players_on_field,application_deadline,max_roster_size,transfer_window_start_date,transfer_window_end_date,regulation_media_id,regulation_updated_at")),
        Map.entry("COMPETITION",new Spec("w_competition","id","season_id,name,active,status,competition_type,yellow_cards_for_suspension,yellow_suspension_matches,red_suspension_matches")),
        Map.entry("DISCIPLINE",new Spec("w_discipline_adjustment","competition_id","player_id,team_id,old_remaining_matches,new_remaining_matches,adjustment_type,reason")),
        Map.entry("TEAM",new Spec("w_team","id","name,short_name,active")),
        Map.entry("PLAYER",new Spec("w_player","id","full_name,active")),
        Map.entry("REFEREE",new Spec("w_referee","id","full_name,active")),
        Map.entry("OFFICIAL",new Spec("w_league_official","id","full_name,active")),
        Map.entry("VENUE",new Spec("w_league_venue","id","name,active")),
        Map.entry("TOUR",new Spec("w_tour","id","season_id,competition_id,name,active,published"))
    );

    public Map<String,Object> snapshot(String type,Long id) {
        Spec spec=SPECS.get(type);
        if(spec==null) throw new IllegalArgumentException("Unsupported audit entity: "+type);
        var rows=jdbc.queryForList("SELECT "+spec.fields+" FROM work."+spec.table+" WHERE "+spec.idColumn+"=? ORDER BY id",id);
        rows.forEach(row -> row.replaceAll((key,value) -> dateValue(value)));
        Map<String,Object> result=new LinkedHashMap<>();result.put("record",rows);
        if(type.equals("PROTOCOL")) result.put("events",jdbc.queryForList("""
            SELECT player_id,team_id,event_type,minute,extra_minute FROM work.w_match_event WHERE match_id=?
            ORDER BY player_id,team_id,event_type,minute,extra_minute
            """,id));
        if(type.equals("SEASON")) {
            result.put("rules",jdbc.queryForList("""
                SELECT win_points,draw_points,loss_points,ranking_rules_json,yellow_cards_for_suspension,yellow_suspension_matches,red_cards_for_suspension
                FROM work.w_season_standings_config WHERE season_id=? ORDER BY id
                """,id));
            result.put("playoff",jdbc.queryForList("""
                SELECT enabled,team_count,third_place_enabled,round_of_16_legs,quarterfinal_legs,semifinal_legs,final_legs,third_place_legs
                FROM work.w_season_playoff_config WHERE season_id=? ORDER BY id
                """,id));
            result.put("referees",jdbc.queryForList("SELECT referee_id FROM work.w_season_referee WHERE season_id=? ORDER BY referee_id",id));
        }
        if(type.equals("TEAM_ACCESS")) result.put("roles",jdbc.queryForList("SELECT role_id,active FROM work.w_user_role WHERE user_id=? ORDER BY id",id));
        return result;
    }

    private Object dateValue(Object value) {
        if(value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant().toString();
        if(value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if(value instanceof OffsetDateTime date) return date.toInstant().toString();
        return value;
    }

    public void record(String type,Long id,String action,Long actor,String name,Map<String,Object> before,Map<String,Object> after) {
        if(before.equals(after)) return;
        if(type.equals("PROTOCOL") && !"PROTOCOL_REOPENED".equals(action)) {
            var previous=status(before);var current=status(after);
            if("VERIFIED".equals(current) && !"VERIFIED".equals(previous)) action="PROTOCOL_VERIFIED";
            else if("VERIFIED".equals(previous)) action="RESULT_UPDATED";
        }
        try {
            repository.saveAndFlush(new BusinessAuditEntry(type,id,action,actor,name,
                mapper.writeValueAsString(Map.of("before",before,"after",after))));
        } catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise business audit",e);
        }
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void recordCreated(String type,Long id,String action,Long actor) {
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String name=authentication!=null && authentication.getPrincipal() instanceof com.footballstats.backend.security.AppUserPrincipal principal ? principal.getName() : null;
        record(type,id,action,actor,name,Map.of("record",List.of()),snapshot(type,id));
    }

    private String status(Map<String,Object> snapshot) {
        var records=(List<?>)snapshot.get("record");
        return records.isEmpty() ? "" : String.valueOf(((Map<?,?>)records.get(0)).get("status"));
    }

    @Transactional(readOnly=true)
    public AuditPage list(OffsetDateTime from,OffsetDateTime to,Long actorUserId,String author,String entityType,Long entityId,String action,int page,int size) {
        if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("Начало периода должно быть раньше конца.");
        if(page<0) throw new IllegalArgumentException("Номер страницы не может быть отрицательным.");
        size=Math.max(1,Math.min(100,size));
        StringBuilder where=new StringBuilder(" WHERE 1=1");List<Object> args=new ArrayList<>();
        filter(where,args,"created_at >= ?",from);filter(where,args,"created_at <= ?",to);
        filter(where,args,"actor_user_id = ?",actorUserId);filter(where,args,"entity_id = ?",entityId);
        if(author!=null && !author.isBlank()) filter(where,args,"LOWER(actor_name) LIKE ?","%"+author.trim().toLowerCase(Locale.ROOT)+"%");
        if(entityType!=null && !entityType.isBlank()) filter(where,args,"entity_type = ?",entityType.trim().toUpperCase(Locale.ROOT));
        if(action!=null && !action.isBlank()) filter(where,args,"action_code = ?",action.trim().toUpperCase(Locale.ROOT));
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM work.w_business_audit_log"+where,Long.class,args.toArray());
        args.add(size);args.add((long)page*size);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM work.w_business_audit_log"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",args.toArray());
        List<AuditItem> items=rows.stream().map(row -> {
            try {
                String type=String.valueOf(row.get("entity_type"));Long id=((Number)row.get("entity_id")).longValue();
                JsonNode changes=mapper.readTree(String.valueOf(row.get("changes_json")));
                return new AuditItem(((Number)row.get("id")).longValue(),type,id,String.valueOf(row.get("action_code")),
                    row.get("actor_user_id"),row.get("actor_name"),dateValue(row.get("created_at")),changes,path(type,id,changes));
            } catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalStateException("Invalid audit data",e);}
        }).toList();
        long count=total==null?0:total;
        return new AuditPage(items,count,page,size,(count+size-1)/size);
    }
    private void filter(StringBuilder sql,List<Object> args,String clause,Object value) {
        if(value!=null){sql.append(" AND ").append(clause);args.add(value);}
    }
    private String path(String type,Long id,JsonNode changes) {
        if(changes.path("after").path("record").isEmpty()) return null;
        JsonNode active=changes.path("after").path("record").path(0).path("active");
        if(active.isBoolean() && !active.asBoolean()) return null;
        return switch(type) {
            case "MATCH","PROTOCOL" -> "/matches/"+id;
            case "PLAYER" -> "/players/"+id;
            case "TEAM" -> "/teams/"+id;
            case "TRANSFER" -> "/team-rep-transfers";
            case "APPLICATION" -> "/season-applications-review";
            case "SEASON" -> "/league?season="+id;
            case "DISCIPLINE" -> "/discipline?season="+jdbc.queryForObject("SELECT season_id FROM work.w_competition WHERE id=?",Long.class,id)+"&competition="+id;
            case "COMPETITION" -> "/seasons/"+changes.path("after").path("record").path(0).path("season_id").asLong()+"/competitions/"+id;
            default -> null;
        };
    }
    public record AuditItem(Long id,String entityType,Long entityId,String action,Object actorUserId,Object actorName,Object createdAt,JsonNode changes,String path) {}
    public record AuditPage(List<AuditItem> items,long totalElements,int pageNumber,int pageSize,long totalPages) {}
}
