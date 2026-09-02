package com.taxpdfparser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HandlerTest {

    private static final Path SAMPLE_STATEMENT =
        Path.of("src/test/resources/fixtures/sample-statement.pdf");

    @Test
    void parsesTheHandoffPdfAndDeletesItAfterSuccess() throws IOException {
        byte[] pdfBytes = Files.readAllBytes(SAMPLE_STATEMENT);
        List<String> deleted = new ArrayList<>();
        PdfSource fakeSource = new PdfSource() {
            @Override
            public byte[] fetch(String bucket, String key) {
                assertThat(bucket).isEqualTo("test-bucket");
                assertThat(key).isEqualTo("test-key.pdf");
                return pdfBytes;
            }

            @Override
            public void delete(String bucket, String key) {
                deleted.add(bucket + "/" + key);
            }
        };
        Handler handler = new Handler(fakeSource);

        ParseRequest request = new ParseRequest();
        request.setBucket("test-bucket");
        request.setKey("test-key.pdf");

        ParseResponse response = handler.handleRequest(request, null);

        assertThat(response.getMarkdown()).contains("Renteopbrengst spaarrekening");
        assertThat(deleted).containsExactly("test-bucket/test-key.pdf");
    }
}
