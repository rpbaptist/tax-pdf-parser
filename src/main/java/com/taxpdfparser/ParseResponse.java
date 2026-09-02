package com.taxpdfparser;

public class ParseResponse {

    private String markdown;

    public ParseResponse() {
    }

    public ParseResponse(String markdown) {
        this.markdown = markdown;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }
}
