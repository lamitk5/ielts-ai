package com.ieltsaitutor.rag.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class DocumentChunker {
    private static final Pattern PARAGRAPH_BOUNDARY = Pattern.compile("\\R\\s*\\R+");

    public List<DocumentChunk> chunk(ExtractedDocument document, ChunkingOptions options) {
        if (document == null || options == null) {
            throw new IllegalArgumentException("Document and chunking options are required");
        }
        List<SectionWork> sections = normalizeSections(document);
        if (sections.isEmpty() && document.text() != null && !document.text().isBlank()) {
            sections = List.of(new SectionWork(null, document.text().trim(), null, document.metadata()));
        }
        List<ChunkDraft> drafts = new ArrayList<>();
        SectionWork previous = null;
        for (SectionWork section : sections) {
            if (previous != null && sameSection(previous, section)
                    && estimateTokens(previous.content()) < options.mergeThresholdTokens()) {
                ChunkDraft last = drafts.remove(drafts.size() - 1);
                drafts.add(new ChunkDraft(last.content() + "\n\n" + section.content(), last.pageNumber(),
                        last.sectionTitle(), mergeMetadata(last.metadata(), section.metadata())));
                previous = new SectionWork(section.title(), last.content(), section.pageNumber(),
                        mergeMetadata(last.metadata(), section.metadata()));
                continue;
            }
            drafts.addAll(splitSection(section, options));
            previous = section;
        }
        List<DocumentChunk> result = new ArrayList<>();
        for (ChunkDraft draft : drafts) {
            if (draft.content().isBlank()) {
                continue;
            }
            result.add(new DocumentChunk(result.size(), draft.content(), draft.pageNumber(), draft.sectionTitle(),
                    estimateTokens(draft.content()), draft.metadata()));
        }
        return List.copyOf(result);
    }

    private List<SectionWork> normalizeSections(ExtractedDocument document) {
        List<SectionWork> result = new ArrayList<>();
        if (document.sections() != null) {
            for (ExtractedSection section : document.sections()) {
                if (section != null && section.content() != null && !section.content().isBlank()) {
                    result.add(new SectionWork(section.title(), section.content().trim(), section.pageNumber(),
                            section.metadata()));
                }
            }
        }
        return result;
    }

    private List<ChunkDraft> splitSection(SectionWork section, ChunkingOptions options) {
        List<String> fragments = new ArrayList<>();
        for (String paragraph : PARAGRAPH_BOUNDARY.split(section.content())) {
            if (!paragraph.isBlank()) {
                fragments.addAll(splitFragment(paragraph.trim(), options.targetTokens()));
            }
        }
        List<ChunkDraft> result = new ArrayList<>();
        String current = "";
        for (String fragment : fragments) {
            String candidate = current.isBlank() ? fragment : current + "\n\n" + fragment;
            if (!current.isBlank() && estimateTokens(candidate) > options.targetTokens()) {
                result.add(new ChunkDraft(current, section.pageNumber(), section.title(), section.metadata()));
                current = overlapTail(current, options.overlapTokens()) + fragment;
            } else {
                current = candidate;
            }
        }
        if (!current.isBlank()) {
            result.add(new ChunkDraft(current, section.pageNumber(), section.title(), section.metadata()));
        }
        return result;
    }

    private List<String> splitFragment(String text, int targetTokens) {
        if (estimateTokens(text) <= targetTokens) {
            return List.of(text);
        }
        List<String> sentences = new ArrayList<>();
        for (String sentence : text.split("(?<=[.!?])\\s+")) {
            if (!sentence.isBlank()) {
                sentences.add(sentence.trim());
            }
        }
        if (sentences.size() == 1) {
            return splitWords(sentences.get(0), targetTokens);
        }
        List<String> result = new ArrayList<>();
        String current = "";
        for (String sentence : sentences) {
            if (estimateTokens(sentence) > targetTokens) {
                if (!current.isBlank()) {
                    result.add(current);
                    current = "";
                }
                result.addAll(splitWords(sentence, targetTokens));
            } else if (current.isBlank() || estimateTokens(current + " " + sentence) <= targetTokens) {
                current = current.isBlank() ? sentence : current + " " + sentence;
            } else {
                result.add(current);
                current = sentence;
            }
        }
        if (!current.isBlank()) {
            result.add(current);
        }
        return result;
    }

    private List<String> splitWords(String text, int targetTokens) {
        String[] words = text.split("\\s+");
        int maxWords = Math.max(1, targetTokens * 4);
        List<String> result = new ArrayList<>();
        for (int start = 0; start < words.length; start += maxWords) {
            int end = Math.min(words.length, start + maxWords);
            result.add(String.join(" ", java.util.Arrays.copyOfRange(words, start, end)));
        }
        return result;
    }

    private String overlapTail(String text, int overlapTokens) {
        if (overlapTokens == 0) {
            return "";
        }
        String[] words = text.split("\\s+");
        int count = Math.min(words.length, Math.max(1, overlapTokens * 4));
        return String.join(" ", java.util.Arrays.copyOfRange(words, words.length - count, words.length)) + " ";
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil(text.codePointCount(0, text.length()) / 4.0));
    }

    private boolean sameSection(SectionWork left, SectionWork right) {
        return java.util.Objects.equals(left.title(), right.title())
                && java.util.Objects.equals(left.pageNumber(), right.pageNumber());
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> left, Map<String, Object> right) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (left != null) merged.putAll(left);
        if (right != null) merged.putAll(right);
        return merged;
    }

    private record SectionWork(String title, String content, Integer pageNumber, Map<String, Object> metadata) {}
    private record ChunkDraft(String content, Integer pageNumber, String sectionTitle, Map<String, Object> metadata) {}
}
