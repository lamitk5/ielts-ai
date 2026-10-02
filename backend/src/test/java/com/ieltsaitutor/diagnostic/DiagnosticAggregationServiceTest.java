package com.ieltsaitutor.diagnostic;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiagnosticAggregationServiceTest {
    @Test void incompleteSectionsAreInsufficientAndNeverOfficialPlacement() {
        UUID user=UUID.randomUUID(); UUID id=UUID.randomUUID();
        DiagnosticSessionService sessions = mock(DiagnosticSessionService.class);
        DiagnosticSession session = mock(DiagnosticSession.class);
        when(session.id()).thenReturn(id);
        when(sessions.get(user, id)).thenReturn(session);
        DiagnosticSectionRepository sections = mock(DiagnosticSectionRepository.class);
        when(sections.findOwnedBySession(user, id)).thenReturn(List.of());
        DiagnosticResult result=new DiagnosticAggregationService(sessions, sections).result(user,id);
        assertThat(result.sufficientEvidence()).isFalse();
        assertThat(result.title()).isEqualTo("Estimated starting profile");
        assertThat(result.disclaimer()).doesNotContain("official");
    }
}
