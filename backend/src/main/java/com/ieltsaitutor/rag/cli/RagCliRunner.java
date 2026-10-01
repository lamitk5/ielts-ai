package com.ieltsaitutor.rag.cli;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.rag.domain.RightsStatus;
import com.ieltsaitutor.rag.ingestion.DocumentIngestionService;
import com.ieltsaitutor.rag.ingestion.UploadCommand;

@Component
public class RagCliRunner implements CommandLineRunner {
    private final DocumentIngestionService ingestion;
    private final RagManifestParser parser;
    private final RagCliProperties properties;

    @Autowired
    public RagCliRunner(DocumentIngestionService ingestion, RagCliProperties properties) {
        this.ingestion = ingestion;
        this.parser = new RagManifestParser(Path.of("backend/rag-data/files"));
        this.properties = properties;
    }

    public RagCliRunner(DocumentIngestionService ingestion, RagManifestParser parser) {
        this.ingestion = ingestion;
        this.parser = parser;
        this.properties = new RagCliProperties();
    }

    @Override
    public void run(String... args) {
        if (!"ingest".equalsIgnoreCase(properties.getCli())) return;
        if (ingest(Path.of(properties.getManifest())) != 0) throw new IllegalStateException("RAG manifest ingestion failed");
    }

    public int ingest(Path manifest) {
        try {
            for (RagManifestEntry entry : parser.parse(manifest).sources()) {
                var receipt = ingestion.upload(new UploadCommand(entry.title(), entry.sourceType(), entry.author(),
                        entry.organization(), entry.language(), entry.skill(), new PathMultipartFile(entry.file())));
                if (receipt.rightsStatus() != RightsStatus.PENDING_REVIEW || receipt.active()) return 1;
                ingestion.extractPreview(receipt.documentId(), receipt.versionId());
            }
            return 0;
        } catch (RuntimeException exception) {
            return 1;
        }
    }

    private static final class PathMultipartFile implements MultipartFile {
        private final Path path;
        private PathMultipartFile(Path path) { this.path = path; }
        @Override public String getName() { return path.getFileName().toString(); }
        @Override public String getOriginalFilename() { return path.getFileName().toString(); }
        @Override public String getContentType() {
            String name = getOriginalFilename().toLowerCase();
            return name.endsWith(".pdf") ? "application/pdf" : name.endsWith(".docx")
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "text/plain";
        }
        @Override public boolean isEmpty() { return size() == 0; }
        @Override public long getSize() { return size(); }
        @Override public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
        @Override public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
        @Override public void transferTo(File destination) throws IOException { Files.copy(path, destination.toPath()); }
        @Override public void transferTo(Path destination) throws IOException { Files.copy(path, destination); }
        private long size() { try { return Files.size(path); } catch (IOException exception) { return 0; } }
    }
}
