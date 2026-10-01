package com.ieltsaitutor.practice.generator.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.practice.generator.domain.GenerationState;
import com.ieltsaitutor.practice.generator.exception.InvalidGenerationStateException;

class GenerationStateMachineTest {

    private GenerationStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new GenerationStateMachine();
    }

    @Test
    void allowsLegalTransitions() {
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.DRAFT, GenerationState.GENERATING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.GENERATING, GenerationState.AUTO_VALIDATING));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.AUTO_VALIDATING, GenerationState.PENDING_REVIEW));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.AUTO_VALIDATING, GenerationState.NEEDS_REVISION));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.PENDING_REVIEW, GenerationState.APPROVED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.PENDING_REVIEW, GenerationState.NEEDS_REVISION));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.PENDING_REVIEW, GenerationState.REJECTED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(GenerationState.NEEDS_REVISION, GenerationState.GENERATING));
    }

    @Test
    void rejectsIllegalTransitions() {
        // Direct generation to public/approved or student visible is forbidden
        assertThrows(InvalidGenerationStateException.class,
                () -> stateMachine.validateTransition(GenerationState.DRAFT, GenerationState.APPROVED));
        assertThrows(InvalidGenerationStateException.class,
                () -> stateMachine.validateTransition(GenerationState.GENERATING, GenerationState.APPROVED));
        assertThrows(InvalidGenerationStateException.class,
                () -> stateMachine.validateTransition(GenerationState.AUTO_VALIDATING, GenerationState.APPROVED));
        assertThrows(InvalidGenerationStateException.class,
                () -> stateMachine.validateTransition(GenerationState.REJECTED, GenerationState.APPROVED));
    }

    @Test
    void studentVisibilityRuleOnlyAllowsApproved() {
        assertTrue(stateMachine.isStudentVisible(GenerationState.APPROVED));
        assertFalse(stateMachine.isStudentVisible(GenerationState.DRAFT));
        assertFalse(stateMachine.isStudentVisible(GenerationState.GENERATING));
        assertFalse(stateMachine.isStudentVisible(GenerationState.AUTO_VALIDATING));
        assertFalse(stateMachine.isStudentVisible(GenerationState.PENDING_REVIEW));
        assertFalse(stateMachine.isStudentVisible(GenerationState.NEEDS_REVISION));
        assertFalse(stateMachine.isStudentVisible(GenerationState.REJECTED));
    }
}
