package com.footballstats.backend.service;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MatchProtocolFileNameTest {

    @Test
    void includesTeamsAndMatchDate() {
        String fileName = MatchProtocolService.buildProtocolExportFileName(
            OffsetDateTime.parse("2026-01-01T18:30:00+03:00"),
            "Спартак",
            "Локомотив"
        );

        assertThat(fileName).isEqualTo("protocol_Спартак_Локомотив_01.01.2026.pdf");
    }

    @Test
    void replacesUnsafeCharactersAndWhitespace() {
        String fileName = MatchProtocolService.buildProtocolExportFileName(
            OffsetDateTime.parse("2026-09-01T20:00:00+03:00"),
            "Команда / А",
            "Команда: Б"
        );

        assertThat(fileName).isEqualTo("protocol_Команда_А_Команда_Б_01.09.2026.pdf");
    }
}
