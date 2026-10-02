package com.ieltsaitutor.mock;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class MockSectionResolver {

    public List<MockTestSection> resolveSections(UUID sessionId, MockTestDefinition definition) {
        MockTestDefinition def = definition != null ? definition : MockTestDefinition.DEFAULT_MOCK;
        Instant now = Instant.now();
        List<MockTestSection> list = new ArrayList<>();

        for (MockTestDefinition.MockSectionSpec spec : def.sectionSpecs()) {
            list.add(new MockTestSection(
                    UUID.randomUUID(),
                    sessionId,
                    spec.order(),
                    spec.skill(),
                    spec.practiceId(),
                    spec.practiceVersionId(),
                    spec.publishedSetId(),
                    null,
                    spec.timeLimitSeconds(),
                    MockTestSectionStatus.NOT_STARTED,
                    now,
                    now
            ));
        }

        return list;
    }
}
