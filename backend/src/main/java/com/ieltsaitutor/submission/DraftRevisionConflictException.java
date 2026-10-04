package com.ieltsaitutor.submission;

public class DraftRevisionConflictException extends SubmissionConflictException {
    private final SubmissionDraftSnapshot latestSnapshot;

    public DraftRevisionConflictException(SubmissionDraftSnapshot latestSnapshot) {
        super("Bản nháp đã được cập nhật từ thiết bị khác.");
        this.latestSnapshot = latestSnapshot;
    }

    public SubmissionDraftSnapshot latestSnapshot() {
        return latestSnapshot;
    }
}
