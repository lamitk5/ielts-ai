package com.ieltsaitutor.rag.ingestion;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;

@Service
public class DefaultRagLifecycleService implements RagLifecycleService {
    private final RagDocumentRepository documents;
    private final RagDocumentVersionRepository versions;

    public DefaultRagLifecycleService(RagDocumentRepository documents, RagDocumentVersionRepository versions) {
        this.documents = documents;
        this.versions = versions;
    }

    @Override
    @Transactional
    public void approve(UUID documentId, RightsReviewCommand command) {
        var document = findDocument(documentId);
        UUID versionId = document.currentVersionId();
        if (versionId == null) throw invalid("RAG_VERSION_REQUIRED", "Tài liệu chưa có version hiện tại.");
        documents.updateRightsStatus(documentId, RightsStatus.APPROVED, command.rightsNote());
        versions.setApprovedAt(versionId, Instant.now());
        if (document.rightsStatus() == RightsStatus.APPROVED || document.active()) {
            versions.updateIndexStatus(versionId, IndexStatus.NOT_INDEXED);
            versions.setIndexedAt(versionId, null);
            documents.deactivate(documentId);
        }
    }

    @Override
    @Transactional
    public void reject(UUID documentId, RightsReviewCommand command) {
        findDocument(documentId);
        documents.updateRightsStatus(documentId, RightsStatus.REJECTED, command.rightsNote());
        documents.deactivate(documentId);
    }

    @Override
    @Transactional
    public void activate(UUID documentId) {
        var document = findDocument(documentId);
        if (document.rightsStatus() != RightsStatus.APPROVED || document.currentVersionId() == null) {
            throw invalid("RAG_ACTIVATION_NOT_ALLOWED", "Chỉ tài liệu đã được duyệt mới có thể kích hoạt.");
        }
        var version = versions.findById(document.currentVersionId())
                .orElseThrow(() -> invalid("RAG_VERSION_NOT_FOUND", "Không tìm thấy version hiện tại."));
        if (version.indexStatus() != IndexStatus.INDEXED || version.approvedAt() == null || version.indexedAt() == null
                || version.indexedAt().isBefore(version.approvedAt())) {
            throw invalid("RAG_ACTIVATION_NOT_ALLOWED", "Version hiện tại chưa được index hợp lệ sau khi duyệt.");
        }
        documents.activate(documentId);
    }

    @Override
    public void deactivate(UUID documentId) { documents.deactivate(documentId); }

    private com.ieltsaitutor.rag.domain.RagDocument findDocument(UUID id) {
        return documents.findById(id).orElseThrow(() -> invalid("RAG_DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu."));
    }

    private RagInvalidStateException invalid(String code, String message) { return new RagInvalidStateException(code, message); }
}
