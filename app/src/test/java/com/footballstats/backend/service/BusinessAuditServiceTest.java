package com.footballstats.backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.footballstats.backend.domain.BusinessAuditEntry;
import com.footballstats.backend.repository.BusinessAuditRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class BusinessAuditServiceTest {
    @Test void verificationAndEditingVerifiedResultHaveDistinctActions(){
        var repository=mock(BusinessAuditRepository.class);
        var service=new BusinessAuditService(mock(JdbcTemplate.class),repository,new ObjectMapper());
        var finished=Map.<String,Object>of("record",List.of(Map.of("status","FINISHED","home_score",1)));
        var verified=Map.<String,Object>of("record",List.of(Map.of("status","VERIFIED","home_score",1)));
        var changed=Map.<String,Object>of("record",List.of(Map.of("status","VERIFIED","home_score",2)));
        service.record("PROTOCOL",1L,"PROTOCOL_UPDATED",2L,"Судья",finished,verified);
        service.record("PROTOCOL",1L,"PROTOCOL_UPDATED",2L,"Судья",verified,changed);
        var entries=ArgumentCaptor.forClass(BusinessAuditEntry.class);
        verify(repository,times(2)).saveAndFlush(entries.capture());
        assertThat(entries.getAllValues()).extracting(BusinessAuditEntry::getActionCode)
            .containsExactly("PROTOCOL_VERIFIED","RESULT_UPDATED");
    }
    @Test void unchangedSnapshotDoesNotCreateEvent(){
        var repository=mock(BusinessAuditRepository.class);
        var service=new BusinessAuditService(mock(JdbcTemplate.class),repository,new ObjectMapper());
        var state=Map.<String,Object>of("record",List.of(Map.of("active",true)));
        service.record("TEAM",1L,"DEACTIVATED",2L,null,state,state);
        verifyNoInteractions(repository);
    }
    @Test void unknownEntityCannotIntroduceArbitrarySqlOrSensitiveSnapshot(){
        var jdbc=mock(JdbcTemplate.class);
        var service=new BusinessAuditService(jdbc,mock(BusinessAuditRepository.class),new ObjectMapper());
        assertThatThrownBy(() -> service.snapshot("w_user_login; DROP TABLE work.w_team",1L)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }
}
