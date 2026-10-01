package com.ieltsaitutor.compatibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.ieltsaitutor.ai.attachment.TutorAttachment;
import com.ieltsaitutor.ai.attachment.TutorAttachment.AttachmentStatus;
import com.ieltsaitutor.ai.attachment.TutorAttachmentContract;
import com.ieltsaitutor.ai.attachment.TutorAttachmentService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AcademicLuxuryCompatibilityTest {
    @Autowired
    private TutorAttachmentService attachmentService;

    @Test
    void applicationContextStartsWithOptionalAiCredentials() {
        assertThat(attachmentService).isNotNull();
    }

    @Test
    void imageAttachmentRemainsReadyWithoutClaimingVisionAnalysis() {
        TutorAttachment attachment = attachmentService.upload(
                UUID.randomUUID(),
                new MockMultipartFile("file", "chart.webp", "image/webp", new byte[] { 1, 2 }),
                "compatibility-image"
        );

        assertThat(attachment.status()).isEqualTo(AttachmentStatus.IMAGE_READY);
        assertThat(attachment.capability()).isEqualTo(TutorAttachmentContract.VISION_NOT_ENABLED);
    }

    @Test
    void attachmentOwnershipDoesNotCrossAccounts() {
        UUID ownerId = UUID.randomUUID();
        TutorAttachment attachment = attachmentService.upload(
                ownerId,
                new MockMultipartFile("file", "notes.txt", "text/plain", "notes".getBytes()),
                "compatibility-owner"
        );

        assertThatThrownBy(() -> attachmentService.get(UUID.randomUUID(), attachment.id()))
                .hasFieldOrPropertyWithValue("code", "ATTACHMENT_NOT_FOUND");
    }

    @Test
    void unsupportedContentUsesNormalizedSafeErrorBoundary() {
        assertThatThrownBy(() -> attachmentService.upload(
                UUID.randomUUID(),
                new MockMultipartFile("file", "page.html", "text/html", "<p>no</p>".getBytes()),
                "compatibility-unsupported"
        ))
                .hasFieldOrPropertyWithValue("code", "ATTACHMENT_TYPE_NOT_SUPPORTED")
                .hasMessageNotContaining("provider")
                .hasMessageNotContaining("secret");
    }
}
