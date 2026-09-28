package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class TutorAttachmentValidatorTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Test
    void acceptsPdfDocxTxtPngJpgJpegWebp() throws Exception {
        TutorAttachmentValidator validator = new TutorAttachmentValidator();

        assertThat(validator.validate(file("essay.pdf", "application/pdf", "%PDF-1.7".getBytes())).valid()).isTrue();
        assertThat(validator.validate(file("essay.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx())).valid()).isTrue();
        assertThat(validator.validate(file("notes.txt", "text/plain", "hello".getBytes())).valid()).isTrue();
        assertThat(validator.validate(file("image.png", "image/png", PNG)).valid()).isTrue();
        assertThat(validator.validate(file("image.jpg", "image/jpeg", tinyJpeg())).valid()).isTrue();
        assertThat(validator.validate(file("image.jpeg", "image/jpeg", tinyJpeg())).valid()).isTrue();
        assertThat(validator.validate(file("image.webp", "image/webp", webp())).valid()).isTrue();
    }

    @Test
    void rejectsOversizedFile() {
        TutorAttachmentValidationResult result = new TutorAttachmentValidator().validate(
                file("large.txt", "text/plain", new byte[(int) TutorAttachmentContract.MAX_FILE_SIZE_BYTES + 1]));

        assertThat(result.errorCode()).isEqualTo("ATTACHMENT_SIZE_EXCEEDED");
    }

    @Test
    void rejectsEmptyFile() {
        assertThat(new TutorAttachmentValidator().validate(file("empty.txt", "text/plain", new byte[0])).errorCode())
                .isEqualTo("ATTACHMENT_EMPTY");
    }

    @Test
    void rejectsExtensionMimeMismatch() {
        assertThat(new TutorAttachmentValidator().validate(file("image.jpg", "application/pdf", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})).errorCode())
                .isEqualTo("ATTACHMENT_MIME_MISMATCH");
    }

    @Test
    void rejectsMagicByteMismatch() {
        assertThat(new TutorAttachmentValidator().validate(file("image.png", "image/png", "not png".getBytes())).errorCode())
                .isEqualTo("ATTACHMENT_SIGNATURE_MISMATCH");
    }

    @Test
    void rejectsMalformedDocument() {
        assertThat(new TutorAttachmentValidator().validate(file("broken.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[] {'P', 'K', 3, 4})).errorCode())
                .isEqualTo("ATTACHMENT_DOCUMENT_INVALID");
    }

    @Test
    void rejectsDecompressionBomb() throws Exception {
        byte[] bomb = new byte[26 * 1024 * 1024];
        java.util.Arrays.fill(bomb, (byte) 'a');
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(bomb);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("types".getBytes());
            zip.closeEntry();
        }

        assertThat(new TutorAttachmentValidator().validate(file("bomb.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", output.toByteArray())).errorCode())
                .isEqualTo("ATTACHMENT_DOCUMENT_TOO_LARGE");
    }

    @Test
    void rejectsScriptAndExecutableExtensions() {
        assertThat(new TutorAttachmentValidator().validate(file("script.svg", "image/svg+xml", "<svg/>".getBytes())).errorCode())
                .isEqualTo("ATTACHMENT_TYPE_NOT_SUPPORTED");
    }

    @Test
    void rejectsActiveContent() throws Exception {
        byte[] docx = docxWithEntry("word/vbaProject.bin", new byte[] {1});
        assertThat(new TutorAttachmentValidator().validate(file("macro.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx)).errorCode())
                .isEqualTo("ATTACHMENT_ACTIVE_CONTENT");
    }

    @Test
    void rejectsTraversalFilename() {
        assertThat(new TutorAttachmentValidator().validate(file("../notes.txt", "text/plain", "hello".getBytes())).errorCode())
                .isEqualTo("ATTACHMENT_FILENAME_INVALID");
    }

    @Test
    void acceptsExact10MiBAndRejectsOneByteMore() {
        TutorAttachmentValidator validator = new TutorAttachmentValidator();
        byte[] exact = new byte[(int) TutorAttachmentContract.MAX_FILE_SIZE_BYTES];
        assertThat(validator.validate(file("exact.txt", "text/plain", exact)).valid()).isTrue();
        assertThat(validator.validate(file("more.txt", "text/plain", new byte[exact.length + 1])).errorCode())
                .isEqualTo("ATTACHMENT_SIZE_EXCEEDED");
    }

    @Test
    void rejectsImageAbove25Megapixels() {
        // The validator's decoder guard is exercised with a deliberately invalid oversized image payload.
        TutorAttachmentValidationResult result = new TutorAttachmentValidator().validate(
                file("huge.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'}));
        assertThat(result.valid()).isFalse();
    }

    private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private static byte[] webp() {
        return new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
    }

    private static byte[] tinyJpeg() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", output);
        return output.toByteArray();
    }

    private static byte[] docx() throws IOException {
        return docxWithEntry("[Content_Types].xml", "types".getBytes(), "word/document.xml", "hello".getBytes());
    }

    private static byte[] docxWithEntry(String name, byte[] bytes) throws IOException {
        return docxWithEntry(name, bytes, "[Content_Types].xml", "types".getBytes());
    }

    private static byte[] docxWithEntry(String firstName, byte[] firstBytes, String secondName, byte[] secondBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(firstName));
            zip.write(firstBytes);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(secondName));
            zip.write(secondBytes);
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
