package com.ieltsaitutor.rag.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.rag.admin.dto.RagActionResponse;
import com.ieltsaitutor.rag.admin.dto.RagDocumentDetail;
import com.ieltsaitutor.rag.admin.dto.RagDocumentSummary;
import com.ieltsaitutor.rag.admin.dto.RagPreviewResponse;
import com.ieltsaitutor.rag.admin.dto.RagUploadRequest;
import com.ieltsaitutor.rag.domain.IndexStatus;
import com.ieltsaitutor.rag.domain.RagIngestionJob;
import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.ingestion.DocumentIngestionService;
import com.ieltsaitutor.rag.ingestion.IndexMode;
import com.ieltsaitutor.rag.ingestion.RagInvalidStateException;
import com.ieltsaitutor.rag.ingestion.RagLifecycleService;
import com.ieltsaitutor.rag.ingestion.RightsReviewCommand;
import com.ieltsaitutor.rag.ingestion.UploadCommand;
import com.ieltsaitutor.rag.repository.RagDocumentRepository;
import com.ieltsaitutor.rag.repository.RagDocumentVersionRepository;
import com.ieltsaitutor.rag.repository.RagIngestionJobRepository;

@Service
public class RagAdminService {
    private final DocumentIngestionService ingestion;
    private final RagLifecycleService lifecycle;
    private final RagDocumentRepository documents;
    private final RagDocumentVersionRepository versions;
    private final RagIngestionJobRepository jobs;

    public RagAdminService(DocumentIngestionService ingestion, RagLifecycleService lifecycle,
            RagDocumentRepository documents, RagDocumentVersionRepository versions, RagIngestionJobRepository jobs) {
        this.ingestion = ingestion;
        this.lifecycle = lifecycle;
        this.documents = documents;
        this.versions = versions;
        this.jobs = jobs;
    }

    public RagActionResponse upload(RagUploadRequest request, MultipartFile file) {
        var receipt = ingestion.upload(new UploadCommand(request.title(), value(request.sourceType(), "ADMIN_UPLOAD"),
                request.author(), request.organization(), request.language(), request.skill(), file));
        return new RagActionResponse("UPLOADED", receipt.documentId(), receipt.rightsStatus().name(),
                receipt.indexStatus().name(), receipt.active());
    }

    public List<RagDocumentSummary> list() {
        return documents.listSummaries().stream().map(document -> {
            IndexStatus index = document.currentVersionId() == null ? IndexStatus.NOT_INDEXED
                    : versions.findById(document.currentVersionId()).map(version -> version.indexStatus()).orElse(IndexStatus.NOT_INDEXED);
            return new RagDocumentSummary(document.id(), document.title(), document.skill().name(),
                    document.rightsStatus().name(), index.name(), document.active(), document.updatedAt());
        }).toList();
    }

    public RagDocumentDetail detail(UUID id) {
        var document = documents.findById(id).orElseThrow(() -> invalid("RAG_DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu."));
        var version = document.currentVersionId() == null ? null : versions.findById(document.currentVersionId()).orElse(null);
        RagPreviewResponse preview = null;
        if (version != null) {
            try {
                var result = ingestion.extractPreview(id, version.id());
                preview = new RagPreviewResponse(result.text(), result.characterCount());
            } catch (RagInvalidStateException ignored) {
                preview = null;
            }
        }
        return new RagDocumentDetail(document.id(), document.title(), document.skill().name(), document.rightsStatus().name(),
                version == null ? IndexStatus.NOT_INDEXED.name() : version.indexStatus().name(), document.active(), preview,
                document.updatedAt());
    }

    public RagActionResponse approve(UUID id, String note) { lifecycle.approve(id, new RightsReviewCommand(note)); return state(id, "APPROVED"); }
    public RagActionResponse reject(UUID id, String note) { lifecycle.reject(id, new RightsReviewCommand(note)); return state(id, "REJECTED"); }
    public RagActionResponse activate(UUID id) { lifecycle.activate(id); return state(id, "ACTIVATED"); }
    public RagActionResponse deactivate(UUID id) { lifecycle.deactivate(id); return state(id, "DEACTIVATED"); }
    public RagActionResponse index(UUID id, UUID versionId, boolean reindex) { ingestion.index(id, versionId, reindex ? IndexMode.REBUILD : IndexMode.SYNC); return state(id, reindex ? "REINDEXED" : "INDEXED"); }
    public RagActionResponse index(UUID id, boolean reindex) {
        var document = documents.findById(id).orElseThrow(() -> invalid("RAG_DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu."));
        if (document.currentVersionId() == null) throw invalid("RAG_VERSION_REQUIRED", "Tài liệu chưa có version hiện tại.");
        return index(id, document.currentVersionId(), reindex);
    }
    public List<RagIngestionJob> jobs() { return jobs.listRecent(50); }
    public RagIngestionJob job(UUID id) { return jobs.findById(id).orElseThrow(() -> invalid("RAG_JOB_NOT_FOUND", "Không tìm thấy ingestion job.")); }

    private RagActionResponse state(UUID id, String action) {
        var document = documents == null ? null : documents.findById(id).orElse(null);
        return new RagActionResponse(action, id, document == null ? null : document.rightsStatus().name(),
                document == null || document.currentVersionId() == null ? IndexStatus.NOT_INDEXED.name()
                        : versions.findById(document.currentVersionId()).map(version -> version.indexStatus().name()).orElse(IndexStatus.NOT_INDEXED.name()),
                document != null && document.active());
    }

    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private RagInvalidStateException invalid(String code, String message) { return new RagInvalidStateException(code, message); }
}
