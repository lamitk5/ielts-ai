package com.ieltsaitutor.rag.ingestion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ieltsaitutor.rag.domain.ExtractionStatus;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RagDocument;
import com.ieltsaitutor.rag.domain.RagDocumentVersion;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.domain.Skill;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;

@ExtendWith(MockitoExtension.class)
class RagLifecycleServiceTest {
    @Mock RagDocumentRepository documents;
    @Mock RagDocumentVersionRepository versions;
    private RagLifecycleService service;

    @BeforeEach
    void setUp() { service = new DefaultRagLifecycleService(documents, versions); }

    @Test
    void requiresRightsNoteForApproval() {
        assertThatThrownBy(() -> service.approve(UUID.randomUUID(), new RightsReviewCommand(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approvesWithoutActivating() {
        UUID id = UUID.randomUUID();
        when(documents.findById(id)).thenReturn(Optional.of(document(id, false, RightsStatus.PENDING_REVIEW, UUID.randomUUID())));
        service.approve(id, new RightsReviewCommand("licensed source"));
        verify(documents).updateRightsStatus(id, RightsStatus.APPROVED, "licensed source");
    }

    @Test
    void activatesOnlyIndexedCurrentVersion() {
        UUID id = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        when(documents.findById(id)).thenReturn(Optional.of(document(id, false, RightsStatus.APPROVED, versionId)));
        when(versions.findById(versionId)).thenReturn(Optional.of(version(id, versionId, IndexStatus.NOT_INDEXED, Instant.now(), Instant.now())));
        assertThatThrownBy(() -> service.activate(id)).isInstanceOf(RagInvalidStateException.class);
    }

    @Test
    void reapprovalInvalidatesOldIndex() {
        UUID id = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
        when(documents.findById(id)).thenReturn(Optional.of(document(id, true, RightsStatus.APPROVED, versionId)));
        service.approve(id, new RightsReviewCommand("renewed rights"));
        verify(versions).updateIndexStatus(versionId, IndexStatus.NOT_INDEXED);
        verify(versions).setIndexedAt(versionId, null);
    }

    @Test
    void deactivationKeepsChunksButDisablesRetrieval() {
        UUID id = UUID.randomUUID();
        service.deactivate(id);
        verify(documents).deactivate(id);
    }

    @Test
    void rejectionDisablesRetrieval() {
        UUID id = UUID.randomUUID();
        when(documents.findById(id)).thenReturn(Optional.of(document(id, true, RightsStatus.APPROVED, UUID.randomUUID())));
        service.reject(id, new RightsReviewCommand("not authorized"));
        verify(documents).updateRightsStatus(id, RightsStatus.REJECTED, "not authorized");
        verify(documents).deactivate(id);
    }

    private RagDocument document(UUID id, boolean active, RightsStatus rights, UUID version) {
        Instant now = Instant.now();
        return new RagDocument(id, "Guide", "UPLOAD", null, null, "en", Skill.GENERAL, rights, "note", active, version, now, now);
    }

    private RagDocumentVersion version(UUID documentId, UUID id, IndexStatus status, Instant approved, Instant indexed) {
        return new RagDocumentVersion(id, documentId, "1", "guide.pdf", "application/pdf", 7, "checksum", "path",
                ExtractionStatus.READY_FOR_REVIEW, status, approved, indexed, Instant.now());
    }
}
