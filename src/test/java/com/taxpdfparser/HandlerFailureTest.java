package com.taxpdfparser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandlerFailureTest {

    @Test
    void aMalformedPdfPropagatesAnErrorInsteadOfReturningEmptyOutput() {
        List<String> deleted = new ArrayList<>();
        PdfSource brokenSource = new PdfSource() {
            @Override
            public byte[] fetch(String bucket, String key) {
                return "this is not a pdf".getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public void delete(String bucket, String key) {
                deleted.add(bucket + "/" + key);
            }
        };
        Handler handler = new Handler(brokenSource);

        ParseRequest request = new ParseRequest();
        request.setBucket("test-bucket");
        request.setKey("garbage.pdf");

        assertThatThrownBy(() -> handler.handleRequest(request, null)).isNotNull();
        // A failed parse must not delete the Handoff PDF — nothing to clean up,
        // and the S3 lifecycle rule remains the only backstop for this object.
        assertThat(deleted).isEmpty();
    }
}
