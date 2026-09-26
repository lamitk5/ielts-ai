package com.ieltsaitutor.practice.generator.service;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.exception.InvalidGenerationStateException;

@Component
public class GenerationStateMachine {

    private static final Map<GenerationState, Set<GenerationState>> ALLOWED_TRANSITIONS = Map.of(
            GenerationState.DRAFT, Set.of(GenerationState.GENERATING),
            GenerationState.GENERATING, Set.of(GenerationState.AUTO_VALIDATING, GenerationState.DRAFT),
            GenerationState.AUTO_VALIDATING, Set.of(GenerationState.PENDING_REVIEW, GenerationState.NEEDS_REVISION),
            GenerationState.PENDING_REVIEW, Set.of(GenerationState.APPROVED, GenerationState.NEEDS_REVISION, GenerationState.REJECTED),
            GenerationState.NEEDS_REVISION, Set.of(GenerationState.GENERATING, GenerationState.AUTO_VALIDATING, GenerationState.REJECTED),
            GenerationState.APPROVED, Set.of(GenerationState.REJECTED),
            GenerationState.REJECTED, Set.of()
    );

    public boolean canTransition(GenerationState from, GenerationState to) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void validateTransition(GenerationState from, GenerationState to) {
        if (from == null || to == null) {
            throw new InvalidGenerationStateException("State transition requires non-null states: from=" + from + ", to=" + to);
        }
        if (from == to) {
            return;
        }
        Set<GenerationState> targets = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!targets.contains(to)) {
            throw new InvalidGenerationStateException(
                    String.format("Illegal practice generation state transition from %s to %s", from, to));
        }
    }

    public boolean isStudentVisible(GenerationState state) {
        return state == GenerationState.APPROVED;
    }
}
