package com.ieltsaitutor.ai.attachment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TutorAttachmentSummaryServiceTest {
    @Test
    void wholeDocumentUsesHierarchicalBatches() {
        UUID id = UUID.randomUUID();
        TutorAttachmentSummaryService service = new TutorAttachmentSummaryService(attachmentId -> List.of(
                chunk(id, 0, "beginning"), chunk(id, 1, "middle"), chunk(id, 2, "ending")));

        AttachmentRepresentation representation = service.summarizeWholeDocument(id);

        assertThat(representation.sections()).hasSize(1);
        assertThat(representation.text()).contains("beginning", "middle", "ending");
    }

    @Test
    void comparisonRepresentsEveryFile() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TutorAttachmentSummaryService service = new TutorAttachmentSummaryService(id -> List.of(chunk(id, 0, id.toString())));

        List<AttachmentRepresentation> representations = service.compareDocuments(List.of(first, second));

        assertThat(representations).extracting(AttachmentRepresentation::attachmentId).containsExactly(first, second);
    }

    @Test
    void focusedSummarySelectsRelevantChunksWithinBudget() {
        UUID id = UUID.randomUUID();
        TutorAttachmentSummaryService service = new TutorAttachmentSummaryService(attachmentId -> List.of(
                chunk(id, 0, "introductory material " + "x".repeat(400)),
                chunk(id, 1, "SECRET_CODE=LUMEN-DOCX-57319 and the requested code"),
                chunk(id, 2, "appendix " + "y".repeat(400))));

        AttachmentRepresentation representation = service.summarizeRelevantDocument(id, "Which secret code?", 30);

        assertThat(representation.text()).contains("LUMEN-DOCX-57319");
        assertThat(representation.tokenEstimate()).isLessThanOrEqualTo(30);
    }

    private static RetrievedAttachmentChunk chunk(UUID id, int index, String content) {
        return new RetrievedAttachmentChunk(id, "file.txt", null, null, index, content, .8, null);
    }
}
