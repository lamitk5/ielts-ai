package com.ieltsaitutor.ai.attachment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TutorAttachmentValidator {
    private static final long MAX_DOCX_UNCOMPRESSED_BYTES = 25 * 1024 * 1024L;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;

    public TutorAttachmentValidationResult validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            return invalid("ATTACHMENT_EMPTY", "Tệp đính kèm không có nội dung.");
        }
        if (file.getSize() > TutorAttachmentContract.MAX_FILE_SIZE_BYTES) {
            return invalid("ATTACHMENT_SIZE_EXCEEDED", "Dung lượng tệp vượt quá 10MB.");
        }

        String original = file.getOriginalFilename();
        if (!safeFilename(original)) {
            return invalid("ATTACHMENT_FILENAME_INVALID", "Tên tệp không hợp lệ.");
        }
        String filename = original.trim();
        String extension = TutorAttachmentContract.extensionOf(filename);
        String lower = filename.toLowerCase(Locale.ROOT);
        if (TutorAttachmentContract.DISALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith)
                || extension.isBlank()) {
            return invalid("ATTACHMENT_TYPE_NOT_SUPPORTED", "Định dạng tệp không được hỗ trợ.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            return invalid("ATTACHMENT_READ_FAILED", "Không thể đọc tệp đính kèm.");
        }

        String supplied = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT).trim();
        Set<String> expectedTypes = TutorAttachmentContract.EXTENSION_MIME_TYPES.get(extension);
        if (!supplied.isBlank() && !supplied.equals("application/octet-stream") && !expectedTypes.contains(supplied)) {
            return invalid("ATTACHMENT_MIME_MISMATCH", "Kiểu tệp không khớp với phần mở rộng.");
        }

        try {
            if (extension.equals(".docx")) {
                validateDocx(bytes);
            } else if (extension.equals(".pdf")) {
                require(bytes.length >= 5 && startsWith(bytes, new byte[] {'%', 'P', 'D', 'F', '-'}), "ATTACHMENT_SIGNATURE_MISMATCH");
            } else if (extension.equals(".png")) {
                require(startsWith(bytes, new byte[] {(byte) 0x89, 'P', 'N', 'G'}), "ATTACHMENT_SIGNATURE_MISMATCH");
                validatePixels(bytes);
            } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                require(startsWith(bytes, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}), "ATTACHMENT_SIGNATURE_MISMATCH");
                validatePixels(bytes);
            } else if (extension.equals(".webp")) {
                require(bytes.length >= 12 && startsWith(bytes, new byte[] {'R', 'I', 'F', 'F'})
                        && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P',
                        "ATTACHMENT_SIGNATURE_MISMATCH");
            }
        } catch (ValidationFailure failure) {
            return invalid(failure.code, failure.message);
        }

        AttachmentKind kind = TutorAttachmentContract.isImage(extension) ? AttachmentKind.IMAGE : AttachmentKind.DOCUMENT;
        return new TutorAttachmentValidationResult(filename, expectedTypes.iterator().next(), kind, bytes.length, bytes, null, null);
    }

    private void validateDocx(byte[] bytes) throws ValidationFailure {
        require(bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K', "ATTACHMENT_DOCUMENT_INVALID");
        boolean contentTypes = false;
        boolean document = false;
        long uncompressed = 0;
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || name.contains("..") || name.startsWith("/") || !entries.add(name)) {
                    throw new ValidationFailure("ATTACHMENT_DOCUMENT_INVALID", "Cấu trúc tài liệu không hợp lệ.");
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("vba") || lower.endsWith(".js") || lower.endsWith(".exe")) {
                    throw new ValidationFailure("ATTACHMENT_ACTIVE_CONTENT", "Tệp chứa nội dung chủ động không được hỗ trợ.");
                }
                if (name.equals("[Content_Types].xml")) contentTypes = true;
                if (name.equals("word/document.xml")) document = true;
                int read;
                while ((read = zip.read(buffer)) >= 0) {
                    uncompressed += read;
                    if (uncompressed > MAX_DOCX_UNCOMPRESSED_BYTES) {
                        throw new ValidationFailure("ATTACHMENT_DOCUMENT_TOO_LARGE", "Nội dung nén vượt quá giới hạn an toàn.");
                    }
                }
            }
        } catch (IOException exception) {
            throw new ValidationFailure("ATTACHMENT_DOCUMENT_INVALID", "Không thể đọc tài liệu.");
        }
        require(contentTypes && document, "ATTACHMENT_DOCUMENT_INVALID");
    }

    private void validatePixels(byte[] bytes) throws ValidationFailure {
        try {
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ValidationFailure("ATTACHMENT_IMAGE_INVALID", "Không thể đọc ảnh.");
            }
            if ((long) image.getWidth() * image.getHeight() > MAX_IMAGE_PIXELS) {
                throw new ValidationFailure("ATTACHMENT_IMAGE_TOO_LARGE", "Ảnh vượt quá giới hạn kích thước an toàn.");
            }
        } catch (IOException exception) {
            throw new ValidationFailure("ATTACHMENT_IMAGE_INVALID", "Không thể đọc ảnh.");
        }
    }

    private static boolean safeFilename(String filename) {
        return filename != null && !filename.isBlank() && !filename.contains("/") && !filename.contains("\\")
                && filename.indexOf('\0') < 0 && !filename.contains("..") && filename.length() <= 255;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (bytes[i] != prefix[i]) return false;
        return true;
    }

    private static void require(boolean condition, String code) throws ValidationFailure {
        if (!condition) throw new ValidationFailure(code, "Nội dung tệp không khớp với định dạng đã chọn.");
    }

    private static TutorAttachmentValidationResult invalid(String code, String message) {
        return new TutorAttachmentValidationResult(null, null, null, 0, null, code, message);
    }

    private static final class ValidationFailure extends RuntimeException {
        private final String code;
        private final String message;

        private ValidationFailure(String code, String message) {
            super(message);
            this.code = code;
            this.message = message;
        }
    }
}
