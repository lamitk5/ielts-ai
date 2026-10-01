package com.ieltsaitutor.ai.attachment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    public AttachmentRepresentation summarizeRelevantDocument(UUID attachmentId, String question, int maxTokens) {
        if (maxTokens <= 0) throw new IllegalArgumentException("Summary token budget must be positive");
        List<RetrievedAttachmentChunk> chunks = chunksFor(attachmentId);
        if (chunks.isEmpty()) {
            return new AttachmentRepresentation(attachmentId, "attachment", List.of(), 0);
        }

        Set<String> terms = terms(question);
        List<RetrievedAttachmentChunk> ranked = chunks.stream()
                .sorted(Comparator.comparingInt((RetrievedAttachmentChunk chunk) -> relevance(chunk, terms))
                        .reversed()
                        .thenComparingInt(RetrievedAttachmentChunk::chunkIndex))
                .toList();
        List<RetrievedAttachmentChunk> selected = new ArrayList<>();
        int tokens = 0;
        for (RetrievedAttachmentChunk chunk : ranked) {
            int chunkTokens = estimateTokens(chunk.content());
            if (tokens + chunkTokens <= maxTokens) {
                selected.add(chunk);
                tokens += chunkTokens;
            }
        }
        if (selected.isEmpty()) {
            RetrievedAttachmentChunk first = chunks.get(0);
            String clipped = clipToTokens(first.content(), maxTokens);
            selected.add(new RetrievedAttachmentChunk(first.attachmentId(), first.filename(), first.pageNumber(),
                    first.sectionLabel(), first.chunkIndex(), clipped, first.similarity(), first.embeddingSpace()));
        }
        selected.sort(Comparator.comparingInt(RetrievedAttachmentChunk::chunkIndex));
        String filename = selected.get(0).filename();
        List<String> sections = selected.stream()
                .map(chunk -> "Chunk " + chunk.chunkIndex() + "\n" + chunk.content())
                .toList();
        return new AttachmentRepresentation(attachmentId, filename == null || filename.isBlank() ? "attachment" : filename,
                sections, estimateTokens(String.join("\n\n", sections)));
    }

    public List<AttachmentRepresentation> compareDocuments(List<UUID> attachmentIds) {
        if (attachmentIds == null) return List.of();
        return attachmentIds.stream().map(this::summarizeWholeDocument).toList();
    }

    private List<RetrievedAttachmentChunk> chunksFor(UUID attachmentId) {
        Set<String> seenContent = new HashSet<>();
        return java.util.Optional.ofNullable(chunkSource.apply(attachmentId)).orElseGet(List::of).stream()
                .sorted(Comparator.comparingInt(RetrievedAttachmentChunk::chunkIndex))
                .filter(chunk -> chunk.content() != null
                        && !chunk.content().isBlank()
                        && seenContent.add(chunk.content().trim()))
                .toList();
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 4.0));
    }

    private Set<String> terms(String question) {
        if (question == null || question.isBlank()) return Set.of();
        Set<String> terms = new HashSet<>();
        for (String term : question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+")) {
            if (term.length() > 1) terms.add(term);
        }
        return terms;
    }

    private int relevance(RetrievedAttachmentChunk chunk, Set<String> terms) {
        if (terms.isEmpty() || chunk.content() == null) return 0;
        String content = chunk.content().toLowerCase(Locale.ROOT);
        return (int) terms.stream().filter(content::contains).count();
    }

    private String clipToTokens(String content, int maxTokens) {
        int maxCharacters = Math.max(1, maxTokens * 4);
        if (content == null || content.length() <= maxCharacters) return content == null ? "" : content;
        return content.substring(0, maxCharacters);
    }
}
