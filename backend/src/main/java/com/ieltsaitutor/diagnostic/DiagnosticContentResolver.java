package com.ieltsaitutor.diagnostic;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ieltsaitutor.learning.intelligence.Skill;

/**
 * Builds the fixed diagnostic content snapshot from the approved Phase 4
 * catalog. A skill without approved content becomes an explicitly unavailable
 * section instead of borrowing content from another skill.
 */
@Component
public class DiagnosticContentResolver {
    private final DiagnosticContentSource source;
    private final String definitionVersion;

    @Autowired
    public DiagnosticContentResolver(DiagnosticContentSource source) {
        this(source, "diagnostic-v1");
    }

    public DiagnosticContentResolver(DiagnosticContentSource source, String definitionVersion) {
        this.source = source;
        this.definitionVersion = definitionVersion;
    }

    public String definitionVersion() {
        return definitionVersion;
    }

    public List<DiagnosticSectionPin> resolveSnapshot() {
        return List.of(Skill.READING, Skill.LISTENING, Skill.WRITING, Skill.SPEAKING).stream()
                .map(this::resolve)
                .toList();
    }

    public DiagnosticSectionPin resolve(Skill skill) {
        return source.newestApproved(skill)
                .map(content -> DiagnosticSectionPin.available(skill, content))
                .orElseGet(() -> DiagnosticSectionPin.unavailable(skill,
                        "Không có nội dung đã duyệt cho kỹ năng này."));
    }
}
