package com.footballstats.backend.service;

import com.footballstats.backend.domain.Role;
import com.footballstats.backend.domain.RoleCode;
import com.footballstats.backend.repository.RoleRepository;
import com.footballstats.backend.repository.SeasonPlayoffTieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "demo.tools.enabled=true")
@ActiveProfiles("test")
class LocalDemoLeagueServiceTest {

    @Autowired
    private LocalDemoLeagueService service;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SeasonPlayoffTieRepository playoffTieRepository;

    @BeforeEach
    void seedRoles() {
        for (RoleCode code : RoleCode.values()) {
            if (roleRepository.findByCode(code).isPresent()) {
                continue;
            }
            Role role = new Role();
            role.setCode(code);
            role.setNameRu(code.name());
            roleRepository.save(role);
        }
    }

    @Test
    void buildsAllProductStagesAndRemovesOnlyTheTrackedDataset() {
        var base = service.createBase(null);
        assertThat(base.exists()).isTrue();
        assertThat(base.stage()).isEqualTo("BASE");
        assertThat(base.counts().teams()).isEqualTo(10);
        assertThat(base.counts().players()).isEqualTo(150);
        assertThat(base.counts().referees()).isEqualTo(8);
        assertThat(base.counts().users()).isEqualTo(4);
        assertThat(base.counts().tours()).isEqualTo(18);

        var schedule = service.createSchedule(null);
        assertThat(schedule.stage()).isEqualTo("SCHEDULE");
        assertThat(schedule.counts().matches()).isEqualTo(90);

        var results = service.addResults(null);
        assertThat(results.stage()).isEqualTo("RESULTS");
        assertThat(results.counts().completedMatches()).isEqualTo(20);

        var transfers = service.prepareTransfers(null);
        assertThat(transfers.stage()).isEqualTo("TRANSFERS");
        assertThat(transfers.counts().transfers()).isEqualTo(1);

        var playoffs = service.preparePlayoffs(null);
        assertThat(playoffs.stage()).isEqualTo("PLAYOFF");
        assertThat(playoffs.counts().playoffTies()).isEqualTo(8);
        assertThat(playoffs.counts().playoffMatches()).isEqualTo(8);
        assertThat(playoffs.counts().matches()).isEqualTo(98);
        assertThat(playoffs.counts().completedMatches()).isEqualTo(96);
        var ties = playoffTieRepository.findAllDetailedBySeasonId(playoffs.seasonId());
        assertThat(ties).filteredOn(tie -> "COMPLETED".equals(tie.getStatus())).hasSize(6);
        assertThat(ties)
            .filteredOn(tie -> "READY".equals(tie.getStatus()))
            .hasSize(2)
            .allSatisfy(tie -> {
                assertThat(tie.getHomeTeam()).isNotNull();
                assertThat(tie.getAwayTeam()).isNotNull();
                assertThat(tie.getWinnerTeam()).isNull();
            });

        var reset = service.reset();
        assertThat(reset.exists()).isFalse();
        assertThat(service.getStatus().exists()).isFalse();
        assertThat(playoffTieRepository.findAll()).isEmpty();
    }
}
