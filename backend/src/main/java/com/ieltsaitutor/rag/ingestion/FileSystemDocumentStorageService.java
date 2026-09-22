package com.ieltsaitutor.rag.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.rag.config.RagProperties;

@Service
public class FileSystemDocumentStorageService implements DocumentStorageService {
    private final Path root;
    private final DocumentFileValidator validator;
    private final DocumentChecksumService checksumService;

    public FileSystemDocumentStorageService(RagProperties properties, DocumentFileValidator validator,
            DocumentChecksumService checksumService) {
        this.root = properties.storageRootPath().toAbsolutePath().normalize();
        this.validator = validator;
        this.checksumService = checksumService;
    }

    @Override
    public StoredDocument store(MultipartFile file, UUID documentId, UUID versionId) {
        validator.validate(file);
        String relative = documentId + "/" + versionId + "/" + UUID.randomUUID() + ".bin";
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new RagValidationException("RAG_PATH_INVALID", "Đường dẫn lưu tài liệu không hợp lệ.");
        }
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            String checksum;
            try (InputStream input = Files.newInputStream(target)) {
                checksum = checksumService.sha256(input);
            }
            return new StoredDocument(target, relative.replace('\\', '/'), file.getOriginalFilename(),
                    file.getContentType(), file.getSize(), checksum);
        } catch (IOException exception) {
            throw new RagValidationException("RAG_STORAGE_FAILED", "Không thể lưu tài liệu.");
        }
    }

    @Override
    public InputStream open(StoredDocument storedDocument) {
        try {
            Path path = storedDocument.absolutePath().toAbsolutePath().normalize();
            if (!path.startsWith(root)) {
                throw new RagValidationException("RAG_PATH_INVALID", "Đường dẫn tài liệu không hợp lệ.");
            }
            return Files.newInputStream(path);
        } catch (IOException exception) {
            throw new RagValidationException("RAG_STORAGE_FAILED", "Không thể mở tài liệu.");
        }
    }
}
