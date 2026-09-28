package com.ieltsaitutor.ai.attachment;

public record PageRange(int firstPage, int lastPage) {
    public PageRange {
        if (firstPage < 1 || lastPage < firstPage) throw new IllegalArgumentException("Page range is invalid");
    }

    public int size() { return lastPage - firstPage + 1; }
}
