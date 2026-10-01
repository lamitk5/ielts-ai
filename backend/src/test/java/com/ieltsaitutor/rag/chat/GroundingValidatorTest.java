package com.ieltsaitutor.rag.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ieltsaitutor.ai.dto.AiSource;

class GroundingValidatorTest {
    private final GroundingValidator validator = new DefaultGroundingValidator();

    @Test
    void acceptsRetrievedCitation() {
        assertThat(validator.validate(List.of(new AiSource("source-1", "Title", "Section")), Set.of("source-1")))
                .hasSize(1);
    }

    @Test
    void rejectsCitationNotInRetrievedSet() {
        assertThat(validator.validate(List.of(new AiSource("invented", "Title", "Section")), Set.of("source-1")))
                .isEmpty();
    }
}
