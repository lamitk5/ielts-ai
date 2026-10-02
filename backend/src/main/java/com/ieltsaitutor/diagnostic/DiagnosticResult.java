package com.ieltsaitutor.diagnostic;

import java.util.List;
import java.util.UUID;

public record DiagnosticResult(UUID sessionId, String state, String title, List<DiagnosticSectionResult> sections,
        boolean sufficientEvidence, String disclaimer) {
    public DiagnosticResult {
        sections = sections == null ? List.of() : List.copyOf(sections);
        title = title == null ? "Estimated starting profile" : title;
        disclaimer = disclaimer == null ? "Đây là hồ sơ khởi điểm ước lượng, không phải kết quả hay xếp lớp IELTS chính thức." : disclaimer;
    }
}
