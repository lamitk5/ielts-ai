package com.ieltsaitutor.rag.cli;

import java.util.List;

public record RagManifest(List<RagManifestEntry> sources) {
    public RagManifest { sources = sources == null ? List.of() : List.copyOf(sources); }
}
