package com.ieltsaitutor.results;

import com.ieltsaitutor.learning.intelligence.LearningEvent;
import com.ieltsaitutor.learning.intelligence.LearningEventIngestionService;
import com.ieltsaitutor.learning.intelligence.LearningEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TrustedResultPublisherTest {
    @Test
    void publishesStableServerOwnedEvent() {
        LearningEventRepository repository = mock(LearningEventRepository.class);
        LearningEventIngestionService ingestion = new LearningEventIngestionService(repository);
        UUID owner = UUID.randomUUID();
        UUID submission = UUID.randomUUID();
        LearnerResult result = new LearnerResult(submission, "reading", "set-1", null,
                "GRADED", ResultStatus.READY, Instant.now(), 0L, 8, 10, BigDecimal.valueOf(80), null, null,
                null, null, null, List.of(), List.of(), null,
                List.of(ResultAction.ASK_TUTOR), null);
        TrustedResultPublisher publisher = new TrustedResultPublisher(ingestion);
        LearningEvent saved = publisher.publish(owner, result);
        assertEquals("result:" + submission + ":practice_completed", saved.sourceReference());
        verify(repository).save(any());
    }
}
