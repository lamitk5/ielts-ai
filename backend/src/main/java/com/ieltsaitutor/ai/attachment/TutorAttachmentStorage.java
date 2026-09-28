package com.ieltsaitutor.ai.attachment;

import java.io.IOException;
import java.io.InputStream;

public interface TutorAttachmentStorage {
    void store(InputStream input, String storageKey, long sizeBytes) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    boolean exists(String storageKey);
}
