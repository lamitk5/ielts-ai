package com.ieltsaitutor.rag.admin;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.rag.admin.dto.RagActionResponse;
import com.ieltsaitutor.rag.admin.dto.RagDocumentDetail;
import com.ieltsaitutor.rag.admin.dto.RagDocumentSummary;
import com.ieltsaitutor.rag.admin.dto.RagUploadRequest;
import com.ieltsaitutor.rag.domain.RagIngestionJob;

@RestController
@RequestMapping("/api/admin/rag")
public class RagAdminController {
    private final RagAdminService service;

    public RagAdminController(RagAdminService service) { this.service = service; }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RagActionResponse upload(@Valid @RequestPart("metadata") RagUploadRequest metadata,
            @RequestPart("file") MultipartFile file) { return service.upload(metadata, file); }

    @GetMapping("/documents")
    public List<RagDocumentSummary> list() { return service.list(); }

    @GetMapping("/documents/{id}")
    public RagDocumentDetail detail(@PathVariable UUID id) { return service.detail(id); }

    @PatchMapping("/documents/{id}")
    public RagDocumentDetail patch(@PathVariable UUID id) { return service.detail(id); }

    @PostMapping("/documents/{id}/approve")
    public RagActionResponse approve(@PathVariable UUID id, @RequestParam String rightsNote) { return service.approve(id, rightsNote); }

    @PostMapping("/documents/{id}/reject")
    public RagActionResponse reject(@PathVariable UUID id, @RequestParam String rightsNote) { return service.reject(id, rightsNote); }

    @PostMapping("/documents/{id}/index")
    public RagActionResponse index(@PathVariable UUID id) { return service.index(id, false); }

    @PostMapping("/documents/{id}/reindex")
    public RagActionResponse reindex(@PathVariable UUID id) { return service.index(id, true); }

    @PostMapping("/documents/{id}/activate")
    public RagActionResponse activate(@PathVariable UUID id) { return service.activate(id); }

    @PostMapping("/documents/{id}/deactivate")
    public RagActionResponse deactivate(@PathVariable UUID id) { return service.deactivate(id); }

    @GetMapping("/jobs")
    public List<RagIngestionJob> jobs() { return service.jobs(); }

    @GetMapping("/jobs/{id}")
    public RagIngestionJob job(@PathVariable UUID id) { return service.job(id); }
}
