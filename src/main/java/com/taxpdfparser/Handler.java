package com.taxpdfparser;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entry point for the Parser (see CONTEXT.md). Fetches the Handoff PDF, parses it with {@link
 * PdfParser}, and deletes it — on success only; a thrown exception here is a Parse failure and
 * propagates to the caller, which falls back to its own pre-existing extraction path.
 */
public class Handler implements RequestHandler<ParseRequest, ParseResponse> {

    private final PdfSource pdfSource;

    public Handler() {
        this(new S3PdfSource());
    }

    public Handler(PdfSource pdfSource) {
        this.pdfSource = pdfSource;
    }

    @Override
    public ParseResponse handleRequest(ParseRequest request, Context context) {
        try {
            byte[] pdfBytes = pdfSource.fetch(request.getBucket(), request.getKey());
            Path tempPdf = Files.createTempFile("handoff-", ".pdf");
            try {
                Files.write(tempPdf, pdfBytes);
                String markdown = PdfParser.parseToMarkdown(tempPdf);
                pdfSource.delete(request.getBucket(), request.getKey());
                return new ParseResponse(markdown);
            } finally {
                Files.deleteIfExists(tempPdf);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
