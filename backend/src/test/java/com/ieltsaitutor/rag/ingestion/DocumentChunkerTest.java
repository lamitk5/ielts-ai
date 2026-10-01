package com.ieltsaitutor.rag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DocumentChunkerTest {
    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void preservesDocumentOrder() {
        ExtractedDocument document = new ExtractedDocument("", List.of(
                new ExtractedSection("First", "alpha", 1, Map.of()),
                new ExtractedSection("Second", "beta", 2, Map.of())), Map.of());

        List<DocumentChunk> chunks = chunker.chunk(document, ChunkingOptions.defaults());

        assertThat(chunks).extracting(DocumentChunk::content).containsExactly("alpha", "beta");
        assertThat(chunks).extracting(DocumentChunk::chunkIndex).containsExactly(0, 1);
    }

    @Test
    void preservesPageMetadata() {
        ExtractedDocument document = new ExtractedDocument("", List.of(
                new ExtractedSection("Reading", "passage", 7, Map.of("source", "fixture"))), Map.of());

        DocumentChunk chunk = chunker.chunk(document, ChunkingOptions.defaults()).get(0);

        assertThat(chunk.pageNumber()).isEqualTo(7);
        assertThat(chunk.metadata()).containsEntry("source", "fixture");
    }

    @Test
    void preservesSectionHeading() {
        ExtractedDocument document = new ExtractedDocument("", List.of(
                new ExtractedSection("Task 2", "Write an essay.", 3, Map.of())), Map.of());

        assertThat(chunker.chunk(document, ChunkingOptions.defaults()).get(0).sectionTitle()).isEqualTo("Task 2");
    }

    @Test
    void appliesConfiguredOverlap() {
        String content = String.join(" ", java.util.stream.IntStream.range(0, 80)
                .mapToObj(i -> "word" + i).toList());
        List<DocumentChunk> chunks = chunker.chunk(new ExtractedDocument("",
                List.of(new ExtractedSection("Long", content, 1, Map.of())), Map.of()),
                new ChunkingOptions(10, 2, 3));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(1).content()).contains("word");
        assertThat(chunks.get(1).content()).contains("word32", "word39");
    }

    @Test
    void mergesShortSectionFragments() {
        ExtractedDocument document = new ExtractedDocument("", List.of(
                new ExtractedSection("Notes", "First paragraph.", 1, Map.of()),
                new ExtractedSection("Notes", "Second paragraph.", 1, Map.of())), Map.of());

        List<DocumentChunk> chunks = chunker.chunk(document, new ChunkingOptions(100, 5, 120));

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).contains("First paragraph.", "Second paragraph.");
    }

    @Test
    void skipsEmptyContent() {
        ExtractedDocument document = new ExtractedDocument("", List.of(
                new ExtractedSection("Empty", "  ", 1, Map.of()),
                new ExtractedSection("Filled", "usable", 2, Map.of())), Map.of());

        assertThat(chunker.chunk(document, ChunkingOptions.defaults())).extracting(DocumentChunk::content)
                .containsExactly("usable");
    }

    @Test
    void keepsSmallDocumentAsOneChunk() {
        ExtractedDocument document = new ExtractedDocument("small", List.of(), Map.of());

        List<DocumentChunk> first = chunker.chunk(document, ChunkingOptions.defaults());
        List<DocumentChunk> second = chunker.chunk(document, ChunkingOptions.defaults());

        assertThat(first).hasSize(1);
        assertThat(first).containsExactlyElementsOf(second);
        assertThat(first.get(0).content()).isEqualTo("small");
    }

    @Test
    void validatesOptionsAndProducesDeterministicOutput() {
        assertThatThrownBy(() -> new ChunkingOptions(0, 1, 1)).isInstanceOf(IllegalArgumentException.class);

        ExtractedDocument document = new ExtractedDocument("", List.of(
                new ExtractedSection("Heading", "A stable fixture.", 1, Map.of())), Map.of());
        assertThat(chunker.chunk(document, ChunkingOptions.defaults()))
                .containsExactlyElementsOf(chunker.chunk(document, ChunkingOptions.defaults()));
    }
}
