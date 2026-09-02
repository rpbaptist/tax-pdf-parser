package com.taxpdfparser;

import org.opendataloader.pdf.api.Config;
import org.opendataloader.pdf.api.OpenDataLoaderPDF;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Turns a PDF into markdown using OpenDataLoader's fast (deterministic,
 * local) mode. Hybrid mode is intentionally never enabled — see
 * CONTEXT.md's "Hybrid mode" entry.
 */
public final class PdfParser {

    private PdfParser() {
    }

    public static String parseToMarkdown(Path pdfPath) throws IOException {
        Config config = new Config();
        config.setGenerateMarkdown(true);
        config.setGenerateJSON(false);
        config.setHybrid(Config.HYBRID_OFF);
        config.setOutputStdout(true);
        config.normalize();

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            OpenDataLoaderPDF.processFile(pdfPath.toString(), config);
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}
