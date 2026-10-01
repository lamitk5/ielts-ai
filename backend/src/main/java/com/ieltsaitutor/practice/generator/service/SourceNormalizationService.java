package com.ieltsaitutor.practice.generator.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ieltsaitutor.practice.generator.exception.SourceNormalizationException;
import com.ieltsaitutor.rag.domain.Skill;

@Service
public class SourceNormalizationService {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("[ \\t]+");
    private static final Pattern MULTIPLE_NEWLINES_PATTERN = Pattern.compile("(\\r?\\n){3,}");

    public String normalizeText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new SourceNormalizationException("Source content cannot be empty");
        }
        String cleaned = SCRIPT_TAG_PATTERN.matcher(rawText).replaceAll("");
        cleaned = HTML_TAG_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = CONTROL_CHAR_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = MULTIPLE_SPACES_PATTERN.matcher(cleaned).replaceAll(" ");
        cleaned = MULTIPLE_NEWLINES_PATTERN.matcher(cleaned).replaceAll("\n\n");
        return cleaned.trim();
    }

    public int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    public void validateWordCount(String text, Skill skill) {
        int words = countWords(text);
        if (skill == null || skill == Skill.READING) {
            if (words < 400 || words > 1200) {
                throw new SourceNormalizationException(
                        "Reading source text length must be between 400 and 1200 words, but was: " + words);
            }
        } else if (skill == Skill.WRITING) {
            if (words < 30 || words > 1000) {
                throw new SourceNormalizationException(
                        "Writing source text length must be between 30 and 1000 words, but was: " + words);
            }
        } else if (skill == Skill.LISTENING) {
            if (words < 250 || words > 1000) {
                throw new SourceNormalizationException(
                        "Listening source text length must be between 250 and 1000 words, but was: " + words);
            }
        } else if (skill == Skill.SPEAKING) {
            if (words < 20 || words > 800) {
                throw new SourceNormalizationException(
                        "Speaking source text length must be between 20 and 800 words, but was: " + words);
            }
        }
    }

    public String computeChecksum(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
