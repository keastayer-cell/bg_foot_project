package com.footballstats.backend.service;

import com.footballstats.backend.repository.BusinessAuditRepository;
import com.footballstats.backend.repository.TeamRepository;
import com.footballstats.backend.security.AppUserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class BusinessAuditIntegrationTest {
    @Autowired TeamService teams;
    @Autowired TeamRepository teamRepository;
    @Autowired BusinessAuditRepository auditRepository;
    @Autowired BusinessAuditService audit;
    @Autowired PlatformTransactionManager transactions;
    @AfterEach void clearAuthentication(){SecurityContextHolder.clearContext();}
    private Long createTeam(){return teams.createTeam(new TeamService.TeamUpsertData("Аудит "+UUID.randomUUID(),"Аудит","Богородск",null),null).getId();}
    private void authenticate(){
        var principal=new AppUserPrincipal(987L,"audit@example.com","Автор проверки",false,List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
    }
    @Test void successfulActionStoresAuthorAndSafeBeforeAfterAndSupportsFiltering(){
        Long teamId=createTeam();authenticate();
        teams.deactivateTeam(teamId,null);
        var page=audit.list(null,null,987L,"Автор","TEAM",teamId,"DEACTIVATED",0,25);
        assertThat(page.totalElements()).isEqualTo(1);
        var entry=page.items().get(0);
        assertThat(entry.actorName()).isEqualTo("Автор проверки");
        assertThat(entry.changes().at("/before/record/0/active").asBoolean()).isTrue();
        assertThat(entry.changes().at("/after/record/0/active").asBoolean()).isFalse();
        assertThat(entry.changes().toString()).doesNotContain("password","token","logo","data:image");
        assertThat(audit.list(null,null,null,null,"TEAM",teamId,"ROLE_GRANTED",0,25).items()).isEmpty();
        teams.deactivateTeam(teamId,null);
        assertThat(audit.list(null,null,null,null,"TEAM",teamId,null,0,25).totalElements()).isEqualTo(1);
    }
    @Test void rolledBackActionRollsBackAuditEntryAndBusinessChange(){
        Long teamId=createTeam();
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
            teams.deactivateTeam(teamId,null);
            assertThat(audit.list(null,null,null,null,"TEAM",teamId,null,0,25).totalElements()).isEqualTo(1);
            throw new IllegalStateException("rollback test");
        })).isInstanceOf(IllegalStateException.class).hasMessage("rollback test");
        assertThat(teamRepository.findById(teamId).orElseThrow().isActive()).isTrue();
        assertThat(audit.list(null,null,null,null,"TEAM",teamId,null,0,25).items()).isEmpty();
    }
    @Test void invalidPeriodDoesNotRunQuery(){
        var now=java.time.OffsetDateTime.now();
        assertThatThrownBy(() -> audit.list(now,now.minusDays(1),null,null,null,null,null,0,25))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
