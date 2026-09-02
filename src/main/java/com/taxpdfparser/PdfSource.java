package com.taxpdfparser;

import java.io.IOException;

/** The Handoff PDF boundary — see CONTEXT.md. */
public interface PdfSource {
    byte[] fetch(String bucket, String key) throws IOException;

    void delete(String bucket, String key);
}
