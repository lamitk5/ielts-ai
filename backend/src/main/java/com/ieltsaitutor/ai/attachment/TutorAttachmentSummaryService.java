package com.ieltsaitutor.ai.attachment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TutorAttachmentSummaryService {
    private static final int PART_TOKEN_TARGET = 2_000;
    private final Function<UUID, List<RetrievedAttachmentChunk>> chunkSource;

    @Autowired
    public TutorAttachmentSummaryService(TutorAttachmentChunkRepository repository) {
        this(repository::findByAttachmentId);
    }

    public TutorAttachmentSummaryService(Function<UUID, List<RetrievedAttachmentChunk>> chunkSource) {
        this.chunkSource = chunkSource;
    }

    public AttachmentRepresentation summarizeWholeDocument(UUID attachmentId) {
        List<RetrievedAttachmentChunk> chunks = chunksFor(attachmentId);
        if (chunks.isEmpty()) {
            return new AttachmentRepresentation(attachmentId, "attachment", List.of(), 0);
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;
        String filename = chunks.get(0).filename();
        for (RetrievedAttachmentChunk chunk : chunks) {
            int chunkTokens = estimateTokens(chunk.content());
            if (!current.isEmpty() && currentTokens + chunkTokens > PART_TOKEN_TARGET) {
                parts.add("Part " + (parts.size() + 1) + "\n" + current);
                current = new StringBuilder();
                currentTokens = 0;
            }
            current.append(current.isEmpty() ? "" : "\n\n").append(chunk.content());
            currentTokens += chunkTokens;
        }
        if (!current.isEmpty()) parts.add("Part " + (parts.size() + 1) + "\n" + current);
        return new AttachmentRepresentation(attachmentId, filename == null || filename.isBlank() ? "attachment" : filename,
                parts, estimateTokens(String.join("\n\n", parts)));
    }

    public List<AttachmentRepresentation> compareDocuments(List<UUID> attachmentIds) {
        if (attachmentIds == null) return List.of();
        return attachmentIds.stream().map(this::summarizeWholeDocument).toList();
    }

    private List<RetrievedAttachmentChunk> chunksFor(UUID attachmentId) {
        return java.util.Optional.ofNullable(chunkSource.apply(attachmentId)).orElseGet(List::of).stream()
                .sorted(Comparator.comparingInt(RetrievedAttachmentChunk::chunkIndex)).toList();
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 4.0));
    }
}
