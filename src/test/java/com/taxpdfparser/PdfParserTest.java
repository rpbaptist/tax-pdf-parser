package com.taxpdfparser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PdfParserTest {

    private static final Path SAMPLE_STATEMENT =
            Path.of("src/test/resources/fixtures/sample-statement.pdf");

    @Test
    void parsesTableRowsIntoMarkdown() throws IOException {
        String markdown = PdfParser.parseToMarkdown(SAMPLE_STATEMENT);

        assertThat(markdown)
                .contains("Renteopbrengst spaarrekening")
                .contains("124,37")
                .contains("Dividendbelasting ingehouden");
    }
}
