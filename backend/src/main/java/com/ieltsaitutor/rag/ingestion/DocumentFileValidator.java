package com.ieltsaitutor.rag.ingestion;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ieltsaitutor.rag.config.RagProperties;

@Service
public class DocumentFileValidator {
    private static final Set<String> EXTENSIONS = Set.of("pdf", "docx", "txt");
    private static final Set<String> MIME_TYPES = Set.of("application/pdf", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private final RagProperties properties;

    public DocumentFileValidator(RagProperties properties) {
        this.properties = properties;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RagValidationException("RAG_EMPTY_DOCUMENT", "Tài liệu không có nội dung.");
        }
        if (file.getSize() > properties.maxUploadBytes()) {
            throw new RagValidationException("RAG_FILE_TOO_LARGE", "Tài liệu vượt quá dung lượng cho phép.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.contains("..") || filename.contains("/")
                || filename.contains("\\")) {
            throw new RagValidationException("RAG_PATH_INVALID", "Tên tài liệu không hợp lệ.");
        }
        String extension = extension(filename);
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension) || !MIME_TYPES.contains(mime)
                || (extension.equals("pdf") && !mime.equals("application/pdf"))
                || (extension.equals("txt") && !mime.equals("text/plain"))
                || (extension.equals("docx") && !mime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))) {
            throw new RagValidationException("RAG_UNSUPPORTED_FILE", "Chỉ hỗ trợ PDF, DOCX và TXT hợp lệ.");
        }
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
