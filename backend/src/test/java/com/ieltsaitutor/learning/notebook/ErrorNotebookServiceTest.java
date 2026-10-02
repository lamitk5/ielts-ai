package com.ieltsaitutor.learning.notebook;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.learning.intelligence.*;

class ErrorNotebookServiceTest {
    @Test void oneMistakeIsNotLabeledRepeated() {
        UUID user = UUID.randomUUID();
        LearningIntelligenceService intelligence = mock(LearningIntelligenceService.class);
        MistakeRecord mistake = mistake(user, UUID.randomUUID(), Instant.now());
        when(intelligence.mistakes(user)).thenReturn(List.of(mistake));
        ErrorNotebookAcknowledgementRepository acknowledgements = mock(ErrorNotebookAcknowledgementRepository.class);
        ErrorNotebookService service = new ErrorNotebookService(intelligence, acknowledgements);
        ErrorNotebookService.NotebookResponse response = service.list(user, new ErrorNotebookQuery(null, null, 0, 20));
        assertEquals(1, response.entries().size());
        assertFalse(response.entries().get(0).repeated());
    }

    @Test void repeatedMistakesAreGroupedAndOwnerScoped() {
        UUID user = UUID.randomUUID(); UUID practice = UUID.randomUUID();
        LearningIntelligenceService intelligence = mock(LearningIntelligenceService.class);
        when(intelligence.mistakes(user)).thenReturn(List.of(mistake(user, practice, Instant.now()), mistake(user, practice, Instant.now().plusSeconds(1))));
        ErrorNotebookService service = new ErrorNotebookService(intelligence, mock(ErrorNotebookAcknowledgementRepository.class));
        var response = service.list(user, new ErrorNotebookQuery("READING", null, 0, 20));
        assertEquals(1, response.entries().size());
        assertEquals(2, response.entries().get(0).repeatCount());
        assertTrue(response.entries().get(0).repeated());
    }

    private MistakeRecord mistake(UUID user, UUID practice, Instant detected) {
        return new MistakeRecord(UUID.randomUUID(), user, Skill.READING, practice.toString(), UUID.randomUUID(), "q1", "MCQ", "A", "B", "DETAIL", MistakeMethod.DETERMINISTIC, .9, "EVIDENCE", "Observed in submitted answer", detected, null, MistakeStatus.OPEN, 1);
    }
}
