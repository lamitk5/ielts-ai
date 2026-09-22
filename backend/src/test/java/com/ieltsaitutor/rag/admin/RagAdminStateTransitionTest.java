package com.ieltsaitutor.rag.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ieltsaitutor.rag.ingestion.DocumentIngestionService;
import com.ieltsaitutor.rag.ingestion.RagLifecycleService;

class RagAdminStateTransitionTest {
    private RagAdminService service;
    private RagLifecycleService lifecycle;
    private DocumentIngestionService ingestion;

    @BeforeEach
    void setUp() {
        lifecycle = mock(RagLifecycleService.class);
        ingestion = mock(DocumentIngestionService.class);
        service = new RagAdminService(ingestion, lifecycle, null, null, null);
    }

    @Test void approveDoesNotActivate() { service.approve(UUID.randomUUID(), "licensed"); verify(lifecycle).approve(any(), any()); }
    @Test void rejectDisablesRetrieval() { service.reject(UUID.randomUUID(), "not authorized"); verify(lifecycle).reject(any(), any()); }
    @Test void indexRequiresApproval() { service.index(UUID.randomUUID(), UUID.randomUUID(), false); verify(ingestion).index(any(), any(), any()); }
    @Test void activateRequiresIndexedVersion() { service.activate(UUID.randomUUID()); verify(lifecycle).activate(any()); }
    @Test void deactivateLeavesVersionAuditable() { service.deactivate(UUID.randomUUID()); verify(lifecycle).deactivate(any()); }
    @Test void reindexCreatesJob() { service.index(UUID.randomUUID(), UUID.randomUUID(), true); verify(ingestion).index(any(), any(), any()); }
}
