package com.ieltsaitutor.learning.notebook;

public record ErrorNotebookQuery(String skill, String status, int page, int size) {
    public ErrorNotebookQuery {
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("page/size out of bounds");
    }
}
